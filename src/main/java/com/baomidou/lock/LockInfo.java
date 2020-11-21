package com.baomidou.lock;

import com.baomidou.lock.executor.LockExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author zengzhihong
 */
@Data
@AllArgsConstructor
public class LockInfo {

    /**
     * 锁名称
     */
    private String lockKey;

    /**
     * 锁值
     */
    private String lockValue;

    /**
     * 过期时间
     */
    private Long expire;

    /**
     * 获取锁超时时间
     */
    private Long acquireTimeout;

    /**
     * 获取锁次数
     */
    private int acquireCount;

    /**
     * 锁执行器
     */
    private LockExecutor lockExecutor;
}
