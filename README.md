memcached-monitoring-extension
==============================
An AppDynamics extension to be used with a stand alone Java machine agent to provide metrics from memcached instances.

## Metrics Provided ##
```
|-----------------------+---------+---------------------------------|
| Name                  | Meaning                                   |
|-----------------------+-------------------------------------------|
| curr_items            | Current number of items stored            |
| total_items           | Total number of items stored since        |
|                       | the server started                        |
| bytes                 | Current number of bytes used              |
|                       | to store items                            |
| curr_connections      | Number of open connections                |
| total_connections     | Total number of connections opened since  |
|                       | the server started running                |
| connection_structures | Number of connection structures allocated |
|                       | by the server                             |
| reserved_fds          | Number of misc fds used internally        |
| cmd_get               | Cumulative number of retrieval reqs       |
| cmd_set               | Cumulative number of storage reqs         |
| cmd_flush             | Cumulative number of flush reqs           |
| cmd_touch             | Cumulative number of touch reqs           |
| get_hits              | Number of keys that have been requested   |
|                       | and found present                         |
| get_misses            | Number of items that have been requested  |
|                       | and not found                             |
| delete_misses         | Number of deletions reqs for missing keys |
| delete_hits           | Number of deletion reqs resulting in      |
|                       | an item being removed.                    |
| incr_misses           | Number of incr reqs against missing keys. |
| incr_hits             | Number of successful incr reqs.           |
| decr_misses           | Number of decr reqs against missing keys. |
| decr_hits             | Number of successful decr reqs.           |
| cas_misses            | Number of CAS reqs against missing keys.  |
| cas_hits              | Number of successful CAS reqs.            |
| cas_badval            | Number of CAS reqs for which a key was    |
|                       | found, but the CAS value did not match.   |
| touch_hits            | Numer of keys that have been touched with |
|                       | a new expiration time                     |
| touch_misses          | Numer of items that have been touched and |
|                       | not found                                 |
| auth_cmds             | Number of authentication commands         |
|                       | handled, success or failure.              |
| auth_errors           | Number of failed authentications.         |
| evictions             | Number of valid items removed from cache  |
|                       | to free memory for new items              |
| reclaimed             | Number of times an entry was stored using |
|                       | memory from an expired entry              |
| bytes_read            | Total number of bytes read by this server |
|                       | from network                              |
| bytes_written         | Total number of bytes sent by this server |
|                       | to network                                |
| limit_maxbytes        | Number of bytes this server is allowed to |
|                       | use for storage.                          |
| threads               | Number of worker threads requested.       |
|                       | (see doc/threads.txt)                     |
| conn_yields           | Number of times any connection yielded to |
|                       | another due to hitting the -R limit.      |
| hash_bytes            | Bytes currently used by hash tables       |
| expired_unfetched     | Items pulled from LRU that were never     |
|                       | touched by get/incr/append/etc before     |
|                       | expiring                                  |
| evicted_unfetched     | Items evicted from LRU that were never    |
|                       | touched by get/incr/append/etc.           |
| slabs_moved           | Total slab pages moved                    |
| crawler_reclaimed     | Total items freed by LRU Crawler          |
|-----------------------+-------------------------------------------|
```
## Installation ##

1. Download and unzip MemcachedMonitor.zip from AppSphere.
2. Copy the MemcachedMonitor directory to `<MACHINE_AGENT_HOME>/monitors`.


## Configuration ##

###Note
Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a yaml validator http://yamllint.com/

1. Configure the memcached instances by editing the config.yaml file in `<MACHINE_AGENT_HOME>/monitors/MemcachedMonitor/`. Below is the format

  ```
  servers:
    - server: "localhost:11211"
      displayName: localhost
    - server: "192.168.57.102:11211"
      displayName: myUbuntu

  metricPrefix:  "Custom Metrics|Memcached|"
  ```

 
2. Configure the path to the config.yaml file by editing the <task-arguments> in the monitor.xml file. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/MemcachedMonitor/config.yaml" />
          ....
     </task-arguments>

     ```
### Cluster level metrics
     
We support cluster level metrics only if each node in the cluster have a separate machine agent installed on it. There are two configurations required for this setup 

1. Make sure that nodes belonging to the same cluster has the same <tier-name> in the <MACHINE_AGENT_HOME>/conf/controller-info.xml, we can gather cluster level metrics.  The tier-name here should be your cluster name. 

2. Make sure that in every node in the cluster, the <MACHINE_AGENT_HOME>/monitors/MemcachedMonitor/config.yaml should emit the same metric path. To achieve this make the displayName to be empty string and remove the trailing "|" in the metricPrefix.  


To make it more clear,assume that Memcached "Node A" and Memcached "Node B" belong to the same cluster "ClusterAB". In order to achieve cluster level as well as node level metrics, you should do the following
        
1. Both Node A and Node B should have separate machine agents installed on them. Both the machine agent should have their own Memcached extension.
    
2. In the Node A's and Node B's machine agents' controller-info.xml make sure that you have the tier name to be your cluster name , "ClusterAB" here. Also, nodeName in controller-info.xml is Node A and Node B resp.
        
3. The config.yaml for Node A and Node B should be

```
  servers:
    - server: "localhost:11211"
      displayName: ""
   

  metricPrefix:  "Custom Metrics|Memcached"

```
Now, if Node A and Node B are reporting say a metric called ReadLatency to the controller, with the above configuration they will be reporting it using the same metric path.
        
Node A reports Custom Metrics | ClusterAB | ReadLatency = 50 
Node B reports Custom Metrics | ClusterAB | ReadLatency = 500
        
The controller will automatically average out the metrics at the cluster (tier) level as well. So you should be able to see the cluster level metrics under
        
Application Performance Management | Custom Metrics | ClusterAB | ReadLatency = 225
        
Also, now if you want to see individual node metrics you can view it under
        
Application Performance Management | Custom Metrics | ClusterAB | Individual Nodes | Node A | ReadLatency = 50 
Application Performance Management | Custom Metrics | ClusterAB | Individual Nodes | Node B | ReadLatency = 500



Please note that for now the cluster level metrics are obtained by the averaging all the individual node level metrics in a cluster.

## Custom Dashboard ##
![](https://raw.githubusercontent.com/Appdynamics/memcached-monitoring-extension/master/memcached-dashboard.png?token=7142645__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvbWVtY2FjaGVkLW1vbml0b3JpbmctZXh0ZW5zaW9uL21hc3Rlci9tZW1jYWNoZWQtZGFzaGJvYXJkLnBuZyIsImV4cGlyZXMiOjEzOTg4MDc5MDh9--1f7ec9a9e4c72826204e1a7adb8ac5d0f5e879b8)

## Contributing ##

Always feel free to fork and contribute any changes directly via [GitHub][].

## Community ##

Find out more in the [Community][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

**Version:** 2.0
**Controller Compatibility:** 3.7 or later
**Memcached Version Tested On:** 1.4.13

[GitHub]: https://github.com/Appdynamics/memcached-monitoring-extension
[Community]: http://community.appdynamics.com/
[AppDynamics Center of Excellence]: mailto:ace-request@appdynamics.com
