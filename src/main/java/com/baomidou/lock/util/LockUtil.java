package com.baomidou.lock.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;

/**
 * 分布式锁工具类
 *
 * @author zengzhihong TaoYu
 */
public class LockUtil {

    /**
     * 获取本机网卡地址
     *
     * @return macAddress
     */
    public static String getLocalMAC() {
        String localMac;
        try {
            InetAddress ia = InetAddress.getLocalHost();
            // 获得网络接口对象（即网卡），并得到mac地址，mac地址存在于一个byte数组中。
            byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
            // 下面代码是把mac地址拼装成String
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mac.length; i++) {
                if (i != 0) {
                    sb.append("-");
                }
                // mac[i] & 0xFF 是为了把byte转化为正整数
                String s = Integer.toHexString(mac[i] & 0xFF);
                sb.append(s.length() == 1 ? 0 + s : s);
            }
            // 把字符串所有小写字母改为大写成为正规的mac地址并返回
            localMac = sb.toString();
        } catch (Exception e) {
            //TODO 获取mac地址应该要做到最大化兼容
            localMac = UUID.randomUUID().toString();
        }
        return localMac.toUpperCase().replace("-", "");
    }

    /**
     * 获取jvmPId
     *
     * @return jvmPid
     */
    public static String getJvmPid() {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        int indexOf = pid.indexOf('@');
        if (indexOf > 0) {
            pid = pid.substring(0, indexOf);
            return pid;
        }
        throw new IllegalStateException("ManagementFactory error");
    }
}
