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


package otterop.transpiler.visitor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import otterop.transpiler.Otterop;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.CaseUtil;
import otterop.transpiler.util.MapStack;
import otterop.transpiler.util.PrintStreamStack;
import otterop.transpiler.visitor.clazz.CClassVisitor;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;
import static otterop.transpiler.util.CaseUtil.isClassName;

public class CParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean memberPrivate = false;
    private boolean memberInternal = false;
    private boolean isMain = false;
    private boolean hasMain = false;
    private boolean insideConstructor = false;
    private boolean insideMemberDeclaration = false;
    private boolean insideFormalParameters = false;
    private boolean insideStaticField = false;
    private boolean insideReturnType = false;
    private boolean isIterableMethod = false;
    private String className = null;
    private String basePackage = null;
    private String fullClassName = null;
    private String fullClassNameType = null;
    private String superClassName = null;
    private String fullSuperClassName = null;
    private int indents = 0;
    private int skipNewlines = 0;
    private static final String INDENT = "    ";
    private static final String THIS = "self";
    private static final String SUPER = "super";
    private static final String MALLOC = "GC_malloc";
    private String packagePrefix = null;
    private String javaPackagePrefix = null;
    private String currentTypeName = null;
    private String currentTypeFullClassName = null;
    private String returnTypeFullClassName = null;
    private String enhancedForControlVariable = null;
    private boolean currentTypePointer = false;
    private String currentInstanceName;
    private Set<String> includes = new LinkedHashSet<>();
    private Set<String> predeclarations = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Set<String> publicMethods = new LinkedHashSet<>();
    private Set<String> privateMethods = new LinkedHashSet<>();
    private Set<String> internalMethods = new LinkedHashSet<>();
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
    private boolean constructorPrivate;
    private boolean constructorInternal;
    private JavaParser.ConstructorDeclarationContext constructor;
    private Map<String,String> fullClassNames = new LinkedHashMap<>();
    private Map<String,String> javaFullClassNames = new LinkedHashMap<>();
    private Map<String,String> methodNameFull = new LinkedHashMap<>();
    private Set<String> arrayArgs = new LinkedHashSet<>();
    private Map<String, String> fieldVariableType = new LinkedHashMap<>();
    private MapStack<String, String> variableType = new MapStack<>();
    private PrintStreamStack out = new PrintStreamStack(outStream);
    private Map<String,String> importDomainMapping;
    private boolean header = false;
    private boolean internalHeader = false;
    private boolean makePure = false;
    private boolean isTestMethod = false;
    private boolean isInterface = false;
    private boolean hasConstructor = false;
    private boolean isTestClass = false;
    private boolean classPublic = false;
    private Set<String> currentTypeParameters = new HashSet<>();
    private Set<String> currentMethodTypeParameters = new HashSet<>();
    private JavaParser.TypeParametersContext methodTypeParametersContext;
    private ClassReader classReader;
    private Map<String,String> headerFullClassNames = Collections.emptyMap();
    private List<JavaParser.MethodDeclarationContext> testMethods = new LinkedList<>();
    private CClassVisitor classVisitor;

    public CParserVisitor(ClassReader classReader,
                          boolean header,
                          boolean internalHeader,
                          CParserVisitor headerVisitor,
                          Map<String,String> importDomainMapping,
                          String basePackage) {
        this.header = header;
        this.internalHeader = internalHeader;
        this.basePackage = basePackage;
        if (headerVisitor != null) {
            headerFullClassNames = headerVisitor.getFullClassNames();
            this.isTestClass = headerVisitor.testClass();
        }
        this.classReader = classReader;
        this.classVisitor = new CClassVisitor(this);
        this.importDomainMapping = new HashMap<>(importDomainMapping);
    }

    private void detectMethods(ParserRuleContext ctx) {
        List<ParseTree> children = new ArrayList<>(ctx.children);
        boolean methodPublic = false;
        boolean methodPrivate = false;
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
                if (!methodPrivate) methodPrivate = modifierChild.PRIVATE() != null;
                if (!methodStatic) methodStatic = modifierChild.STATIC() != null;
            }
            if (child instanceof JavaParser.InterfaceCommonBodyDeclarationContext) {
                JavaParser.InterfaceCommonBodyDeclarationContext declarationChild = (JavaParser.InterfaceCommonBodyDeclarationContext) child;
                interfaceMethods.add(declarationChild);
                var name = declarationChild.identifier().getText();
                publicMethods.add(name);
                methodPublic = false;
                methodPrivate = false;
                methodStatic = false;
            }
            if (child instanceof JavaParser.MethodDeclarationContext) {
                JavaParser.MethodDeclarationContext declarationChild = (JavaParser.MethodDeclarationContext) child;
                var name = declarationChild.identifier().getText();
                methods.add(declarationChild);
                methodsMap.put(name, declarationChild);
                if (methodPublic) publicMethods.add(name);
                else if (methodPrivate) privateMethods.add(name);
                else internalMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodPrivate = false;
                methodStatic = false;
            }
            if (child instanceof JavaParser.ConstructorDeclarationContext) {
                if (methodPublic) constructorPublic = true;
                else if (methodPrivate) constructorPrivate = true;
                else constructorInternal = true;
                constructor = (JavaParser.ConstructorDeclarationContext) child;
            }
            if (child instanceof JavaParser.GenericMethodDeclarationContext) {
                JavaParser.GenericMethodDeclarationContext declarationChild = (JavaParser.GenericMethodDeclarationContext) child;
                var name = declarationChild.methodDeclaration().identifier().getText();
                genericMethods.add(declarationChild);
                genericMethodsMap.put(name, declarationChild);
                if (methodPublic) publicMethods.add(name);
                else if (methodPrivate) privateMethods.add(name);
                else internalMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodPrivate = false;
                methodStatic = false;
                skipChildren = true;
            }
            if (child instanceof JavaParser.AnnotationContext) {
                JavaParser.AnnotationContext annotationChild = (JavaParser.AnnotationContext) child;
                this.visitAnnotation(annotationChild);
                skipChildren = true;
            }
            if (child instanceof JavaParser.FieldDeclarationContext) {
                JavaParser.FieldDeclarationContext declarationChild = (JavaParser.FieldDeclarationContext) child;
                var name = declarationChild.variableDeclarators().variableDeclarator(0).variableDeclaratorId().getText();
                if (methodPublic && methodStatic) headerStaticFields.put(name, declarationChild);
                methodPublic = false;
                methodPrivate = false;
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
        isInterface = true;
        detectMethods(ctx);
        out.print(INDENT.repeat(indents));
        visitTypeParameters(ctx.typeParameters(), true, false);
        out.print("typedef struct ");
        className = ctx.identifier().getText();
        fullClassName = packagePrefix + "_" + className;
        fullClassNames.put(className, fullClassName);
        fullClassNameType = fullClassName + "_t";
        out.print(fullClassName);
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
        out.print(fullClassNameType);
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
            addToIncludes(fullClassName);
            super.visitInterfaceBody(ctx.interfaceBody());
        }

        return null;
    }

    private void implementInterfaces(List<JavaParser.TypeTypeContext> typeTypes) {
        for (JavaParser.TypeTypeContext typeType : typeTypes) {
            if (!typeType.classOrInterfaceType().isEmpty()) {
                var className = typeType.classOrInterfaceType().identifier(0).getText();
                var javaFullClassName = this.javaFullClassNames.getOrDefault(className, className);
                if (!javaFullClassName.contains(".")) {
                    if ("Iterable".equals(className))
                        continue;
                    javaFullClassName = javaPackagePrefix + "." + javaFullClassName;
                }
                var interfaceFullClassName = Arrays.stream(javaFullClassName.split("\\."))
                        .collect(Collectors.joining("_"));
                if (header) addToIncludes(interfaceFullClassName);

                final String finaljavaFullClassName = javaFullClassName;
                Class<?> interfaceClass = classReader.getClass(javaFullClassName);
                var methods = classReader.findMethods(interfaceClass);
                out.print("\n");
                out.print(interfaceFullClassName);
                out.print("_t\n");
                out.print("*");
                out.print(fullClassName + "__to_" + interfaceFullClassName);
                out.print("(");
                out.print(fullClassNameType);
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
                    out.print(fullClassName);
                    out.print("_");
                    out.print(camelCaseToSnakeCase(method));
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
        var annotationName = ctx.qualifiedName().identifier().get(0).getText();
        var fullAnnotationName = javaFullClassNames.get(annotationName);
        makePure |= Otterop.WRAPPED_CLASS.equals(fullAnnotationName);
        boolean isTestAnnotation = Otterop.TEST_ANNOTATION.equals(fullAnnotationName);
        if (isTestAnnotation) {
            this.isTestMethod = true;
            this.isTestClass = true;
        }
        return null;
    }

    @Override
    public Void visitClassOrInterfaceModifier(JavaParser.ClassOrInterfaceModifierContext ctx) {
        if (ctx.getParent() instanceof JavaParser.TypeDeclarationContext) {
            this.classPublic = ctx.PUBLIC() != null;
        }
        return super.visitClassOrInterfaceModifier(ctx);
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        detectMethods(ctx);
        visitTypeParameters(ctx.typeParameters(), true, false);
        if (!header && ctx.EXTENDS() != null) {
            out.print(INDENT.repeat(indents));
            superClassName = ctx.typeType().getText();
            checkCurrentPackageImports(superClassName);
            if (!fullClassNames.containsKey(superClassName))
                addClassToFullClassNames(superClassName);
            fullSuperClassName = fullClassNames.get(superClassName);
        }
        out.print("typedef struct ");
        className = ctx.identifier().getText();
        fullClassName = packagePrefix + "_" + className;
        fullClassNames.put(className, fullClassName);
        javaFullClassNames.put(className, javaPackagePrefix + "." + className);
        fullClassNameType = fullClassName + "_t";

        out.print(fullClassName);
        out.print("_s");
        if (header) {
            out.print(" ");
        } else {
            out.print(" {\n");
            indents++;
            if (superClassName != null) {
                out.print(INDENT.repeat(indents));
                out.print(fullSuperClassName);
                out.println("_t *_super;");
            }
            for (var field : fields) {
                out.print(INDENT.repeat(indents));
                visitTypeType(field.typeType());
                var variableDeclaratorId = field.variableDeclarators()
                        .variableDeclarator(0).variableDeclaratorId();
                String variableDeclaratorText = variableDeclaratorId.getText();
                String typeName = typeName(field.typeType());
                this.fieldVariableType.put(variableDeclaratorText, typeName);
                this.fieldVariableType.put("this." + variableDeclaratorText, typeName);
                out.print(" ");
                visitVariableDeclaratorId(variableDeclaratorId);
                out.print(";\n");
            }
            indents--;
            out.print("} ");
        }
        out.print(fullClassNameType);
        out.println(";\n\n");
        // Pre declare all public methods in header
        // and all private methods in implementation
        // to avoid declaration order problems
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
            memberPrivate = privateMethods.contains(methodName);
            memberInternal = internalMethods.contains(methodName);
            memberStatic = staticMethods.contains(methodName);
            if (header && memberPublic ||
                header && memberInternal && internalHeader ||
                !header && memberPrivate)
                visitMethodDeclaration(method);
            insideMemberDeclaration = false;
        }
        for(var method : genericMethods) {
            insideMemberDeclaration = true;
            var methodName = method.methodDeclaration().identifier().getText();
            memberPublic = publicMethods.contains(methodName);
            memberPrivate = privateMethods.contains(methodName);
            memberInternal = internalMethods.contains(methodName);
            memberStatic = staticMethods.contains(methodName);
            if (header && memberPublic ||
                header && memberInternal && internalHeader ||
                !header && memberPrivate)
                visitGenericMethodDeclaration(method);
            insideMemberDeclaration = false;
        }
        if (!header) {
            addToIncludes(fullClassName);
            for (var entry : this.fieldVariableType.entrySet())
                this.variableType.put(entry.getKey(), entry.getValue());
            this.variableType.newContext();
            super.visitClassBody(ctx.classBody());
            this.variableType.endContext();
        }

        if (ctx.IMPLEMENTS() != null) {
            out.println();
            implementInterfaces(ctx.typeList().get(0).typeType());
        }

        out.println();
        Class<?> currentClass = classReader.getClass(javaFullClassNames.get(className));
        var methods = classReader.findInheritedMethods(currentClass);
        for (var method : methods) {
            out.println();
            this.classVisitor.visitMethod(method);
        }

        return null;
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        if (header && constructorPrivate)
            return null;
        if (header && constructorInternal && !internalHeader)
            return null;
        hasConstructor = true;
        insideConstructor = true;
        out.print("\n\n");
        out.print(fullClassNameType);
        out.print(" *");
        out.print(fullClassName);
        out.print("_new");
        visitFormalParameters(ctx.formalParameters());
        if (insideMemberDeclaration) out.print(";");
        else visitBlock(ctx.block());
        insideConstructor = false;
        return null;
    }

    @Override
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = false;
        this.memberPrivate = false;
        this.memberInternal = false;
        this.isTestMethod = false;
        return super.visitClassBodyDeclaration(ctx);
    }

    private void interfaceConstructorImplementation() {
        out.print(fullClassNameType);
        out.print(" *");
        out.print(fullClassName);
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
        printMalloc(THIS, fullClassNameType);
        out.print(INDENT.repeat(indents));
        out.print(THIS + "->implementation = implementation;\n");
        for (var method: interfaceMethods) {
            out.print(INDENT.repeat(indents));
            out.print(THIS + "->");
            var name = method.identifier().getText();
            name = camelCaseToSnakeCase(name);
            out.print(name);
            out.print(" = ");
            out.print(name);
            out.print(";\n");
        }
        out.print(INDENT.repeat(indents));
        out.print("return " + THIS + ";\n");
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
        out.print(THIS + "->");
        out.print(methodName);
        out.print("(" + THIS + "->implementation");
        if (ctx.formalParameters().formalParameterList() != null) {
            for (var arg : ctx.formalParameters().formalParameterList().formalParameter()) {
                out.print(", ");
                visitVariableDeclaratorId(arg.variableDeclaratorId());
            }
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
    }

    private void visitImplementationMethodType(JavaParser.MethodDeclarationContext ctx) {
        currentTypePointer = false;
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        if (currentTypePointer) {
            out.print(" *");
            currentTypePointer = false;
        }
        out.print(" (*)");
        this.interfaceImplementationThis = true;
        this.printParameterNames = false;
        visitFormalParameters(ctx.formalParameters());
        this.printParameterNames = true;
        this.interfaceImplementationThis = false;
    }

    @Override
    public Void visitInterfaceCommonBodyDeclaration(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        visitTypeParameters(methodTypeParametersContext, false, true);
        currentTypePointer = false;
        variableType.newContext();
        arrayArgs.clear();
        out.print("\n\n");
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        if (currentTypePointer) {
            out.print("*");
            currentTypePointer = false;
        }
        var name = ctx.identifier().getText();
        if (!isMain) name = camelCaseToSnakeCase(name);
        out.print(fullClassName + "_" + name);
        visitFormalParameters(ctx.formalParameters());
        if (insideMemberDeclaration) out.print(";\n");
        else interfaceMethodImplementation(ctx, name);
        arrayArgs.clear();
        variableType.endContext();
        return null;
    }

    @Override
    public Void visitGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        this.methodTypeParametersContext = ctx.typeParameters();
        return visitMethodDeclaration(ctx.methodDeclaration());
    }

    public void visitMethodName(String methodName, boolean superClass) {
        out.print(" ");
        if (currentTypePointer) {
            out.print("*");
            currentTypePointer = false;
        }
        isMain = methodName.equals("main");
        if (isMain) hasMain = true;
        if (!isMain) methodName = camelCaseToSnakeCase(methodName);
        if (superClass)
            out.print(fullSuperClassName + "_" + methodName);
        else
            out.print(fullClassName + "_" + methodName);
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (isTestMethod)
            testMethods.add(ctx);

        currentTypePointer = false;
        variableType.newContext();
        arrayArgs.clear();
        returnTypeFullClassName = null;

        out.startCapture();
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else {
            insideReturnType = true;
            visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
            insideReturnType = false;
        }
        String typeTypeString = out.endCapture();

        if (isIterableMethod) {
            isIterableMethod = false;
            return null;
        }

        out.print("\n\n");
        visitTypeParameters(methodTypeParametersContext, false, true);
        out.print(typeTypeString);
        visitMethodName(ctx.identifier().getText(), false);
        visitFormalParameters(ctx.formalParameters());
        if (insideMemberDeclaration) out.print(";\n");
        else visitMethodBody(ctx.methodBody());
        this.isMain = false;
        arrayArgs.clear();
        variableType.endContext();
        returnTypeFullClassName = null;
        return null;
    }

    public void visitFormalParametersAnd(boolean printTypes, boolean useSuper, Consumer<Boolean> r) {
        insideFormalParameters = true;
        out.print("(");
        var addThis = !memberStatic && !insideConstructor && !interfaceImplementationThis;
        if (addThis) {
            if (printTypes) {
                if (useSuper) {
                    out.print(fullSuperClassName + "_t");
                } else {
                    out.print(fullClassNameType);
                }
                out.print(" *");
            }
            if (printParameterNames) {
                if (useSuper) {
                    out.print(THIS);
                    out.print("->_super");
                } else {
                    out.print(THIS);
                }
            }
        }
        if (interfaceImplementationThis) {
            if (printTypes) {
                out.print("void *");
            }
            if (printParameterNames)
                out.print(THIS);
        }
        var hasComma = addThis || interfaceImplementationThis;
        r.accept(hasComma);
        out.print(")");
        insideFormalParameters = false;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        visitFormalParametersAnd(true, false, (hasComma) -> {
            if (ctx.formalParameterList() != null) {
                if (hasComma) out.print(", ");
                visitFormalParameterList(ctx.formalParameterList());
            }
        });
        return null;
    }

    @Override
    public Void visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        boolean isLast = ctx.getParent().children.get(ctx.getParent().getChildCount()-1) == ctx;
        var parameterName = ctx.variableDeclaratorId().getText();
        var parameterType = ctx.typeType();
        var isJavaArray = parameterType.LBRACK().size() > 0;
        variableType.put(parameterName, typeName(parameterType));
        var declaratorId = ctx.variableDeclaratorId();
        visitTypeType(parameterType);
        out.print(" ");
        if (isJavaArray)
            out.print("*");

        if (printParameterNames) {
            visitVariableDeclaratorId(declaratorId);
        } else if (currentTypePointer) {
            out.print("*");
        }

        currentTypePointer = false;
        if (isJavaArray) {
            out.print(", ");
            arrayArgs.add(parameterName);
            usesStdint();
            out.print("int32_t ");
            if (printParameterNames) {
                visitVariableDeclaratorId(declaratorId);
                out.print("_cnt");
            }
        }
        if (!isLast) out.print(", ");
        return null;
    }

    @Override
    public Void visitModifier(JavaParser.ModifierContext ctx) {
        if (ctx.getText().equals("static"))
            memberStatic = true;
        if (ctx.getText().equals("public"))
            memberPublic = true;
        if (ctx.getText().equals("private"))
            memberPrivate = true;
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
        variableType.newContext();
        indents++;
        if (enhancedForControlVariable != null) {
            out.print(INDENT.repeat(indents));
            out.print(enhancedForControlVariable);
            out.println(";");
            this.enhancedForControlVariable = null;
        }

        var insideConstructorFirst = this.insideConstructor;
        this.insideConstructor = false;
        if (insideConstructorFirst) {
            printMalloc(THIS, fullClassNameType);
        }
        super.visitBlock(ctx);
        if (insideConstructorFirst) {
            out.print(INDENT.repeat(indents));
            out.print("return " + THIS + ";\n");
        }
        variableType.endContext();
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
                checkSingleLineStatement(ctx.statement(1).statement(0));
                checkElseStatement(ctx.statement(1));
            } else {
                out.print(" else");
                checkSingleLineStatement(ctx.statement(1));
            }
        }
    }

    private void checkSingleLineStatement(JavaParser.StatementContext ctx) {
        if (ctx.SEMI() != null) {
            out.println(" {");
            indents++;
            out.print(INDENT.repeat(indents));
            if (enhancedForControlVariable != null) {
                out.print(enhancedForControlVariable);
                out.println(";");
                out.print(INDENT.repeat(indents));
                this.enhancedForControlVariable = null;
            }
        }
        visitStatement(ctx);
        if (ctx.SEMI() != null) {
            out.println();
            indents--;
            out.print(INDENT.repeat(indents));
            out.println("}");
            out.print(INDENT.repeat(indents));
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
            checkSingleLineStatement(ctx.statement(0));
            checkElseStatement(ctx);
        } else if (ctx.FOR() != null) {
            var forControl = ctx.forControl();
            variableType.newContext();
            if (forControl.enhancedForControl() != null) {
                JavaParser.EnhancedForControlContext enhancedForControlContext = forControl.enhancedForControl();
                out.startCapture();
                visitExpression(enhancedForControlContext.expression());
                String iterableVariableName = out.endCapture();
                String iteratorVariableName = "__it_" + iterableVariableName.replaceAll("->", "_");

                out.startCapture();
                visitTypeType(enhancedForControlContext.typeType());
                String elementVariableType = enhancedForControlContext.typeType().classOrInterfaceType().getText();
                out.print(" ");

                visitVariableDeclaratorId(enhancedForControlContext.variableDeclaratorId());
                String variableName = enhancedForControlContext.variableDeclaratorId().getText();

                out.print(" = otterop_lang_OOPIterator_next(");
                out.print(iteratorVariableName);
                out.print(")");
                enhancedForControlVariable = out.endCapture();

                variableType.put(variableName, elementVariableType);

                out.print("otterop_lang_OOPIterator_t *");

                out.print(iteratorVariableName + " = ");
                String iterableClassName =
                        variableType.get(enhancedForControlContext.expression().getText());
                out.print(fullClassNames.get(iterableClassName));
                out.print("_oop_iterator(");
                out.print(iterableVariableName);
                out.print(");\n");
                out.print(INDENT.repeat(indents));
                out.print("for (; otterop_lang_OOPIterator_has_next(");
                out.print(iteratorVariableName + ");)");
            } else {
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
            }
            checkSingleLineStatement(ctx.statement(0));
            variableType.endContext();
        } else {
            if (ctx.RETURN() != null) {
                this.currentTypeFullClassName = this.returnTypeFullClassName;
                out.print("return ");
            }
            super.visitStatement(ctx);
            if (ctx.SEMI() != null) {
                out.print(";");
            }
            this.currentTypeFullClassName = null;
        }
        return null;
    }

    @Override
    public Void visitVariableDeclarator(JavaParser.VariableDeclaratorContext ctx) {
        var variableName = ctx.variableDeclaratorId().getText();
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        if (!insideMemberDeclaration && ctx.variableInitializer() != null) {
            out.print(" = ");
            var variableInitializerText = ctx.variableInitializer().getText();
            if (variableInitializerText.startsWith(THIS + "."))
                variableInitializerText = variableInitializerText.substring(THIS.length() + 1);
            String className = null;
            if (variableType.containsKey(variableInitializerText)) {
                className = variableType.get(variableInitializerText);
            }
            variableType.put(variableName, currentTypeName);
            checkInterfaceCreation(className, () -> {
                super.visitVariableInitializer(ctx.variableInitializer());
            });
        } else {
            variableType.put(variableName, currentTypeName);
        }
        return null;
    }

    public void visitVariableDeclaratorId() {
        if (currentTypePointer) out.print("*");
        if (insideStaticField) {
            out.print(fullClassName);
            out.print("_");
        }
    }

    @Override
    public Void visitVariableDeclaratorId(JavaParser.VariableDeclaratorIdContext ctx) {
        this.visitVariableDeclaratorId();
        return super.visitVariableDeclaratorId(ctx);
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        var first = ctx.getParent().getChild(0) == ctx;

        String methodName;
        if (ctx.SUPER() != null)
            methodName = THIS + "->_super = " + fullSuperClassName + "_new";
        else {
            methodName = ctx.identifier().getText();
            if (first && methodNameFull.containsKey(methodName))
                methodName = methodNameFull.get(methodName);
            else if (first) {
                currentInstanceName = THIS;
                out.print(fullClassName);
                out.print("_");
                methodName = camelCaseToSnakeCase(methodName);
            }
            else
                methodName = camelCaseToSnakeCase(methodName);
        }

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

    private String typeName(JavaParser.TypeTypeContext ctx) {
        if (ctx.classOrInterfaceType() != null) {
            return ctx.classOrInterfaceType().identifier(0).getText();
        } else if (ctx.primitiveType() != null) {
            return ctx.primitiveType().getText();
        }
        return null;
    }

    private void changeCurrentTypeName(String currentTypeName) {
        this.currentTypeName = currentTypeName;
        if (currentTypeName == null) {
            this.currentTypeFullClassName = null;
        } else if (javaFullClassNames.get(currentTypeName) != null) {
            this.currentTypeFullClassName = fullClassNames.get(currentTypeName);
        }
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
                if ("this".equals(name) || className.equals(name)) {
                    if ("this".equals(name)) {
                        currentInstanceName = THIS;
                    }
                    out.print(fullClassName + "_");
                } else if (SUPER.equals(name)) {
                    currentInstanceName = THIS + "->_super";
                    out.print(fullSuperClassName + "_");
                } else if (variableType.containsKey(name)) {
                    changeCurrentTypeName(variableType.get(name));
                    currentInstanceName = camelCaseToSnakeCase(name.replace("this.", "self->"));
                    out.print(currentTypeFullClassName + "_");
                } else if (fullClassNames.containsKey(name)) {
                    var fullClassName = fullClassNames.get(name);
                    out.print(fullClassName + "_");
                } else if (isClassName(name)) {
                    addClassToFullClassNames(name);
                    out.print(fullClassNames.get(name) + "_");
                } else {
                    out.startCapture();
                    visitExpression(ctx.expression(0));
                    currentInstanceName = out.endCapture();
                    out.print(currentTypeFullClassName + "_");
                }
                visitMethodCall(ctx.methodCall());
            }
            if (!methodCall) {
                var name = ctx.expression(0).getText();
                var isClassName = isClassName(name);
                var bop = ctx.bop.getText();
                if ("=".equals(bop)) {
                    changeCurrentTypeName(variableType.get(name));
                }

                if (ctx.expression(0) != null) visitExpression(ctx.expression(0));
                if (!bop.equals(".")) bop = " " + bop + " ";
                else if (isClassName) bop = "_";
                else bop = "->";
                out.print(bop);
                if (ctx.expression(1) != null) visitExpression(ctx.expression(1));
                else if (ctx.identifier() != null) {
                    visitIdentifier(ctx.identifier());
                    var identifierText = ctx.identifier().getText();
                    if (fieldVariableType.containsKey(identifierText)) {
                        changeCurrentTypeName(fieldVariableType.get(identifierText));
                    }
                }
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
        return Otterop.WRAPPED_CLASS.equals(javaFullClassName) ||
               Otterop.TEST_ANNOTATION.equals(javaFullClassName) ||
               Otterop.ITERATOR.equals(javaFullClassName);
    }

    private String javaFullClassNameToC(List<String> identifiers) {
        String className = identifiers.get(identifiers.size() - 1);
        var prefixIdentifiers = identifiers.subList(0, identifiers.size() - 1);
        return prefixIdentifiers.stream()
                .map(identifier -> camelCaseToSnakeCase(identifier))
                .collect(Collectors.joining("_"))
                + "_" + className;
    }

    public void addToIncludes(List<String> identifiers) {
        String className = identifiers.get(identifiers.size() - 1);
        var isThisClass = this.className != null && this.className.equals(className);
        var prefixIdentifiers = identifiers.subList(0, identifiers.size() - 1);
        var includeIdentifiers = replaceBasePackageIdentifiers(prefixIdentifiers);
        var fileName = camelCaseToSnakeCase(className);
        var includeStr = String.join("/", includeIdentifiers);
        var fullClassName = javaFullClassNameToC(identifiers);
        var javaFullClassName = identifiers.stream()
                .collect(Collectors.joining("."));
        var samePackage = javaFullClassName.startsWith(basePackage + ".");

        var includeStatement = "#include <" + includeStr + "/" + fileName;
        if (samePackage && !(isThisClass && isTestClass) && (!header || internalHeader))
            includeStatement = "#include <" + includeStr + "/int/" + fileName;
        includeStatement += ".h>";
        var add = false;
        if (!excludeImports(javaFullClassName)) {
            if (header) {
                add = true;
            } else {
                add = isThisClass || !headerFullClassNames.containsKey(className);
            }
        }

        if (add) {
            includes.add(includeStatement);
            if (header)
                predeclarations.add("typedef struct " + fullClassName + "_s " + fullClassName + "_t;");
        }
        fullClassNames.put(className, fullClassName);
        javaFullClassNames.put(className, javaFullClassName);
    }

    public void addToIncludes(String fullClassName) {
        addToIncludes(List.of(fullClassName.split("_")));
    }

    private void checkCurrentPackageImports(String name) {
        if (CaseUtil.isClassName(name) && !fullClassNames.containsKey(name)) {
            addToIncludes(packagePrefix + "_" + name);
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
        var identifiersString = identifiers.subList(0, classNameIdx + 1).stream()
                .map(identifier -> identifier.getText()).collect(Collectors.toList());

        addToIncludes(identifiersString);
        if (isStatic) {
            String className = identifiersString.get(classNameIdx);
            var prefix = fullClassNames.get(className);
            var methodName = identifiers.get(classNameIdx + 1).getText();
            var methodNameSnake = camelCaseToSnakeCase(methodName);
            methodNameFull.put(methodName, prefix + "_" + methodNameSnake);
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
        } else if (ctx.BOOL_LITERAL() != null) {
            if ("true".equals(ctx.BOOL_LITERAL().getText()))
                out.print("1");
            else
                out.print("0");
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
            if (!insideFormalParameters && arrayArgs.contains(text)) {
                out.print(", ");
                out.print(cName);
                out.print("_cnt");
            }
        }
        return null;
    }

    @Override
    public Void visitPrimary(JavaParser.PrimaryContext ctx) {
        if (ctx.THIS() != null)  out.print(THIS);
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
        String typeName = typeName(ctx.typeType());
        currentTypePointer = fullClassNames.containsKey(currentTypeName);
        visitTypeType(ctx.typeType());
        changeCurrentTypeName(typeName);
        out.print(" ");
        visitVariableDeclarators(ctx.variableDeclarators());
        changeCurrentTypeName(null);
        currentTypePointer = false;
        return null;
    }

    private void checkInterfaceCreation(String name, Runnable runnable) {
        if (name == null) {
            runnable.run();
            return;
        }

        var fullClassName = fullClassNames.get(name);
        boolean interfaceCreation = currentTypeFullClassName != null && fullClassName != null &&
                !fullClassName.equals(currentTypeFullClassName);
        if (interfaceCreation) {
            out.print(fullClassName);
            out.print("__to_");
            out.print(currentTypeFullClassName);
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
        checkCurrentPackageImports(name);
        var fullClassName = fullClassNames.get(name);
        checkInterfaceCreation(name, () -> {
            out.print(fullClassName + "_new");
            visitClassCreatorRest(ctx.classCreatorRest());
        });
        return null;
    }

    public void visitClassOrInterfaceType(String className) {
        if ("Integer".equals(className)) {
            out.print("int");
        } else if ("Object".equals(className)) {
            currentTypePointer = true;
            out.print("void");
        } else if (Otterop.ITERATOR.equals(javaFullClassNames.get(className))) {
            this.isIterableMethod = true;
        } else if ("java.lang.String".equals(className)) {
            currentTypePointer = true;
            out.print("char");
        } else if ("Boolean".equals(className)) {
            out.print("int");
        } else if (currentTypeParameters.contains(className) ||
                currentMethodTypeParameters.contains(className)) {
            currentTypePointer = true;
            out.print("void");
        } else {
            checkCurrentPackageImports(className);
            String fullClassName = fullClassNames.get(className);
            if (this.insideReturnType) {
                this.returnTypeFullClassName = fullClassName;
            }
            currentTypePointer = true;
            out.print(fullClassName + "_t");
        }
    }

    @Override
    public Void visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        var className = ctx.identifier().stream().map(i -> i.getText())
                    .collect(Collectors.joining("."));
        visitClassOrInterfaceType(className);
        return null;
    }

    private void usesStdint() {
        includes.add("#include <stdint.h>");
    }

    public void visitPrimitiveType(String name) {
        switch (name) {
            case "boolean":
                out.print("unsigned char");
                break;
            case "byte":
                out.print("unsigned char");
                break;
            case "short":
                usesStdint();
                out.print("int16_t");
                break;
            case "int":
                usesStdint();
                out.print("int32_t");
                break;
            case "long":
                usesStdint();
                out.print("int64_t");
                break;
            default:
                out.print(name);
                break;
        }
        currentTypePointer = false;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        visitPrimitiveType(ctx.getText());
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
        javaPackagePrefix = stringIdentifiers.stream()
                .collect(Collectors.joining("."));
        return null;
    }

    private boolean mustPrintDefaultConstructor() {
        return !hasConstructor && !isInterface();
    }

    private void printDefaultConstructor(PrintStream ps) {
        if (!mustPrintDefaultConstructor())
            return;
        ps.print("\n");
        ps.print(fullClassNameType);
        ps.print("* ");
        ps.print(fullClassName);
        ps.print("_new()");
        if (isHeader()) {
            ps.print(";\n");
            return;
        }
        ps.print(" {\n");
        indents++;
        ps.print(INDENT.repeat(indents));
        ps.print(fullClassNameType);
        ps.print(" *" + THIS + " = ");
        ps.print(MALLOC);
        ps.print("(sizeof(*" + THIS + "));\n");
        if (this.superClassName != null) {
            ps.print(INDENT.repeat(indents));
            ps.print(THIS + "->_super = ");
            ps.print(fullSuperClassName);
            ps.print("_new();\n");
        }
        ps.print(INDENT.repeat(indents));
        ps.print("return " + THIS + ";\n");
        indents--;
        ps.print("}\n");
    }

    public boolean makePure() {
        return this.makePure && testMethods.isEmpty();
    }

    public boolean testClass() {
        return !testMethods.isEmpty();
    }

    public String fullClassName() {
        return this.fullClassName;
    }

    public void printTo(PrintStream ps) {
        if (header) {
            ps.print("#ifndef __" + fullClassName);
            if (internalHeader)
                ps.print("_int");
            ps.println();
            ps.print("#define __" + fullClassName);
            if (internalHeader)
                ps.print("_int");
            ps.println();
        }
        if (!header && !testMethods.isEmpty()) {
            ps.println("#include \"unity.h\"");
            ps.println("#include \"unity_fixture.h\"");
        }
        if (!isHeader() && mustPrintDefaultConstructor()) {
            includes.add("#include <gc.h>");
        }
        for (String includeStatement : includes) {
            ps.println(includeStatement);
        }

        if (!predeclarations.isEmpty())
            ps.println();
        for (String predeclaration : predeclarations) {
            ps.println(predeclaration);
        }

        ps.println("");
        ps.print(outStream.toString());

        printDefaultConstructor(ps);

        if (!header && !testMethods.isEmpty()) {
            ps.print("\nTEST_GROUP(");
            ps.print(fullClassName);
            ps.println(");");

            ps.print("\nTEST_SETUP(");
            ps.print(fullClassName);
            ps.println(") {}");

            ps.print("\nTEST_TEAR_DOWN(");
            ps.print(fullClassName);
            ps.println(") {}");

            for (var testMethod : testMethods) {
                var methodName = camelCaseToSnakeCase(testMethod.identifier().getText());
                var testName = fullClassName + "_" + methodName;
                var constructor = fullClassName + "_new()";
                ps.print("\nTEST(");
                ps.print(fullClassName);
                ps.print(", ");
                ps.print(testName);
                ps.println(") {");
                indents++;
                ps.print(INDENT.repeat(indents));
                ps.print(testName);
                ps.print("(");
                ps.print(constructor);
                ps.println(");");
                indents--;
                ps.print("}\n");
            }

            ps.print("\nTEST_GROUP_RUNNER(");
            ps.print(fullClassName);
            ps.println(") {");
            for (var testMethod : testMethods) {
                var methodName = camelCaseToSnakeCase(testMethod.identifier().getText());
                var testName = fullClassName + "_" + methodName;
                indents++;
                ps.print(INDENT.repeat(indents));
                ps.print("RUN_TEST_CASE(");
                ps.print(fullClassName);
                ps.print(", ");
                ps.print(testName);
                ps.print(");\n");
                indents--;
            }
            ps.println("}");
        }
        if (hasMain) {
            ps.print("\n\nint main(int args_cnt, char *args_arr[])");
            if (!header) {
                ps.print(" {\n");
                ps.print(INDENT + "args_cnt = args_cnt - 1;\n");
                ps.print(INDENT + "char **args = args_arr + 1;\n");
                ps.print(INDENT + fullClassName + "_main(args, args_cnt);\n");
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

    public PrintStream getPrintStream() {
        return this.out;
    }

    public void printIndents() {
        out.print(INDENT.repeat(indents));
    }

    public void increaseIndents() {
        indents++;
    }

    public void decreaseIndents() {
        indents--;
    }

    public boolean isHeader() {
        return this.header;
    }

    public boolean isInterface() {
        return this.isInterface;
    }

    public boolean isClassPublic() {
        return this.classPublic;
    }

    public boolean hasMain(){
        return this.hasMain;
    }

    public void setCurrentTypePointer(boolean currentTypePointer) {
        this.currentTypePointer = currentTypePointer;
    }
}
