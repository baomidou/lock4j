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

package com.baomidou.lock.test;

import com.baomidou.lock.test.model.User;
import com.baomidou.lock.test.service.UserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("ALL")
@SpringBootTest(classes = SpringBootLockTest.class)
@SpringBootApplication
public class SpringBootLockTest {

    private static final Random RANDOM = new Random();

    @Autowired
    UserService userService;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootLockTest.class, args);
    }

    @SneakyThrows
    @Test
    public void simple1Test() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    userService.simple1();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        for (int i = 0; i < 1000; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    @SneakyThrows
    @Test
    public void simple2Test() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    userService.simple2("xxx_key");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    @SneakyThrows
    @Test
    public void spel1Test() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    userService.method1(new User(1L, "苞米豆"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    @SneakyThrows
    @Test
    public void spel2Test() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    userService.method2(new User(1L, "苞米豆"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    @SneakyThrows
    @Test
    public void spel3Test() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    userService.method3();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }
    /**
     * 编程式锁
     */
    @SneakyThrows
    @Test
    public void programmaticLock() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                userService.programmaticLock("admin");
            }
        };
        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * 重入锁
     */
    @Test
    public void reentrantLock() {
        userService.reentrantMethod1();
        userService.reentrantMethod1();
        userService.reentrantMethod2();
    }

    /**
     * 不自动解锁
     */
    @SneakyThrows
    @Test
    public void nonAutoReleaseLock() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                userService.nonAutoReleaseLock();
            }
        };
        for (int i = 0; i < 1; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * redisTemplate锁续期
     */
    @SneakyThrows
    @Test
    public void  renewExpirationTemplate(){
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                userService.renewExpirationTemplate();
            }
        };
        for (int i = 0; i < 1; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * Lock4j注解适用于接口方法
     */
    @SneakyThrows
    @Test
    public void usedInInterface(){
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                userService.usedInInterface();
            }
        };
        for (int i = 0; i < 5; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * 方法级自定义锁失败策略
     */
    @SneakyThrows
    @Test
    public void customLockFailureStrategy() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                userService.customLockFailureStrategy();
            }
        };
        for (int i = 0; i < 5; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * 方法级自定义锁失败策略通过order|PriorityOrdered接口控制
     */
    @SneakyThrows
    @Test
    public void customLockFailureStrategy2(){
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                userService.customLockFailureStrategy2();
            }
        };
        for (int i = 0; i < 5; i++) {
            executorService.submit(task);
        }
        Thread.sleep(Long.MAX_VALUE);
    }

}