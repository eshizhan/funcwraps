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

import io.github.eshizhan.funcwraps.LRUCacheWrapper;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainTest
{
    @Test
    public void testWraps()
    {
        System.out.println("starting testWraps");
        TestWraps testWraps = new TestWraps();
        String ret = testWraps.test();
        System.out.println(ret);
        assertTrue("#start#s1#end".equals(ret));

        ret = testWraps.testWrapped("#s1", "#s2");
        System.out.println(ret);
        assertTrue("#start#s1#s2#end".equals(ret));
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

    @Test
    public void testLRUCacheWrapper()
    {
        System.out.println("starting testLRUCacheWrapper");
        TestWraps testWraps = new TestWraps();
        String ret1 = testWraps.testLRUCacheWrapper("#s1");
        String ret2 = testWraps.testLRUCacheWrapper("#s1");
        System.out.println(ret1);
        System.out.println(ret2);
        assertTrue(ret1.equals(ret2));

        LRUCacheWrapper.remove(testWraps.getClass(), "testLRUCacheWrapper");
        String ret3 = testWraps.testLRUCacheWrapper("#s1");
        System.out.println(ret3);
        assertFalse(ret1.equals(ret3));

        String ret_s1 = testWraps.testLRUCacheWrapper("#s1");
        String ret_s2 = testWraps.testLRUCacheWrapper("#s2");
        String ret_s3 = testWraps.testLRUCacheWrapper("#s3");
        String ret_s4 = testWraps.testLRUCacheWrapper("#s4");
        assertTrue(testWraps.testLRUCacheWrapper("#s2").equals(ret_s2) &&
                    testWraps.testLRUCacheWrapper("#s3").equals(ret_s3) &&
                    testWraps.testLRUCacheWrapper("#s4").equals(ret_s4) &&
                    !testWraps.testLRUCacheWrapper("#s1").equals(ret_s1));
    }

    /**
     * for testing java agent
     */
    public static void main(String[] args) throws Throwable {
        new MainTest().testWraps();
        new MainTest().testWrapsWithParams();
        new MainTest().testWrapsWithCopy();
        new MainTest().testWrapsWithCopyAndParams();
        new MainTest().testLRUCacheWrapper();
    }
}
