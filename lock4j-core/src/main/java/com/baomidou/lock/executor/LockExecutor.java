/*
 *  Copyright (c) 2018-2021, baomidou (63976799@qq.com).
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


/**
 * 分布式锁核心处理器
 *
 * @author zengzhihong TaoYu
 */
public interface LockExecutor<T> {

    /**
     * 续期，目前只有redisson支持，切expire参数为-1才会续期
     * @return
     */
    default boolean renewal() {
        return false;
    }
    /**
     * 加锁
     *
     * @param lockKey        锁标识
     * @param lockValue      锁值
     * @param expire         锁有效时间
     * @param acquireTimeout 获取锁超时时间
     * @return 锁信息
     */
    T acquire(String lockKey, String lockValue, long expire, long acquireTimeout);

    /**
     * 解锁
     *
     * <pre>
     * 为何解锁需要校验lockValue
     * 客户端A加锁，一段时间之后客户端A解锁，在执行releaseLock之前，锁突然过期了。
     * 此时客户端B尝试加锁成功，然后客户端A再执行releaseLock方法，则将客户端B的锁给解除了。
     * </pre>
     *
     * @param key          加锁key
     * @param value        加锁value
     * @param lockInstance 锁实例
     * @return 是否释放成功
     */
    boolean releaseLock(String key, String value, T lockInstance);

}
