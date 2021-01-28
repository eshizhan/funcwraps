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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LRUCacheWrapper {
    private static final Map<String, Map<String, Object>> methodMap = new ConcurrentHashMap<>();

    public static Map<String, Object> remove(Class<?> wrapperClass, String methodName) {
        String s = wrapperClass.getName() + "." + methodName;
        return methodMap.remove(s);
    }

    /**
     * @param wrapParams    wrapParams[0] is max size of cache.
     */
    public static Object wrap(Method method, Object[] args, Object target, String[] wrapParams) throws Throwable {
        String methodName = method.getName();
        methodName = methodName.substring(0, methodName.lastIndexOf(WrapsProcessorConst.WRAPPED_SUFFIX));
        String methodKey = method.getDeclaringClass().getName() + "." + methodName;
        String argsKey = Arrays.toString(args);

        Object cacheResult = getFromCache(methodKey, argsKey, wrapParams);
        if (cacheResult != null)
            return cacheResult;
//        System.out.println("### start");
        Object ret = method.invoke(target, args);
        if (ret != null) {
            addToCache(methodKey, argsKey, ret);
        }
//        System.out.println("### end");
        return ret;
    }

    private static void addToCache(String methodKey, String argsKey, Object ret) {
        Map<String, Object> results = methodMap.get(methodKey);
        results.put(argsKey, ret);
    }

    private static Object getFromCache(String methodKey, String argsKey, String[] wrapParams) {
        int maxEntries = Integer.parseUnsignedInt(wrapParams[0]);
        Object result = null;
        if (methodMap.containsKey(methodKey)) {
            Map<String, Object> results = methodMap.get(methodKey);
            if (results.containsKey(argsKey))
                result = results.get(argsKey);
        } else {
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(maxEntries, 0.75f, true) {
                private static final long serialVersionUID = 5685245246488195173L;

                @Override
                protected boolean removeEldestEntry(Map.Entry entry) {
                    return size() > maxEntries;
                }
            };
            methodMap.put(methodKey, Collections.synchronizedMap(map));
        }
        return result;
    }
}
