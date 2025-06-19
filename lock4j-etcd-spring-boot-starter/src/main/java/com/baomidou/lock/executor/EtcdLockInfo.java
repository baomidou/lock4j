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

/**
 * Etcd锁信息类，用于保存锁的相关信息
 *
 * @author yourname
 */
public class EtcdLockInfo {
    
    /**
     * 锁的键
     */
    private final ByteSequence lockKey;
    
    /**
     * 租约ID
     */
    private final long leaseId;
    
    /**
     * 锁的值
     */
    private final String lockValue;
    
    public EtcdLockInfo(ByteSequence lockKey, long leaseId, String lockValue) {
        this.lockKey = lockKey;
        this.leaseId = leaseId;
        this.lockValue = lockValue;
    }
    
    public ByteSequence getLockKey() {
        return lockKey;
    }
    
    public long getLeaseId() {
        return leaseId;
    }
    
    public String getLockValue() {
        return lockValue;
    }
    
    @Override
    public String toString() {
        return "EtcdLockInfo{" +
                "lockKey=" + lockKey +
                ", leaseId=" + leaseId +
                ", lockValue='" + lockValue + '\'' +
                '}';
    }
} 