package com.baomidou.lock.executor;

import com.baomidou.lock.exception.LockException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>基于{@link ReentrantLock}实现的单机本地锁执行器，用于提供加锁和解锁功能。
 *
 * @author huangchengxing
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class LocalLockExecutor extends AbstractLockExecutor<LocalLockExecutor.LocalLock> {

    /**
     * 是否为公平锁
     */
    private boolean fair = true;

    /**
     * <p>当前正在使用中的本地锁列表，每个key有且仅对应一个锁实例：
     * <ul>
     *     <li>当一个Key没有对应的锁时，线程应当创建并向列表中添加一个新的锁实例；</li>
     *     <li>当列表中的锁未过期时，多个线程可以正常的竞争同一把锁；</li>
     *     <li>当列表中的锁已过期时，新的线程将会将其替换为新的锁实例，旧的锁实例将不再影响其他线程；</li>
     *     <li>当线程解锁后，若没有其他竞争者正在使用该实例，则需要将锁实例从列表中移除；</li>
     * </ul>
     *
     * <p>当用户直接通过{@link LocalLock}的API而非{@link #releaseLock}时进行解锁时，
     * 可能会导致一个已经过期、且没有被任何线程持有的锁实例长时间存在于列表中，
     * 为了防止内存泄露，我们将其列表对锁实例的引用设置为弱引用，以便在此情况下借助GC时对其进行回收。
     */
    private final ConcurrentMap<String, LocalLock> lockMap = new ConcurrentReferenceHashMap<>(
        32, ConcurrentReferenceHashMap.ReferenceType.WEAK
    );

    /**
     * <p>尝试针对 key 加锁：
     * <ul>
     *     <li>若key尚无对应的锁实例，则创建一个锁实例并进行加锁；</li>
     *     <li>若已有对应的锁实例，且锁未过期，则在尝试在超时时间内获取锁，若获取成功则重置锁的过期时间；</li>
     *     <li>若已有对应的锁实例，且锁已过期，则直接创建新的锁实例，并替换旧的锁实例；</li>
     * </ul>
     *
     * @param lockKey        锁标识
     * @param lockValue      锁值
     * @param expire         锁有效时间，单位毫秒
     * @param acquireTimeout 获取锁超时时间，单位毫秒
     * @return 锁实例
     */
    @Override
    public LocalLock acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        // 如果锁不存在，则创建一个新实例
        // 如果存在但是已经过期，则使用一个新实例替换它，此时旧锁的解锁与否将不再影响其他线程
        // 如果锁仍然未过期，则继续复用该实例，此时多个线程可能会竞争同一把锁
        LocalLock lock = lockMap.compute(lockKey, (key, current) ->
            Objects.isNull(current) || current.isExpired() ? new LocalLock(fair, expire) : current
        );
        try {
            if (lock.tryLock(acquireTimeout, TimeUnit.MILLISECONDS)) {
                // 不管是重入还是新加锁，都需要重置过期时间
                lock.resetExpire(expire);
                return lock;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("lock fail", e);
            throw new LockException();
        }
        return null;
    }

    /**
     * <p>尝试针对 key 解锁：
     * <ul>
     *     <li>若锁不被当前线程持有，则直接返回；</li>
     *     <li>锁未过期，且无其他线程尝试获取锁，则线程A正常释放锁，随后将锁实例从列表中移除；</li>
     *     <li>锁未过期，但已经有其他线程尝试获取锁，则线程A正常释放锁，但并不将锁实例从列表中移除；</li>
     *     <li>锁已过期，此时其他线程已经获取了新的锁实例，则线程A直接释放锁，并不做任何处理；</li>
     * </ul>
     *
     * @param key          加锁key
     * @param value        加锁value
     * @param lockInstance 锁实例
     * @return 是否释放成功
     */
    @Override
    public boolean releaseLock(String key, String value, LocalLock lockInstance) {
        // 当用户直接通过锁实例进行解锁时，则此时锁可能已经被释放
        if (!lockInstance.isHeldByCurrentThread()) {
            return false;
        }
        boolean expired = lockInstance.isExpired();
        // 进入此步骤，说明当前线程仍然持有锁，不过可能已经过期，也无法保证在列表中依然存在
        lockMap.computeIfPresent(key, (k, current) -> {
            // 若锁已经在列表中不存在，说明其可能已因过期而被其他线程移除，故不做任何处理
            if (current != lockInstance) {
                return current;
            }
            // 若锁仍然存在，且可重入数量大于1，则不做任何处理，等待后续重入
            if (current.getHoldCount() > 1) {
                return current;
            }
            // 锁仍然存在，且已经是最后一次重入，
            // 此时，除非有其他线程已经获取到该实例并进行竞争，否则将其从列表移除
            return current.getQueueLength() > 0 ? current : null;
        });
        // 无论如何都进行一次解锁操作，因此除非锁已经过期，否则总是认为解锁成功
        lockInstance.unlock();
        return !expired;
    }

    /**
     * 基于{@link ReentrantLock}实现的本地锁,
     * 当创建时需要指定锁的有效时间，
     * 如果有效时间小于0则表示永不过期。
     *
     * @author huangchengxing
     */
    public static class LocalLock extends ReentrantLock {

        /**
         * 永不过期
         */
        public static final long NEVER_EXPIRE = -1L;

        /**
         * 构造器
         *
         * @param fair 是否为公平锁
         * @param expireTime 锁的有效时间
         */
        LocalLock(boolean fair, long expireTime) {
            super(fair);
            this.expireTime = expireTime;
        }

        /**
         * 过期时间点
         */
        private long expireTime;

        /**
         * 当前锁是否已经过期
         *
         * @return 是否
         */
        public boolean isExpired() {
            return expireTime != NEVER_EXPIRE
                && System.currentTimeMillis() > expireTime;
        }

        /**
         * 更新过期时间
         *
         * @param expire 锁的有效时间
         */
        public void resetExpire(long expire) {
            this.expireTime = expire < 0 ?
                NEVER_EXPIRE : System.currentTimeMillis() + expire;
        }
    }
}
