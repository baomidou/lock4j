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
 * @since 1.0.0
 */
public interface LockExecutor {

    /**
     * 加锁
     *
     * @param key     缓存KEY
     * @param expire  到期时间 毫秒
     * @param timeout 尝试获取锁超时时间 毫秒
     * @return 锁信息
     */
    LockInfo tryLock(String key, long expire, long timeout) throws Exception;


    /**
     * 解锁
     *
     * <pre>
     * 为何解锁需要校验lockId
     * 客户端A加锁，一段时间之后客户端A解锁，在执行releaseLock之前，锁突然过期了。
     * 此时客户端B尝试加锁成功，然后客户端A再执行releaseLock方法，则将客户端B的锁给解除了。
     * </pre>
     *
     * @param lockInfo 获取锁返回的对象
     * @return 是否释放成功
     */
    boolean releaseLock(LockInfo lockInfo);

}
