package com.baomidou.lock.aop;

import com.baomidou.lock.LockFailureStrategy;
import com.baomidou.lock.LockKeyBuilder;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 锁拦截器抽象类，用于提供关于配置信息的解析的基本实现
 *
 * @author huangchengxing
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractLockOpsInterceptor
    implements InitializingBean, ApplicationContextAware, Lock4jMethodInterceptor {

    /**
     * 空占位符
     */
    private static final LockOps NULL = new NullLockOps();

    /**
     * 锁操作信息缓存
     */
    private final Map<Method, LockOps> lockOpsCaches = new ConcurrentReferenceHashMap<>(16);

    /**
     * Spring上下文
     */
    @Setter
    protected ApplicationContext applicationContext;

    /**
     * 配置
     */
    protected final Lock4jProperties lock4jProperties;

    /**
     * 默认的锁操作配置
     */
    private LockOps defaultLockOps;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getThis();
        if (Objects.isNull(target)) {
            return invocation.proceed();
        }
        Class<?> cls = AopProxyUtils.ultimateTargetClass(target);
        if (!cls.equals(invocation.getThis().getClass())) {
            return invocation.proceed();
        }
        LockOps lockOps = lockOpsCaches.computeIfAbsent(
            invocation.getMethod(),
            method -> Optional.ofNullable(resolveLockOps(invocation)).orElse(NULL)
        );
        return lockOps == NULL ?
            invocation.proceed() : doLock(invocation, lockOps);
    }

    @Override
    public void afterPropertiesSet() {
        this.defaultLockOps = initDefaultLockOps();
        Assert.notNull(applicationContext, "ApplicationContext must not be null");
    }

    /**
     * 获取锁操作信息
     *
     * @param invocation 方法调用
     * @return 锁操作信息，若不存在则返回null
     * @see #createLockOps
     */
    @Nullable
    protected LockOps resolveLockOps(MethodInvocation invocation) {
        Lock4j annotation = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), Lock4j.class);
        return Objects.nonNull(annotation) ? createLockOps(annotation) : null;
    }

    /**
     * 获取锁操作信息
     *
     * @param annotation 注解
     * @return 锁操作信息
     */
    protected LockOps createLockOps(Lock4j annotation) {
        // TODO 支持根据 beanName 获取相应组件
        // 获取key构建器，若未在注解中指定，则遵循默认的全局配置
        LockKeyBuilder keyBuilder = Optional.ofNullable(annotation.keyBuilderStrategy())
            .filter(type -> !Objects.equals(type, LockKeyBuilder.class))
            .map(applicationContext::getBeansOfType)
            .map(Map::values)
            .flatMap(components -> components.stream().min(AnnotationAwareOrderComparator.INSTANCE))
            .map(LockKeyBuilder.class::cast)
            .orElse(defaultLockOps.getLockKeyBuilder());
        // 获取失败回调策略，若未在注解中指定，则遵循默认的全局配置
        LockFailureStrategy failureStrategy = Optional.ofNullable(annotation.failStrategy())
            .filter(type -> !Objects.equals(type, LockFailureStrategy.class))
            .map(applicationContext::getBeansOfType)
            .map(Map::values)
            .flatMap(components -> components.stream().min(AnnotationAwareOrderComparator.INSTANCE))
            .map(LockFailureStrategy.class::cast)
            .orElse(defaultLockOps.getLockFailureStrategy());
        return LockOpsImpl.builder()
            .annotation(annotation)
            .lockKeyBuilder(keyBuilder)
            .lockFailureStrategy(failureStrategy)
            .build();
    }


    /**
     * 初始化默认的锁操作配置
     *
     * @return 默认的锁操作配置
     */
    @NonNull
    protected LockOps initDefaultLockOps() {
        LockKeyBuilder lockKeyBuilder = obtainComponent(LockKeyBuilder.class, lock4jProperties.getPrimaryKeyBuilder());
        LockFailureStrategy lockFailureStrategy = obtainComponent(LockFailureStrategy.class, lock4jProperties.getPrimaryFailureStrategy());
        return LockOpsImpl.builder()
            .lockKeyBuilder(lockKeyBuilder)
            .lockFailureStrategy(lockFailureStrategy)
            .build();
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
     * @param invocation 方法调用
     * @param lockOps 锁操作
     * @return 锁信息
     * @throws Throwable 调用异常
     */
    protected abstract Object doLock(MethodInvocation invocation, LockOps lockOps) throws Throwable;

    /**
     * 锁操作
     *
     * @author huangchengxing
     * @see #createLockOps
     */
    protected interface LockOps extends Ordered {

        /**
         * 获取锁注解
         *
         * @return 注解
         */
        Lock4j getAnnotation();

        /**
         * 获取锁构建器
         *
         * @return 构建器
         */
        LockKeyBuilder getLockKeyBuilder();

        /**
         * 获取锁失败处理策略
         *
         * @return 策略
         */
        LockFailureStrategy getLockFailureStrategy();

        /**
         * 获取顺序
         *
         * @return 顺序值
         */
        @Override
        default int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }

    /**
     * 空操作，用于占位
     */
    private static class NullLockOps implements LockOps {
        @Override
        public Lock4j getAnnotation() {
            return null;
        }
        @Override
        public LockKeyBuilder getLockKeyBuilder() {
            return null;
        }
        @Override
        public LockFailureStrategy getLockFailureStrategy() {
            return null;
        }
    }

    /**
     * 默认的锁操作实现
     */
    @Getter
    @SuperBuilder
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    protected static class LockOpsImpl implements LockOps {

        /**
         * 锁注解
         */
        @EqualsAndHashCode.Include
        private final Lock4j annotation;

        /**
         * key生成器
         */
        private LockKeyBuilder lockKeyBuilder;

        /**
         * 锁失败策略
         */
        private LockFailureStrategy lockFailureStrategy;

        /**
         * 顺序
         */
        @Builder.Default
        private int order = Ordered.LOWEST_PRECEDENCE;
    }
}
