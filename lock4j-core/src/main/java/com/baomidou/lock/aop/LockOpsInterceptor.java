package com.baomidou.lock.aop;

import com.baomidou.lock.LockFailureStrategy;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockKeyBuilder;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;

/**
 * 基于{@link Lock4j}注解的锁操作拦截器
 *
 * @author huangchengxing
 */
@Slf4j
public class LockOpsInterceptor extends AbstractConditionalLockChainInterceptor {

    private final LockTemplate lockTemplate;
    protected final Lock4jProperties lock4jProperties;

    public LockOpsInterceptor(
        ExpressionParser expressionParser, ParameterNameDiscoverer parameterNameDiscoverer,
        LockTemplate lockTemplate, Lock4jProperties lock4jProperties) {
        super(expressionParser, parameterNameDiscoverer);
        this.lockTemplate = lockTemplate;
        this.lock4jProperties = lock4jProperties;
    }

    public LockOpsInterceptor(
        Lock4jProperties lock4jProperties, LockTemplate lockTemplate) {
        this(new SpelExpressionParser(), new DefaultParameterNameDiscoverer(), lockTemplate, lock4jProperties);
    }

    /**
     * 初始化默认的锁操作配置
     *
     * @return 默认的锁操作配置
     */
    @NonNull
    @Override
    protected LockOps initDefaultLockOps() {
        LockKeyBuilder lockKeyBuilder = obtainComponent(LockKeyBuilder.class, lock4jProperties.getPrimaryKeyBuilder());
        LockFailureStrategy lockFailureStrategy = obtainComponent(LockFailureStrategy.class, lock4jProperties.getPrimaryFailureStrategy());
        return new LockOpsImpl(null)
            .setLockKeyBuilder(lockKeyBuilder)
            .setLockFailureStrategy(lockFailureStrategy);
    }

    private <C> C obtainComponent(Class<C> type, @Nullable Class<? extends C> defaultType) {
        // TODO 支持根据 beanName 获取相应组件
        if (Objects.nonNull(defaultType)) {
            return applicationContext.getBean(defaultType);
        }
        Collection<C> components = applicationContext.getBeansOfType(type).values();
        return components.stream()
            .min(AnnotationAwareOrderComparator.INSTANCE)
            .orElseThrow(() -> new IllegalArgumentException("No component of type " + type.getName() + " found"));
    }

    /**
     * 进行加锁
     *
     * @param lockOps    锁操作
     * @param invocation 方法调用
     * @return 锁信息
     * @throws Throwable 调用异常
     */
    @Override
    protected Object doLock(LockOps lockOps, MethodInvocation invocation) throws Throwable {
        Lock4j annotation = lockOps.getAnnotation();
        LockInfo lockInfo = null;
        try {
            String key = resolveKey(invocation, lockOps);
            lockInfo = lockTemplate.lock(key, annotation.expire(), annotation.acquireTimeout(), annotation.executor());
            if (Objects.nonNull(lockInfo)) {
                log.debug("Lock success, lockKey={}, lockValue={}", lockInfo.getLockKey(), lockInfo.getLockValue());
                return invocation.proceed();
            }
            log.debug("Lock failure, lockKey={}", key);
            // lock failure
            lockOps.getLockFailureStrategy()
                .onLockFailure(key, invocation.getMethod(), invocation.getArguments());
            return null;
        } finally {
            doUnlock(lockInfo, annotation);
        }
    }

    private void doUnlock(@Nullable LockInfo lockInfo, Lock4j annotation) {
        if (Objects.isNull(lockInfo) || annotation.autoRelease()) {
            return;
        }
        final boolean releaseLock = lockTemplate.releaseLock(lockInfo);
        if (!releaseLock) {
            log.error("Release lock fail, lockKey={}, lockValue={}", lockInfo.getLockKey(),
                lockInfo.getLockValue());
        }
        log.debug("Release lock success, lockKey={}, lockValue={}", lockInfo.getLockKey(),
            lockInfo.getLockValue());
    }

    private String resolveKey(MethodInvocation invocation, LockOps lockOps) {
        String prefix = lock4jProperties.getLockKeyPrefix() + ":";
        Method method = invocation.getMethod();
        Lock4j annotation = lockOps.getAnnotation();
        prefix += StringUtils.hasText(annotation.name()) ? annotation.name() :
            method.getDeclaringClass().getName() + method.getName();
        String key = prefix + "#" + lockOps.getLockKeyBuilder().buildKey(invocation, annotation.keys());
        if (log.isDebugEnabled()) {
            log.debug("generate lock key [{}] for invocation of [{}]", key, method);
        }
        return key;
    }
}
