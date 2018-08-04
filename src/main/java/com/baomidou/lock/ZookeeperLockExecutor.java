/**
 * ﻿Copyright © 2018 organization 苞米豆
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.lock;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁zookeeper处理器
 *
 * @author zengzh
 * @since 1.0.0
 */
@Slf4j
public class ZookeeperLockExecutor implements LockExecutor {

    CuratorFramework curatorFramework;

    public ZookeeperLockExecutor(@NonNull CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public boolean acquireLock(String key, long acquireTimeout, long expire)throws  Exception {
        if(!CuratorFrameworkState.STARTED.equals(curatorFramework.getState())){
            log.warn("instance must be started before calling this method");
            return false;
        }
        String nodePath = "/curator/lock/%s";
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, String.format(nodePath, key));
        boolean locked = mutex.acquire(acquireTimeout, TimeUnit.SECONDS);
        ZookeeperLockContext.setContext(mutex);
        return locked;
    }

    @Override
    public void releaseLock(String key) {
        InterProcessLock interProcessLock = ZookeeperLockContext.getContext();
        if (null != interProcessLock) {
            try {
                interProcessLock.release();
            } catch (Exception e) {
                log.warn("zookeeper lock release error", e);
            }
        }
    }

}