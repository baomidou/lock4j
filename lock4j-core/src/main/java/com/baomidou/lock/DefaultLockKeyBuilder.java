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

package com.baomidou.lock;

import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 分布式锁Key生成器
 *
 * @author zengzhihong TaoYu
 */
@RequiredArgsConstructor
public class DefaultLockKeyBuilder implements LockKeyBuilder {

    private final MethodBasedExpressionEvaluator methodBasedExpressionEvaluator;
    private static final String EMPTY_KEY = "";

    @Override
    public String buildKey(MethodInvocation invocation, String[] definitionKeys) {
        return ObjectUtils.isEmpty(definitionKeys) ?
            EMPTY_KEY : getSpelDefinitionKey(definitionKeys,invocation);
    }

    protected String getSpelDefinitionKey(String[] definitionKeys, MethodInvocation invocation) {
        Method method = invocation.getMethod();
        Object[] arguments = invocation.getArguments();
        return Stream.of(definitionKeys)
            .filter(StringUtils::hasText)
            .map(k -> methodBasedExpressionEvaluator.getValue(method, arguments, k, String.class))
            .collect(Collectors.joining("."));
    }
}
