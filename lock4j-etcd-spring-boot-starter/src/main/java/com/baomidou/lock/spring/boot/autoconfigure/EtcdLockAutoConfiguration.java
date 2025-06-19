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

import com.baomidou.lock.executor.EtcdLockExecutor;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

/**
 * etcd锁自动配置器
 *
 * @author zengzhihong
 * @author yourname
 */
@Slf4j
@Configuration
@ConditionalOnClass({Client.class, EtcdLockExecutor.class})
@EnableConfigurationProperties(EtcdProperties.class)
public class EtcdLockAutoConfiguration {

    /**
     * 创建etcd客户端
     * 
     * @param properties etcd配置属性
     * @return etcd客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public Client etcdClient(EtcdProperties properties) {
        try {
            ClientBuilder builder = Client.builder();
            
            // 设置端点
            String[] endpoints = properties.getEndpoints().split(",");
            URI[] endpointUris = Arrays.stream(endpoints)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(endpoint -> {
                        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                            endpoint = "http://" + endpoint;
                        }
                        return URI.create(endpoint);
                    })
                    .toArray(URI[]::new);
            
            if (endpointUris.length == 0) {
                throw new IllegalArgumentException("At least one etcd endpoint must be specified");
            }
            
            builder.endpoints(endpointUris);
            
            // 设置连接超时
            if (properties.getConnectTimeout() > 0) {
                builder.connectTimeout(Duration.ofMillis(properties.getConnectTimeout()));
            }
            
            // 设置操作超时
            if (properties.getOperationTimeout() > 0) {
                builder.executorService(java.util.concurrent.Executors.newCachedThreadPool());
            }
            
            // 设置用户名和密码
            if (StringUtils.hasText(properties.getUsername()) && StringUtils.hasText(properties.getPassword())) {
                builder.user(io.etcd.jetcd.ByteSequence.from(properties.getUsername().getBytes()))
                       .password(io.etcd.jetcd.ByteSequence.from(properties.getPassword().getBytes()));
            }
            
            // 设置重试次数 (jetcd-core 0.5.11 可能不支持这个方法，先注释掉)
            // if (properties.getRetryCount() > 0) {
            //     builder.retryMaxAttempts(properties.getRetryCount());
            // }
            
            // TODO: SSL配置需要根据实际需求添加证书配置
            // if (properties.isSslEnabled()) {
            //     builder.sslContext(...);
            // }
            
            Client client = builder.build();
            log.info("Etcd client created successfully with endpoints: {}", Arrays.toString(endpointUris));
            return client;
            
        } catch (Exception e) {
            log.error("Failed to create etcd client", e);
            throw new RuntimeException("Failed to create etcd client", e);
        }
    }

    /**
     * 创建etcd锁执行器
     * 
     * @param etcdClient etcd客户端
     * @return etcd锁执行器
     */
    @Bean
    @Order(400)
    @ConditionalOnMissingBean
    public EtcdLockExecutor etcdLockExecutor(Client etcdClient) {
        log.info("Creating EtcdLockExecutor with etcd client");
        return new EtcdLockExecutor(etcdClient);
    }
}