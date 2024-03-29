/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;

import org.hyperic.sigar.*;
import sirius.kernel.async.CallContext;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.health.MemoryBasedHealthMonitor;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
@Register
public class SystemMetricProvider implements MetricProvider {

    private List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
    private Sigar sigar = new Sigar();
    private boolean sigarEnabled = true;
    private static final Log LOG = Log.get("sigar");

    @Part
    private MemoryBasedHealthMonitor monitor;

    @Override
    public void gather(MetricsCollector collector) {
        for (GarbageCollectorMXBean gc : gcs) {
            collector.differentialMetric("sys-gc-" + gc.getName(),
                                         "sys-gc",
                                         "GC - " + gc.getName(),
                                         gc.getCollectionCount(),
                                         "/min");
        }
        collector.differentialMetric("sys-interactions",
                                     "sys-interactions",
                                     "Interactions",
                                     CallContext.getInteractionCounter().getCount(),
                                     "/min");
        collector.differentialMetric("sys-logs", "sys-logs", "Log Messages", monitor.getNumLogMessages(), "/min");
        collector.differentialMetric("sys-incidents", "sys-incidents", "Incidents", monitor.getNumIncidents(), "/min");
        collector.differentialMetric("sys-unique-incidents",
                                     "sys-unique-incidents",
                                     "Unique Incidents",
                                     monitor.getNumUniqueIncidents(),
                                     "/min");

        try {
            if (sigarEnabled) {
                gatherCPUandMem(collector);
                gatherNetworkStats(collector);
                gatherFS(collector);
            }
        } catch (SigarException e) {
            Exceptions.handle(LOG, e);
        } catch (UnsatisfiedLinkError e) {
            LOG.SEVERE("Sigar native library not found! Disabling sigar.");
            sigarEnabled = false;
        }
    }

    private void gatherNetworkStats(MetricsCollector collector) throws SigarException {
        long rxSum = 0;
        long txSum = 0;
        for (String eth : sigar.getNetInterfaceList()) {
            NetInterfaceStat stat = sigar.getNetInterfaceStat(eth);
            rxSum += stat.getRxBytes();
            txSum += stat.getTxBytes();
        }
        collector.differentialMetric("sys-eth-tx", "sys-eth-tx", "Network Bytes-Out", txSum / 1024d, "KB");
        collector.differentialMetric("sys-eth-rx", "sys-eth-rx", "Network Bytes-In", rxSum / 1024d, "KB");
    }

    private void gatherCPUandMem(MetricsCollector collector) throws SigarException {
        CpuPerc cpu = sigar.getCpuPerc();
        collector.metric("sys-cpu", "System CPU Usage", cpu.getCombined() * 100d, "%");
        Mem mem = sigar.getMem();
        mem.gather(sigar);
        collector.metric("sys-mem", "System Memory Usage", mem.getUsedPercent(), "%");
        ProcCpu proc = sigar.getProcCpu(sigar.getPid());
        collector.metric("jvm-cpu", "JVM CPU Usage", proc.getPercent(), "%");
        Runtime rt = Runtime.getRuntime();
        collector.metric("jvm-heap",
                         "JVM Heap Usage",
                         (double) (rt.totalMemory() - rt.freeMemory()) / rt.maxMemory() * 100d,
                         "%");
    }

    private void gatherFS(MetricsCollector collector) throws SigarException {
        for (FileSystem fs : sigar.getFileSystemList()) {
            FileSystemUsage fsu = sigar.getMountedFileSystemUsage(fs.getDirName());
            if (fsu.getFree() != 0 || fsu.getUsePercent() != 0d) {
                collector.metric("sys-fs", "FS: Usage of " + fs.getDirName(), fsu.getUsePercent() * 100d, "%");
            }
        }
    }

}
