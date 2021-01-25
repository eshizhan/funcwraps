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

import javassist.ClassPool;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AnnotationParser {
    private static final String METHOD_PARAMS_DESC = "(java.lang.reflect.Method,java.lang.Object[],java.lang.Object)";
    private static final String METHOD_PARAMS_DESC_WITH_WRAPPER_PARAMS =
            "(java.lang.reflect.Method,java.lang.Object[],java.lang.Object,java.lang.String[])";
    private static final String METHOD_PARAMS_DESC_BY_COPY = "(java.lang.Object[],java.util.Map)";

    private ClassPool classPool;
    private boolean useCopyWrapper = false;

    private CtMethod wrapperMethod;
    private List<String> wrapperMethodParameters;

    public AnnotationParser(ClassPool classPool, CtMethod methodOrig, boolean useCopyWrapper) {
        this.useCopyWrapper = useCopyWrapper;
        this.classPool = classPool;
        this.wrapperMethod = getWrapperMethod(methodOrig);
    }

    public AnnotationParser(ClassPool classPool, CtMethod methodOrig) {
        this(classPool, methodOrig, false);
    }

    public boolean parsed() {
        return wrapperMethod != null;
    }

    private CtMethod getWrapperMethod(CtMethod methodOrig) {
        CtMethod methodWrapper = null;
        try {
            if (methodOrig.getName().contains(WrapsProcessorConst.WRAPPED_SUFFIX)) {
                throw new RuntimeException("the method has been wrapped");
            }
            Annotation annotation = ((AnnotationsAttribute) methodOrig.getMethodInfo()
                    .getAttribute(AnnotationsAttribute.visibleTag))
                    .getAnnotation(Wraps.class.getName());
            String clazz = ((ClassMemberValue) annotation.getMemberValue("clazz"))
                    .getValue().replace('$', '.');
            String method = ((StringMemberValue) annotation.getMemberValue("method"))
                    .getValue();
            String methodName = decodeMethodMember(method);

            methodWrapper = classPool.getMethod(clazz, methodName);

            if (!useCopyWrapper && !Modifier.isStatic(methodWrapper.getModifiers()))
                throw new RuntimeException("wrapper method must be static");
            String params = Descriptor.toString(methodWrapper.getSignature());
            String assertMethodParamsDesc = !useCopyWrapper ?
                    (wrapperMethodParameters.isEmpty() ? METHOD_PARAMS_DESC : METHOD_PARAMS_DESC_WITH_WRAPPER_PARAMS) :
                    METHOD_PARAMS_DESC_BY_COPY;
            if (!params.equals(assertMethodParamsDesc))
                throw new RuntimeException("wrapper method parameters must be " + assertMethodParamsDesc);
        } catch (RuntimeException | NotFoundException e) {
            System.out.println("skip the method, cause can not get correct wrapper method:");
            e.printStackTrace();
        }
        return methodWrapper;
    }

    private String decodeMethodMember(String method) {
        this.wrapperMethodParameters = new ArrayList<>();
        if (!method.contains("(")) {
            return method;
        } else {
            Matcher matcher = Pattern.compile("(\\b[^()]+)\\((.*)\\)$").matcher(method);
            if (matcher.find()) {
                String methodName = matcher.group(1);
                String params = matcher.group(2);

                Matcher matcherParams = Pattern.compile("([^,]+\\(.+?\\))|([^,]+)").matcher(params);
                while (matcherParams.find())
                    this.wrapperMethodParameters.add(matcherParams.group().trim());

                return methodName;
            } else {
                throw new RuntimeException("annotation element 'method' has wrong format: " + method);
            }
        }
    }

    public CtMethod getWrapperMethod() {
        return wrapperMethod;
    }

    public List<String> getWrapperMethodParameters() {
        return wrapperMethodParameters;
    }
}
