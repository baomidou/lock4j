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

import org.apache.curator.framework.recipes.locks.InterProcessLock;

/**
 * zookeeper上下文
 *
 * @author zengzh
 * @since 1.0.0
 */
public class ZookeeperLockContext {

    private static final ThreadLocal<InterProcessLock> lockContext = new ThreadLocal<>();

    public static InterProcessLock getContext(){
        return lockContext.get();
    }

    public static void setContext(InterProcessLock interProcessLock){
        lockContext.set(interProcessLock);
    }
}
