package com.baomidou.lock;

/**
 * @author zengzhihong
 * @date 2020-01-10
 */
public enum LockType {

    /**
     * 可重入锁
     */
    REENTRANT,

    /**
     * 公平锁
     */
    Fair,

    /**
     * 读写锁
     */
    READ,

    /**
     * 写锁
     */
    WRITE,

    ;
}
