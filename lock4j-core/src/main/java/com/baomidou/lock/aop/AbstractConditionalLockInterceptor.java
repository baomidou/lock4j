package com.baomidou.lock.aop;

import com.baomidou.lock.MethodBasedExpressionEvaluator;
import com.baomidou.lock.annotation.Lock4j;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.StringUtils;

/**
 * 支持条件表达式的{@link LockOpsInterceptor}扩展类
 *
 * @author huangchengxing
 */
@RequiredArgsConstructor
public abstract class AbstractConditionalLockInterceptor extends AbstractLockInterceptor {

    /**
     * 方法调用表达式执行器
     */
    private final MethodBasedExpressionEvaluator methodBasedExpressionEvaluator;

    /**
     * 获取锁操作信息
     *
     * @param annotation 注解
     * @return 锁操作信息
     */
    @Override
    protected LockOps createLockOps(Lock4j annotation) {
        LockOps delegate = super.createLockOps(annotation);
        String condition = annotation.condition();
        if (!StringUtils.hasText(condition)) {
            return delegate;
        }
        return new ConditionalLockOps(delegate, condition);
    }

    /**
     * 执行解析条件表达式
     *
     * @param condition 条件表达式
     * @param invocation 方法调用
     * @return 是否满足条件
     */
    protected final boolean evaluateCondition(String condition, MethodInvocation invocation) {
        Boolean result = methodBasedExpressionEvaluator.getValue(
            invocation.getMethod(), invocation.getArguments(), condition, Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 携带有条件表达式的锁操作信息
     *
     * @author huangchengxing
     */
    @Getter
    protected class ConditionalLockOps extends AbstractLockOpsDelegate {
        private final String condition;
        public ConditionalLockOps(LockOps delegate, String condition) {
            super(delegate);
            this.condition = condition;
        }
        @Override
        public MethodInvocation attach(MethodInvocation invocation) {
            return evaluateCondition(condition, invocation) ?
                delegate.attach(invocation) : invocation;
        }
    }
}
