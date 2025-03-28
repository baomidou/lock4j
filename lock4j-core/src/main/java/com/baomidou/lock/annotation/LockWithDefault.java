package com.baomidou.lock.annotation;

import com.baomidou.lock.AbortLockFailureStrategy;
import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.LockKeyBuilder;
import com.baomidou.lock.exception.LockFailureException;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 基于默认配置进行加锁
 *
 * @author huangchengxing
 * @see Lock4j
 * @see AbortLockFailureStrategy.Options
 */
@AbortLockFailureStrategy.Options
@Lock4j(failStrategy = AbortLockFailureStrategy.class)
@Target(value = { ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface LockWithDefault {

    /**
     * 应用条件表达式，当执行结果为{@code true}或{@code 'true'}时，才会执行锁操作
     *
     * @return 名称
     */
    String condition() default "";

    /**
     * 用于多个方法锁同一把锁 可以理解为锁资源名称 为空则会使用 包名+类名+方法名
     *
     * @return 名称
     */
    String name() default "";

    /**
     * @return lock 执行器
     */
    Class<? extends LockExecutor> executor() default LockExecutor.class;

    /**
     * support SPEL expresion 锁的key = name + keys
     *
     * @return KEY
     */
    String[] keys() default "";

    /**
     * @return 过期时间 单位：毫秒
     * 
     *         <pre>
     *     过期时间一定是要长于业务的执行时间. 未设置则为默认时间30秒 默认值：{@link Lock4jProperties#expire}
     * </pre>
     */
    long expire() default -1;

    /**
     * @return 获取锁超时时间 单位：毫秒
     * 
     *         <pre>
     *     结合业务,建议该时间不宜设置过长,特别在并发高的情况下. 未设置则为默认时间3秒 默认值：{@link Lock4jProperties#acquireTimeout}
     * </pre>
     */
    long acquireTimeout() default -1;

    /**
     * 业务方法执行完后（方法内抛异常也算执行完）自动释放锁，如果为false，锁将不会自动释放直至到达过期时间才释放
     * {@link com.baomidou.lock.annotation.Lock4j#expire()}
     *
     * @return 是否自动释放锁
     */
    boolean autoRelease() default true;

    /**
     * key生成器策略，默认使用{@link DefaultLockKeyBuilder}
     *
     * @return LockKeyBuilder
     */
    Class<? extends LockKeyBuilder> keyBuilderStrategy() default DefaultLockKeyBuilder.class;

    /**
     * 获取顺序，值越小越先执行
     *
     * @return 顺序值
     */
    int order() default Ordered.LOWEST_PRECEDENCE;

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
     * @return String
     * @see AbortLockFailureStrategy.Options#lockFailureMessage
     */
    @AliasFor(annotation = AbortLockFailureStrategy.Options.class, attribute = "lockFailureMessage")
    String lockFailureMessage() default AbortLockFailureStrategy.DEFAULT_EXCEPTION_MESSAGE;

    /**
     * <p>
     * 获取锁失败时，抛出的异常。
     * </p>
     * <p>
     * 异常类必须提供一个无参构造函数，或者提供一个接受{@code String}类型参数的构造函数。
     * </p>
     *
     * @return String
     * @see AbortLockFailureStrategy.Options#lockFailureException
     */
    @AliasFor(annotation = AbortLockFailureStrategy.Options.class, attribute = "lockFailureException")
    Class<? extends Exception> lockFailureException() default LockFailureException.class;
}
