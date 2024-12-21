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


package otterop.transpiler.visitor.pure;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import otterop.transpiler.Otterop;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.CaseUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;
import static otterop.transpiler.util.CaseUtil.isClassName;

public class PureCParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean isMain = false;
    private boolean hasMain = false;
    private boolean insideConstructor = false;
    private boolean insideMemberDeclaration = false;
    private boolean insideMethodCall = false;
    private boolean insideStaticField = false;
    private String className = null;
    private String fullClassName = null;
    private String fullClassNameType = null;
    private String pureFullClassName = null;
    private String pureFullClassNameType = null;
    private int indents = 0;
    private int skipNewlines = 0;
    private static final String INDENT = "    ";
    private static final String THIS = "this";
    private static final String MALLOC = "GC_malloc";
    private String packagePrefix = null;
    private String purePackagePrefix = null;
    private String javaPackagePrefix = null;
    private String javaPurePackagePrefix = null;
    private JavaParser.TypeTypeContext currentType = null;
    private boolean currentTypePointer = false;
    private boolean currentReturnTypeArray = false;
    private boolean currentReturnTypeIterable = false;
    private String currentVariablePrefix = null;
    private String currentInstanceName;
    private Set<String> includes = new LinkedHashSet<>();
    private Map<String,String> classIncludes = new LinkedHashMap<>();
    private Map<String,String> classIncludesOtterop = new LinkedHashMap<>();
    private Map<String, String> predeclarations = new LinkedHashMap<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Set<String> publicMethods = new LinkedHashSet<>();
    private Set<String> staticMethods = new LinkedHashSet<>();
    private List<JavaParser.FieldDeclarationContext> fields = new LinkedList<>();
    private List<JavaParser.MethodDeclarationContext> methods = new LinkedList<>();
    private List<JavaParser.InterfaceCommonBodyDeclarationContext> interfaceMethods = new LinkedList<>();
    private List<JavaParser.GenericMethodDeclarationContext> genericMethods = new LinkedList<>();
    private Map<String, JavaParser.MethodDeclarationContext> methodsMap = new LinkedHashMap<>();
    private Map<String, JavaParser.GenericMethodDeclarationContext> genericMethodsMap = new LinkedHashMap<>();
    private Map<String, JavaParser.FieldDeclarationContext> headerStaticFields = new LinkedHashMap<>();
    private boolean interfaceImplementationThis = false;
    private boolean printParameterNames = true;
    private boolean constructorPublic;
    private String lastTypeWrapped = null;
    private String pureLastTypeWrapped = null;
    private boolean lastTypeArray = false;
    private boolean lastTypeIterable = false;
    private boolean anyTypeIterable = false;
    private boolean insideIterableAsArray = false;
    private JavaParser.ConstructorDeclarationContext constructor;
    private Map<String,String> fullClassNames = new LinkedHashMap<>();
    private Map<String,String> pureClassNames = new LinkedHashMap<>();
    private Map<String,String> javaFullClassNames = new LinkedHashMap<>();
    private Map<String,String> javaPureFullClassNames = new LinkedHashMap<>();
    private Map<String,String> methodNameFull = new LinkedHashMap<>();
    private Set<String> arrayArgs = new LinkedHashSet<>();
    private Map<String, JavaParser.TypeTypeContext> variableType = new LinkedHashMap<>();
    private PrintStream out = new PrintStream(outStream);
    private Map<String,String> importDomainMapping;
    private boolean header = false;
    private Set<String> currentTypeParameters = new HashSet<>();
    private Set<String> currentMethodTypeParameters = new HashSet<>();
    private Map<String,String> specialClasses = new LinkedHashMap<>();
    private Map<String,String> wrapperClassName = new LinkedHashMap<>();
    private JavaParser.TypeParametersContext methodTypeParametersContext;
    private ClassReader classReader;
    private Map<String,String> unusedImports = new LinkedHashMap<>();
    private Map<String,String> mappedArguments = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentArray = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentIterable = new LinkedHashMap<>();
    private Map<String,String> mappedArgumentClass = new LinkedHashMap<>();
    private Map<String,String> pureMappedArgumentClass = new LinkedHashMap<>();
    private String calledOn = null;
    private Map<String,String> headerFullClassNames = Collections.emptyMap();

    public PureCParserVisitor(ClassReader classReader, boolean header,
                              PureCParserVisitor headerVisitor,
                              Map<String,String> importDomainMapping) {
        this.header = header;
        this.classReader = classReader;
        if (headerVisitor != null) {
            headerFullClassNames = headerVisitor.getFullClassNames();
        }
        this.specialClasses.put("otterop_lang_String", "char");
        this.wrapperClassName.put("otterop_lang_String", "otterop_lang_String");
        this.wrapperClassName.put("otterop_lang_OOPIterable", "otterop_lang_WrapperOOPIterable");
        this.importDomainMapping = new HashMap<>(importDomainMapping);
    }

    private void detectMethods(ParserRuleContext ctx) {
        List<ParseTree> children = new ArrayList<>(ctx.children);
        boolean methodPublic = false;
        boolean methodStatic = false;
        while (children.size() > 0) {
            boolean skipChildren = false;
            ParseTree child = children.remove(0);
            if (child instanceof JavaParser.FieldDeclarationContext) {
                fields.add((JavaParser.FieldDeclarationContext) child);
            }
            if (child instanceof JavaParser.ClassOrInterfaceModifierContext) {
                JavaParser.ClassOrInterfaceModifierContext modifierChild = (JavaParser.ClassOrInterfaceModifierContext) child;
                if (!methodPublic) methodPublic = modifierChild.PUBLIC() != null;
                if (!methodStatic) methodStatic = modifierChild.STATIC() != null;
            }
            if (child instanceof JavaParser.InterfaceCommonBodyDeclarationContext) {
                JavaParser.InterfaceCommonBodyDeclarationContext declarationChild = (JavaParser.InterfaceCommonBodyDeclarationContext) child;
                interfaceMethods.add(declarationChild);
                var name = declarationChild.identifier().getText();
                publicMethods.add(name);
                methodPublic = false;
                methodStatic = false;
            }
            if (child instanceof JavaParser.MethodDeclarationContext) {
                JavaParser.MethodDeclarationContext declarationChild = (JavaParser.MethodDeclarationContext) child;
                var name = declarationChild.identifier().getText();
                methods.add(declarationChild);
                methodsMap.put(name, declarationChild);
                if (methodPublic) publicMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodStatic = false;
            }
            if (child instanceof JavaParser.ConstructorDeclarationContext) {
                if (methodPublic) constructorPublic = true;
                constructor = (JavaParser.ConstructorDeclarationContext) child;
            }
            if (child instanceof JavaParser.GenericMethodDeclarationContext) {
                JavaParser.GenericMethodDeclarationContext declarationChild = (JavaParser.GenericMethodDeclarationContext) child;
                var name = declarationChild.methodDeclaration().identifier().getText();
                genericMethods.add(declarationChild);
                genericMethodsMap.put(name, declarationChild);
                if (methodPublic) publicMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodStatic = false;
                skipChildren = true;
            }
            if (child instanceof JavaParser.FieldDeclarationContext) {
                JavaParser.FieldDeclarationContext declarationChild = (JavaParser.FieldDeclarationContext) child;
                var name = declarationChild.variableDeclarators().variableDeclarator(0).variableDeclaratorId().getText();
                if (methodPublic && methodStatic) headerStaticFields.put(name, declarationChild);
                methodPublic = false;
                methodStatic = false;
                skipChildren = true;
            }
            if (!skipChildren) {
                for (int i = 0; i < child.getChildCount(); i++) {
                    children.add(child.getChild(i));
                }
            }
        }
    }

    private void visitTypeParameters(JavaParser.TypeParametersContext typeParametersContext, boolean classDeclaration, boolean methodDeclaration) {
        if (typeParametersContext != null) {
            if (methodDeclaration) {
                currentMethodTypeParameters.clear();
            }
            for (var t: typeParametersContext.typeParameter()) {
                if (classDeclaration) currentTypeParameters.add(t.identifier().getText());
                else if (methodDeclaration) {
                    currentMethodTypeParameters.add(t.identifier().getText());
                }
            }
        }
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        detectMethods(ctx);
        out.print(INDENT.repeat(indents));
        visitTypeParameters(ctx.typeParameters(), true, false);
        out.print("typedef struct ");
        className = ctx.identifier().getText();
        var currentFullClassName = packagePrefix + "_" + className;
        var currentPureClassName = purePackagePrefix + "_" + className;
        fullClassNames.put(className, currentFullClassName);
        pureClassNames.put(className, currentPureClassName);

        fullClassName = currentFullClassName;
        fullClassNameType = fullClassName + "_t";
        pureFullClassName = currentPureClassName;
        pureFullClassNameType = pureFullClassName + "_t";
        out.print(pureFullClassName);
        out.print("_s");
        if (header) {
            out.print(" ");
        } else {
            out.print(" {\n");
            indents++;
            out.print(INDENT.repeat(indents));
            out.print("void *implementation;\n");
            for (var method: interfaceMethods) {
                out.print(INDENT.repeat(indents));
                visitInterfaceMethodType(method);
                out.print(";\n");
            }
            indents--;
            out.print("} ");
        }
        out.print(pureFullClassNameType);
        out.println(";\n\n");
        interfaceConstructorImplementation();

        // Pre declare all methods in header
        // and in implementation
        // to avoid declaration order problems
        for(var method : interfaceMethods) {
            insideMemberDeclaration = true;
            visitInterfaceCommonBodyDeclaration(method);
            insideMemberDeclaration = false;
        }
        if (!header) {
            addToIncludes(pureFullClassName, true);
            super.visitInterfaceBody(ctx.interfaceBody());
        }

        return null;
    }

    private void implementInterfaces(List<JavaParser.TypeTypeContext> typeTypes) {
        for (JavaParser.TypeTypeContext typeType : typeTypes) {
            if (!typeType.classOrInterfaceType().isEmpty()) {
                var className = typeType.classOrInterfaceType().getText();
                var javaFullClassName = javaPureFullClassNames.getOrDefault(className, className);
                if (!javaFullClassName.contains(".")) {
                    javaFullClassName = javaPurePackagePrefix + "." + javaFullClassName;
                }
                var interfaceFullClassName = Arrays.stream(javaFullClassName.split("\\."))
                        .collect(Collectors.joining("_"));
                if (header) addToIncludes(interfaceFullClassName, true);

                final String finaljavaFullClassName = javaFullClassName;
                Class<?> interfaceClass = classReader.getClass(javaFullClassName);
                var methods = classReader.findMethods(interfaceClass);
                out.print("\n");
                out.print(interfaceFullClassName);
                out.print("_t\n");
                out.print("*");
                out.print(pureFullClassName + "__to_" + interfaceFullClassName);
                out.print("(");
                out.print(pureFullClassNameType);
                out.print(" *");
                out.print(THIS);
                out.print(")");
                if (header) {
                    out.print(";\n\n");
                    continue;
                }
                out.print(" {\n");
                indents++;
                out.print(INDENT.repeat(indents));
                out.print("return ");
                out.print(interfaceFullClassName);
                out.print("_new(");
                out.print(THIS);
                indents++;
                for (var method : methods) {
                    out.print(",\n");
                    out.print(INDENT.repeat(indents));
                    out.print("(");
                    if (methodsMap.containsKey(method)) {
                        var methodContext = methodsMap.get(method);
                        visitImplementationMethodType(methodContext);
                    }
                    out.print(") ");
                    out.print(pureFullClassName);
                    out.print("_");
                    out.print(method);
                }
                indents--;
                out.print(");\n");
                indents--;
                out.print("}\n\n");
            }
        }
    }

    @Override
    public Void visitAnnotation(JavaParser.AnnotationContext ctx) {
        return null;
    }

    private void wrapperConstructor() {
        out.print(INDENT.repeat(indents));
        out.print("static ");
        out.print(pureFullClassNameType);
        out.print(" *");
        out.print(pureFullClassName);
        out.print("_wrap(");
        out.print(fullClassNameType);
        out.print(" *wrapped) {\n");
        indents++;
        printMalloc(THIS, pureFullClassNameType);
        out.print(INDENT.repeat(indents));
        out.print("this->_otterop = wrapped;\n");
        out.print(INDENT.repeat(indents));
        out.print("return this;\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    private void unwrapMethod() {
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print(fullClassNameType);
        out.print(" *");
        out.print(pureFullClassName);
        out.print("_unwrap(");
        out.print(pureFullClassNameType);
        out.print(" *this)");
        if (header) {
            out.print(";\n");
            return;
        }
        out.print(" {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("return this->_otterop;\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        detectMethods(ctx);
        visitTypeParameters(ctx.typeParameters(), true, false);

        className = ctx.identifier().getText();
        var currentFullClassName = packagePrefix + "_" + className;
        var currentPureClassName = purePackagePrefix + "_" + className;
        fullClassNames.put(className, currentFullClassName);
        pureClassNames.put(className, currentPureClassName);

        fullClassName = currentFullClassName;
        if (header)
            addToIncludes(fullClassName, false);
        fullClassNameType = fullClassName + "_t";
        pureFullClassName = currentPureClassName;
        pureFullClassNameType = pureFullClassName + "_t";

        if (!header) {
            addToIncludes(pureFullClassName, true);
        }

        out.print(INDENT.repeat(indents));
        out.print("typedef struct ");
        out.print(pureFullClassName);
        out.print("_s");
        if (header) {
            out.print(" ");
        } else {
            out.print(" {\n");
            indents++;
            out.print(INDENT.repeat(indents));
            out.print(fullClassNameType);
            out.print(" *_otterop;\n");
            indents--;
            out.print("} ");
        }
        out.print(pureFullClassNameType);
        out.println(";\n\n");
        // Pre declare all public methods in header
        // and all private methods in implementation
        // to avoid declaration order problems
        if (!header)
            this.wrapperConstructor();
        if (constructor != null) {
            insideMemberDeclaration = true;
            if (header)
                visitConstructorDeclaration(constructor);
            insideMemberDeclaration = false;
        }
        for (var field : headerStaticFields.values()) {
            memberPublic = true;
            memberStatic = true;
            insideMemberDeclaration = true;
            visitFieldDeclaration(field);
            insideMemberDeclaration = false;
            memberPublic = false;
            memberStatic = false;
        }
        for(var method : methods) {
            insideMemberDeclaration = true;
            var methodName = method.identifier().getText();
            memberPublic = publicMethods.contains(methodName);
            memberStatic = staticMethods.contains(methodName);
            if (header && memberPublic || !header && !memberPublic)
                visitMethodDeclaration(method);
            insideMemberDeclaration = false;
        }
        // TODO: generic pure wrapper
        //        for(var method : genericMethods) {
        //            insideMemberDeclaration = true;
        //            var methodName = method.methodDeclaration().identifier().getText();
        //            memberPublic = publicMethods.contains(methodName);
        //            memberStatic = staticMethods.contains(methodName);
        //            if (header && memberPublic || !header && !memberPublic)
        //                visitGenericMethodDeclaration(method);
        //            insideMemberDeclaration = false;
        //        }s

        if (!header) {
            super.visitClassBody(ctx.classBody());
        }
        unwrapMethod();

        if (ctx.IMPLEMENTS() != null) {
            out.println();
            implementInterfaces(ctx.typeList().get(0).typeType());
        }

        return null;
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        if (!constructorPublic)
            return null;
        insideConstructor = true;
        out.print("\n\n");
        out.print(pureFullClassNameType);
        out.print(" *");
        out.print(pureFullClassName);
        out.print("_new");
        visitFormalParameters(ctx.formalParameters());
        if (insideMemberDeclaration) out.print(";");
        else {
            out.print(" {\n");
            out.print(INDENT.repeat(indents));
            indents++;
            mapArguments();
            printMalloc(THIS, pureFullClassNameType);
            out.print(INDENT.repeat(indents));
            out.print("this->_otterop = ");
            out.print(fullClassName);
            out.print("_new");
            insideMethodCall = true;
            visitFormalParameters(ctx.formalParameters());
            insideMethodCall = false;
            out.print(";\n");
            out.print(INDENT.repeat(indents));
            out.print("return this;\n");
            indents--;
            out.print(INDENT.repeat(indents));
            out.print("}\n\n");
        }
        insideConstructor = false;
        currentReturnTypeArray = false;
        currentReturnTypeIterable = false;
        lastTypeArray = false;
        lastTypeIterable = false;
        return null;
    }

    @Override
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = false;
        return super.visitClassBodyDeclaration(ctx);
    }

    private void interfaceConstructorImplementation() {
        out.print(pureFullClassNameType);
        out.print(" *");
        out.print(pureFullClassName);
        out.print("_new(void *implementation, ");
        boolean rest = false;
        for (var method: interfaceMethods) {
            if (rest) out.print(", ");
            else rest = true;
            visitInterfaceMethodType(method);
        }
        out.print(")");
        if (header) {
            out.print(";\n");
            return;
        }

        out.print(" {\n");
        indents++;
        printMalloc(THIS, pureFullClassNameType);
        out.print(INDENT.repeat(indents));
        out.print("this->implementation = implementation;\n");
        for (var method: interfaceMethods) {
            out.print(INDENT.repeat(indents));
            out.print("this->");
            var name = method.identifier().getText();
            name = camelCaseToSnakeCase(name);
            out.print(name);
            out.print(" = ");
            out.print(name);
            out.print(";\n");
        }
        out.print(INDENT.repeat(indents));
        out.print("return this;\n");
        indents--;
        out.print("}\n");
    }

    private void interfaceMethodImplementation(JavaParser.InterfaceCommonBodyDeclarationContext ctx,
                                               String methodName) {
        out.print(" {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        if (ctx.typeTypeOrVoid().VOID() == null) {
            out.print("return ");
        }
        out.print("this->");
        out.print(methodName);
        out.print("(this->implementation");
        for(var arg: ctx.formalParameters().formalParameterList().formalParameter()) {
            out.print(", ");
            visitVariableDeclaratorId(arg.variableDeclaratorId());
            if (arrayArgs.contains(arg.variableDeclaratorId().getText())) {
                out.print(", ");
                visitVariableDeclaratorId(arg.variableDeclaratorId());
                out.print("_cnt");
            }
        }
        if (currentReturnTypeArray || currentReturnTypeIterable && insideIterableAsArray) {
            out.print(", _ret_cnt");
        }
        out.print(");\n");
        indents--;
        out.print("}");
    }

    private void visitInterfaceMethodType(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        currentTypePointer = false;
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        if (lastTypeArray) {
            out.print("*");
            this.currentReturnTypeArray = true;
            this.lastTypeArray = false;
        }
        if (lastTypeIterable && insideIterableAsArray) {
            out.print("*");
            this.currentReturnTypeIterable = true;
            this.lastTypeIterable = false;
        }
        if (currentTypePointer) {
            out.print("*");
            currentTypePointer = false;
        }
        var name = ctx.identifier().getText();
        name = camelCaseToSnakeCase(name);
        out.print("(*");
        out.print(name);
        out.print(")");
        this.interfaceImplementationThis = true;
        this.printParameterNames = false;
        visitFormalParameters(ctx.formalParameters());
        this.printParameterNames = true;
        this.interfaceImplementationThis = false;
        this.currentReturnTypeArray = false;
        this.currentReturnTypeIterable = false;
    }

    private void visitImplementationMethodType(JavaParser.MethodDeclarationContext ctx) {
        currentTypePointer = false;
        currentReturnTypeArray = false;
        currentReturnTypeIterable = false;
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        if (lastTypeArray) {
            out.print("*");
            lastTypeArray = false;
            currentReturnTypeArray = true;
        }
        if (lastTypeIterable && insideIterableAsArray) {
            out.print("*");
            this.currentReturnTypeIterable = true;
            this.lastTypeIterable = false;
        }
        if (currentTypePointer) {
            out.print("*");
            currentTypePointer = false;
        }
        var name = ctx.identifier().getText();
        name = camelCaseToSnakeCase(name);
        out.print("(*)");
        this.interfaceImplementationThis = true;
        this.printParameterNames = false;
        visitFormalParameters(ctx.formalParameters());
        this.printParameterNames = true;
        this.interfaceImplementationThis = false;
        currentTypePointer = false;
        currentReturnTypeArray = false;
        currentReturnTypeIterable = false;
    }

    private String mapArgument(String argName, String originalClass, String mappedClass) {
        if (wrapperClassName.containsKey(mappedClass)) {
            return mappedClass + "_wrap(" + argName + ")";
        } else {
            return originalClass + "_unwrap(" + argName + ")";
        }
    }

    private String unmapArgument(String argName, String originalClass, String mappedClass) {
        if (wrapperClassName.containsKey(originalClass)) {
            return originalClass + "_unwrap(" + argName + ")";
        } else {
            return mappedClass + "_wrap(" + argName + ")";
        }
    }

    private void mapArguments() {
        boolean firstArray = false;
        if (!mappedArguments.isEmpty()) {
            for (var entry: mappedArguments.entrySet()) {
                out.print(INDENT.repeat(indents));
                var paramName = entry.getKey();
                var mapperParamName = entry.getValue();
                var mappedClass = mappedArgumentClass.get(paramName);
                var pureMappedClass = pureMappedArgumentClass.get(paramName);
                var mappedClassType = mappedClass + "_t";

                if (mappedArgumentArray.getOrDefault(paramName, false)) {
                    if (!firstArray) {
                        firstArray = true;
                        out.print("int i;\n");
                        out.print(INDENT.repeat(indents));
                    }
                    var mappedArrayName = mapperParamName + "_array";
                    out.print(mappedClassType);
                    out.print(" **");
                    out.print(mappedArrayName);
                    out.print(" = ");
                    out.print(MALLOC);
                    includes.add("#include <gc.h>");
                    out.print("(");
                    out.print(paramName);
                    out.print("_cnt * sizeof(");
                    out.print(mappedClassType);
                    out.print(" *));\n");
                    out.print(INDENT.repeat(indents));
                    out.print("for (i = 0; i < ");
                    out.print(paramName);
                    out.print("_cnt; i++) {\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print(mappedArrayName);
                    out.print("[i] = ");
                    out.print(mapArgument(paramName + "[i]", pureMappedClass, mappedClass));
                    out.print(";\n");
                    indents--;
                    out.print(INDENT.repeat(indents));
                    out.print("}\n");
                    out.print(INDENT.repeat(indents));
                    out.print("otterop_lang_Array_t *");
                    out.print(mapperParamName);
                    out.print(" = otterop_lang_Array_wrap(");
                    out.print(mappedArrayName);
                    out.print(", ");
                    out.print(paramName);
                    out.print("_cnt);\n");
                } else if (mappedArgumentIterable.getOrDefault(paramName, false)) {
                    addToIncludes("otterop_lang_WrapperOOPIterable", false);
                    unusedImports.remove("WrapperOOPIterable");
                    out.print("otterop_lang_OOPIterable_t *");
                    out.print(mapperParamName);
                    out.print(" = otterop_lang_WrapperOOPIterable_wrap");
                    if (insideIterableAsArray)
                        out.print("_array");
                    out.print("(");
                    if (insideIterableAsArray)
                        out.print("(void **) ");
                    out.print(paramName);
                    if (!insideIterableAsArray)
                        out.print("_it_self");
                    out.print(", ");
                    out.print(paramName);
                    if (insideIterableAsArray)
                        out.print("_cnt, ");
                    else
                        out.print("_it, ");
                    if (!"void".equals(mappedClass)) {
                        out.print("(void *(*)(void *)) ");
                        out.print(mappedClass);
                        out.print("_wrap");
                    } else {
                        out.print("NULL");
                    }
                    out.print(");\n");
                } else {
                    out.print(mappedClassType);
                    out.print(" *");
                    out.print(mapperParamName);
                    out.print(" = ");
                    out.print(mapArgument(paramName, pureMappedClass, mappedClass));
                    out.print(";\n");
                }
            }
        }
    }

    @Override
    public Void visitInterfaceCommonBodyDeclaration(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        visitTypeParameters(methodTypeParametersContext, false, true);
        currentTypePointer = false;
        variableType.clear();
        arrayArgs.clear();
        out.print("\n\n");
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        if (lastTypeArray) {
            out.print("*");
            lastTypeArray = false;
            this.currentReturnTypeArray = true;
        }
        if (lastTypeIterable && insideIterableAsArray) {
            out.print("*");
            this.currentReturnTypeIterable = true;
            this.lastTypeIterable = false;
        }
        if (currentTypePointer) {
            out.print("*");
            currentTypePointer = false;
        }
        var name = ctx.identifier().getText();
        if (!isMain) name = camelCaseToSnakeCase(name);
        out.print(pureFullClassName + "_" + name);
        visitFormalParameters(ctx.formalParameters());
        if (insideMemberDeclaration) out.print(";\n");
        else interfaceMethodImplementation(ctx, name);
        arrayArgs.clear();
        variableType.clear();
        this.currentReturnTypeArray = false;
        this.currentReturnTypeIterable = false;
        return null;
    }

    @Override
    public Void visitGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        this.methodTypeParametersContext = ctx.typeParameters();
        return visitMethodDeclaration(ctx.methodDeclaration());
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (!memberPublic) return null;
        anyTypeIterable = false;
        var hasReturn = ctx.typeTypeOrVoid().VOID() == null;
        var returnTypeArray = false;
        var returnTypeIterable = false;
        var returnTypePointer = true;
        var returnTypePrimitive = false;
        String returnTypePure = null;
        String returnTypeString = null;
        String returnTypeArrayArgument = null;
        if (hasReturn) {
            var returnType = ctx.typeTypeOrVoid().typeType().classOrInterfaceType();
            var returnPrimitiveType = ctx.typeTypeOrVoid().typeType().primitiveType();
            if (returnType != null) {
                returnTypeString = returnType.identifier().stream().map(i -> i.getText())
                        .collect(Collectors.joining("."));
                returnTypeArray = returnType.identifier(0).getText().equals("Array");
                returnTypeIterable = returnType.identifier(0).getText().equals("OOPIterable");
                if (!returnTypeArray && !returnTypeIterable) {
                    returnTypePure = pureClassNames.get(returnTypeString);
                } else {
                    returnTypeArrayArgument =
                            returnType.typeArguments().get(0).typeArgument(0).typeType().getText();
                    if (specialClasses.containsKey(
                            fullClassNames.get(returnTypeArrayArgument)))
                        returnTypePrimitive = true;
                    returnTypePure = pureClassNames.get(returnTypeArrayArgument);
                }
                returnTypeString = fullClassNames.get(returnTypeString);
            } else if (returnPrimitiveType != null) {
                returnTypePointer = false;
                returnTypePrimitive = true;
                returnTypeString = returnPrimitiveType.getText();
            }
        }

        if (returnTypeArrayArgument != null) {
            returnTypeArrayArgument = fullClassNames.get(returnTypeArrayArgument);
        }

        mappedArguments.clear();
        mappedArgumentArray.clear();
        mappedArgumentIterable.clear();
        mappedArgumentClass.clear();
        pureMappedArgumentClass.clear();

        currentReturnTypeArray = false;
        currentReturnTypeIterable = false;
        visitTypeParameters(methodTypeParametersContext, false, true);
        currentTypePointer = false;
        variableType.clear();
        arrayArgs.clear();
        out.print("\n\n");
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        if (lastTypeArray) {
            out.print("*");
            currentReturnTypeArray = true;
            lastTypeArray = false;
        }
        if (lastTypeIterable) {
            if (insideIterableAsArray)
                out.print("*");
            this.currentReturnTypeIterable = true;
            this.lastTypeIterable = false;
        }
        if (currentTypePointer) {
            out.print("*");
            currentTypePointer = false;
        }
        var name = ctx.identifier().getText();
        isMain = name.equals("main");
        if (isMain) hasMain = true;
        if (!isMain) name = camelCaseToSnakeCase(name);
        var declaration_name = name;
        if ("oop_string".equals(declaration_name))
            declaration_name = "string";
        out.print(pureFullClassName + "_" + declaration_name);
        if (insideIterableAsArray)
            out.print("_array");
        visitFormalParameters(ctx.formalParameters());
        if (insideMemberDeclaration) out.print(";\n");
        else {
            out.print(" {\n");
            indents++;
            mapArguments();
            out.print(INDENT.repeat(indents));
            if (hasReturn) {
                out.print(returnTypeString);
                if (returnTypePointer)
                    out.print("_t *");
                out.print(" ret_otterop = ");
            }
            out.print(fullClassName);
            out.print("_");
            out.print(name);
            insideMethodCall = true;
            calledOn = "this->_otterop";
            visitFormalParameters(ctx.formalParameters());
            insideMethodCall = false;
            out.print(";\n");
            if (hasReturn) {
                if (returnTypeArray) {
                    out.print(INDENT.repeat(indents));
                    out.print("int _ret_size = otterop_lang_Array_size(ret_otterop), _ret_i;\n");
                    out.print(INDENT.repeat(indents));
                    out.print("*_ret_cnt = _ret_size;\n");
                    out.print(INDENT.repeat(indents));
                    out.print(returnTypePure);
                    if (!returnTypePrimitive)
                        out.print("_t");
                    out.print(" **ret = ");
                    out.print(MALLOC);
                    includes.add("#include <gc.h>");
                    out.print("(_ret_size * sizeof(void *));\n");
                    out.print(INDENT.repeat(indents));

                    out.print("for (_ret_i = 0; _ret_i < _ret_size; _ret_i++) {\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print(returnTypeArrayArgument);
                    out.print("_t *ret_i = otterop_lang_Array_get(ret_otterop, _ret_i);\n");
                    if (returnTypePure.equals(pureClassNames.get(className)) && !memberStatic) {
                        out.print(INDENT.repeat(indents));
                        out.print("if (ret_i == this->_otterop) {\n");
                        indents++;
                        out.print(INDENT.repeat(indents));
                        out.print("ret[_ret_i] = this;\n");
                        out.print(INDENT.repeat(indents));
                        out.print("continue;\n");
                        indents--;
                        out.print(INDENT.repeat(indents));
                        out.print("}\n");
                    }
                    out.print(INDENT.repeat(indents));
                    out.print("ret[_ret_i] = ");
                    out.print(this.unmapArgument("ret_i", returnTypeArrayArgument, returnTypePure));
                    out.print(";\n");
                    indents--;
                    out.print(INDENT.repeat(indents));
                    out.print("}\n");
                } else if (returnTypeIterable) {
                    addToIncludes("otterop_lang_WrapperOOPIterable", false);
                    unusedImports.remove("WrapperOOPIterable");
                    out.print(INDENT.repeat(indents));
                    out.print("void *");
                    if (insideIterableAsArray)
                        out.print("*");
                    out.print("ret = otterop_lang_WrapperOOPIterable_unwrap");
                    if (insideIterableAsArray)
                        out.print("_array");
                    out.print("(ret_otterop, NULL, ");
                    if (insideIterableAsArray)
                        out.print("_ret_cnt");
                    else
                        out.print("_ret_it");
                    out.print(");\n");
                } else {
                    if (returnTypePure == null) {
                        out.print(INDENT.repeat(indents));
                        out.print(returnTypeString);
                        if (returnTypePointer)
                            out.print("_t *");
                        out.print(" ret = ret_otterop;\n");
                    } else {
                        if (returnTypePure.equals(pureClassNames.get(className)) && !memberStatic) {
                            out.print(INDENT.repeat(indents));
                            out.print("if (ret_otterop == this->_otterop) return this;\n");
                        }
                        out.print(INDENT.repeat(indents));
                        if (specialClasses.containsKey(returnTypeString))
                            out.print(specialClasses.get(returnTypeString));
                        else {
                            out.print(returnTypePure);
                            out.print("_t");
                        }
                        out.print(" *ret = ");
                        out.print(
                                this.unmapArgument("ret_otterop", returnTypeString, returnTypePure)
                        );
                        out.print(";\n");
                    }
                }
                out.print(INDENT.repeat(indents));
                out.print("return ret;\n");
            }
            indents--;
            out.print("}\n");
        }
        this.isMain = false;
        arrayArgs.clear();
        variableType.clear();
        currentReturnTypeArray = false;
        currentReturnTypeIterable = false;
        lastTypeArray = false;
        lastTypeIterable = false;
        if (anyTypeIterable && !insideIterableAsArray) {
            insideIterableAsArray = true;
            visitMethodDeclaration(ctx);
            insideIterableAsArray = false;
        }
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        out.print("(");
        var addCalledOn = !memberStatic && !insideConstructor && !interfaceImplementationThis;
        var calledOn = this.calledOn != null ? this.calledOn : THIS;
        this.calledOn = null;
        if (addCalledOn) {
            if (!insideMethodCall) {
                out.print(pureFullClassNameType);
                out.print(" *");
            }
            if (printParameterNames)
                out.print(calledOn);
        }
        if (interfaceImplementationThis) {
            out.print("void *");
            if (printParameterNames)
                out.print(calledOn);
        }
        var hasComma = addCalledOn || interfaceImplementationThis;
        if (ctx.formalParameterList() != null) {
            if (hasComma) out.print(", ");
            visitFormalParameterList(ctx.formalParameterList());
            hasComma = true;
        }
        if (!insideMethodCall) {
            if (currentReturnTypeArray || currentReturnTypeIterable && insideIterableAsArray) {
                if (hasComma) out.print(", ");
                out.print("int *");
                if (printParameterNames) {
                    out.print("_ret_cnt");
                }
            } else if (currentReturnTypeIterable) {
                if (hasComma) out.print(", ");
                out.print("int (**_");
                if (printParameterNames)
                    out.print("ret_it");
                out.print(")(void *, void **)");
            }
        }
        out.print(")");
        return null;
    }

    @Override
    public Void visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        boolean isLast = ctx.getParent().children.get(ctx.getParent().getChildCount()-1) == ctx;
        var parameterName = ctx.variableDeclaratorId().getText();
        var parameterType = ctx.typeType();
        lastTypeWrapped = null;
        pureLastTypeWrapped = null;
        lastTypeArray = false;
        lastTypeIterable = false;
        currentVariablePrefix = null;

        variableType.put(parameterName, parameterType);
        var declaratorId = ctx.variableDeclaratorId();
        visitTypeType(parameterType);
        if (lastTypeWrapped != null) {
            mappedArguments.put(parameterName, "_" + parameterName);
            mappedArgumentArray.put(parameterName, lastTypeArray);
            mappedArgumentIterable.put(parameterName, lastTypeIterable);
            mappedArgumentClass.put(parameterName, lastTypeWrapped);
            pureMappedArgumentClass.put(parameterName, pureLastTypeWrapped);
            if (insideMethodCall)
                currentVariablePrefix = "_";
        }

        if (printParameterNames) {
            if (!insideMethodCall) {
                out.print(" ");
                if (lastTypeArray)
                    out.print("*");
               if (lastTypeIterable && insideIterableAsArray)
                    out.print("*");
            }
            visitVariableDeclaratorId(declaratorId);
            if (!insideMethodCall && lastTypeIterable && !insideIterableAsArray)
                out.print("_it_self");
        } else if (currentTypePointer && !insideMethodCall)
            out.print(" *");
        currentTypePointer = false;
        var isTypeArray = lastTypeArray;
        var isTypeIterable = lastTypeIterable;
        if (!insideMethodCall) {
            if (parameterType.LBRACK().size() > 0 ||
                lastTypeArray ||
                (insideIterableAsArray && lastTypeIterable)) {
                lastTypeArray = false;
                lastTypeIterable = false;
                if (!printParameterNames)
                    out.print("*");
                out.print(", ");
                arrayArgs.add(parameterName);
                out.print("int");
                if (printParameterNames) {
                    out.print(" ");
                    this.currentVariablePrefix = null;
                    visitVariableDeclaratorId(declaratorId);
                    out.print("_cnt");
                }
                lastTypeArray = isTypeArray;
                lastTypeIterable = isTypeIterable;
            } else if (lastTypeIterable) {
                lastTypeIterable = false;
                out.print(", int (*");
                if (printParameterNames) {
                    this.currentVariablePrefix = null;
                    visitVariableDeclaratorId(declaratorId);
                }
                out.print("_it)(void *self, void **next)");
                lastTypeIterable = isTypeIterable;
            }
        }
        if (!isLast) out.print(", ");
        this.currentVariablePrefix = null;
        return null;
    }

    @Override
    public Void visitModifier(JavaParser.ModifierContext ctx) {
        if (ctx.getText().equals("static"))
            memberStatic = true;
        if (ctx.getText().equals("public"))
            memberPublic = true;
        super.visitModifier(ctx);
        return null;
    }

    private void printMalloc(String name, String type) {
        out.print(INDENT.repeat(indents));
        out.print(type);
        out.print(" *");
        out.print(name);
        out.print(" = ");
        out.print(MALLOC);
        includes.add("#include <gc.h>");
        out.print("(sizeof(");
        out.print(type);
        out.print("));\n");
    }

    @Override
    public Void visitBlock(JavaParser.BlockContext ctx) {
        out.print(" {\n");
        indents++;
        var insideConstructorFirst = this.insideConstructor;
        this.insideConstructor = false;
        if (insideConstructorFirst) {
            printMalloc(THIS, pureFullClassNameType);
        }
        super.visitBlock(ctx);
        if (insideConstructorFirst) {
            out.print(INDENT.repeat(indents));
            out.print("return this;\n");
        }
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}");
        return null;
    }

    private void checkElseStatement(JavaParser.StatementContext ctx) {
        if (ctx.ELSE() != null) {
            if (ctx.statement(1).IF() != null) {
                out.print(" else if (");
                visitParExpression(ctx.statement(1).parExpression());
                out.print(")");
                super.visitStatement(ctx.statement(1).statement(0));
                checkElseStatement(ctx.statement(1));
            } else {
                out.print(" else");
                super.visitStatement(ctx.statement(1));
            }
        }
    }

    @Override
    public Void visitStatement(JavaParser.StatementContext ctx) {
        if (ctx.IF() != null || ctx.WHILE() != null) {
            out.print("\n");
            out.print(INDENT.repeat(indents));
            if(ctx.IF() != null) out.print("if (");
            if(ctx.WHILE() != null) out.print("while (");
            super.visit(ctx.parExpression());
            out.print(")");
            super.visitStatement(ctx.statement(0));
            checkElseStatement(ctx);
        } else if (ctx.FOR() != null) {
            var forControl = ctx.forControl();
            out.print("for (");
            visitChildren(forControl.forInit());
            out.print("; ");
            if (forControl.expression() != null)
                visitExpression(forControl.expression());
            out.print("; ");
            if (forControl.forUpdate != null) {
                visitChildren(forControl.forUpdate);
            }
            out.print(")");
            visitStatement(ctx.statement(0));
        } else {
            if (ctx.RETURN() != null) {
                out.print("return ");
            }
            super.visitStatement(ctx);
            if (ctx.SEMI() != null) {
                out.print(";");
            }
        }
        return null;
    }

    @Override
    public Void visitVariableDeclarator(JavaParser.VariableDeclaratorContext ctx) {
        var variableName = ctx.variableDeclaratorId().getText();
        variableType.put(variableName, currentType);
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        if (!insideMemberDeclaration && ctx.variableInitializer() != null) {
            out.print(" = ");
            var variableInitializerText = ctx.variableInitializer().getText();
            String className = null;
            if (variableType.containsKey(variableInitializerText)) {
                className = variableType.get(variableInitializerText).getText();
            }
            checkInterfaceCreation(className, () -> {
                super.visitVariableInitializer(ctx.variableInitializer());
            });
        }
        return null;
    }

    @Override
    public Void visitVariableDeclaratorId(JavaParser.VariableDeclaratorIdContext ctx) {
        if (currentTypePointer && !insideMethodCall) out.print("*");
        if (insideStaticField) {
            out.print(pureFullClassName);
            out.print("_");
        }
        if (this.currentVariablePrefix != null)
            out.print(this.currentVariablePrefix);
        return super.visitVariableDeclaratorId(ctx);
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        var first = ctx.getParent().getChild(0) == ctx;
        var methodName = ctx.identifier().getText();
        if (first && methodNameFull.containsKey(methodName))
            methodName = methodNameFull.get(methodName);
        else
            methodName = camelCaseToSnakeCase(methodName);
        out.print(methodName);
        out.print("(");
        if (currentInstanceName != null) {
            out.print(currentInstanceName);
            if (ctx.expressionList() != null) out.print(", ");
            currentInstanceName = null;
        }
        visitExpressionList(ctx.expressionList());
        out.print(")");
        return null;
    }

    @Override
    public Void visitArguments(JavaParser.ArgumentsContext ctx) {
        out.print("(");
        visitExpressionList(ctx.expressionList());
        out.print(")");
        return null;
    }

    private void addClassToFullClassNames(String className) {
        fullClassNames.put(className, packagePrefix + "_" + className);
    }

    @Override
    public Void visitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.LPAREN() != null) {
            out.print("(");
            visitTypeType(ctx.typeType(0));
            if (currentTypePointer) {
                out.print(" *");
                currentTypePointer = false;
            }
            out.print(") ");
            visitExpression(ctx.expression(0));
            return null;
        }
        if (ctx.prefix != null) {
            var prefixText = ctx.prefix.getText();
            out.print(prefixText);
        }
        if (ctx.bop != null) {
            boolean methodCall = false;
            if (ctx.methodCall() != null && ctx.expression(0) != null) {
                var name = ctx.expression(0).getText();
                methodCall = true;
                if (THIS.equals(name) || className.equals(name)) {
                    if (THIS.equals(name)) {
                        currentInstanceName = THIS;
                    }
                    out.print(pureFullClassName + "_");
                } else if (variableType.containsKey(name)) {
                    currentInstanceName = camelCaseToSnakeCase(name);
                    JavaParser.TypeTypeContext type = variableType.get(name);
                    var typeName = type.primitiveType() != null ? type.getText()
                                          : type.classOrInterfaceType().identifier(0).getText();
                    var fullClassName = fullClassNames.get(typeName);
                    out.print(fullClassName + "_");
                } else if (fullClassNames.containsKey(name)) {
                    var fullClassName = fullClassNames.get(name);
                    out.print(fullClassName + "_");
                } else if (isClassName(name)) {
                    addClassToFullClassNames(name);
                    out.print(fullClassNames.get(name) + "_");
                }
                visitMethodCall(ctx.methodCall());
            }
            if (!methodCall) {
                var name = ctx.expression(0).getText();
                var isClassName = isClassName(name);
                var bop = ctx.bop.getText();
                if ("=".equals(bop)) currentType = variableType.get(name);

                if (ctx.expression(0) != null) visitExpression(ctx.expression(0));
                if (!bop.equals(".")) bop = " " + bop + " ";
                else if (isClassName) bop = "_";
                else bop = "->";
                out.print(bop);
                if (ctx.expression(1) != null) visitExpression(ctx.expression(1));
                else if (ctx.identifier() != null) visitIdentifier(ctx.identifier());
                currentType = null;
            }
        } else {
            super.visitExpression(ctx);
        }
        if (ctx.postfix != null) {
            var postfixText = ctx.postfix.getText();
            out.print(postfixText);
        }
        return null;
    }

    private boolean excludeImports(String javaFullClassName) {
        return Otterop.WRAPPED_CLASS.equals(javaFullClassName);
    }

    private String javaFullClassNameToC(List<String> identifiers) {
        String className = identifiers.get(identifiers.size() - 1);
        var prefixIdentifiers = identifiers.subList(0, identifiers.size() - 1);
        return prefixIdentifiers.stream()
                .map(identifier -> camelCaseToSnakeCase(identifier))
                .collect(Collectors.joining("_"))
                + "_" + className;
    }

    private void addToIncludes(List<String> identifiers, boolean isPure) {
        var otteropIdentifiers = new ArrayList<>(identifiers);
        if (isPure && otteropIdentifiers.get(otteropIdentifiers.size() - 2).equals("pure")) {
            otteropIdentifiers.remove(otteropIdentifiers.size() - 2);
        }
        String className = identifiers.get(identifiers.size() - 1);
        var isThisClass = this.className != null && this.className.equals(className);
        var prefixIdentifiers = identifiers.subList(0, identifiers.size() - 1);
        var prefixIdentifiersOtterop = otteropIdentifiers.subList(0, otteropIdentifiers.size() - 1);
        var includeIdentifiers = replaceBasePackageIdentifiers(prefixIdentifiers);
        var includeIdentifiersOtterop = replaceBasePackageIdentifiers(prefixIdentifiersOtterop);
        var fileName = camelCaseToSnakeCase(className);
        var includeStr = String.join("/", includeIdentifiers);
        var includeStrOtterop = String.join("/", includeIdentifiersOtterop);
        var fullClassName = javaFullClassNameToC(identifiers);
        var javaFullClassName = identifiers.stream()
                .collect(Collectors.joining("."));

        var includeStatement = "#include <" + includeStr + "/" + fileName + ".h>";
        var includeStatementOtterop = "#include <" + includeStrOtterop + "/" + fileName + ".h>";

        var add = false;
        if (!excludeImports(javaFullClassName)) {
            if (header) {
                add = isPure || isThisClass;
            } else {
                add = !isPure || isThisClass || !headerFullClassNames.containsKey(className);
            }
        }

        if (add) {
            classIncludes.put(className, includeStatement);
            if (isPure && !header) {
                classIncludesOtterop.put(className, includeStatementOtterop);
            }
            if (header)
                predeclarations.put(className, "typedef struct " + fullClassName + "_s " + fullClassName + "_t;");
            unusedImports.put(className, includeStatement);
        }
        if (isPure)
            return;

        identifiers = new LinkedList<>(identifiers);
        identifiers.add(identifiers.size() - 1, "pure");
        var javaPureFullClassName = identifiers.stream().collect(Collectors.joining("."));
        fullClassNames.put(className, fullClassName);
        javaFullClassNames.put(className, javaFullClassName);
        javaPureFullClassNames.put(className, javaPureFullClassName);
    }

    private void addToIncludes(String fullClassName, boolean isPure) {
        addToIncludes(List.of(fullClassName.split("_")), isPure);
    }

    private void checkCurrentPackageImports(String name) {
        if (CaseUtil.isClassName(name) && !fullClassNames.containsKey(name)) {
            addToIncludes(packagePrefix + "_" + name, true);
        }
    }

    private AbstractMap.SimpleEntry<String,String> replaceBasePackage(String packageName) {
        var replacement = "";
        for(var replacePackage : importDomainMapping.keySet()) {
            if (packageName.equals(replacePackage) || packageName.startsWith(replacePackage + ".")) {
                replacement = importDomainMapping.get(replacePackage);
                if (packageName.equals(replacePackage)) packageName = "";
                else packageName = packageName.replace(replacePackage + ".", "");
                break;
            }
        }
        return new AbstractMap.SimpleEntry(packageName, replacement);
    }

    private List<String> replaceBasePackageIdentifiers(List<String> identifiers) {
        var replacement = replaceBasePackage(
                identifiers.stream().collect(Collectors.joining("."))
        );
        var rest = replacement.getKey().split("\\.");
        var replacementArray = replacement.getValue().split("/");
        var ret = new ArrayList<String>();
        if (replacementArray.length >= 1 && replacementArray[0].length() > 0)
            Collections.addAll(ret, replacementArray);
        if (rest.length >= 1 && rest[0].length() > 0)
            Collections.addAll(ret, rest);
        return ret;
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier();
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        String className = identifiers.get(classNameIdx).getText();
        var identifiersString = identifiers.subList(0, classNameIdx + 1).stream()
                .map(identifier -> identifier.getText()).collect(Collectors.toList());

        addToIncludes(identifiersString, false);
        if (isStatic) {
            var methodName = identifiers.get(classNameIdx + 1).getText();
            var methodNameSnake = camelCaseToSnakeCase(methodName);
            methodNameFull.put(methodName, fullClassName + "_" + methodNameSnake);
        }
        var fullClassName = fullClassNames.get(className);
        var pureFullClassName = javaPureFullClassNames.get(className);
        if (specialClasses.containsKey(fullClassName)) {
            pureClassNames.put(className, specialClasses.get(fullClassName));
        } else {
            pureClassNames.put(className, pureFullClassName);
        }
        return null;
    }

    @Override
    public Void visitBlockStatement(JavaParser.BlockStatementContext ctx) {
        out.print(INDENT.repeat(indents));
        super.visitBlockStatement(ctx);
        if (ctx.SEMI() != null)
            out.print(";");

        if (skipNewlines > 0) skipNewlines--;
        else out.print("\n");
        return null;
    }

    @Override
    public Void visitLiteral(JavaParser.LiteralContext ctx) {
        if (ctx.NULL_LITERAL() != null) {
            includes.add("#include <stdlib.h>");
            out.print("NULL");
        }
        else out.print(ctx.getText());
        super.visitLiteral(ctx);
        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        var text = ctx.getText();
        if (fullClassNames.containsKey(text))
            out.print(fullClassNames.get(text));
        else if (methodNameFull.containsKey(text))
            out.print(methodNameFull.get(text));
        else {
            var cName = camelCaseToSnakeCase(text);
            out.print(cName);
        }
        return null;
    }

    @Override
    public Void visitPrimary(JavaParser.PrimaryContext ctx) {
        if (ctx.THIS() != null)  out.print("this");
        if (ctx.LPAREN() != null) out.print('(');
        super.visitPrimary(ctx);
        if (ctx.RPAREN() != null) out.print(')');
        return null;
    }

    @Override
    public Void visitExpressionList(JavaParser.ExpressionListContext ctx) {
        int i = 0;
        if(ctx == null) return null;
        for (JavaParser.ExpressionContext expression : ctx.expression()) {
            visitExpression(expression);
            if(i < ctx.expression().size() - 1) out.print(", ");
            i++;
        }
        return null;
    }

    @Override
    public Void visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        currentType = ctx.typeType();
        var currentTypeName = currentType.getText();
        currentTypePointer = fullClassNames.containsKey(currentTypeName);
        visitTypeType(currentType);
        out.print(" ");
        visitVariableDeclarators(ctx.variableDeclarators());
        currentType = null;
        currentTypePointer = false;
        return null;
    }

    private void checkInterfaceCreation(String name, Runnable runnable) {
        if (name == null) {
            runnable.run();
            return;
        }

        var fullClassName = fullClassNames.get(name);
        var currentTypeName = currentType != null ? currentType.getText()
                : null;
        boolean interfaceCreation = currentTypeName != null && !name.equals(currentTypeName);
        if (interfaceCreation) {
            out.print(fullClassName);
            out.print("__to_");
            out.print(fullClassNames.get(currentTypeName));
            out.print("(");
        }
        runnable.run();
        if (interfaceCreation) {
            out.print(")");
        }
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        var name = ctx.createdName().identifier(0).getText();
        var fullClassName = fullClassNames.get(name);
        checkInterfaceCreation(name, () -> {
            out.print(fullClassName + "_new");
            visitClassCreatorRest(ctx.classCreatorRest());
        });
        return null;
    }

    @Override
    public Void visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        var className = ctx.identifier().stream().map(i -> i.getText())
                    .collect(Collectors.joining("."));
        if (!header)
            unusedImports.remove(className);
        if ("Array".equals(className)) {
            var typeArgument = ctx.typeArguments().get(0).typeArgument().get(0).typeType();
            var typeArgumentString = typeArgument.classOrInterfaceType().getText();
            if (!insideMethodCall) {
                visitTypeType(typeArgument);
            }
            lastTypeArray = true;
            currentTypePointer = true;
            lastTypeWrapped = fullClassNames.get(typeArgumentString);
            pureLastTypeWrapped = pureClassNames.get(typeArgumentString);
        } else if ("OOPIterable".equals(className)) {
            var typeArgument = ctx.typeArguments().get(0).typeArgument().get(0).typeType();
            var typeArgumentString = typeArgument.classOrInterfaceType().getText();
            if (!insideMethodCall) {
                if (insideIterableAsArray)
                    visitTypeType(typeArgument);
                else
                    out.print("void");
            }
            lastTypeIterable = true;
            anyTypeIterable = true;
            currentTypePointer = true;
            if (!"Object".equals(typeArgumentString)) {
                lastTypeWrapped = fullClassNames.get(typeArgumentString);
                pureLastTypeWrapped = pureClassNames.get(typeArgumentString);
            } else {
                lastTypeWrapped = "void";
                pureLastTypeWrapped = lastTypeWrapped;
            }
        } else if ("String".equals(className) || "java.lang.String".equals(className)) {
            if (!insideMethodCall) {
                out.print("char");
            }
            currentTypePointer = true;
            lastTypeWrapped = "otterop_lang_String";
            pureLastTypeWrapped = "char";
        } else if ("Integer".equals(className)) {
            out.print("int");
        } else if ("Object".equals(className)) {
            currentTypePointer = true;
            if (!insideMethodCall)
                out.print("void");
        } else if ("Boolean".equals(className)) {
            out.print("int");
        } else if (currentTypeParameters.contains(className) ||
                currentMethodTypeParameters.contains(className)) {
            currentTypePointer = true;
            if (!insideMethodCall)
                out.print("void");
        } else {
            unusedImports.remove(className);
            currentTypePointer = true;
            checkCurrentPackageImports(className);
            if (!insideMethodCall) {
                var pureClass = pureClassNames.get(className);
                if (pureClass == null) pureClass = purePackagePrefix + "_" + className;
                checkCurrentPackageImports(pureClass);
                out.print(pureClass);
                out.print("_t");
            }
            lastTypeWrapped = fullClassNames.get(className);
            pureLastTypeWrapped = pureClassNames.get(className);
        }
        return null;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        if (!insideMethodCall) {
            if ("boolean".equals(ctx.getText()))
                out.print("int");
            else
                out.print(ctx.getText());
        }
        currentTypePointer = false;
        return null;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        var identifiers = ctx.qualifiedName().identifier();
        var stringIdentifiers = identifiers.stream().map(
                identifier ->
                        identifier.getText()
        ).collect(Collectors.toList());

        packagePrefix = stringIdentifiers.stream().map(
                identifier ->
                    camelCaseToSnakeCase(identifier)
        ).collect(Collectors.joining("_"));
        purePackagePrefix = packagePrefix + "_pure";
        var javaPackagePrefixList = stringIdentifiers.stream().collect(Collectors.toList());
        javaPackagePrefix = javaPackagePrefixList.stream().collect(Collectors.joining("."));
        javaPackagePrefixList.add("pure");
        javaPurePackagePrefix = javaPackagePrefixList.stream().collect(Collectors.joining("."));
        return null;
    }

    public void printTo(PrintStream ps) {
        if (header) {
            ps.println("#ifndef __" + pureFullClassName);
            ps.println("#define __" + pureFullClassName);
        }
        for (String className : unusedImports.keySet()) {
            classIncludes.remove(className);
            classIncludesOtterop.remove(className);
            predeclarations.remove(className);
        }
        for (String includeStatement : includes) {
            ps.println(INDENT.repeat(indents) + includeStatement);
        }
        for (String includeStatement : classIncludesOtterop.values()) {
            ps.println(INDENT.repeat(indents) + includeStatement);
        }
        for (String includeStatement : classIncludes.values()) {
            ps.println(INDENT.repeat(indents) + includeStatement);
        }

        if (!predeclarations.isEmpty())
            ps.println();
        for (String predeclaration : predeclarations.values()) {
            ps.println(predeclaration);
        }

        ps.println("");
        ps.print(outStream.toString());
        if (hasMain) {
            ps.print("\n\nint main(int args_cnt, char *args_arr[])");
            if (!header) {
                ps.print(" {\n");
                ps.print(INDENT + "args_cnt = args_cnt - 1;\n");
                ps.print(INDENT + "char **args = args_arr + 1;\n");
                ps.print(INDENT + pureFullClassName + "_main(args_cnt, args);\n");
                ps.print("}");
            } else {
                ps.print(";\n");
            }
        }
        if (header) {
            ps.println("#endif");
        }
    }

    public Map<String,String> getFullClassNames() {
        return new HashMap<>(this.fullClassNames);
    }
}
