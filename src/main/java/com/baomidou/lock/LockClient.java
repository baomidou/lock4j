package com.baomidou.lock;

/**
 * @author zengzhihong
 */
public enum LockClient {

    /**
     * spring redis template
     */
    REDIS_TEMPLATE,

    /**
     * default redission
     */
    REDISSON,

    /**
     * zk
     */
    ZOOKEEPER,

    ;
}
