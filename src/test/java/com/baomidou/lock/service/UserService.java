package com.baomidou.lock.service;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private int counter = 1;

    @Lock4j
    public void simple1() {
        System.out.println("执行简单方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
    }

    @Lock4j(keys = "#myKey")
    public void simple2(String myKey) {
        System.out.println("执行简单方法2 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));

    }

    @Lock4j(keys = "#user.id")
    public User method1(User user) {
        System.out.println("执行spel方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        return user;
    }

    @Lock4j(keys = {"#user.id", "#user.name"}, acquireTimeout = 5000, expire = 5000)
    public User method2(User user) {
        System.out.println("执行spel方法2 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        //模拟锁占用
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return user;
    }

}