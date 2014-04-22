package com.appdynamics.extensions.memcached;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;


import java.util.Map;

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
}
