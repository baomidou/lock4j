package com.baomidou.lock.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * zookeeper注入条件判断类
 *
 * @author zengzhihong
 */
public class ZookeeperCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        return conditionContext.getEnvironment().containsProperty("spring.coordinate.zookeeper.zkServers");
    }
}
