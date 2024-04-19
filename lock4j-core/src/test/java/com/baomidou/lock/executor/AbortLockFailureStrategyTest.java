package com.baomidou.lock.executor;

import com.baomidou.lock.AbortLockFailureStrategy;
import com.baomidou.lock.SpelMethodBasedExpressionEvaluator;
import com.baomidou.lock.annotation.LockWithDefault;
import com.baomidou.lock.exception.LockFailureException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.expression.spel.SpelEvaluationException;

import java.lang.reflect.Method;

/**
 * test for {@link AbortLockFailureStrategy}
 *
 * @author huangchengxing
 */
@SpringBootTest(
    properties = "example.service.message=k3",
    classes = {SpelMethodBasedExpressionEvaluator.class, AbortLockFailureStrategy.class}
)
public class AbortLockFailureStrategyTest {

    @Autowired
    public AbortLockFailureStrategy abortLockFailureStrategy;
    private static Method method;
    private static Method annotatedMethod1;
    private static Method annotatedMethod2;

    @SneakyThrows
    @BeforeAll
    public static void init() {
        method = AbortLockFailureStrategyTest.class.getDeclaredMethod("method", String.class, String.class);
        annotatedMethod1 = AbortLockFailureStrategyTest.class.getDeclaredMethod("annotatedMethod1", String.class, String.class);
        annotatedMethod2 = AbortLockFailureStrategyTest.class.getDeclaredMethod("annotatedMethod2", String.class, String.class);
    }

    @SneakyThrows
    @Test
    void testNonAnnotatedMethod() {
        Assertions.assertThrows(
            LockFailureException.class,
            () -> abortLockFailureStrategy.onLockFailure(
                "test", method, new Object[]{ "param1", "param2"}
            ),
            "request failed, please retry it."
        );
    }

    @SneakyThrows
    @Test
    void testAnnotatedMethod1() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> abortLockFailureStrategy.onLockFailure(
                "test", annotatedMethod1, new Object[]{ "param1", "param2"}
            ),
            "error:param1"
        );
    }

    @SneakyThrows
    @Test
    void testAnnotatedMethod2() {
        Assertions.assertThrows(
            TestException.class,
            () -> abortLockFailureStrategy.onLockFailure(
                "test", annotatedMethod2, new Object[]{ "param1", "param2"}
            ),
            "error"
        );

        abortLockFailureStrategy.setAllowedMakeNonExecutableExpressionsAsString(false);
        Assertions.assertThrows(
            SpelEvaluationException.class,
            () -> abortLockFailureStrategy.onLockFailure(
                "test", annotatedMethod2, new Object[]{ "param1", "param2"}
            )
        );
        abortLockFailureStrategy.setAllowedMakeNonExecutableExpressionsAsString(true);
    }

    public static class TestException extends RuntimeException {
    }

    @AbortLockFailureStrategy.Options(
        lockFailureException = TestException.class,
        lockFailureMessage = "error"
    )
    private void annotatedMethod2(String param1, String param2) {
        // do nothing
    }

    @LockWithDefault(
        lockFailureException = IllegalArgumentException.class,
        lockFailureMessage = "'error:' + #param1"
    )
    private void annotatedMethod1(String param1, String param2) {
        // do nothing
    }
    private void method(String param1, String param2) {
        // do nothing
    }
}
