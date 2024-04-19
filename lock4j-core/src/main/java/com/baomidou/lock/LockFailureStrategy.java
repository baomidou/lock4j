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

package com.baomidou.lock;

import java.lang.reflect.Method;

/**
 * 当加锁失败时的处理策略
 *
 * @author zengzhihong
 */
public interface LockFailureStrategy {

    /**
     * 当加锁失败时的处理策略
     *
     * @param key 用于获取锁的key
     * @param method 方法
     * @param arguments 方法参数
     * @throws Exception 处理过程中可能抛出的异常，如果抛出异常则会终止方法执行
     */
    void onLockFailure(String key, Method method, Object[] arguments) throws Exception;

    // TODO 释放锁失败时也应该进行处理，具体参见：https://gitee.com/baomidou/lock4j/issues/I4LG1U
}
