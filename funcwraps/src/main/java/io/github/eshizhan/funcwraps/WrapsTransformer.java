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
import javassist.bytecode.BadBytecode;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class WrapsTransformer implements ClassFileTransformer {
    private final ClassPool classPool;
    private final WrapsProcessor wrapsProcessor;
    private final String packageName;

    public WrapsTransformer(String packageName) throws NotFoundException {
        this.classPool = ClassPool.getDefault();
        this.wrapsProcessor = new WrapsProcessor(this.classPool);
        this.packageName = packageName;
        // generate modify class file
//        CtClass.debugDump = "./transform_debug_dump";
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String classNameParam, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        String className = classNameParam.replaceAll("/", ".");
        if (!className.startsWith(packageName))
            return null;
        try {
            CtClass ctClass = classPool.getCtClass(className);
            for (CtMethod methodOrig : wrapsProcessor.findAnnotationMethod(Wraps.class, className)) {

                AnnotationParser annotationParser = new AnnotationParser(classPool, methodOrig);
                if (annotationParser.parsed()) {
                    System.out.println("transform: " + methodOrig);

                    CtMethod methodNew;
                    if (!annotationParser.isCopyToTarget())
                        methodNew = wrapsProcessor.makeBridgeMethod(ctClass, methodOrig, annotationParser);
                    else
                        methodNew = wrapsProcessor.makeBridgeMethodByCopy(ctClass, methodOrig, annotationParser);

                    ctClass.addMethod(methodNew);
                }
            }
            if (ctClass.isModified()){
                byte[] bytecode = ctClass.toBytecode();
                // clean avoid OOM
                ctClass.detach();
                return bytecode;
            }
        } catch (NotFoundException | IOException | CannotCompileException | BadBytecode e) {
            e.printStackTrace();
        }
        return null;
    }
}
