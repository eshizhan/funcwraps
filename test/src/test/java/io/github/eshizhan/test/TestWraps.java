/*
 * funcwraps, using annotation for wrapped a method.
 * Copyright (c) 2021 Shi Zhan. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.eshizhan.test;

import io.github.eshizhan.funcwraps.Wraps;

import java.lang.reflect.Method;

public class TestWraps {
    /**
     * @see WrapMethods#wrap(Method, Object[], Object)
     */
    @Wraps(clazz = WrapMethods.class, method = "wrap")
    public String testWrapped(String x, String y) {
        System.out.println("inside wrapped method");
        return x + y;
    }

    /**
     * @see WrapMethods#wrapWithParams(Method, Object[], Object, String[])
     */
    @Wraps(clazz = WrapMethods.class, method = "wrapWithParams(param1, param2)")
    public String testWithParams(String x, String y) {
        System.out.println("inside wrapped method");
        return x + y;
    }

//    @Wraps(clazz = WrapMethods.class, method = "wrapByCopy")
//    public Integer add2(Integer x, Integer y) {
//        System.out.println("inside testWrapped method");
//        return x + y;
//    }
}
