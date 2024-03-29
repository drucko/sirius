/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import com.google.common.collect.Maps;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.util.ResourceLeakDetector;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Context;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Average;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.timer.EveryTenSeconds;
import sirius.web.health.MetricProvider;
import sirius.web.health.MetricsCollector;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

/**
 * Responsible for setting up and starting netty as HTTP server.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Register
public class WebServer implements Lifecycle, MetricProvider {

    /**
     * Used to log all web / http relevant messages
     */
    public static final Log LOG = Log.get("web");

    /**
     * Config value of the HTTP port used (<tt>http.port</tt>). The default port for HTTP is 80, but the default value
     * here is 9000 because non root users cannot open ports less than 1024
     */
    @ConfigValue("http.port")
    private static int port;

    /**
     * Returns the port used by the web server
     *
     * @return the port served by the HTTP server
     */
    public static int getPort() {
        return port;
    }

    /**
     * Config value of the HTTP bind address used (<tt>http.bindAddress</tt>). If the value is empty, we bind all
     * addresses. Otherwise this can be used to bind against a single IP address in order to setup multiple servers
     * on the same port.
     */
    @ConfigValue("http.bindAddress")
    private String bindAddress;

    /**
     * Config value of the max size for uploads which are kept entirely in memory (<tt>"http.uploadDiskThreshold</tt>).
     */
    @ConfigValue("http.uploadDiskThreshold")
    private long uploadDiskThreshold;

    /**
     * Config value of the min free space on disk (<tt>"http.minUploadFreespace</tt>). If the free space on disk drops
     * below this value, we cancel an upload. This provides large uploads which never crash a system by jamming a
     * disk.
     */
    @ConfigValue("http.minUploadFreespace")
    private static long minUploadFreespace;

    /**
     * Config value of the max upload size (<tt>http.maxUploadSize</tt>). Can be used to specify an upper limit for
     * file uploads.
     */
    @ConfigValue("http.maxUploadSize")
    private static long maxUploadSize;

    /**
     * Contains a list of IP ranges which are permitted to access this server. Access from unauthorized IPs will be
     * blocked at the lowest level possible (probably no connection will be accepted). The format accepted by this
     * field is defined by {@link IPRange#paraseRangeSet(String)}.
     *
     * @see IPRange#paraseRangeSet(String)
     */
    @ConfigValue("http.firewall.filterIPs")
    private static String ipFilter;
    private static IPRange.RangeSet filterRanges;
    private Channel channel;

    /**
     * Returns an ip filter which determines which IPs may connect to the web server.
     *
     * @return a range set describing all accepted ranges
     */
    protected static IPRange.RangeSet getIPFilter() {
        if (filterRanges == null) {
            try {
                filterRanges = IPRange.paraseRangeSet(ipFilter);
            } catch (Throwable e) {
                Exceptions.handle()
                          .to(LOG)
                          .error(e)
                          .withSystemErrorMessage(
                                  "Error parsing config value: 'http.firewall.filterIPs': %s (%s). Defaulting to localhost!")
                          .handle();
                filterRanges = IPRange.LOCALHOST;
            }
        }

        return filterRanges;
    }

    /**
     * Contains a list of IP ranges which are trusted. This rule will not change the processing of requests, it will
     * only set the return value of {@link WebContext#isTrusted()}. The format accepted by this field is defined by
     * {@link IPRange#paraseRangeSet(String)}.
     *
     * @see IPRange#paraseRangeSet(String)
     */
    @ConfigValue("http.firewall.trustedIPs")
    private static String trustedIPs;
    private static IPRange.RangeSet trustedRanges;

    /**
     * Returns all trusted IPs as {@link IPRange.RangeSet}
     *
     * @return a range set describing all trusted ranges
     */
    protected static IPRange.RangeSet getTrustedRanges() {
        if (trustedRanges == null) {
            try {
                trustedRanges = IPRange.paraseRangeSet(trustedIPs);
                // If no trust range is given, we trust nobody but good old LOCALHOST....This seems to be a better
                // alternative than trusting everybody
                if (trustedRanges.isEmpty()) {
                    trustedRanges = IPRange.LOCALHOST;
                }
            } catch (Throwable e) {
                Exceptions.handle()
                          .to(LOG)
                          .error(e)
                          .withSystemErrorMessage("Error parsing config value: 'http.firewall.trustedIPs': %s (%s)")
                          .handle();
                trustedRanges = IPRange.LOCALHOST;
            }
        }

        return trustedRanges;
    }

    @ConfigValue("http.firewall.proxyIPs")
    private static String proxyIPs;
    private static IPRange.RangeSet proxyRanges;

    /**
     * Returns all proxy IPs as {@link IPRange.RangeSet}
     *
     * @return a range set describing all proxy ranges. This is probably just a single IP addressed, however, using
     *         {@link IPRange.RangeSet} permit to name several, even sub nets. If a request comes from a proxy IP
     *         its X-Forwarded-For header is checked for the original IP.
     */
    protected static IPRange.RangeSet getProxyIPs() {
        if (proxyRanges == null) {
            try {
                proxyRanges = IPRange.paraseRangeSet(proxyIPs);
            } catch (Throwable e) {
                Exceptions.handle()
                          .to(LOG)
                          .error(e)
                          .withSystemErrorMessage("Error parsing config value: 'http.firewall.proxyIPs': %s (%s)")
                          .handle();
                proxyRanges = IPRange.NO_FILTER;
            }
        }

        return proxyRanges;
    }

    @Context
    private GlobalContext ctx;

    private static HttpDataFactory httpDataFactory;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bootstrap;

    /*
     * Statistics
     */
    protected static volatile long bytesIn = 0;
    protected static volatile long bytesOut = 0;
    protected static volatile long messagesIn = 0;
    protected static volatile long messagesOut = 0;
    protected static volatile long connections = 0;
    protected static volatile long blocks = 0;
    protected static volatile long requests = 0;
    protected static volatile long chunks = 0;
    protected static volatile long keepalives = 0;
    protected static volatile long idleTimeouts = 0;
    protected static volatile long clientErrors = 0;
    protected static volatile long serverErrors = 0;
    protected static Map<WebServerHandler, WebServerHandler> openConnections = Maps.newConcurrentMap();
    protected static Average responseTime = new Average();

    /**
     * Returns the minimal value of free disk space accepted until an upload is aborted.
     *
     * @return the minimal allowed free disk space in bytes
     */
    protected static long getMinUploadFreespace() {
        return minUploadFreespace;
    }

    /**
     * Returns the maximal upload size in bytes
     *
     * @return the max size of uploaded files
     */
    protected static long getMaxUploadSize() {
        return maxUploadSize;
    }

    /**
     * Returns the data factory used to handle file uploads and posts.
     *
     * @return the data factory used for processing POST and PUT requests.
     */
    protected static HttpDataFactory getHttpDataFactory() {
        return httpDataFactory;
    }

    @Override
    public void started() {
        if (port <= 0) {
            LOG.INFO("web server is disabled (http.port is <= 0)");
            return;
        }
        LOG.INFO("Initializing netty at port %d", port);
        if (Strings.isFilled(bindAddress)) {
            LOG.INFO("Binding netty to %s", bindAddress);
        }

        if (Sirius.isDev()) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
            LOG.INFO("Enabling PARANOID resource leak detection...");
        }

        // Setup disk handling.
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
        httpDataFactory = new DefaultHttpDataFactory(uploadDiskThreshold);

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                 .channel(NioServerSocketChannel.class)
                .childHandler(ctx.wire(new WebServerInitializer()))
                        // At mose have 128 connections waiting to be "connected" - drop everything else...
                .option(ChannelOption.SO_BACKLOG, 128)
                        // Send a KEEPALIVE packet every 2h and expect and ACK on the TCP layer
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                        // Tell the kernel not to buffer our data - we're quite aware of what we're doing and
                        // will not create "mini writes" anyway
                .childOption(ChannelOption.TCP_NODELAY, true);

        // Bind and start to accept incoming connections.
        try {
            if (Strings.isFilled(bindAddress)) {
                channel = bootstrap.bind(new InetSocketAddress(bindAddress, port)).sync().channel();
            } else {
                channel = bootstrap.bind(new InetSocketAddress(port)).sync().channel();
            }
        } catch (InterruptedException e) {
            Exceptions.handle(e);
        }
    }

    @Override
    public void stopped() {
        try {
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException e) {
            Exceptions.handle(e);
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public String getName() {
        return "web (netty HTTP Server)";
    }

    /**
     * Returns the total bytes received so far
     *
     * @return the total bytes received via the http port
     */
    public static long getBytesIn() {
        return bytesIn;
    }

    /**
     * Returns the total bytes sent so far
     *
     * @return the total bytes sent via the http port
     */
    public static long getBytesOut() {
        return bytesOut;
    }

    /**
     * Returns the total messages (packets) sent so far
     *
     * @return the total messages sent via the http port
     */
    public static long getMessagesIn() {
        return messagesIn;
    }

    /**
     * Returns the total messages (packets) received so far
     *
     * @return the total messages received via the http port
     */
    public static long getMessagesOut() {
        return messagesOut;
    }

    /**
     * Returns the total number of connections opened so far
     *
     * @return the total number of connections opened on the http port
     */
    public static long getConnections() {
        return connections;
    }

    /**
     * Returns the total number of connections blocked so far
     *
     * @return the total number of connections blocked via a firewall rule on the http port
     */
    public static long getBlockedConnections() {
        return blocks;
    }

    /**
     * Returns the number of currently open connection
     *
     * @return the number of open connections on the http port
     */
    public static long getNumberOfOpenConnections() {
        return openConnections.size();
    }

    /**
     * Returns the total number of HTTP requests received by the web server
     *
     * @return the total number of requests received
     */
    public static long getRequests() {
        return requests;
    }

    /**
     * Returns the total number of HTTP chunks received
     *
     * @return the total number of chunks received
     */
    public static long getChunks() {
        return chunks;
    }

    /**
     * Returns the number of keepalives supported
     *
     * @return the number of connections not closed in order to keep them alive.
     */
    public static long getKeepalives() {
        return keepalives;
    }

    /**
     * Returns the number of idle connections killed
     *
     * @return the number of connections closed because they were found to be idle.
     */
    public static long getIdleTimeouts() {
        return idleTimeouts;
    }

    /**
     * Returns the number of HTTP responses with an 4xx status code.
     *
     * @return the number of HTTP responses with an 4xx status code.
     */
    public static long getClientErrors() {
        return clientErrors;
    }

    /**
     * Returns the number of HTTP responses with an 5xx status code.
     *
     * @return the number of HTTP responses with an 5xx status code.
     */
    public static long getServerErrors() {
        return serverErrors;
    }

    /**
     * Returns the average response time of the last requests.
     *
     * @return the average response time of the last requests in milliseconds.
     */
    public static double getAvgResponseTime() {
        return responseTime.getAvg();
    }

    @Override
    public void gather(MetricsCollector collector) {
        collector.differentialMetric("http-bytes-in", "http-bytes-in", "HTTP Bytes-In", bytesIn / 1024d, "KB");
        collector.differentialMetric("http-bytes-out", "http-bytes-out", "HTTP Bytes-Out", bytesOut / 1024d, "KB");
        collector.differentialMetric("http-connects", "http-connects", "HTTP Connects", connections, null);
        collector.differentialMetric("http-requests", "http-requests", "HTTP Requests", requests, null);
        collector.differentialMetric("http-blocks", "http-blocks", "HTTP Blocked Requests", blocks, null);
        collector.differentialMetric("http-timeouts", "http-timeouts", "HTTP Idle Timeouts", idleTimeouts, null);
        collector.differentialMetric("http-client-errors",
                                     "http-errors",
                                     "HTTP Client Errors (4xx)",
                                     clientErrors,
                                     null);
        collector.differentialMetric("http-server-errors",
                                     "http-errors",
                                     "HTTP Server Errors (5xx)",
                                     serverErrors,
                                     null);
        collector.metric("http-open-connections", "HTTP Open Connections", openConnections.size(), null);
        collector.metric("http-response-time", "HTTP Avg. Reponse Time", responseTime.getAvg(), "ms");
        collector.metric("http-sessions", "HTTP Sessions", ServerSession.getSessions().size(), null);
    }

    @Register
    public static class BandwidthUpdater implements EveryTenSeconds {

        @Override
        public void runTimer() throws Exception {
            for (WebServerHandler handler : openConnections.values()) {
                handler.updateBandwidth();
            }
        }
    }


    public static void addOpenConnection(WebServerHandler webServerHandler) {
        openConnections.put(webServerHandler, webServerHandler);
    }

    public static void removeOpenConnection(WebServerHandler webServerHandler) {
        openConnections.remove(webServerHandler);
    }

    public static Collection<? extends ActiveHTTPConnection> getOpenConnections() {
        return (Collection<? extends ActiveHTTPConnection>) openConnections.values();
    }
}
