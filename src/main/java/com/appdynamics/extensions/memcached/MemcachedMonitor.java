/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.memcached;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.file.FileLoader;
import com.appdynamics.extensions.memcached.config.Configuration;
import com.appdynamics.extensions.memcached.config.Server;
import com.appdynamics.extensions.util.metrics.Metric;
import com.appdynamics.extensions.util.metrics.MetricFactory;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.appdynamics.TaskInputArgs.ENCRYPTION_KEY;
import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;
import static com.appdynamics.extensions.util.metrics.MetricConstants.METRICS_SEPARATOR;


/**
 * An entry point into AppDynamics extensions.
 */
public class MemcachedMonitor extends AManagedMonitor{

    public static final String CONFIG_ARG = "config-file";
    public static final Logger logger = LoggerFactory.getLogger(MemcachedMonitor.class);
    public static final String METRICS_COLLECTION_SUCCESSFUL = "Metrics Collection Successful";
    public static final String FAILED = "0";
    public static final String SUCCESS = "1";
    private volatile boolean initialized;
    private Configuration config;
    private Cache<String, BigInteger> cache;

    public MemcachedMonitor(){
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException {
        logVersion();
        try {
            initialize(taskArgs);
            //collect the metrics
            List<InstanceMetric> instanceMetrics = collectMetrics();
            //adding metric overrides
            MetricFactory<String> metricFactory = new MetricFactory<String>(config.getMetricOverrides());
            for(InstanceMetric instance : instanceMetrics){
                instance.getAllMetrics().addAll(metricFactory.process(instance.getMetricsMap()));

            }
            //print the metrics
            for(InstanceMetric instance: instanceMetrics){
                printMetrics(instance.getAllMetrics(),instance.getDisplayName());
                if(!instance.getAllMetrics().isEmpty()){
                    printMetric(getMetricPrefix(instance.getDisplayName()) + METRICS_COLLECTION_SUCCESSFUL, SUCCESS, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
                else {
                    printMetric(getMetricPrefix(instance.getDisplayName()) + METRICS_COLLECTION_SUCCESSFUL, FAILED, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
            }
            logger.info("Memcached monitor run completed successfully.");
            return new TaskOutput("Memcached monitor run completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Metrics collection failed", e);
        }
        throw new TaskExecutionException("Memcached monitoring run completed with failures.");
    }


    private void initialize(Map<String, String> taskArgs) {
        if(!initialized){
            //read the config.
            final String configFilePath = taskArgs.get(CONFIG_ARG);
            File configFile = PathResolver.getFile(configFilePath, AManagedMonitor.class);
            if(configFile != null && configFile.exists()){
                FileLoader.load(new FileLoader.Listener() {
                    public void load(File file) {
                        String path = file.getAbsolutePath();
                        try {
                            if (path.contains(configFilePath)) {
                                logger.info("The file [{}] has changed, reloading the config", file.getAbsolutePath());
                                reloadConfig(file);
                            } else {
                                logger.warn("Unknown file [{}] changed, ignoring", file.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            logger.error("Exception while reloading the file {}" ,file.getAbsolutePath(), e);
                        }
                    }
                }, configFilePath);
            }
            else{
                logger.error("Config file is not found.The config file path {} is resolved to {}",
                        taskArgs.get(CONFIG_ARG), configFile != null ? configFile.getAbsolutePath() : null);
            }
            initialized = true;
            cache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
        }
    }

    private void reloadConfig(File file) {
        config = YmlReader.readFromFile(file, Configuration.class);
        if (config != null) {
            //decrypt password
            if(config.getEncryptionKey() != null){
                for(Server server : config.getServers()) {
                    Map cryptoMap = Maps.newHashMap();
                    cryptoMap.put(PASSWORD_ENCRYPTED,server.getEncryptedPassword());
                    cryptoMap.put(ENCRYPTION_KEY,config.getEncryptionKey());
                    server.setPassword(CryptoUtil.getPassword(cryptoMap));
                }
            }
        }
        else {
            throw new IllegalArgumentException("The config cannot be initialized from the file " + file.getAbsolutePath());
        }
    }


    /**
     * Collects all the metrics by connecting to memcached servers through XmemcachedClient.
     * @throws Exception
     */
    private List<InstanceMetric> collectMetrics() throws Exception {
        MemcachedClient memcachedClient = null;
        try {
            Map<String, String> lookup = createDisplayNameLookup();
            memcachedClient = getMemcachedClient();

            Map<InetSocketAddress, Map<String, String>> stats = memcachedClient.getStats(config.getTimeout());
            return translateMetrics(stats, lookup);
        }
        catch(Exception e){
            logger.error("Unable to collect memcached metrics ", e);
            throw e;
        }
        finally {
            if (memcachedClient != null) {
                memcachedClient.shutdown();
            }
        }
    }


    private void printMetrics(List<Metric> allMetrics,String displayName) {
        Set<String> ignoreDelta = config.getIgnoreDelta();
        String prefix = getMetricPrefix(displayName);
        for(Metric aMetric:allMetrics) {
            String metricPath = prefix + aMetric.getMetricPath();
            BigInteger metricValue = aMetric.getMetricValue();
            if (ignoreDelta.contains(aMetric.getMetricPath())) {
                logger.debug("Ignore delta calculation for {}" + metricPath);
                printMetric(metricPath, metricValue.toString(), aMetric.getAggregator(), aMetric.getTimeRollup(), aMetric.getClusterRollup());
            }
            else{
                BigInteger prevValue = cache.getIfPresent(metricPath);
                cache.put(metricPath, metricValue);
                if(prevValue != null){
                    BigInteger deltaValue = metricValue.subtract(prevValue);
                    printMetric(metricPath, deltaValue.toString(), aMetric.getAggregator(), aMetric.getTimeRollup(), aMetric.getClusterRollup());
                }

            }
        }
    }



    private String getMetricPrefix(String displayName) {
        if(!Strings.isNullOrEmpty(displayName)) {
            return config.getMetricPrefix() + displayName + METRICS_SEPARATOR;
        }
        else{
            return config.getMetricPrefix();
        }
    }


    /**
     * Creates a lookup dictionary from configuration.
     * @return Map
     */
    private Map<String,String> createDisplayNameLookup() {
        Map<String,String> lookup = new HashMap<String,String>();
        if(config != null && config.getServers() != null){
            for(Server server : config.getServers()) {
                String splits[] = server.getServer().split(":");
                String hostname = "";
                int port = 11211;
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
    private List<InstanceMetric> translateMetrics(Map<InetSocketAddress, Map<String, String>> stats,Map<String,String> lookup) {
        List<InstanceMetric> metricsForAllInstances = new ArrayList<InstanceMetric>();
        if(stats != null){
            Iterator<InetSocketAddress> it = stats.keySet().iterator();
            while(it.hasNext()){
                InetSocketAddress sockAddress = it.next();
                Map<String, String> statsForSockAddress = stats.get(sockAddress);
                String displayName = lookup.get(sockAddress.toString());
                if(displayName != null) {
                    metricsForAllInstances.add(new InstanceMetric(displayName, statsForSockAddress));
                }
                else{
                    logger.error("Unable to lookup a client::"+sockAddress.toString());
                }
            }
        }
        return metricsForAllInstances;
    }


    /**
     * Builds a memcached client.
     * @return MemcachedClient
     * @throws IOException
     */
    private MemcachedClient getMemcachedClient() throws IOException {
        String aStringOfServers = getAllServersAsAString();
        MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(aStringOfServers));
        builder.setCommandFactory(new BinaryCommandFactory());
        try {
            MemcachedClient client =  builder.build();
            logger.debug("Built a memcached client for servers {}",aStringOfServers);
            return client;
        } catch (IOException e) {
            logger.error("Cannot create Memcached Client for servers :: {}",aStringOfServers , e);
            throw e;
        }
    }


    /**
     * Returns all the servers in the config as a string eg. "hostname:port hostname1:port1"
     * @return
     */
    private String getAllServersAsAString() {
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
     //   System.out.println("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
     //           + "] metric = " + metricName + " = " + metricValue);
        logger.debug("Sending [{}|{}|{}] metric= {},value={}", aggType, timeRollupType, clusterRollupType,metricName,metricValue);
        metricWriter.printMetric(metricValue);
    }


    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }

    public String getImplementationVersion() {
        return MemcachedMonitor.class.getPackage().getImplementationTitle();
    }



}
