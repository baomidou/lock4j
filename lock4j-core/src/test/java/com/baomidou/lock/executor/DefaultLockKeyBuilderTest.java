package com.baomidou.lock.executor;

import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.SpelMethodBasedExpressionEvaluator;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * test for {@link DefaultLockKeyBuilder}
 *
 * @author huangchengxing
 */
@SpringBootTest(
    properties = "example.service.message=k3",
    classes = {SpelMethodBasedExpressionEvaluator.class, DefaultLockKeyBuilder.class}
)
class DefaultLockKeyBuilderTest {

    @Autowired
    private DefaultLockKeyBuilder defaultLockKeyBuilder;

    @SneakyThrows
    @Test
    void test() {
        Method method = DefaultLockKeyBuilderTest.class.getDeclaredMethod(
            "something", String.class, String.class);
        TestInvocation invocation = new TestInvocation(
            new Object(), method, new Object[]{ "k1", "k2"}
        );

        Assertions.assertEquals("", defaultLockKeyBuilder.buildKey(invocation, new String[]{}));
        Assertions.assertEquals(
            "k1.k2.k3",
            defaultLockKeyBuilder.buildKey(invocation, new String[]{ "#param1", "#param2", "'${example.service.message}'"})
        );
    }


    private void something(String param1, String param2) {
        // do nothing
    }

    private static class TestInvocation extends ReflectiveMethodInvocation {
        @SneakyThrows
        public TestInvocation(Object target, Method method, Object[] arguments) {
            super(
                new Object(), target, method, arguments,
                DefaultLockKeyBuilderTest.class, Collections.emptyList()
            );
        }
    }
}
