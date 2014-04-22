package com.appdynamics.extensions.memcached;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.memcached.config.Configuration;
import com.appdynamics.extensions.memcached.config.Server;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An entry point into AppDynamics extensions.
 */
public class MemcachedMonitor extends AManagedMonitor{

    public static final String CONFIG_ARG = "config-file";
    public static final Logger logger = Logger.getLogger(MemcachedMonitor.class);
    public static final String COLON = ":";
    public static final String METRIC_SEPARATOR = "|";
    public static final int DEFAULT_MEMCACHED_PORT = 11211;
    public static final int TIMEOUT = 60000;
    public static final String LOG_PREFIX = "log-prefix";

    private static String logPrefix;

    public MemcachedMonitor(){
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        setLogPrefix(taskArgs.get(LOG_PREFIX));
        String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
        try {
            //read the config.
            Configuration config = readConfig(configFilename);
            //collect the metrics
            Map<String,Metrics> stats = collectMetrics(config);
            //print the metrics
            printStats(config,stats);
            return new TaskOutput(getLogPrefix() + "Memcached monitoring task completed successfully.");
        } catch (FileNotFoundException e) {
            logger.error(getLogPrefix() + "Config file not found :: " + configFilename,e);
        } catch (Exception e) {
            logger.error(getLogPrefix() + "Metrics collection failed",e);
        }
        throw new TaskExecutionException(getLogPrefix() + "Memcached monitoring task completed with failures.");
    }


    /**
     * Collects all the metrics by connecting to memcached servers through XmemcachedClient.
     * @param config
     * @return Map
     * @throws Exception
     */
    private Map<String, Metrics> collectMetrics(Configuration config) throws Exception {
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient(config);
            Map<InetSocketAddress, Map<String, String>> stats = memcachedClient.getStats(TIMEOUT);
            Map<String, String> lookup = createDisplayNameLookup(config);
            return translateMetrics(stats, lookup);
        }
        catch(Exception e){
            logger.error(getLogPrefix() + "Unable to collect memcached metrics ", e);
            throw e;
        }
        finally {
            memcachedClient.shutdown();
        }
    }


    /**
     * Creates a lookup dictionary from configuration.
     * @param config
     * @return Map
     */
    private Map<String,String> createDisplayNameLookup(Configuration config) {
        Map<String,String> lookup = new HashMap<String,String>();
        if(config != null && config.getServers() != null){
            for(Server server : config.getServers()) {
                String splits[] = server.getServer().split(COLON);
                String hostname = "";
                int port = DEFAULT_MEMCACHED_PORT;
                if(splits != null && splits.length > 1){
                    hostname = splits[0];
                    port = Integer.parseInt(splits[1]);
                }
                InetSocketAddress sockAddress = new InetSocketAddress(hostname,port);
                lookup.put(sockAddress.toString(),server.getDisplayName());
            }
        }
        return lookup;
    }


    /**
     * Translates the metrics returned from the XmemcachedClient to custom Map
     * @param stats
     * @param lookup
     * @return Map
     */
    private Map<String, Metrics> translateMetrics(Map<InetSocketAddress, Map<String, String>> stats,Map<String,String> lookup) {
        Map<String,Metrics> metricsMap = new HashMap<String,Metrics>();
        if(stats != null){
            Iterator<InetSocketAddress> it = stats.keySet().iterator();
            while(it.hasNext()){
                InetSocketAddress sockAddress = it.next();
                Map<String, String> statsForSockAddress = stats.get(sockAddress);
                String displayName = lookup.get(sockAddress.toString());
                if(displayName != null) {
                    metricsMap.put(displayName, new Metrics(statsForSockAddress));
                }
                else{
                    logger.error(getLogPrefix()+"Unable to lookup a client::"+sockAddress.toString());
                }
            }
        }
        return metricsMap;
    }


    /**
     * Builds a memcached client.
     * @param config
     * @return MemcachedClient
     * @throws IOException
     */
    private MemcachedClient getMemcachedClient(Configuration config) throws IOException {
        String aStringOfServers = getAllServersAsAString(config);
        MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(aStringOfServers));
        builder.setCommandFactory(new BinaryCommandFactory());
        try {
            return builder.build();
        } catch (IOException e) {
            logger.error(getLogPrefix()+"Cannot create Memcached Client for servers :: " + aStringOfServers , e);
            throw e;
        }
    }


    /**
     * Returns all the servers in the config as a string eg. "hostname:port hostname1:port1"
     * @param config
     * @return
     */
    private String getAllServersAsAString(Configuration config) {
        StringBuffer str = new StringBuffer();
        if(config != null && config.getServers() != null){
            for(Server server : config.getServers()) {
                str.append(server.getServer());
                str.append(" ");
            }
        }
        return str.toString();
    }

    /**
     * Returns a custom java object from a config YAML.
     * @param configFilename
     * @return Configuration
     * @throws FileNotFoundException
     */
    private Configuration readConfig(String configFilename) throws FileNotFoundException {
        logger.info(getLogPrefix()+"Reading config file::" + configFilename);
        Yaml yaml = new Yaml(new Constructor(Configuration.class));
        Configuration config = (Configuration) yaml.load(new FileInputStream(configFilename));
        return config;
    }


    /**
     * Returns a config file name,
     * @param filename
     * @return String
     */
    private String getConfigFilename(String filename) {
        //for absolute paths
        if(new File(filename).exists()){
            return filename;
        }
        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if(!Strings.isNullOrEmpty(filename)){
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }


    private void printStats(Configuration config,Map<String, Metrics> stats) {
        Iterator<String> it = stats.keySet().iterator();
        while(it.hasNext()){
            String serverName = it.next();
            Metrics serverMetrics = stats.get(serverName);
            String metricName = config.getMetricPrefix() + serverName;
            printServerMetrics(metricName, serverMetrics);
        }
    }


    private void printServerMetrics(String metricName, Metrics serverMetrics) {
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CURR_ITEMS,serverMetrics.getCurrentItems());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.TOTAL_ITEMS,serverMetrics.getTotalItems());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.BYTES,serverMetrics.getBytes());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CURR_CONNECTIONS,serverMetrics.getCurrentConnections());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.TOTAL_CONNECTIONS,serverMetrics.getTotalConnections());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CONNECTION_STRUCTURES,serverMetrics.getConnectionStructures());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.RESERVED_FDS,serverMetrics.getReservedFds());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CMD_GET,serverMetrics.getGetCommands());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CMD_SET,serverMetrics.getSetCommands());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CMD_FLUSH,serverMetrics.getFlushCommands());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CMD_TOUCH,serverMetrics.getTouchCommands());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.GET_HITS,serverMetrics.getGetHits());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.GET_MISSES,serverMetrics.getGetMisses());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.DELETE_MISSES,serverMetrics.getDeleteMisses());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.DELETE_HITS,serverMetrics.getDeleteHits());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.INCR_HITS,serverMetrics.getIncrHits());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.INCR_MISSES,serverMetrics.getIncrMisses());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.DECR_HITS,serverMetrics.getDecrHits());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.DECR_MISSES,serverMetrics.getDecrMisses());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CAS_HITS,serverMetrics.getCasHits());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CAS_MISSES,serverMetrics.getCasMisses());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CAS_BADVAL,serverMetrics.getCasBadValues());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.TOUCH_HITS,serverMetrics.getTouchHits());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.TOUCH_MISSES,serverMetrics.getTouchMisses());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.AUTH_CMDS,serverMetrics.getAuthCommands());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.AUTH_ERRORS,serverMetrics.getAuthErrors());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.EVICTIONS,serverMetrics.getEvictions());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.RECLAIMED,serverMetrics.getReclaimed());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.BYTES_READ,serverMetrics.getBytesRead());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.BYTES_WRITTEN,serverMetrics.getBytesWritten());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.LIMIT_MAXBYTES,serverMetrics.getLimitMaxBytes());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.THREADS,serverMetrics.getThreads());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CONN_YIELDS,serverMetrics.getYieldedConnections());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.HASH_BYTES,serverMetrics.getHashBytes());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.SLABS_MOVED,serverMetrics.getSlabsMoved());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.EXPIRED_UNFETCHED,serverMetrics.getExpiredUnfetched());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.EVICTED_UNFETCHED,serverMetrics.getEvictedUnfetched());
        printCollectiveObservedCurrent(metricName + METRIC_SEPARATOR + Metrics.CRAWLER_RECLAIMED,serverMetrics.getCrawlerReclaimed());

    }


    private void printCollectiveObservedCurrent(String metricName, String metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    /**
     * A helper method to report the metrics.
     * @param metricName
     * @param metricValue
     * @param aggType
     * @param timeRollupType
     * @param clusterRollupType
     */
    private void printMetric(String metricName,String metricValue,String aggType,String timeRollupType,String clusterRollupType){
        MetricWriter metricWriter = getMetricWriter(metricName,
                aggType,
                timeRollupType,
                clusterRollupType
        );
     //   System.out.println(getLogPrefix()+"Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
     //           + "] metric = " + metricName + " = " + metricValue);
        if (logger.isDebugEnabled()) {
            logger.debug(getLogPrefix()+"Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
                    + "] metric = " + metricName + " = " + metricValue);
        }
        metricWriter.printMetric(metricValue);
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = (logPrefix != null) ? logPrefix : "";
    }

    public static String getImplementationVersion() {
        return MemcachedMonitor.class.getPackage().getImplementationTitle();
    }

}
