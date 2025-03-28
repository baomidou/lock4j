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
import io.etcd.jetcd.lock.UnlockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class EtcdLockExecutor extends AbstractLockExecutor<Boolean> {

    private final Client etcdClient;

    private static final long DEFAULT_LEASE_TTL = 30; // 默认租约时间（秒）

    @Override
    public boolean renewal() {
        return true;
    }

    @Override
    public Boolean acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        try {
            // 创建租约
            Lease leaseClient = etcdClient.getLeaseClient();
            LeaseGrantResponse leaseGrantResponse = leaseClient.grant(DEFAULT_LEASE_TTL).get();
            long leaseId = leaseGrantResponse.getID();

            // 尝试获取锁
            Lock lockClient = etcdClient.getLockClient();
            LockResponse lockResponse = lockClient.lock(bytesOf(lockKey),
                    leaseId
            ).get(expire, TimeUnit.SECONDS);
            // 锁成功获取
            if (lockResponse != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public boolean releaseLock(String key, String value, Boolean lockInstance) {
        try {
            Lock lockClient = etcdClient.getLockClient();
            UnlockResponse unlockResponse = lockClient.unlock(bytesOf(key)).get();
            System.out.println("锁释放成功: " + unlockResponse.toString());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 将字符串转换为字节数组
     */
    private ByteSequence bytesOf(String value) {
        return ByteSequence.from(value.getBytes(StandardCharsets.UTF_8));
    }
}
