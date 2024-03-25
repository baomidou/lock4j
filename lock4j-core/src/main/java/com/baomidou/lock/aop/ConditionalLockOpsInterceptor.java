package com.baomidou.lock.aop;

import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;
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
public class ConditionalLockOpsInterceptor
    extends LockOpsInterceptor implements EmbeddedValueResolverAware {

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
     * 创建一个新的{@link ConditionalLockOpsInterceptor}实例。
     *
     * @param lock4jProperties       锁配置
     * @param lockTemplate           锁模板
     */
    public ConditionalLockOpsInterceptor(
        Lock4jProperties lock4jProperties, LockTemplate lockTemplate) {
        this(lock4jProperties, lockTemplate, new SpelExpressionParser(), new DefaultParameterNameDiscoverer());
    }

    /**
     * 创建一个新的{@link ConditionalLockOpsInterceptor}实例。
     *
     * @param lock4jProperties       锁配置
     * @param lockTemplate           锁模板
     * @param expressionParser       表达式解析器
     * @param parameterNameDiscoverer 参数名发现器
     */
    public ConditionalLockOpsInterceptor(
        Lock4jProperties lock4jProperties, LockTemplate lockTemplate,
        @NonNull ExpressionParser expressionParser, @NonNull ParameterNameDiscoverer parameterNameDiscoverer) {
        super(lock4jProperties, lockTemplate);
        this.expressionParser = expressionParser;
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

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
        return StringUtils.hasText(condition) ?
            new ConditionalLockOps(delegate, condition) : delegate;
    }

    /**
     * 进行加锁
     *
     * @param invocation 方法调用
     * @param lockOps    锁操作
     * @return 锁信息
     * @throws Throwable 调用异常
     */
    @Override
    protected Object doLock(MethodInvocation invocation, LockOps lockOps) throws Throwable {
        if (!(lockOps instanceof ConditionalLockOps)) {
            return super.doLock(invocation, lockOps);
        }
        String condition = ((ConditionalLockOps) lockOps).getCondition();
        condition = embeddedValueResolver.resolveStringValue(condition);
        if (!evaluateCondition(condition, lockOps, invocation)) {
            return invocation.proceed();
        }
        return super.doLock(invocation, lockOps);
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
     *     <li>{@code #arg0, #arg1...}：方法的调用参数；</li>
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
        // 注册参数，格式为#arg0, #arg1...
        for (int i = 0; i < arguments.length; i++) {
            context.setVariable("arg" + i, arguments[i]);
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
    @RequiredArgsConstructor
    protected static class ConditionalLockOps implements LockOps {
        @Delegate
        private final LockOps delegate;
        private final String condition;
    }
}
