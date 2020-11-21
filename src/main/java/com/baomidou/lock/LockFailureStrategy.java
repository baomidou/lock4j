package com.baomidou.lock;

/**
 * @author zengzhihong
 */
public interface LockFailureStrategy {

    void onLockFailure(long timeout, int acquireCount);

}
