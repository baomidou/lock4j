package com.baomidou.lock.test.zookeeper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SpringBootZookeeperLockTest
 *
 * @author XS
 */
@Slf4j
@SuppressWarnings("ALL")
@SpringBootTest(classes = SpringBootZookeeperLockTest.class)
@SpringBootApplication
public class SpringBootZookeeperLockTest {
    
    @Autowired
    UserService userService;
    
    public static void main(String[] args) {
        SpringApplication.run(SpringBootZookeeperLockTest.class, args);
    }
    
    @SneakyThrows
    @Test
    void zookeeperLockTest() {
        userService.simple2("zookeeper-key-test");
    }
}
