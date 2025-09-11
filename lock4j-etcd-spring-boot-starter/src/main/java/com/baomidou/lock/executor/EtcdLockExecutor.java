/*
 *  Copyright (c) 2018-2022, baomidou (63976799@qq.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.baomidou.lock.executor;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lock.LockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Etcd分布式锁执行器
 * 
 * @author yourname
 */
@Slf4j
@RequiredArgsConstructor
public class EtcdLockExecutor extends AbstractLockExecutor<EtcdLockInfo> {

    private final Client etcdClient;

    /**
     * Etcd目前不支持自动续期，所以返回false
     */
    @Override
    public boolean renewal() {
        return false;
    }

    /**
     * 获取锁
     * 
     * @param lockKey        锁标识
     * @param lockValue      锁值
     * @param expire         锁有效时间（毫秒）
     * @param acquireTimeout 获取锁超时时间（毫秒）
     * @return 锁信息，如果获取失败返回null
     */
    @Override
    public EtcdLockInfo acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            log.error("Lock key cannot be null or empty");
            return null;
        }

        try {
            // 将毫秒转换为秒，etcd租约时间单位是秒
            long expireSeconds = Math.max(1, expire / 1000);
            
            // 1. 创建租约
            Lease leaseClient = etcdClient.getLeaseClient();
            CompletableFuture<LeaseGrantResponse> leaseFuture = leaseClient.grant(expireSeconds);
            LeaseGrantResponse leaseGrantResponse = leaseFuture.get(acquireTimeout, TimeUnit.MILLISECONDS);
            long leaseId = leaseGrantResponse.getID();
            
            log.debug("Created lease with ID: {} for lock key: {}", leaseId, lockKey);

            try {
                // 2. 尝试获取锁
                Lock lockClient = etcdClient.getLockClient();
                ByteSequence lockKeyBytes = bytesOf(lockKey);
                
                CompletableFuture<LockResponse> lockFuture = lockClient.lock(lockKeyBytes, leaseId);
                LockResponse lockResponse = lockFuture.get(acquireTimeout, TimeUnit.MILLISECONDS);
                
                if (lockResponse != null && lockResponse.getKey() != null) {
                    log.debug("Successfully acquired lock for key: {} with lease: {}", lockKey, leaseId);
                    return new EtcdLockInfo(lockResponse.getKey(), leaseId, lockValue);
                } else {
                    log.warn("Failed to acquire lock for key: {}, lock response is null or invalid", lockKey);
                    // 获取锁失败，需要撤销租约
                    revokeLease(leaseId);
                    return null;
                }
                
            } catch (Exception e) {
                log.error("Error acquiring lock for key: {}", lockKey, e);
                // 获取锁过程中出现异常，需要撤销租约
                revokeLease(leaseId);
                return null;
            }

        } catch (TimeoutException e) {
            log.warn("Timeout acquiring lock for key: {} after {}ms", lockKey, acquireTimeout);
            return null;
        } catch (InterruptedException e) {
            log.warn("Interrupted while acquiring lock for key: {}", lockKey);
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            log.error("Execution error while acquiring lock for key: {}", lockKey, e.getCause());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error while acquiring lock for key: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放锁
     * 
     * @param key          加锁key
     * @param value        加锁value（用于验证锁的所有权）
     * @param lockInfo     锁实例信息
     * @return 是否释放成功
     */
    @Override
    public boolean releaseLock(String key, String value, EtcdLockInfo lockInfo) {
        if (lockInfo == null) {
            log.warn("Cannot release lock: lockInfo is null for key: {}", key);
            return false;
        }

        // 验证锁值是否匹配（防止释放他人的锁）
        if (!value.equals(lockInfo.getLockValue())) {
            log.warn("Lock value mismatch for key: {}, expected: {}, actual: {}", 
                    key, lockInfo.getLockValue(), value);
            return false;
        }

        boolean unlockSuccess = false;
        boolean leaseRevokeSuccess = false;

        try {
            // 1. 释放锁
            Lock lockClient = etcdClient.getLockClient();
            lockClient.unlock(lockInfo.getLockKey()).get(5000, TimeUnit.MILLISECONDS); // 5秒超时
            unlockSuccess = true;
            log.debug("Successfully unlocked key: {}", key);
            
        } catch (TimeoutException e) {
            log.error("Timeout unlocking key: {}", key, e);
        } catch (InterruptedException e) {
            log.error("Interrupted while unlocking key: {}", key, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Execution error while unlocking key: {}", key, e.getCause());
        } catch (Exception e) {
            log.error("Unexpected error while unlocking key: {}", key, e);
        }

        // 2. 撤销租约（无论解锁是否成功都要尝试撤销租约，防止租约泄漏）
        leaseRevokeSuccess = revokeLease(lockInfo.getLeaseId());

        boolean success = unlockSuccess && leaseRevokeSuccess;
        if (success) {
            log.debug("Successfully released lock and revoked lease for key: {}", key);
        } else {
            log.warn("Lock release partially failed for key: {}, unlock: {}, lease revoke: {}", 
                    key, unlockSuccess, leaseRevokeSuccess);
        }

        return success;
    }

    /**
     * 撤销租约
     * 
     * @param leaseId 租约ID
     * @return 是否成功
     */
    private boolean revokeLease(long leaseId) {
        try {
            Lease leaseClient = etcdClient.getLeaseClient();
            leaseClient.revoke(leaseId).get(3000, TimeUnit.MILLISECONDS); // 3秒超时
            log.debug("Successfully revoked lease: {}", leaseId);
            return true;
        } catch (TimeoutException e) {
            log.error("Timeout revoking lease: {}", leaseId, e);
        } catch (InterruptedException e) {
            log.error("Interrupted while revoking lease: {}", leaseId, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Execution error while revoking lease: {}", leaseId, e.getCause());
        } catch (Exception e) {
            log.error("Unexpected error while revoking lease: {}", leaseId, e);
        }
        return false;
    }

    /**
     * 将字符串转换为字节序列
     * 
     * @param value 字符串值
     * @return 字节序列
     */
    private ByteSequence bytesOf(String value) {
        return ByteSequence.from(value.getBytes(StandardCharsets.UTF_8));
    }
}
