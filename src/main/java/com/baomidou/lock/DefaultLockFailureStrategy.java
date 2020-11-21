package com.baomidou.lock;

import com.baomidou.lock.exception.LockFailureException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zengzhihong
 */
@Slf4j
public class DefaultLockFailureStrategy implements LockFailureStrategy {

    @Override
    public void onLockFailure(long timeout, int acquireCount) {
        log.debug("acquire lock fail,timeout:{} acquireCount:{}", timeout, acquireCount);
        throw new LockFailureException("lock failed,please retry it.");
    }
}
