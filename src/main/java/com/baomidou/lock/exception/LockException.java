package com.baomidou.lock.exception;

/**
 * @author zengzhihong
 * @date 2020-07-24
 */
public class LockException extends RuntimeException {

    public LockException() {
        super();
    }

    public LockException(String message) {

        super(message);
    }

}
