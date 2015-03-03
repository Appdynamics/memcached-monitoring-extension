/**
 * Copyright 2014 AppDynamics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.memcached;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.memcached.config.Configuration;
import com.appdynamics.extensions.memcached.config.Server;
import com.appdynamics.extensions.util.metrics.Metric;
import com.appdynamics.extensions.util.metrics.MetricFactory;
import com.appdynamics.extensions.yml.YmlReader;
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
import java.util.*;

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

    private static String logPrefix;

    public MemcachedMonitor(){
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if(taskArgs != null) {
            logger.info("Starting the Memcached Monitoring task.");
            if (logger.isDebugEnabled()) {
                logger.debug("Task Arguments Passed ::" + taskArgs);
            }
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                //read the config.
                Configuration config = YmlReader.readFromFile(configFilename,Configuration.class);
                //collect the metrics
                List<InstanceMetric> instanceMetrics = collectMetrics(config);
                //adding metric overrides
                MetricFactory<String> metricFactory = new MetricFactory<String>(config.getMetricOverrides());
                for(InstanceMetric instance : instanceMetrics){
                    instance.getAllMetrics().addAll(metricFactory.process(instance.getMetricsMap()));
                }
                //print the metrics
                for(InstanceMetric instance: instanceMetrics){
                    printMetrics(instance.getAllMetrics(),config,instance);
                }
                logger.info("Memcached monitoring task completed successfully.");
                return new TaskOutput("Memcached monitoring task completed successfully.");
            } catch (FileNotFoundException e) {
                logger.error("Config file not found :: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics collection failed", e);
            }
        }
        throw new TaskExecutionException("Memcached monitoring task completed with failures.");
    }


    private void printMetrics(List<Metric> allMetrics,Configuration configuration,InstanceMetric instance) {
        String prefix = getMetricPathPrefix(configuration,instance);
        for(Metric aMetric:allMetrics){
            printMetric(prefix + aMetric.getMetricPath(),aMetric.getMetricValue().toString(),aMetric.getAggregator(),aMetric.getTimeRollup(),aMetric.getClusterRollup());
        }
    }

    private String getMetricPathPrefix(Configuration config, InstanceMetric instance) {
        return config.getMetricPathPrefix() + instance.getDisplayName() + METRIC_SEPARATOR;
    }
    /**
     * Collects all the metrics by connecting to memcached servers through XmemcachedClient.
     * @param config
     * @return Map
     * @throws Exception
     */
    private List<InstanceMetric> collectMetrics(Configuration config) throws Exception {
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient(config);
            Map<InetSocketAddress, Map<String, String>> stats = memcachedClient.getStats(TIMEOUT);
            Map<String, String> lookup = createDisplayNameLookup(config);
            return translateMetrics(stats, lookup);
        }
        catch(Exception e){
            logger.error("Unable to collect memcached metrics ", e);
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
            logger.error("Cannot create Memcached Client for servers :: " + aStringOfServers , e);
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
     * Returns a config file name,
     * @param filename
     * @return String
     */
    private String getConfigFilename(String filename) {
        if(filename == null){
            return "";
        }
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
        if (logger.isDebugEnabled()) {
            logger.debug("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
                    + "] metric = " + metricName + " = " + metricValue);
        }
        metricWriter.printMetric(metricValue);
    }

    public static String getImplementationVersion() {
        return MemcachedMonitor.class.getPackage().getImplementationTitle();
    }

}
