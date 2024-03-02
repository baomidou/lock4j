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

package com.baomidou.lock.test.zookeeper;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.executor.ZookeeperLockExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    
    
    private int counter = 1;
    
    @Override
    @Lock4j(executor = ZookeeperLockExecutor.class)
    public void simple1() {
        System.out.println(
                "执行简单方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
    }
    
    @Override
    @Lock4j(name = "test-lock-name", keys = "#myKey", autoRelease = false, expire = 360)
    public void simple2(String myKey) {
        System.out.println(
                "执行简单方法2 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        
    }
}