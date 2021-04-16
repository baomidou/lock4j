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

package com.baomidou.lock.test;

import com.baomidou.lock.test.model.User;
import com.baomidou.lock.test.service.UserService;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("ALL")
@RunWith(SpringRunner.class)
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
        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }
        Thread.sleep(1000000);
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
        Thread.sleep(30000);
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
        Thread.sleep(30000);
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
        Thread.sleep(30000);
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
        Thread.sleep(30000);
    }

    @Test
    public void reentrantLock() {
        userService.reentrantMethod1();
        userService.reentrantMethod1();
        userService.reentrantMethod2();
    }

}