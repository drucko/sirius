/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import sirius.kernel.commons.Callback;
import sirius.kernel.commons.Tuple;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Counter;
import sirius.kernel.health.Exceptions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

/**
 * Implementation of <tt>Cache</tt> used by the <tt>CacheManager</tt>
 *
 * @param <K> the type of the keys used by this cache
 * @param <V> the type of the values supported by this cache
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
class ManagedCache<K, V> implements Cache<K, V>, RemovalListener<Object, Object> {

    protected static final int MAX_HISTORY = 25;
    protected List<Long> usesHistory = new ArrayList<Long>(MAX_HISTORY);
    protected List<Long> hitRateHistory = new ArrayList<Long>(MAX_HISTORY);

    protected int maxSize;
    protected ValueComputer<K, V> computer;
    protected com.google.common.cache.Cache<K, CacheEntry<K, V>> data;
    protected Counter hits = new Counter();
    protected Counter misses = new Counter();
    protected Date lastEvictionRun = null;
    protected final String name;
    protected long timeToLive;
    protected final ValueVerifier<V> verifier;
    protected long verificationInterval;
    protected Callback<Tuple<K, V>> removeListener;

    private static final String EXTENSION_TYPE_CACHE = "cache";
    private static final String CONFIG_KEY_MAX_SIZE = "maxSize";
    private static final String CONFIG_KEY_TTL = "ttl";
    private static final String CONFIG_KEY_VERIFICATION = "verification";

    /**
     * Creates a new cache. This is not intended to be called outside of <tt>CacheManager</tt>.
     *
     * @param name          name of the cache which is also used to fetch the config settings
     * @param valueComputer used to compute absent cache values for given keys. May be null.
     * @param verifier      used to verify cached values before they are delivered to the caller.
     */
    protected ManagedCache(String name,
                           @Nullable ValueComputer<K, V> valueComputer,
                           @Nullable ValueVerifier<V> verifier) {
        this.name = name;
        this.computer = valueComputer;
        this.verifier = verifier;
    }

    /*
     * Initializes the cache on first use. This is necessary since most of  the caches will be created by the
     * static initializes when their classes are created. At this point, the config has not been loaded yet.
     */
    protected void init() {
        if (data != null) {
            return;
        }

        Extension cacheInfo = Extensions.getExtension(EXTENSION_TYPE_CACHE, name);
        if (cacheInfo == null) {
            CacheManager.LOG.WARN("Cache %s does not exist! Using defaults...", name);
            cacheInfo = Extensions.getExtension(EXTENSION_TYPE_CACHE, Extensions.DEFAULT);
        }
        this.verificationInterval = cacheInfo.getMilliseconds(CONFIG_KEY_VERIFICATION);
        this.timeToLive = cacheInfo.getMilliseconds(CONFIG_KEY_TTL);
        this.maxSize = cacheInfo.get(CONFIG_KEY_MAX_SIZE).asInt(100);
        if (maxSize > 0) {
            this.data = CacheBuilder.newBuilder().maximumSize(maxSize).removalListener(this).build();
        } else {
            this.data = CacheBuilder.newBuilder().removalListener(this).build();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public int getSize() {
        if (data == null) {
            return 0;
        }
        return (int) data.size();
    }

    @Override
    public long getUses() {
        return hits.getCount() + misses.getCount();
    }

    @Override
    public Long getHitRate() {
        long h = hits.getCount();
        long m = misses.getCount();
        return h + m == 0L ? 0L : Math.round(100d * (double) h / (double) (h + m));
    }

    @Override
    public Date getLastEvictionRun() {
        return lastEvictionRun;
    }

    @Override
    public void runEviction() {
        if (data == null) {
            return;
        }
        usesHistory.add(getUses());
        if (usesHistory.size() > MAX_HISTORY) {
            usesHistory.remove(0);
        }
        hitRateHistory.add(getHitRate());
        if (hitRateHistory.size() > MAX_HISTORY) {
            hitRateHistory.remove(0);
        }
        hits.reset();
        misses.reset();
        lastEvictionRun = new Date();
        if (timeToLive <= 0) {
            return;
        }
        // Remove all outdated entries...
        long now = System.currentTimeMillis();
        int numEvicted = 0;
        Iterator<Entry<K, CacheEntry<K, V>>> iter = data.asMap().entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K, CacheEntry<K, V>> next = iter.next();
            if (next.getValue().getMaxAge() > now) {
                iter.remove();
                numEvicted++;
            }
        }
        if (numEvicted > 0 && CacheManager.LOG.isFINE()) {
            CacheManager.LOG.FINE("Evicted %d entries from %s", numEvicted, name);
        }
    }

    @Override
    public void clear() {
        if (data == null) {
            return;
        }
        data.asMap().clear();
        misses.reset();
        hits.reset();
        lastEvictionRun = new Date();
    }

    @Override
    public V get(K key) {
        return get(key, this.computer);
    }

    @Override
    public boolean contains(K key) {
        if (data == null) {
            return false;
        }
        return data.asMap().containsKey(key);
    }

    @Override
    public V get(final K key, final ValueComputer<K, V> computer) {
        try {
            if (data == null) {
                init();
            }
            long now = System.currentTimeMillis();
            CacheEntry<K, V> entry = null;
            if (computer != null) {
                entry = data.get(key, new Callable<CacheEntry<K, V>>() {
                    @Override
                    public CacheEntry<K, V> call() throws Exception {
                        misses.inc();
                        V value = computer.compute(key);
                        return new CacheEntry<K, V>(key,
                                                    value,
                                                    timeToLive > 0 ? timeToLive + System.currentTimeMillis() : 0,
                                                    verificationInterval + System.currentTimeMillis());
                    }
                });
            } else {
                entry = data.getIfPresent(key);
            }
            if (entry != null && entry.getMaxAge() > 0 && entry.getMaxAge() < now) {
                data.invalidate(key);
                entry = null;
                if (computer != null) {
                    V value = computer.compute(key);
                    entry = new CacheEntry<K, V>(key,
                                                value,
                                                timeToLive > 0 ? timeToLive + System.currentTimeMillis() : 0,
                                                verificationInterval + System.currentTimeMillis());
                }
            }
            if (verifier != null && entry != null && verificationInterval > 0 && entry.getNextVerification() < now) {
                if (!verifier.valid(entry.getValue())) {
                    entry = null;
                } else {
                    entry.setNextVerification(verificationInterval + now);
                }
            }
            if (entry != null) {
                hits.inc();
                entry.getHits().inc();
                return entry.getValue();
            } else {
                misses.inc();
                return null;
            }
        } catch (Throwable e) {
            throw Exceptions.handle(CacheManager.LOG, e);
        }
    }

    @Override
    public void put(K key, V value) {
        if (data == null) {
            init();
        }
        CacheEntry<K, V> cv = new CacheEntry<K, V>(key,
                                                   value,
                                                   timeToLive > 0 ? timeToLive + System.currentTimeMillis() : 0,
                                                   verificationInterval + System.currentTimeMillis());
        data.put(key, cv);
    }

    @Override
    public void remove(K key) {
        if (data == null) {
            return;
        }
        data.invalidate(key);
    }

    @Override
    public Iterator<K> keySet() {
        if (data == null) {
            init();
        }
        return data.asMap().keySet().iterator();
    }

    @Override
    public List<CacheEntry<K, V>> getContents() {
        if (data == null) {
            init();
        }
        return new ArrayList<CacheEntry<K, V>>(data.asMap().values());
    }

    @Override
    public List<Long> getUseHistory() {
        return usesHistory;
    }

    @Override
    public List<Long> getHitRateHistory() {
        return hitRateHistory;
    }

    @Override
    public Cache<K, V> onRemove(Callback<Tuple<K, V>> onRemoveCallback) {
        removeListener = onRemoveCallback;
        return this;
    }

    @Override
    public void onRemoval(RemovalNotification<Object, Object> notification) {
        if (removeListener != null) {
            try {
                CacheEntry<K, V> entry = (CacheEntry<K, V>) notification.getValue();
                removeListener.invoke(Tuple.create(entry.getKey(), entry.getValue()));
            } catch (Throwable e) {
                Exceptions.handle(e);
            }
        }
    }
}
