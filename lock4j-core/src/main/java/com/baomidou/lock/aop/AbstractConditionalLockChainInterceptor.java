package com.baomidou.lock.aop;

import com.baomidou.lock.MethodBasedExpressionEvaluator;
import com.baomidou.lock.annotation.Lock4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>支持多个{@link Lock4j}注解的锁操作拦截器，
 * 当执行时，将会遵循{@link Lock4j}注解的依次进行加锁和解锁操作。<br/>
 * 例如：<br/>
 * <pre>
 *     &#064;Lock4j(key = "key1", order = 0)
 *     &#064;Lock4j(key = "key2, order = 1)
 *     &#064;Lock4j(key = "key3, order = 2)
 *     public void method() {
 *      // do something
 *     }
 * </pre>
 * 当执行时，将依次加锁key1、key2、key3，执行完毕后依次解锁key1、key2、key3。
 *
 * <p><strong>中断</strong>
 * <p>多级锁操作链将根据某一环节是否未获取到锁而决定是否中断，若中断则会依次解锁已加锁的锁。
 * 比如，若获取key3锁时失败，则会调用失败处理策略，随后解锁key2，与key1。
 *
 * <p><strong>条件</strong>
 * <p>多级锁的条件是彼此独立的，
 * 比如，若Key2的表达式执行结果为true时，其他表达式执行结果为false，此时将会正常获取Key1与Key2的锁。
 * 同理，若Key2的表达式为false，Key3的表达式为true，此时将会正常获取Key1与Key3的锁
 *
 * @author huangchengxing
 */
public abstract class AbstractConditionalLockChainInterceptor extends AbstractConditionalLockInterceptor {

    /**
     * 创建一个新的{@link AbstractConditionalLockInterceptor}实例。
     *
     * @param methodBasedExpressionEvaluator 方法调用表达式执行器
     */
    protected AbstractConditionalLockChainInterceptor(
        MethodBasedExpressionEvaluator methodBasedExpressionEvaluator) {
        super(methodBasedExpressionEvaluator);
    }

    /**
     * 获取锁操作信息
     *
     * @param invocation 方法调用
     * @return 锁操作信息，若不存在则返回null
     * @see #createLockOps
     */
    @Nullable
    @Override
    protected LockOps resolveLockOps(MethodInvocation invocation) {
        Set<LockOps> ops = AnnotatedElementUtils.findMergedRepeatableAnnotations(invocation.getMethod(), Lock4j.class).stream()
            .map(this::createLockOps)
            .sorted(AnnotationAwareOrderComparator.INSTANCE)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(ops)) {
            return null;
        }
        if (ops.size() == 1) {
            return ops.iterator().next();
        }
        return getMultiLock(ops);
    }

    /**
     * 获取由多个{@link LockOps}组成的联锁
     *
     * @param ops 锁操作
     * @return 锁操作链
     */
    protected LockOps getMultiLock(Set<LockOps> ops) {
        Iterator<LockOps> iterator = ops.iterator();
        LockOpsChain head = new LockOpsChain(iterator.next());
        LockOpsChain current = head;
        while (iterator.hasNext()) {
            current.next = new LockOpsChain(iterator.next());
            current = current.next;
        }
        return head;
    }

    /**
     * 由多个{@link LockOps}组成的链
     */
    protected static class LockOpsChain extends AbstractLockOpsDelegate {
        @Nullable
        private LockOpsChain next;
        public LockOpsChain(LockOps delegate) {
            super(delegate);
        }
        @Override
        public MethodInvocation attach(MethodInvocation invocation) {
            return Objects.isNull(next) ?
                delegate.attach(invocation) :
                delegate.attach(next.attach(invocation));
        }
    }
}
