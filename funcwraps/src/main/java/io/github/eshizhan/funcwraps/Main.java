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

import javassist.NotFoundException;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Throwable {
        System.out.println("transform class by funcwraps");
        Path classPath = Paths.get(args[0]);

        WrapsProcessor wrapsProcessor = new WrapsProcessor(classPath);
        wrapsProcessor.process();
    }

    /**
     * java -javaagent:funcwraps/target/funcwraps-1.0-SNAPSHOT.jar -cp XXX XXX
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            inst.addTransformer(new WrapsTransformer("io.github.eshizhan.test"), true);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}