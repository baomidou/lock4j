package com.baomidou.lock.executor;

import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * test for {@link LocalLockExecutor}
 *
 * @author huangchengxing
 */
class LocalLockExecutorTest {

    private static final String KEY = "key";
    private static final String VALUE = "value";

    private LocalLockExecutor localLockExecutor;

    @BeforeEach
    void init() {
        localLockExecutor = new LocalLockExecutor();
    }

    @Test
    void simpleTest() {
        // 获取锁
        LocalLockExecutor.LocalLock lock = localLockExecutor.acquire(
            KEY, VALUE, 250, 0
        );
        Assertions.assertNotNull(lock);
        Assertions.assertFalse(lock.isExpired());
        Assertions.assertTrue(lock.isLocked());
        Assertions.assertTrue(lock.isHeldByCurrentThread());

        // 解锁
        Assertions.assertTrue(localLockExecutor.releaseLock(KEY, VALUE, lock));
        Assertions.assertFalse(lock.isLocked());
        Assertions.assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    void timeoutTest() {
        // 获取锁
        LocalLockExecutor.LocalLock lock = localLockExecutor.acquire(
            KEY, VALUE, 250, 0
        );
        Assertions.assertNotNull(lock);
        Assertions.assertFalse(lock.isExpired());
        Assertions.assertTrue(lock.isLocked());
        Assertions.assertTrue(lock.isHeldByCurrentThread());
        // 阻塞一段时间，确保锁过期
        block(500);

        // 解锁，由于锁已经过期，因此认为释放失败
        Assertions.assertTrue(lock.isExpired());
        Assertions.assertTrue(lock.isLocked());
        Assertions.assertFalse(localLockExecutor.releaseLock(KEY, VALUE, lock));
        Assertions.assertFalse(lock.isLocked());
        Assertions.assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    void reentrantTest() {
        // 获取 lock1，有效期 0.25 秒
        LocalLockExecutor.LocalLock lock1 = localLockExecutor.acquire(
            KEY, VALUE, 250, 0
        );
        Assertions.assertNotNull(lock1);
        Assertions.assertFalse(lock1.isExpired());
        Assertions.assertTrue(lock1.isLocked());
        Assertions.assertTrue(lock1.isHeldByCurrentThread());

        // 重入加锁，此时由于锁未过期，因此拿到的是依然是 lock1
        LocalLockExecutor.LocalLock lock2 = localLockExecutor.acquire(
            KEY, VALUE, 250, 0
        );
        Assertions.assertNotNull(lock2);
        Assertions.assertSame(lock1, lock2);
        Assertions.assertFalse(lock2.isExpired());
        Assertions.assertTrue(lock2.isLocked());
        Assertions.assertTrue(lock2.isHeldByCurrentThread());

        // 释放一次
        Assertions.assertTrue(localLockExecutor.releaseLock(KEY, VALUE, lock2));
        Assertions.assertTrue(lock2.isLocked());
        Assertions.assertTrue(lock2.isHeldByCurrentThread());
        // 释放两次，此时锁已被彻底释放
        Assertions.assertTrue(localLockExecutor.releaseLock(KEY, VALUE, lock2));
        Assertions.assertFalse(lock2.isLocked());
        Assertions.assertFalse(lock2.isHeldByCurrentThread());

        // 因为当前线程已经不再持有锁，因此次再次释放无效
        Assertions.assertFalse(localLockExecutor.releaseLock(KEY, VALUE, lock1));
        Assertions.assertFalse(lock1.isLocked());
        Assertions.assertFalse(lock1.isHeldByCurrentThread());
    }

    @Test
    void timeoutReentrantTest() {
        // 获取 lock1，有效期 0.25 秒
        LocalLockExecutor.LocalLock lock1 = localLockExecutor.acquire(
            KEY, VALUE, 250, 250
        );
        Assertions.assertNotNull(lock1);
        Assertions.assertFalse(lock1.isExpired());
        Assertions.assertTrue(lock1.isLocked());
        Assertions.assertTrue(lock1.isHeldByCurrentThread());
        // 阻塞一段时间，确保 lock1 过期
        block(500);
        Assertions.assertTrue(lock1.isExpired());

        // 重入加锁，由于锁已经过期，因此拿到的是新的锁 lock2
        LocalLockExecutor.LocalLock lock2 = localLockExecutor.acquire(
            KEY, VALUE, 500, 250
        );
        Assertions.assertNotNull(lock2);
        Assertions.assertNotSame(lock1, lock2);
        Assertions.assertFalse(lock2.isExpired());
        Assertions.assertTrue(lock2.isLocked());
        Assertions.assertTrue(lock2.isHeldByCurrentThread());

        // 释放 lock1，由于 lock1 已经过期，因此认为释放失败
        Assertions.assertTrue(lock1.isLocked());
        Assertions.assertFalse(localLockExecutor.releaseLock(KEY, VALUE, lock1));
        Assertions.assertFalse(lock1.isLocked());
        Assertions.assertFalse(lock1.isHeldByCurrentThread());

        // 释放 lock2，由于 lock2 未过期，因此释放成功
        Assertions.assertTrue(localLockExecutor.releaseLock(KEY, VALUE, lock2));
        Assertions.assertFalse(lock2.isLocked());
        Assertions.assertFalse(lock2.isHeldByCurrentThread());
    }

    @Test
    void concurrentTest() {
        Thread mainThread = Thread.currentThread();
        Worker successLockWorker = new Worker();
        Worker failLockWorker = new Worker();
        AtomicReference<Throwable> ex = new AtomicReference<>();

        successLockWorker.setTask(() -> {
            try {
                // 获取锁，该锁永不过期
                LocalLockExecutor.LocalLock lock = localLockExecutor.acquire(
                    KEY, VALUE, LocalLockExecutor.LocalLock.NEVER_EXPIRE, 0
                );
                Assertions.assertNotNull(lock);
                Assertions.assertFalse(lock.isExpired());
                Assertions.assertTrue(lock.isLocked());
                Assertions.assertTrue(lock.isHeldByCurrentThread());

                // 阻塞当前线程，等待另一线程获取锁失败后再继续执行
                failLockWorker.start();
                successLockWorker.park();

                // 已被唤醒，进行解锁操作
                Assertions.assertFalse(lock.isExpired());
                Assertions.assertTrue(localLockExecutor.releaseLock(KEY, VALUE, lock));
                Assertions.assertFalse(lock.isLocked());
                Assertions.assertFalse(lock.isHeldByCurrentThread());
            }
            catch (Throwable e) {
                ex.set(e);
            }
            finally {
                // 唤醒主线程
                successLockWorker.interrupt();
                LockSupport.unpark(mainThread);
            }
        });

        failLockWorker.setTask(() -> {
            try {
                // 获取锁，由于另一线程未释放该锁，因此将会超时
                LocalLockExecutor.LocalLock lock = localLockExecutor.acquire(
                    KEY, VALUE, 250, 250
                );
                Assertions.assertNull(lock);
            }
            catch (Throwable e) {
                ex.set(e);
            }
            finally {
                failLockWorker.interrupt();
                successLockWorker.unpark();
            }
        });

        successLockWorker.start();
        // 阻塞主线程，等待子线程执行完毕
        LockSupport.park(mainThread);

        if (Objects.nonNull(ex.get())) {
            Assertions.fail(ex.get());
        }
    }

    @Test
    void timeoutConcurrentTest() {
        Thread mainThread = Thread.currentThread();
        Worker work1 = new Worker();
        Worker work2 = new Worker();
        AtomicReference<Throwable> ex = new AtomicReference<>();

        work1.setTask(() -> {
            try {
                // 获取锁，该锁有效期 0.25 秒
                LocalLockExecutor.LocalLock lock = localLockExecutor.acquire(
                    KEY, VALUE, 250, 0
                );
                Assertions.assertNotNull(lock);
                Assertions.assertFalse(lock.isExpired());
                Assertions.assertTrue(lock.isLocked());
                Assertions.assertTrue(lock.isHeldByCurrentThread());

                // 阻塞一段时间，确保锁过期
                block(500);
                work2.start();

                // 已被唤醒，进行解锁操作，此时由于锁已经过期，因此释放失败
                Assertions.assertFalse(localLockExecutor.releaseLock(KEY, VALUE, lock));
                Assertions.assertFalse(lock.isLocked());
                Assertions.assertFalse(lock.isHeldByCurrentThread());
            }
            catch (Throwable e) {
                ex.set(e);
            }
            finally {
                // 唤醒主线程
                work1.interrupt();
                LockSupport.unpark(mainThread);
            }
        });

        work2.setTask(() -> {
            try {
                // 获取锁，由于另一线程锁已经过期，因此可以获取到锁
                LocalLockExecutor.LocalLock lock = localLockExecutor.acquire(
                    KEY, VALUE, 250, 0
                );
                Assertions.assertNotNull(lock);
                Assertions.assertFalse(lock.isExpired());
                Assertions.assertTrue(lock.isLocked());
                Assertions.assertTrue(lock.isHeldByCurrentThread());

                // 进行解锁操作
                Assertions.assertTrue(localLockExecutor.releaseLock(KEY, VALUE, lock));
                Assertions.assertFalse(lock.isLocked());
                Assertions.assertFalse(lock.isHeldByCurrentThread());
            }
            catch (Throwable e) {
                ex.set(e);
            }
            finally {
                // 唤醒另一线程
                work2.interrupt();
                work1.unpark();
            }
        });

        work1.start();
        // 阻塞主线程，等待子线程执行完毕
        LockSupport.park(mainThread);

        if (Objects.nonNull(ex.get())) {
            Assertions.fail(ex.get());
        }
    }

    @Test
    void unReleaseTest() {
        Thread mainThread = Thread.currentThread();
        AtomicReference<LocalLockExecutor.LocalLock> oldLock = new AtomicReference<>();

        Worker work1 = new Worker();
        work1.setTask(() -> {
            try {
                // 获取锁，然后不通过 LocalLockExecutor 直接进行解锁
                LocalLockExecutor.LocalLock lock = localLockExecutor.acquire(KEY, VALUE, 250, 0);
                Assertions.assertNotNull(lock);
                oldLock.set(lock);
                lock.unlock();
            }
            finally {
                work1.interrupt();
                LockSupport.unpark(mainThread);
            }
        });

        work1.start();
        LockSupport.park(mainThread);
        // 手动触发一次GC，确保该无效的 LocalLock 实例被回收
        System.gc();

        // 由于 LocalLock 实例已经被回收，因此释放失败
        Assertions.assertFalse(localLockExecutor.releaseLock(KEY, VALUE, oldLock.get()));
        // 并且，再次获取新锁也不会受到影响
        LocalLockExecutor.LocalLock newLock = localLockExecutor.acquire(KEY, VALUE, 250, 0);
        Assertions.assertNotNull(newLock);
        Assertions.assertNotSame(newLock, oldLock.get());
    }

    @Setter
    private static class Worker extends Thread {
        private Runnable task;
        @Override
        public void run() {
            task.run();
        }
        public void park() {
            LockSupport.park(this);
        }
        public void unpark() {
            LockSupport.unpark(this);
        }
    }

    private static void block(long milliseconds) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds));
    }
}