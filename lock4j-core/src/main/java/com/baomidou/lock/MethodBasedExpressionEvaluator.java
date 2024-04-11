package com.baomidou.lock;

import java.lang.reflect.Method;

/**
 * 基于方法调用的表达式计算器
 *
 * @author huangchengxing
 */
public interface MethodBasedExpressionEvaluator {

    /**
     * 执行表达式，返回执行结果
     *
     * @param method 方法
     * @param arguments 调用参数
     * @param expression 表达式
     * @param resultType 返回值类型
     * @return 表达式执行结果
     */
    <T> T getValue(Method method, Object[] arguments, String expression, Class<T> resultType);
}
