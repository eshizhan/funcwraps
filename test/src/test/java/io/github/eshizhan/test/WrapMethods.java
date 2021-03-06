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

import io.github.eshizhan.funcwraps.ProceedMarker;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class WrapMethods {
    public static Object wrap(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("### start");
        String test = "#start";
        Object ret = method.invoke(target, args);
        test += ret;
        System.out.println("### end");
        test += "#end";
        return test;
    }

    public static Object wrapWithParams(Method method, Object[] args, Object target, String[] wrapParams) throws Throwable {
        System.out.println("### start");
        String test = "#start";
        Object ret = method.invoke(target, args);
        test += ret;
        System.out.println("### end");
        test += "#end#";
        test += Arrays.toString(wrapParams);
        return test;
    }

    public Object wrapCopy(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("### start");
        String test = "#start";
        String ret = ProceedMarker.proceed();
        test += ret;
        System.out.println("### end");
        test += "#end";
        return test;
    }

    public Object wrapWithCopyAndParams(Method method, Object[] args, Object target, String[] wrapParams) throws Throwable {
        System.out.println("### start");
        String test = "#start";
        String ret = ProceedMarker.proceed();
        test += ret;
        System.out.println("### end");
        test += "#end#";
        test += Arrays.toString(wrapParams);
        return test;
    }
}
