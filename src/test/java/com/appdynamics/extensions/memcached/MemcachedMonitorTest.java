/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.memcached;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;


import java.util.Map;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertTrue;


public class MemcachedMonitorTest {

    MemcachedMonitor memcachedMonitor = new MemcachedMonitor();

    @Test
    public void testMemcachedMonitor() throws TaskExecutionException {
        Map<String,String> taskArgs = Maps.newHashMap();
        taskArgs.put("config-file","src/test/resources/conf/config.yaml");
        TaskOutput output = memcachedMonitor.execute(taskArgs, null);
        assertTrue(output.getStatusMessage().contains("successfully"));
    }

    @Test(expected = TaskExecutionException.class)
    public void testWithNonExistentConfigFile() throws TaskExecutionException {
        Map<String,String> taskArgs = Maps.newHashMap();
        taskArgs.put("config-file","src/test/resources/conf/config1.yaml");
        TaskOutput output = memcachedMonitor.execute(taskArgs, null);
    }

    @Test(expected = TaskExecutionException.class)
    public void testMemcachedMonitorWithErroneousConfig() throws TaskExecutionException {
        Map<String,String> taskArgs = Maps.newHashMap();
        taskArgs.put("config-file","src/test/resources/conf/config_with_error.yaml");
        TaskOutput output = memcachedMonitor.execute(taskArgs, null);
    }

    @Test(expected = TaskExecutionException.class)
    public void testMemcachedMonitorWithNoConfig() throws TaskExecutionException {
        Map<String,String> taskArgs = Maps.newHashMap();
        TaskOutput output = memcachedMonitor.execute(taskArgs, null);
    }

    @Test(expected = TaskExecutionException.class)
    public void shouldThrowExceptionWhenTaskArgsIsNull() throws TaskExecutionException {
        TaskOutput output = memcachedMonitor.execute(null, null);
    }
}
