package com.baomidou.lock.test.custom;

import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.MethodBasedExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomLockKeyBuilder2 extends DefaultLockKeyBuilder implements Ordered {
    public CustomLockKeyBuilder2(MethodBasedExpressionEvaluator expressionEvaluator) {
        super(expressionEvaluator);
    }

    @Override
    public String buildKey(MethodInvocation invocation, String[] definitionKeys) {
        log.info("不同的key生成器2,invocation={},definitionKeys={}", invocation, definitionKeys);
        if (definitionKeys.length > 1 || !"".equals(definitionKeys[0])) {
            return getSpelDefinitionKey(definitionKeys, invocation);
        }
        return "";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
