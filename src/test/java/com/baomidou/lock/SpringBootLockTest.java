package com.baomidou.lock;

import com.baomidou.lock.model.User;
import com.baomidou.lock.service.UserService;
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

    @Test
    public void simple1Test() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
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

    @Test
    public void simple2Test() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
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
        Thread.sleep(3000);
    }

    @Test
    public void spel1Test() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
            public void run() {
                try {
                    userService.method1(new User(RANDOM.nextLong(), "苞米豆"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }
        Thread.sleep(3000);
    }

    @Test
    public void spel2Test() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Runnable() {
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
}