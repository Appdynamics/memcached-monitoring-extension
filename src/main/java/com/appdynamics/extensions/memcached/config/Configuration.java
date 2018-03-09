/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.memcached.config;

import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.google.common.collect.Sets;

import java.util.Set;

import static com.appdynamics.extensions.util.metrics.MetricConstants.METRICS_SEPARATOR;

/**
 * An object holder for the configuration file
 */
public class Configuration {

    String metricPrefix;
    Server[] servers;
    MetricOverride[] metricOverrides;
    String encryptionKey;
    long timeout = 60000;
    Set<String> ignoreDelta;

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
        if(!metricPrefix.endsWith(METRICS_SEPARATOR)){
            metricPrefix = metricPrefix + METRICS_SEPARATOR;
        }
        this.metricPrefix = metricPrefix;
    }


    public MetricOverride[] getMetricOverrides() {
        return metricOverrides;
    }

    public void setMetricOverrides(MetricOverride[] metricOverrides) {
        this.metricOverrides = metricOverrides;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Set<String> getIgnoreDelta() {
        if(ignoreDelta == null){
            ignoreDelta = Sets.newHashSet();
        }
        return ignoreDelta;
    }

    public void setIgnoreDelta(Set<String> ignoreDelta) {
        this.ignoreDelta = ignoreDelta;
    }
}
