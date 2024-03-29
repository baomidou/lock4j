package com.baomidou.lock.aop;

import com.baomidou.lock.annotation.Lock4j;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 支持条件表达式的{@link LockOpsInterceptor}扩展类
 *
 * @author huangchengxing
 */
@RequiredArgsConstructor
public abstract class AbstractConditionalLockInterceptor
    extends AbstractLockInterceptor implements EmbeddedValueResolverAware {

    private static final MapAccessor MAP_ACCESSOR = new MapAccessor();
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentReferenceHashMap<>(32);

    /**
     * 表达式解析器
     */
    private final ExpressionParser expressionParser;

    /**
     * 参数名发现器
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer;

    /**
     * 值解析器
     */
    @Setter
    private StringValueResolver embeddedValueResolver;

    /**
     * Bean解析器
     */
    private BeanResolver beanResolver;

    /**
     * Spring上下文
     *
     * @param applicationContext Spring上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
        this.beanResolver = new BeanFactoryResolver(applicationContext);
    }

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
        condition = embeddedValueResolver.resolveStringValue(condition);
        return new ConditionalLockOps(delegate, condition);
    }

    /**
     * 执行解析条件表达式
     *
     * @param condition 条件表达式
     * @param lockOps   锁操作
     * @param invocation 方法调用
     * @return 是否满足条件
     */
    protected final boolean evaluateCondition(String condition, LockOps lockOps, MethodInvocation invocation) {
        // TODO 将此部分逻辑提取至公共组件 MethodBasedExpressionEvaluator，DefaultLockKeyBuilder 亦同
        Expression expression = EXPRESSION_CACHE.computeIfAbsent(
            condition, key -> expressionParser.parseExpression(condition)
        );
        StandardEvaluationContext context = createEvaluationContext(invocation, lockOps);
        return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
    }

    /**
     * 创建表达式上下文，默认情况下，将会注册下述变量：
     * <ul>
     *     <li>{@code rootObject}：{@link MethodInvocation}对象；</li>
     *     <li>{@code #p0, #p1...}：方法的调用参数；</li>
     *     <li>{@code #参数名1, #参数名2...}：方法的调用参数；</li>
     * </ul>
     *
     * @param invocation 方法调用
     * @param lockOps   锁操作
     * @return 表达式上下文
     */
    @SuppressWarnings("unused")
    protected StandardEvaluationContext createEvaluationContext(
        MethodInvocation invocation, LockOps lockOps) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(beanResolver);
        context.setRootObject(invocation);
        context.addPropertyAccessor(MAP_ACCESSOR);
        registerInvocationArguments(invocation.getMethod(), invocation.getArguments(), context);
        return context;
    }

    private void registerInvocationArguments(
        Method method, Object[] arguments, StandardEvaluationContext context) {
        if (ObjectUtils.isEmpty(arguments)) {
            return;
        }
        // 注册参数，格式为#p1, #p2...
        for (int i = 0; i < arguments.length; i++) {
            context.setVariable("p" + i, arguments[i]);
        }
        // 注册参数，格式为#参数名1, #参数名2...
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (!ObjectUtils.isEmpty(parameterNames)) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], arguments[i]);
            }
        }
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
            return evaluateCondition(condition, delegate, invocation) ?
                delegate.attach(invocation) : invocation;
        }
    }
}
