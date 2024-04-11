package com.baomidou.lock.executor;

import com.baomidou.lock.SpelMethodBasedExpressionEvaluator;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;

/**
 * test for {@link SpelMethodBasedExpressionEvaluator}
 *
 * @author huangchengxing
 */
@SpringBootTest(
    properties = "example.service.message=Hello",
    classes = SpelMethodBasedExpressionEvaluator.class
)
class SpelMethodBasedExpressionEvaluatorTest {

    @Autowired
    private SpelMethodBasedExpressionEvaluator spelMethodBasedExpressionEvaluator;
    private static Method method;

    @SneakyThrows
    @BeforeAll
    public static void init() {
        method = SpelMethodBasedExpressionEvaluatorTest.class.getDeclaredMethod(
            "something", String.class, String.class
        );
        Assertions.assertNotNull(method);
    }

    @Test
    void testStringKey() {
        // 测试获取字符串类型的键值
        String str = spelMethodBasedExpressionEvaluator.getValue(
            method, new Object[]{"wo", "rld"}, "'${example.service.message} ' + #a0 + #param2 + ' ' + #root.getName()", String.class
        );
        Assertions.assertEquals(
            "Hello world something", str
        );
    }

    @SneakyThrows
    @Test
    void testBooleanKey() {
        // 测试获取布尔类型的键值
        Assertions.assertTrue(
            spelMethodBasedExpressionEvaluator.getValue(
                method, new Object[]{"wo", "rld"}, "#a0 + #p1 == 'world'", Boolean.class
            )
        );
        Assertions.assertFalse(
            spelMethodBasedExpressionEvaluator.getValue(
                method, new Object[]{"wo", "rld"}, "#param1 == 'world'", Boolean.class
            )
        );
        Assertions.assertTrue(
            spelMethodBasedExpressionEvaluator.getValue(
                method, new Object[]{"tr", "ue"}, "#param1 + #param2", Boolean.class
            )
        );
    }

    private void something(String param1, String param2) {
        // do nothing
    }
}
