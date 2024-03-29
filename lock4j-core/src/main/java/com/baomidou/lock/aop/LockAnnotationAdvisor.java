/*
 *  Copyright (c) 2018-2022, baomidou (63976799@qq.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.baomidou.lock.aop;

import com.baomidou.lock.annotation.Lock4j;
import lombok.NonNull;
import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 分布式锁aop通知
 *
 * @author zengzhihong TaoYu
 */
public class LockAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

    private final Lock4jMethodInterceptor advice;

    private final Pointcut pointcut = new ComposablePointcut(new AnnotationMethodPoint(Lock4j.class))
        .union(new AnnotationMethodPoint(Lock4j.List.class));

    public LockAnnotationAdvisor(@NonNull Lock4jMethodInterceptor lockInterceptor, int order) {
        this.advice = lockInterceptor;
        setOrder(order);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (this.advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
        }
    }

    private static class AnnotationMethodPoint implements Pointcut {

        private final Class<? extends Annotation> annotationType;

        public AnnotationMethodPoint(Class<? extends Annotation> annotationType) {
            Assert.notNull(annotationType, "Annotation type must not be null");
            this.annotationType = annotationType;
        }

        @Override
        public ClassFilter getClassFilter() {
            return ClassFilter.TRUE;
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return new AnnotationMethodMatcher(annotationType);
        }

        private static class AnnotationMethodMatcher extends StaticMethodMatcher {
            private final Class<? extends Annotation> annotationType;

            public AnnotationMethodMatcher(Class<? extends Annotation> annotationType) {
                this.annotationType = annotationType;
            }

            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                if (matchesMethod(method)) {
                    return true;
                }
                // Proxy classes never have annotations on their redeclared methods.
                if (Proxy.isProxyClass(targetClass)) {
                    return false;
                }
                // The method may be on an interface, so let's check on the target class as well.
                Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
                return (specificMethod != method && matchesMethod(specificMethod));
            }

            private boolean matchesMethod(Method method) {
                return AnnotatedElementUtils.hasAnnotation(method, this.annotationType);
            }
        }
    }

}