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

package com.baomidou.lock.spring.boot.autoconfigure;

import com.baomidou.lock.condition.ZookeeperCondition;
import com.baomidou.lock.executor.ZookeeperLockExecutor;
import lombok.Data;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.Order;

/**
 * Zookeeper锁自动配置器
 *
 * @author zengzhihong
 */
@Conditional(ZookeeperCondition.class)
@ConfigurationProperties(prefix = "spring.coordinate.zookeeper")
@Data
class ZookeeperLockAutoConfiguration {

    private String zkServers;

    private int sessionTimeout = 30000;

    private int connectionTimeout = 5000;

    private int baseSleepTimeMs = 1000;

    private int maxRetries = 3;

    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnMissingBean(CuratorFramework.class)
    public CuratorFramework curatorFramework() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(this.baseSleepTimeMs, this.maxRetries);
        return CuratorFrameworkFactory.builder()
                .connectString(this.zkServers)
                .sessionTimeoutMs(this.sessionTimeout)
                .connectionTimeoutMs(this.connectionTimeout)
                .retryPolicy(retryPolicy)
                .build();
    }

    @Bean
    @Order(300)
    public ZookeeperLockExecutor zookeeperLockExecutor(CuratorFramework curatorFramework) {
        return new ZookeeperLockExecutor(curatorFramework);
    }
}