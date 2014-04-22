package com.appdynamics.extensions.memcached.config;

/**
 * An object holder for the configuration file
 */
public class Configuration {

    String metricPrefix;
    Server[] servers;

    public Server[] getServers() {
        return servers;
    }

    public void setServers(Server[] servers) {
        this.servers = servers;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

}
