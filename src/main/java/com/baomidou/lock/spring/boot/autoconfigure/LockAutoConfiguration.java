package com.baomidou.lock.spring.boot.autoconfigure;

import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.aop.LockAnnotationAdvisor;
import com.baomidou.lock.aop.LockInterceptor;
import com.baomidou.lock.condition.ZookeeperCondition;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.executor.LockExecutorFactory;
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
import org.springframework.context.annotation.Configuration;

/**
 * 分布式锁自动配置器
 *
 * @author zengzhihong TaoYu
 */
@Configuration
public class LockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LockTemplate lockTemplate(LockExecutorFactory lockExecutorFactory) {
        return new LockTemplate(lockExecutorFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockAnnotationAdvisor lockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        return new LockAnnotationAdvisor(lockInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockInterceptor lockInterceptor(LockTemplate lockTemplate) {
        return new LockInterceptor(lockTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockExecutorFactory lockExecutorFactory() {
        return new LockExecutorFactory();
    }


    @Conditional(ZookeeperCondition.class)
    @ConfigurationProperties(prefix = "spring.coordinate.zookeeper")
    @Data
    public static class CoordinateConfiguration {

        private String zkServers;

        private int sessionTimeout = 30000;

        private int connectionTimeout = 5000;

        private int baseSleepTimeMs = 1000;

        private int maxRetries = 3;

        @Bean(destroyMethod = "close")
        public CuratorFramework curatorFramework() {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(this.baseSleepTimeMs, this.maxRetries);
            CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                    .connectString(this.zkServers)
                    .sessionTimeoutMs(this.sessionTimeout)
                    .connectionTimeoutMs(this.connectionTimeout)
                    .retryPolicy(retryPolicy)
                    .build();
            curatorFramework.start();
            return curatorFramework;
        }

        @Bean
        public LockExecutor zookeeperLockExecutor(CuratorFramework curatorFramework) {
            return new ZookeeperLockExecutor(curatorFramework);
        }
    }
}
