package com.baomidou.lock.util;

public class LockUtilTest {

    public static void main(String[] args) {
        System.out.println("当前JVM Process ID: " + LockUtil.getJvmPid());
        System.out.println("当前机器MAC地址: " + LockUtil.getLocalMAC());
    }
}
