/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.memcached;


import com.appdynamics.extensions.util.metrics.Metric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstanceMetric {

    private String displayName;
    private Map<String,String> metricsMap;
    private List<Metric> allMetrics;


    public InstanceMetric(String displayName,Map<String,String> metricsMap){
        this.displayName = displayName;
        this.metricsMap = metricsMap;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, String> getMetricsMap() {
        if(metricsMap == null){
            metricsMap = new HashMap<String, String>();
        }
        return metricsMap;
    }

    public void setMetricsMap(Map<String, String> metricsMap) {
        this.metricsMap = metricsMap;
    }

    public List<Metric> getAllMetrics() {
        if(allMetrics == null){
            allMetrics = new ArrayList<Metric>();
        }
        return allMetrics;
    }

    public void setAllMetrics(List<Metric> allMetrics) {
        this.allMetrics = allMetrics;
    }
}
