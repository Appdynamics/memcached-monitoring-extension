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
        /*if(!metricPrefix.endsWith(METRICS_SEPARATOR)){
            metricPrefix = metricPrefix + METRICS_SEPARATOR;
        }*/
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
