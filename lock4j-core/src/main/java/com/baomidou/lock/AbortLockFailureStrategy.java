package com.baomidou.lock;

import com.baomidou.lock.exception.LockFailureException;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 当加锁失败时，抛出异常以终止方法执行。
 * </p>
 * <p>
 * 该策略可以通过{@link Options}注解来指定当失败时要抛出的异常和异常信息。
 * </p>
 *
 * @author huangchengxing
 * @see Options
 */
@RequiredArgsConstructor
public class AbortLockFailureStrategy implements LockFailureStrategy {

    /**
     * 在SpEL表达式中可通过该参数名引用获取失败的锁键值
     */
    private static final String KEY_NAME = "key";

    /**
     * 默认的错误信息，与{@link DefaultLockFailureStrategy}保持一致
     */
    public static final String DEFAULT_EXCEPTION_MESSAGE = "request failed, please retry it.";

    /**
     * 默认的异常处理器，即直接抛出{@link LockFailureException}异常
     */
    private final FailureHandler defaultExceptionFactory = new FailureHandler();

    /**
     * 异常处理器缓存
     */
    private final Map<Method, FailureHandler> exceptionHandlerCaches = new ConcurrentReferenceHashMap<>(8);

    /**
     * 表达式执行器
     */
    private final MethodBasedExpressionEvaluator methodBasedExpressionEvaluator;

    /**
     * 是否允许将不可执行的表达式作为字符串使用
     */
    @Setter
    private boolean allowedMakeNonExecutableExpressionsAsString = true;

    /**
     * 当加锁失败时的处理策略
     *
     * @param key       用于获取锁的key
     * @param method    方法
     * @param arguments 方法参数
     */
    @Override
    public void onLockFailure(String key, Method method, Object[] arguments) throws Exception {
        exceptionHandlerCaches.computeIfAbsent(method, this::resolveExceptionHandler)
                .handle(key, method, arguments);
    }

    /**
     * 获取该方法对应的失败处理器。
     *
     * @param method 方法
     * @return 失败处理器
     */
    @NonNull
    protected FailureHandler resolveExceptionHandler(Method method) {
        Options options = AnnotatedElementUtils.findMergedAnnotation(method, Options.class);
        return Objects.isNull(options) ? defaultExceptionFactory
                : new FailureMessageFailureHandler(options.lockFailureException(), options.lockFailureMessage());
    }

    /**
     * 获取异常信息
     *
     * @param key        获取失败的锁的key
     * @param method     方法
     * @param arguments  调用参数
     * @param expression 用于获取异常信息的表达式
     * @return 异常信息
     */
    @Nullable
    protected final String resolveMessage(
            String key, Method method, Object[] arguments, String expression) {
        // 若未指定错误信息，则返回默认的异常信息
        if (!StringUtils.hasText(expression)) {
            return DEFAULT_EXCEPTION_MESSAGE;
        }
        try {
            return methodBasedExpressionEvaluator.getValue(
                    method, arguments, expression, String.class, Collections.singletonMap(KEY_NAME, key));
        } catch (Exception ex) {
            if (allowedMakeNonExecutableExpressionsAsString) {
                return expression;
            }
            throw ex;
        }
    }

    /**
     * 失败处理器
     *
     * @author huangchengxing
     */
    protected static class FailureHandler {
        public void handle(String key, Method method, Object[] arguments) throws Exception {
            throw new LockFailureException(DEFAULT_EXCEPTION_MESSAGE);
        }
    }

    /**
     * 当用户在方法上配置了{@link Options}注解时使用的失败处理器，
     * 将根据用户配置抛出携带有指定信息的异常。
     *
     * @author huangchengxing
     */
    @RequiredArgsConstructor
    private class FailureMessageFailureHandler extends FailureHandler {

        private final Class<? extends Exception> exceptionType;
        private final String messageTemplate;
        @SuppressWarnings("java:S3077")
        private volatile Constructor<? extends Exception> constructor;

        @Override
        public void handle(String key, Method method, Object[] arguments) throws Exception {
            String message = resolveMessage(key, method, arguments, messageTemplate);
            throw newExceptionInstance(message);
        }

        private Exception newExceptionInstance(String message) {
            if (constructor == null) {
                synchronized (this) {
                    if (constructor == null) {
                        constructor = determineConstructor();
                    }
                }
            }
            try {
                return constructor.getParameterCount() > 0 ? constructor.newInstance(message)
                        : constructor.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("创建异常实例失败，指定的异常类型："
                        + constructor.getDeclaringClass().getName() + "是否可以被实例化？", e);
            }
        }

        private Constructor<? extends Exception> determineConstructor() {
            // 异常类需要保证至少有一个可用的构造器
            Constructor<? extends Exception> constr = ClassUtils.getConstructorIfAvailable(exceptionType, String.class);
            if (Objects.isNull(constr)) {
                constr = ClassUtils.getConstructorIfAvailable(exceptionType);
            }
            Assert.notNull(constr, "异常类型[" + exceptionType.getName() +
                    "]必须有一个无参构造函数，或有有一个接受String类型参数的造函数");
            if (!constr.isAccessible()) {
                ReflectionUtils.makeAccessible(constr);
            }
            return constr;
        }
    }

    /**
     * 当加锁失败时，要抛出的异常信息
     *
     * @author huangchengxing
     */
    @Target(value = { ElementType.METHOD, ElementType.ANNOTATION_TYPE })
    @Retention(value = RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    public @interface Options {

        /**
         * <p>
         * 抛出异常的错误消息。
         *
         * <p>
         * 错误信息支持 SpEL 表达式，你可以在表达式中引用上下文参数：
         * <ul>
         * <li>通过{@code #key} 引用获取失败的锁的key；</li>
         * <li>通过{@code #root} 引用方法对象；</li>
         * <li>通过{@code #参数名}或{@code #p参数下标}引用方法的调用参数；</li>
         * </ul>
         *
         * @return 错误消息
         */
        String lockFailureMessage() default DEFAULT_EXCEPTION_MESSAGE;

        /**
         * <p>
         * 获取锁失败时，抛出的异常。
         * </p>
         * <p>
         * 异常类必须提供一个无参构造函数，或者提供一个接受{@code String}类型参数的构造函数。
         * </p>
         *
         * @return 异常类型
         */
        Class<? extends Exception> lockFailureException() default LockFailureException.class;
    }
}
