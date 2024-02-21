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

package com.baomidou.lock.spring.boot.autoconfigure;

import com.baomidou.lock.condition.ZookeeperCondition;
import com.baomidou.lock.executor.ZookeeperLockExecutor;
import lombok.Data;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.Order;

/**
 * Zookeeper锁自动配置器
 *
 * @author zengzhihong
 */
@Conditional(ZookeeperCondition.class)
@EnableConfigurationProperties(ZookeeperLockProperties.class)
@Data
class ZookeeperLockAutoConfiguration {

    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnMissingBean(CuratorFramework.class)
    public CuratorFramework curatorFramework(ZookeeperLockProperties zookeeperLockProperties) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(zookeeperLockProperties.getBaseSleepTimeMs(), zookeeperLockProperties.getMaxRetries());
        return CuratorFrameworkFactory.builder()
                .connectString(zookeeperLockProperties.getZkServers())
                .sessionTimeoutMs(zookeeperLockProperties.getSessionTimeout())
                .connectionTimeoutMs(zookeeperLockProperties.getConnectionTimeout())
                .retryPolicy(retryPolicy)
                .namespace(zookeeperLockProperties.getNamespace())
                .build();
    }

    @Bean
    @Order(300)
    public ZookeeperLockExecutor zookeeperLockExecutor(ZookeeperLockProperties zookeeperLockProperties, CuratorFramework curatorFramework) {
        return new ZookeeperLockExecutor(curatorFramework, zookeeperLockProperties);
    }
}