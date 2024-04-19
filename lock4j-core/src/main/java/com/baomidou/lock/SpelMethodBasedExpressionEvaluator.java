package com.baomidou.lock;

import lombok.Setter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * <p>基于方法调用的SpEL表达式计算器，支持在表达式中通过下述方式引用相关参数：
 * <ul>
 *     <li>#p0, #p1...：按照参数顺序引用调用；</li>
 *     <li>#a0, #a1...：按照参数顺序引用调用；</li>
 *     <li>#参数名1, #参数名2...：按照参数名引用调用参数；</li>
 *     <li>#root：引用方法对象；</li>
 *     <li>@beanName：引用Spring容器中的Bean；</li>
 * </ul>
 *
 * @author huangchengxing
 * @see MethodBasedEvaluationContext
 * @see BeanFactoryResolver
 */
public class SpelMethodBasedExpressionEvaluator
    implements MethodBasedExpressionEvaluator, EmbeddedValueResolverAware, BeanFactoryAware {

    private static final MapAccessor MAP_ACCESSOR = new MapAccessor();
    private final Map<String, Expression> expressionCache = new ConcurrentReferenceHashMap<>(16);
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private BeanResolver beanResolver;
    @Setter
    private StringValueResolver embeddedValueResolver;

    /**
     * 执行表达式，返回执行结果
     *
     * @param method 方法
     * @param arguments 调用参数
     * @param expression 表达式
     * @param resultType 返回值类型
     * @param variables 表达式中的变量
     * @return 表达式执行结果
     */
    @Override
    public <T> T getValue(
        Method method, Object[] arguments, String expression, Class<T> resultType, @NonNull Map<String, Object> variables) {
        EvaluationContext context = createEvaluationContext(method, arguments);
        if (!variables.isEmpty()) {
            variables.forEach(context::setVariable);
        }
        Expression exp = parseExpression(expression, expressionParser);
        return exp.getValue(context, resultType);
    }

    protected EvaluationContext createEvaluationContext(Method method, Object[] args) {
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(method, method, args, parameterNameDiscoverer);
        context.setBeanResolver(beanResolver);
        context.addPropertyAccessor(MAP_ACCESSOR);
        return context;
    }

    /**
     * 解析表达式
     *
     * @param expression 表达式
     * @param parser 表达式解析器
     * @return 表达式对象
     */
    protected Expression parseExpression(String expression, ExpressionParser parser) {
        return expressionCache.computeIfAbsent(expression, exp -> {
            exp = embeddedValueResolver.resolveStringValue(exp);
            Assert.notNull(exp, "Expression must not be null: " + exp);
            return parser.parseExpression(exp);
        });
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        beanResolver = new BeanFactoryResolver(beanFactory);
    }
}
