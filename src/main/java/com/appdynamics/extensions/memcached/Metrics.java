/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.memcached;

import java.util.Map;

/**
 * A container for all the metrics supported from memcached.
 */
public class Metrics {

    public static final String CURR_ITEMS = "curr_items";
    public static final String TOTAL_ITEMS = "total_items";
    public static final String BYTES = "bytes";
    public static final String CURR_CONNECTIONS = "curr_connections";
    public static final String TOTAL_CONNECTIONS = "total_connections";
    public static final String CONNECTION_STRUCTURES = "connection_structures";
    public static final String RESERVED_FDS = "reserved_fds";
    public static final String CMD_GET = "cmd_get";
    public static final String CMD_SET = "cmd_set";
    public static final String CMD_FLUSH = "cmd_flush";
    public static final String CMD_TOUCH = "cmd_touch";
    public static final String GET_HITS = "get_hits";
    public static final String GET_MISSES = "get_misses";
    public static final String DELETE_MISSES = "delete_misses";
    public static final String DELETE_HITS = "delete_hits";
    public static final String INCR_HITS = "incr_hits";
    public static final String INCR_MISSES = "incr_misses";
    public static final String DECR_HITS = "decr_hits";
    public static final String DECR_MISSES = "decr_misses";
    public static final String CAS_HITS = "cas_hits";
    public static final String CAS_MISSES = "cas_misses";
    public static final String CAS_BADVAL = "cas_badval";
    public static final String TOUCH_HITS = "touch_hits";
    public static final String TOUCH_MISSES = "touch_misses";
    public static final String AUTH_CMDS = "auth_cmds";
    public static final String AUTH_ERRORS = "auth_errors";
    public static final String EVICTIONS = "evictions";
    public static final String RECLAIMED = "reclaimed";
    public static final String BYTES_READ = "bytes_read";
    public static final String BYTES_WRITTEN = "bytes_written";
    public static final String LIMIT_MAXBYTES = "limit_maxbytes";
    public static final String THREADS = "threads";
    public static final String CONN_YIELDS = "conn_yields";
    public static final String HASH_BYTES = "hash_bytes";
    public static final String SLABS_MOVED = "slabs_moved";
    public static final String EXPIRED_UNFETCHED = "expired_unfetched";
    public static final String EVICTED_UNFETCHED = "evicted_unfetched";
    public static final String CRAWLER_RECLAIMED = "crawler_reclaimed";
    /*Current number of items stored*/
    private String currentItems;

    /*Total number of items stored since  the server started*/
    private String totalItems;

    /*Current number of bytes used to store items */
    private String bytes;

    /*Number of open connections */
    private String currentConnections;

    /* Total number of connections opened since the server started running*/
    private String totalConnections;

    /*Number of connection structures allocated by the server*/
    private String connectionStructures;

    /*Number of misc fds used internally*/
    private String reservedFds;

    /*Cumulative number of retrieval reqs   */
    private String getCommands;

    /*Cumulative number of storage reqs */
    private String setCommands;

    /*Cumulative number of flush reqs */
    private String flushCommands;

    /*Cumulative number of touch reqs  */
    private String touchCommands;

    /*Number of keys that have been requested and found present*/
    private String getHits;

    /*Number of items that have been requested and not found*/
    private String getMisses;

    /*Number of deletion reqs resulting in  an item being removed. */
    private String deleteMisses;

    /* Number of deletions reqs for missing keys */
    private String deleteHits;

    /*Number of successful incr reqs.*/
    private String incrHits;

    /*Number of incr reqs against missing keys. */
    private String incrMisses;

    /*Number of successful decr reqs.*/
    private String decrHits;

    /*Number of decr reqs against missing keys.*/
    private String decrMisses;

    /* Number of successful CAS reqs.*/
    private String casHits;

    /*Number of CAS reqs against missing keys. */
    private String casMisses;

    /*Number of CAS reqs for which a key was found, but the CAS value did not match.*/
    private String casBadValues;

    /*Numer of keys that have been touched with a new expiration time */
    private String touchHits;

    /*Numer of items that have been touched and not found*/
    private String touchMisses;

    /*Number of authentication commands handled, success or failure.*/
    private String authCommands;

    /*Number of failed authentications.*/
    private String authErrors;

    /*Number of valid items removed from cache to free memory for new items */
    private String evictions;

    /*Number of times an entry was stored using memory from an expired entry  */
    private String reclaimed;

    /*Total number of bytes read by this server from network */
    private String bytesRead;

    /*Total number of bytes sent by this server to network*/
    private String bytesWritten;

    /* Number of bytes this server is allowed to use for storage.  */
    private String limitMaxBytes;

    /*Number of worker threads requested.*/
    private String threads;

    /*Number of times any connection yielded to another due to hitting the -R limit.*/
    private String yieldedConnections;

    /*Bytes currently used by hash tables */
    private String hashBytes;

    /*Total slab pages moved */
    private String slabsMoved;

    /*Items pulled from LRU that were never touched by get/incr/append/etc before expiring */
    private String expiredUnfetched;

    /*Items evicted from LRU that were never touched by get/incr/append/etc.*/
    private String evictedUnfetched;

    /*Total items freed by LRU Crawler*/
    private String crawlerReclaimed;

    public Metrics(Map<String,String> metrics) {
        this.currentItems = metrics.get(CURR_ITEMS);
        this.totalItems = metrics.get(TOTAL_ITEMS);
        this.bytes = metrics.get(BYTES);
        this.currentConnections = metrics.get(CURR_CONNECTIONS);
        this.totalConnections = metrics.get(TOTAL_CONNECTIONS);
        this.connectionStructures = metrics.get(CONNECTION_STRUCTURES);
        this.reservedFds = metrics.get(RESERVED_FDS);
        this.getCommands = metrics.get(CMD_GET);
        this.setCommands = metrics.get(CMD_SET);
        this.flushCommands = metrics.get(CMD_FLUSH);
        this.touchCommands = metrics.get(CMD_TOUCH);
        this.getHits = metrics.get(GET_HITS);
        this.getMisses = metrics.get(GET_MISSES);
        this.deleteMisses = metrics.get(DELETE_MISSES);
        this.deleteHits = metrics.get(DELETE_HITS);
        this.incrHits = metrics.get(INCR_HITS);
        this.incrMisses = metrics.get(INCR_MISSES);
        this.decrHits = metrics.get(DECR_HITS);
        this.decrMisses = metrics.get(DECR_MISSES);
        this.casHits = metrics.get(CAS_HITS);
        this.casMisses = metrics.get(CAS_MISSES);
        this.casBadValues = metrics.get(CAS_BADVAL);
        this.touchHits = metrics.get(TOUCH_HITS);
        this.touchMisses = metrics.get(TOUCH_MISSES);
        this.authCommands = metrics.get(AUTH_CMDS);
        this.authErrors = metrics.get(AUTH_ERRORS);
        this.evictions = metrics.get(EVICTIONS);
        this.reclaimed = metrics.get(RECLAIMED);
        this.bytesRead = metrics.get(BYTES_READ);
        this.bytesWritten = metrics.get(BYTES_WRITTEN);
        this.limitMaxBytes = metrics.get(LIMIT_MAXBYTES);
        this.threads = metrics.get(THREADS);
        this.yieldedConnections = metrics.get(CONN_YIELDS);
        this.hashBytes = metrics.get(HASH_BYTES);
        this.slabsMoved = metrics.get(SLABS_MOVED);
        this.expiredUnfetched = metrics.get(EXPIRED_UNFETCHED);
        this.evictedUnfetched = metrics.get(EVICTED_UNFETCHED);
        this.crawlerReclaimed = metrics.get(CRAWLER_RECLAIMED);
    }

    public String getCurrentItems() {
        return currentItems;
    }

    public String getTotalItems() {
        return totalItems;
    }

    public String getBytes() {
        return bytes;
    }

    public String getCurrentConnections() {
        return currentConnections;
    }

    public String getTotalConnections() {
        return totalConnections;
    }

    public String getConnectionStructures() {
        return connectionStructures;
    }

    public String getReservedFds() {
        return reservedFds;
    }

    public String getGetCommands() {
        return getCommands;
    }

    public String getSetCommands() {
        return setCommands;
    }

    public String getFlushCommands() {
        return flushCommands;
    }

    public String getTouchCommands() {
        return touchCommands;
    }

    public String getGetHits() {
        return getHits;
    }

    public String getGetMisses() {
        return getMisses;
    }

    public String getDeleteMisses() {
        return deleteMisses;
    }

    public String getDeleteHits() {
        return deleteHits;
    }

    public String getIncrHits() {
        return incrHits;
    }

    public String getIncrMisses() {
        return incrMisses;
    }

    public String getDecrHits() {
        return decrHits;
    }

    public String getDecrMisses() {
        return decrMisses;
    }

    public String getCasHits() {
        return casHits;
    }

    public String getCasMisses() {
        return casMisses;
    }

    public String getCasBadValues() {
        return casBadValues;
    }

    public String getTouchHits() {
        return touchHits;
    }

    public String getTouchMisses() {
        return touchMisses;
    }

    public String getAuthCommands() {
        return authCommands;
    }

    public String getAuthErrors() {
        return authErrors;
    }

    public String getEvictions() {
        return evictions;
    }

    public String getReclaimed() {
        return reclaimed;
    }

    public String getBytesRead() {
        return bytesRead;
    }

    public String getBytesWritten() {
        return bytesWritten;
    }

    public String getLimitMaxBytes() {
        return limitMaxBytes;
    }

    public String getThreads() {
        return threads;
    }

    public String getYieldedConnections() {
        return yieldedConnections;
    }

    public String getHashBytes() {
        return hashBytes;
    }

    public String getSlabsMoved() {
        return slabsMoved;
    }

    public String getExpiredUnfetched() {
        return expiredUnfetched;
    }

    public String getEvictedUnfetched() {
        return evictedUnfetched;
    }

    public String getCrawlerReclaimed() {
        return crawlerReclaimed;
    }
}
