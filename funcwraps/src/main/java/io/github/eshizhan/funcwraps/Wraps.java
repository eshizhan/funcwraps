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

package io.github.eshizhan.funcwraps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For example, add annotation on wrapped method.
 * <pre>
 * &#064;Wraps(clazz = WrapMethods.class, method = "wrap")
 * public String testWrapped(String x, String y) {
 *     System.out.println("inside wrapped method");
 *     return x + y;
 * }
 * </pre>
 *
 * <p> Writing wrapper as following.
 * <pre>
 * public class WrapMethods {
 *     public static Object wrap(Method method, Object[] args, Object target) throws Throwable {
 *         System.out.println("### start");
 *         // Calling wrapped method
 *         Object ret = method.invoke(target, args);
 *         System.out.println("### end");
 *         return ret;
 *     }
 * }
 * </pre>
 *
 * <p> If you want passing arguments to wrapper method, you can do this:
 * <pre>
 * &#064;Wraps(clazz = WrapMethods.class, method = "wrapWithParams(param1, param2)")
 * public String testWithParams(String x, String y) {
 *     System.out.println("inside wrapped method");
 *     return x + y;
 * }
 * </pre>
 *
 * <p>The arguments will passing as `String[]` type.
 * <pre>
 * public static Object wrapWithParams(Method method, Object[] args, Object target, String[] wrapParams) throws Throwable {
 *     System.out.println("### start");
 *     Object ret = method.invoke(target, args);
 *     System.out.println("### end");
 *     // wrapParams = ["param1", "param2"]
 *     System.out.println(Arrays.toString(wrapParams));
 *     return test;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Wraps {
    Class<?> clazz();
    String method();
}
