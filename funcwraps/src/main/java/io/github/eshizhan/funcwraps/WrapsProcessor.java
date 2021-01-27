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
import javassist.bytecode.*;
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
    private Path classPath;
    private ClassPool classPool;

    public WrapsProcessor(ClassPool classPool) throws NotFoundException {
        this(classPool, null);
    }

    public WrapsProcessor(Path classPath) throws NotFoundException {
        this(null, classPath);
    }

    public WrapsProcessor(ClassPool classPool, Path classPath) throws NotFoundException {
        this.classPool = classPool != null ? classPool : ClassPool.getDefault();
        this.classPath = classPath;
        if (classPath != null)
            this.classPool.appendClassPath(classPath.toString());
    }

    public void processClassPath() throws NotFoundException, IOException, ClassNotFoundException, CannotCompileException, BadBytecode {
        if (classPath == null)
            throw new RuntimeException("classPath not set with constructor");
        int processed = 0;

        for (String className : findAllClasses()) {
            CtClass ctClass = classPool.get(className);
            for (CtMethod methodOrig : findAnnotationMethod(Wraps.class, className)) {
                AnnotationParser annotationParser = new AnnotationParser(classPool, methodOrig);
                if (annotationParser.parsed()) {
                    System.out.println("transform: " + methodOrig);

                    CtMethod methodNew;
                    if (!annotationParser.isCopyToTarget())
                        methodNew = makeBridgeMethod(ctClass, methodOrig, annotationParser);
                    else
                        methodNew = makeBridgeMethodByCopy(ctClass, methodOrig, annotationParser);

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

    public CtMethod makeBridgeMethod(CtClass ctClass, CtMethod methodOrig, AnnotationParser annotationParser)
            throws CannotCompileException, NotFoundException {
        CtMethod methodWrapper = annotationParser.getWrapperMethod();
        String methodWrapperFullName = methodWrapper.getDeclaringClass().getName() + "." + methodWrapper.getName();
        CtMethod methodNew = CtNewMethod.copy(methodOrig, ctClass, null);
        SyntheticAttribute syntheticAttribute = new SyntheticAttribute(ctClass.getClassFile2().getConstPool());

        String methodOrigName = methodOrig.getName();
        String methodOrigRename = methodOrigName + WrapsProcessorConst.WRAPPED_SUFFIX;
        methodOrig.setName(methodOrigRename);

        MethodInfo methodInfoOrig = methodOrig.getMethodInfo();
        methodInfoOrig.setAccessFlags(AccessFlag.setPrivate(methodInfoOrig.getAccessFlags()));
        methodInfoOrig.addAttribute(syntheticAttribute);

        String paramClasses = Arrays.stream(methodOrig.getParameterTypes())
                                    .map(t -> t.getName() + ".class")
                                    .collect(Collectors.joining(", "));
        String methodFieldName = methodOrigRename + WrapsProcessorConst.REFLECT_SUFFIX + methodInfoOrig.getLineNumber(0);
        List<String> wrapperMethodParameters = annotationParser.getWrapperMethodParameters();

        StringBuffer sbField = new StringBuffer();
        sbField.append("private static java.lang.reflect.Method ").append(methodFieldName).append(" = ")
               .append(ctClass.getName()).append(".class.getDeclaredMethod(\"").append(methodOrigRename)
               .append("\", new java.lang.Class[] {").append(paramClasses).append("});\n");
//        System.out.println(sbField.toString());
        CtField field = CtField.make(sbField.toString(), ctClass);
        field.getFieldInfo().addAttribute(syntheticAttribute);
        ctClass.addField(field);

        StringBuffer sbBody = new StringBuffer();
        sbBody.append("{\nif (!").append(methodFieldName).append(".isAccessible()) {")
              .append(methodFieldName).append(".setAccessible(true);}")
              .append("return ($r)").append(methodWrapperFullName)
              .append("(").append(methodFieldName).append(", $args, $0");
        if (!wrapperMethodParameters.isEmpty()) {
            sbBody.append(", new java.lang.String[] {\"")
                  .append(String.join("\",\"", wrapperMethodParameters))
                  .append("\"}");
        }
        sbBody.append(");\n}");
//        System.out.println(sbBody.toString());
        methodNew.setBody(sbBody.toString());
        return methodNew;
    }

    public CtMethod makeBridgeMethodByCopy(CtClass ctClass, CtMethod methodOrig, AnnotationParser annotationParser)
            throws CannotCompileException, NotFoundException, BadBytecode {
        final String markerClassName = "io.github.eshizhan.funcwraps.ProceedMarker";
        final String markerMethodName = "proceed";

        CtMethod methodWrapper = annotationParser.getWrapperMethod();
        CtMethod methodNew = CtNewMethod.copy(methodOrig, ctClass, null);
        CtMethod methodWrapperCopy = CtNewMethod.copy(methodWrapper, ctClass, null);
        SyntheticAttribute syntheticAttribute = new SyntheticAttribute(ctClass.getClassFile2().getConstPool());

        String methodOrigName = methodOrig.getName();
        String methodOrigRename = methodOrigName + WrapsProcessorConst.WRAPPED_SUFFIX;
        methodOrig.setName(methodOrigRename);

        MethodInfo methodInfoOrig = methodOrig.getMethodInfo();
        methodInfoOrig.setAccessFlags(AccessFlag.setPrivate(methodInfoOrig.getAccessFlags()));
        methodInfoOrig.addAttribute(syntheticAttribute);

        List<String> wrapperMethodParameters = annotationParser.getWrapperMethodParameters();
        List<String> params = getWrappedMethodParams(methodOrig, methodWrapperCopy);
        String paramsString = String.join(", ", params);
//        System.out.println("paramsString: " + paramsString);

        methodWrapperCopy.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals(markerClassName) && m.getMethodName().equals(markerMethodName))
                    m.replace("{ $_ = " + methodOrigRename + "(" + paramsString + "); }");
            }
        });
        String methodWrappedName = methodOrigName + WrapsProcessorConst.WRAPPER_SUFFIX;
        methodWrapperCopy.setName(methodWrappedName);
        ctClass.addMethod(methodWrapperCopy);

        MethodInfo methodInfoWrapperCopy = methodWrapperCopy.getMethodInfo();
        methodInfoWrapperCopy.setAccessFlags(AccessFlag.setPrivate(methodInfoWrapperCopy.getAccessFlags()));
        methodInfoWrapperCopy.addAttribute(syntheticAttribute);

        StringBuffer body = new StringBuffer();
        body.append("{\n");
        body.append("return ($r)" + methodWrappedName + "(null, $args, $0");
        if (!wrapperMethodParameters.isEmpty()) {
            body.append(", new java.lang.String[] {\"")
                .append(String.join("\",\"", wrapperMethodParameters))
                .append("\"}");
        }
        body.append(");\n}");
//        System.out.println(body.toString());
        methodNew.setBody(body.toString());
        return methodNew;
    }

    private List<String> getWrappedMethodParams(CtMethod methodOrig, CtMethod methodWrapped) throws NotFoundException {
        CodeAttribute codeAttribute = methodWrapped.getMethodInfo().getCodeAttribute();
        LocalVariableAttribute localVarTable = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
//        MethodParametersAttribute methodParamsAttr = (MethodParametersAttribute) methodWrapped.getMethodInfo().getAttribute(MethodParametersAttribute.tag);
        CtClass[] parameterTypes = methodOrig.getParameterTypes();
//        System.out.println("###### localVarTable: " + localVarTable);
//        System.out.println("###### methodParamsAttr: " + methodParamsAttr);

        List<String> params = new ArrayList<>();
        if (localVarTable != null) {
            int modifiers = methodWrapped.getModifiers();
            int index = Modifier.isStatic(modifiers) ? 0 : 1;
            // get second argument
            index += 1;

            String argsParamName = "args";
            for (int i = 0; i < localVarTable.tableLength(); i++) {
                if (localVarTable.index(i) == index) {
                    argsParamName = localVarTable.variableName(i);
                    break;
                }
            }

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
}
