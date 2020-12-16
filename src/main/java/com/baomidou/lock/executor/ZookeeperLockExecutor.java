/*
 *  Copyright (c) 2018-2020, baomidou (63976799@qq.com).
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁zookeeper处理器
 *
 * @author zengzhihong
 */
@Slf4j
@RequiredArgsConstructor
public class ZookeeperLockExecutor extends AbstractLockExecutor<InterProcessMutex> implements LockExecutor<InterProcessMutex> {

    private final CuratorFramework curatorFramework;

    @Override
    public InterProcessMutex acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
        if (!CuratorFrameworkState.STARTED.equals(curatorFramework.getState())) {
            log.warn("instance must be started before calling this method");
            return null;
        }
        String nodePath = "/curator/lock4j/%s";
        try {
            InterProcessMutex mutex = new InterProcessMutex(curatorFramework, String.format(nodePath, lockKey));
            final boolean locked = mutex.acquire(acquireTimeout, TimeUnit.SECONDS);
            return obtainLockInstance(locked, mutex);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean releaseLock(String key, String value, InterProcessMutex lockInstance) {
        try {
            final InterProcessMutex instance = lockInstance;
            instance.release();
        } catch (Exception e) {
            log.warn("zookeeper lock release error", e);
            return false;
        }
        return true;
    }

}