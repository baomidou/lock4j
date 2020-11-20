package com.baomidou.lock.exception;

/**
 * @author zengzhihong
 * @date 2020-07-24
 */
public class LockFailureException extends LockException {


    public LockFailureException() {

    }

    public LockFailureException(String message) {
        super(message);
    }
}
