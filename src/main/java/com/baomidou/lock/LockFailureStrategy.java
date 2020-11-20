package com.baomidou.lock;

/**
 * @author zengzhihong
 * @date 2020-07-24
 */
public interface LockFailureStrategy {

    void onLockFailure(long timeout, int acquireCount);

}
