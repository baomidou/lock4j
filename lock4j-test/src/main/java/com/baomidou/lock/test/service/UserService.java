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

package com.baomidou.lock.test.service;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.executor.RedisTemplateLockExecutor;
import com.baomidou.lock.test.model.User;

/**
 * @author zengzhihong
 */
public interface UserService {

    void localLock1(long blockTimeMillis);

    void multiCondition1(Integer id);

    void condition1(Integer id);

    void simple1();

    void simple2(String myKey);

    User method1(User user);

    User method2(User user);

    void method3();

    void programmaticLock(String userId);

    void reentrantMethod1();

    void reentrantMethod2();

    void nonAutoReleaseLock();

    void renewExpirationTemplate();

    @Lock4j(keys ="1",expire = -1,executor = RedisTemplateLockExecutor.class)
    void usedInInterface();

    void customLockFailureStrategy();

    void customLockFailureStrategy2();
}
