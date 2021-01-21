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

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class WrapsProcessor {
    private final String WRAPPED_SUFFIX = "$funcwraps$wrapped";
    private final String WRAPPER_SUFFIX = "$funcwraps$wrapper";
    private Path classPath;
    private ClassPool classPool;
    private boolean isCopyWrapper;

    public WrapsProcessor(Path classPath) throws NotFoundException {
        this.classPath = classPath;
        this.classPool = ClassPool.getDefault();
        if (classPath != null)
            this.classPool.appendClassPath(classPath.toString());
        this.isCopyWrapper = Boolean.parseBoolean(System.getProperty("copy-wrapper"));
    }

    public CtClass getClass(String className) throws NotFoundException {
        return classPool.get(className);
    }

    public void process() throws NotFoundException, IOException, ClassNotFoundException, CannotCompileException {
        int processed = 0;

        for (String className : findAllClasses()) {
            CtClass ctClass = classPool.get(className);
            for (CtMethod methodOrig : findAnnotationMethod(Wraps.class, className)) {
                CtMethod wrapperMethod = getWrapperMethod(methodOrig);
                if (wrapperMethod != null) {
                    System.out.println("transform: " + methodOrig);

                    CtMethod methodNew;
                    if (!isCopyWrapper)
                        methodNew = makeBridgeMethod(ctClass, methodOrig, wrapperMethod);
                    else
                        methodNew = makeBridgeMethodByCopy(ctClass, methodOrig, wrapperMethod);

                    ctClass.addMethod(methodNew);

                    processed++;
                }
            }
            if (ctClass.isModified())
                ctClass.writeFile(classPath.toString());
        }
        System.out.println("total processed methods: " + processed);
    }

    private List<String> findAllClasses() throws IOException {
        int prefixLength = classPath.toString().length() + 1;
        List<String> allClasses = Files.find(classPath, Integer.MAX_VALUE, (path, attr) ->
                attr.isRegularFile() && path.toString().endsWith(".class"))
                .map(path -> {
                    String s = path.toString();
                    return s.substring(prefixLength, s.length() - 6);
                })
                .map(s -> s.replaceAll("[\\\\/]", "."))
                .collect(Collectors.toList());
        return allClasses;
    }

    public List<CtMethod> findAnnotationMethod(Class<?> annotationClass, String className) throws IOException {
        List<CtMethod> ret = new ArrayList<>();
        try {
            CtClass ctClass = classPool.get(className);
            for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
                if (ctMethod.hasAnnotation(annotationClass))
                    ret.add(ctMethod);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public CtMethod getWrapperMethod(CtMethod methodOrig) {
        final String methodParamsDesc = "(java.lang.Object,java.lang.reflect.Method,java.lang.Object[])";
        final String methodParamsDescByCopy = "(java.lang.Object[],java.util.Map)";
        CtMethod methodWrapper = null;
        try {
            if (methodOrig.getName().contains(WRAPPED_SUFFIX)) {
                throw new RuntimeException("the method has been wrapped");
            }
            Annotation annotation = ((AnnotationsAttribute) methodOrig.getMethodInfo()
                                     .getAttribute(AnnotationsAttribute.visibleTag))
                                     .getAnnotation(Wraps.class.getName());
            String clazz = annotation.getMemberValue("clazz").toString();
            clazz = clazz.substring(0, clazz.length() - 6);
            String method = annotation.getMemberValue("method").toString();
            method = method.substring(1, method.length() - 1);

            methodWrapper = classPool.getMethod(clazz, method);
            String params = Descriptor.toString(methodWrapper.getSignature());
            if (!isCopyWrapper) {
                if (!Modifier.isStatic(methodWrapper.getModifiers())) {
                    throw new RuntimeException("wrapper method must be static");
                }
                if (!params.equals(methodParamsDesc)) {
                    throw new RuntimeException("wrapper method parameters must be " + methodParamsDesc);
                }
            } else {
                if (!params.equals(methodParamsDescByCopy)) {
                    throw new RuntimeException("wrapper method parameters must be " + methodParamsDescByCopy);
                }
            }
        } catch (RuntimeException | NotFoundException e) {
            System.out.println("skip the method, cause can not get correct wrapper method:");
            e.printStackTrace();
        }
        return methodWrapper;
    }

    public CtMethod makeBridgeMethod(CtClass ctClass, CtMethod methodOrig, CtMethod methodWrapper) throws CannotCompileException, NotFoundException {
        String methodWrapperFullName = methodWrapper.getDeclaringClass().getName() + "." + methodWrapper.getName();
        CtMethod methodNew = CtNewMethod.copy(methodOrig, ctClass, null);

        String methodOrigName = methodOrig.getName();
        String methodOrigRename = methodOrigName + WRAPPED_SUFFIX;
        methodOrig.setName(methodOrigRename);
//        MethodInfo methodInfoOrig = methodOrig.getMethodInfo();
//        methodInfoOrig.setAccessFlags(AccessFlag.setPrivate(methodInfoOrig.getAccessFlags()));
//        methodInfoOrig.addAttribute(new SyntheticAttribute(ctClass.getClassFile2().getConstPool()));

        String paramClasses = Arrays.stream(methodOrig.getParameterTypes())
                                    .map(t -> t.getName() + ".class")
                                    .collect(Collectors.joining(", "));
        StringBuffer body = new StringBuffer();
        body.append("{\njava.lang.reflect.Method method = $0.getClass().getDeclaredMethod(\"");
        body.append(methodOrigRename + "\", new java.lang.Class[] {" + paramClasses + "});\n");
        body.append("return ($r)" + methodWrapperFullName + "($0, method, $args);\n}");

//        System.out.println(body.toString());
        methodNew.setBody(body.toString());
        return methodNew;
    }

    public CtMethod makeBridgeMethodByCopy(CtClass ctClass, CtMethod methodOrig, CtMethod methodWrapper) throws CannotCompileException, NotFoundException {
        final String markerClassName = "io.github.eshizhan.funcwraps.ProceedMarker";
        final String markerMethodName = "proceed";

        CtMethod methodNew = CtNewMethod.copy(methodOrig, ctClass, null);
        CtMethod methodWrapperCopy = CtNewMethod.copy(methodWrapper, ctClass, null);

        String methodOrigName = methodOrig.getName();
        String methodOrigRename = methodOrigName + WRAPPED_SUFFIX;
        methodOrig.setName(methodOrigRename);
//        MethodInfo methodInfoOrig = methodOrig.getMethodInfo();
//        methodInfoOrig.setAccessFlags(AccessFlag.setPrivate(methodInfoOrig.getAccessFlags()));
//        methodInfoOrig.addAttribute(new SyntheticAttribute(ctClass.getClassFile2().getConstPool()));

        List<String> params = getWrappedMethodParams(methodNew, methodWrapperCopy);
        String paramsString = String.join(", ", params);
//        System.out.println("paramsString: " + paramsString);

        methodWrapperCopy.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals(markerClassName) && m.getMethodName().equals(markerMethodName))
                    m.replace("{ $_ = " + methodOrigRename + "(" + paramsString + "); }");
            }
        });

        String methodWrappedName = methodOrigName + WRAPPER_SUFFIX;
        methodWrapperCopy.setName(methodWrappedName);
        ctClass.addMethod(methodWrapperCopy);

        String fullName = methodNew.getName() + Descriptor.toString(methodNew.getSignature());
        String returnType = methodNew.getReturnType().getName();
        StringBuffer body = new StringBuffer();
        body.append("{\njava.util.Map/*<String, Object>*/ methodInfo = new java.util.HashMap/*<>*/();\n");
        body.append("methodInfo.put(\"this\", $0);\n");
        body.append("methodInfo.put(\"returnType\", \"" + returnType + "\");\n");
        body.append("methodInfo.put(\"methodDesc\", \"" + fullName + "\");\n");
        body.append("return ($r)" + methodWrappedName + "($args, methodInfo);\n}");

//        System.out.println(body.toString());
        methodNew.setBody(body.toString());
        return methodNew;
    }

    private List<String> getWrappedMethodParams(CtMethod methodNew, CtMethod methodWrapped) throws NotFoundException {
        CodeAttribute codeAttribute = methodWrapped.getMethodInfo().getCodeAttribute();
        LocalVariableAttribute localVarTable = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
//        MethodParametersAttribute methodParamsAttr = (MethodParametersAttribute) methodWrapped.getMethodInfo().getAttribute(MethodParametersAttribute.tag);
        CtClass[] parameterTypes = methodNew.getParameterTypes();
//        System.out.println("###### localVarTable: " + localVarTable);
//        System.out.println("###### methodParamsAttr: " + methodParamsAttr);

        List<String> params = new ArrayList<>();
        if (localVarTable != null) {
            int offset = 0;
            int modifiers = methodWrapped.getModifiers();
            if (Modifier.isSynchronized(modifiers))
                offset++;
            if (!Modifier.isStatic(modifiers))
                offset++;
            int idx = 0;
            for (int i = 0; i < localVarTable.tableLength(); i++) {
                if (localVarTable.index(i) == offset) {
                    idx = i;
                    break;
                }
            }
            String argsParamName = localVarTable.variableName(idx);

            for (int i = 0; i < parameterTypes.length; i++) {
                params.add(String.format("(%s) %s[%s]", parameterTypes[i].getName(), argsParamName, i));
            }
        }
//        else if (methodParamsAttr != null) {
//            ConstPool constPool = methodParamsAttr.getConstPool();
//            for (int i = 0; i < parameterTypes.length; i++) {
//                params.add(constPool.getUtf8Info(methodParamsAttr.name(i)));
//            }
//        }
        return params;
    }

    public boolean isCopyWrapper() {
        return isCopyWrapper;
    }

}