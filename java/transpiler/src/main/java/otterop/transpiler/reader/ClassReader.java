/**
 * Copyright (c) 2023 The OtterOP Authors. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *    * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


package otterop.transpiler.reader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class ClassReader {

    public Class<?> getClass(String binaryName) {
        try {
            return getClass().getClassLoader().loadClass(binaryName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<String> findMethods(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        var ret = new ArrayList<String>(methods.length);
        for (Method m : methods) {
            if (m.getDeclaringClass().equals(Iterable.class))
                continue;
            ret.add(m.getName());
        }
        return ret;
    }

    public Method findMethod(Class<?> clazz, String methodName) {
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            Method[] methods = currentClazz.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName))
                    return m;
            }
            currentClazz = clazz.getSuperclass();
        }
        return null;
    }

    public boolean isPublicClass(String className) {
        return Modifier.isPublic(
                getClass(className).getModifiers());
    }

    public boolean isInterface(String className) {
        return getClass(className).isInterface();
    }

    public boolean isPublicMethod(Method m) {
        return Modifier.isPublic(m.getModifiers());
    }

    public boolean hasPublicConstructor(String className) {
        Class<?> clazz = getClass(className);
        Constructor[] constructors = clazz.getConstructors();
        return constructors.length > 0;
    }

    public boolean isPublicMethod(String className, String methodName) {
        return isPublicMethod(findMethod(getClass(className), methodName));
    }

    public Collection<Method> findInheritedMethods(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        var ret = new ArrayList<Method>(methods.length);
        for (Method m : methods) {
            var declaringClass = m.getDeclaringClass();
            if (declaringClass.equals(clazz) || declaringClass.equals(Object.class)
                || declaringClass.equals(Iterable.class))
                continue;
            ret.add(m);
        }
        return ret;
    }
}
