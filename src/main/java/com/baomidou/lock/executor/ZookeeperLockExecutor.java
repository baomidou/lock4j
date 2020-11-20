/**
 * ﻿Copyright © 2018 organization 苞米豆
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.lock.executor;

import com.baomidou.lock.LockInfo;
import lombok.NonNull;
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
public class ZookeeperLockExecutor implements LockExecutor {

    private InterProcessMutex mutex;

    private final CuratorFramework curatorFramework;

    public ZookeeperLockExecutor(@NonNull CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public boolean acquire(String lockKey, String lockValue, long timeout, long expire) {
        if (!CuratorFrameworkState.STARTED.equals(curatorFramework.getState())) {
            log.warn("instance must be started before calling this method");
            return false;
        }
        String nodePath = "/curator/lock4j/%s";

        boolean locked;
        try {
            mutex = new InterProcessMutex(curatorFramework, String.format(nodePath, lockKey));
            locked = mutex.acquire(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
        return locked;
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        try {
            mutex.release();
        } catch (Exception e) {
            log.warn("zookeeper lock release error", e);
            return false;
        }
        return true;
    }

}