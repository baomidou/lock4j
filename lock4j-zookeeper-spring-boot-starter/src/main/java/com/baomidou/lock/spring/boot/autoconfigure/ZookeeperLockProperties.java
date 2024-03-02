package com.baomidou.lock.spring.boot.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.coordinate.zookeeper")
public class ZookeeperLockProperties {
    
    private String zkServers;
    
    private int sessionTimeout = 30000;
    
    private int connectionTimeout = 5000;
    
    private int baseSleepTimeMs = 1000;
    
    private int maxRetries = 3;
    
    /**
     * Zookeeper Namespace
     */
    private String namespace;
    
    /**
     * Full path = namespace + basePath + lockKey
     */
    private String basePath = "/curator/lock4j";
}
