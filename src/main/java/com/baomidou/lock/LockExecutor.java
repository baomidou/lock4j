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

/**
 * 分布式锁核心处理器
 *
 * @author zengzh TaoYu
 * @see RedisTemplateLockExecutor
 * @see RedissonLockExecutor
 * @since 1.0.0
 */
public interface LockExecutor {

    /**
     * 加锁
     *
     * @param key            分布式锁KEY
     * @param acquireTimeout 尝试获取锁超时时间 毫秒
     * @param expire         锁自动释放时间 毫秒
     * @return 是否上锁成功
     * @throws Exception 加锁过程中的所有异常
     */
    boolean acquireLock(String key, long acquireTimeout, long expire) throws Exception;


    /**
     * 释放锁
     *
     * @param key 分布式锁KEY
     */
    void releaseLock(String key);

}
