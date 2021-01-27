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

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MainTest
{
    @Test
    public void testWraps()
    {
        System.out.println("starting testWraps");
        TestWraps testWraps = new TestWraps();
        String exp = "#start#s1#s2#end";
        String ret = testWraps.testWrapped("#s1", "#s2");
        System.out.println(ret);
        assertTrue(exp.equals(ret));
    }

    @Test
    public void testWrapsWithParams()
    {
        System.out.println("starting testWrapsWithParams");
        TestWraps testWraps = new TestWraps();
        String exp = "#start#s1#s2#end#[param1, param2]";
        String ret = testWraps.testWithParams("#s1", "#s2");
        System.out.println(ret);
        assertTrue(exp.equals(ret));
    }

    @Test
    public void testWrapsWithCopy() throws Throwable {
        System.out.println("starting testWrapsWithCopy");
        TestWraps testWraps = new TestWraps();
        String exp = "#start#s1#s2#end";
        String ret = testWraps.testWithCopy("#s1", "#s2");
        System.out.println(ret);
        assertTrue(exp.equals(ret));
    }

    @Test
    public void testWrapsWithCopyAndParams()
    {
        System.out.println("starting testWrapsWithCopy");
        TestWraps testWraps = new TestWraps();
        String exp = "#start#s1#s2#end#[param1, param2]";
        String ret = testWraps.testWithCopyAndParams("#s1", "#s2");
        System.out.println(ret);
        assertTrue(exp.equals(ret));
    }

    /**
     * for testing java agent
     */
    public static void main(String[] args) throws Throwable {
        new MainTest().testWraps();
        new MainTest().testWrapsWithParams();
        new MainTest().testWrapsWithCopy();
        new MainTest().testWrapsWithCopyAndParams();
    }
}
