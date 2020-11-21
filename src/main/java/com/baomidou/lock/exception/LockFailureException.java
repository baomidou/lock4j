package com.baomidou.lock.exception;

/**
 * @author zengzhihong
 */
public class LockFailureException extends LockException {


    public LockFailureException() {

    }

    public LockFailureException(String message) {
        super(message);
    }
}
