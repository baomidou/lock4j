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

package com.baomidou.lock.test.custom;

import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.MethodBasedExpressionEvaluator;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 自定义lock key builder
 *
 * @author zengzhihong
 */
@Component
public class CustomLockKeyBuilder extends DefaultLockKeyBuilder implements Ordered {

    public CustomLockKeyBuilder(MethodBasedExpressionEvaluator expressionEvaluator) {
        super(expressionEvaluator);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
