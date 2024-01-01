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

import org.antlr.v4.runtime.tree.ParseTree;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.PrintStreamStack;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToPascalCase;
import static otterop.transpiler.util.CaseUtil.isClassName;

public class PureGoParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean constructorPublic = false;
    private boolean isMain = false;
    private boolean hasMain = false;
    private boolean insideConstructor = false;
    private boolean insideMethodCall = false;
    private boolean insideTypeArgument = false;
    private boolean insideForControl = false;
    private String lastTypeWrapped = null;
    private boolean lastTypeArray = false;
    private JavaParser.TypeTypeContext currentType = null;
    private String className = null;
    private String fullClassName = null;
    private String pureFullClassName = null;
    private String packageName = null;
    private String purePackageName = null;
    private String javaFullPackageName = null;
    private String fullPackageName = null;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private Set<String> imports = new LinkedHashSet<>();
    private Map<String,String> modules = new LinkedHashMap<>();
    private Map<String,String> unusedImports = new LinkedHashMap<>();
    private Map<String,String> fromImports = new LinkedHashMap<>();
    private Map<String,String> javaFullClassNames = new LinkedHashMap<>();
    private Set<String> staticImports = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Set<String> publicMethods = new LinkedHashSet<>();
    private Set<String> staticMethods = new LinkedHashSet<>();
    private Map<String,String> mappedArguments = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentArray = new LinkedHashMap<>();
    private Map<String,String> mappedArgumentClass = new LinkedHashMap<>();
    private Map<String, JavaParser.TypeTypeContext> variableType = new LinkedHashMap<>();
    private List<JavaParser.FieldDeclarationContext> fields = new LinkedList<>();
    private PrintStreamStack out = new PrintStreamStack(outStream);
    private Map<String,String> importDomainMapping;
    private JavaParser.TypeParametersContext classTypeParametersContext;
    private JavaParser.TypeParametersContext methodTypeParametersContext;
    private Set<String> currentTypeParameters = new HashSet<>();
    private Set<String> currentMethodTypeParameters = new HashSet<>();
    private ClassReader classReader;
    private Map<String,String> unwrappedClassName = new LinkedHashMap<>();
    private Map<String,String> classPackage = new LinkedHashMap<>();
    private Map<String,String> pureClassNames = new LinkedHashMap<>();
    private Map<String,String> fullClassNames = new LinkedHashMap<>();

    public PureGoParserVisitor(ClassReader classReader, Map<String,String> importDomainMapping) {
        this.classReader = classReader;
        this.importDomainMapping = new HashMap<>(importDomainMapping);
        this.unwrappedClassName.put("*otteropstring.String", "*string");
    }

    private void detectMethods(JavaParser.ClassDeclarationContext ctx) {
        List<ParseTree> children = new ArrayList<>(ctx.children);
        boolean methodPublic = false;
        boolean methodStatic = false;
        while (children.size() > 0) {
            ParseTree child = children.remove(0);
            if (child instanceof JavaParser.FieldDeclarationContext) {
                fields.add((JavaParser.FieldDeclarationContext) child);
            }
            if (child instanceof JavaParser.ClassOrInterfaceModifierContext) {
                JavaParser.ClassOrInterfaceModifierContext modifierChild = (JavaParser.ClassOrInterfaceModifierContext) child;
                methodPublic = modifierChild.PUBLIC() != null;
                methodStatic = modifierChild.STATIC() != null;
            }
            if (child instanceof JavaParser.MethodDeclarationContext) {
                JavaParser.MethodDeclarationContext declarationChild = (JavaParser.MethodDeclarationContext) child;
                var name = declarationChild.identifier().getText();
                if (methodPublic) publicMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodStatic = false;
            }
            if (child instanceof JavaParser.GenericMethodDeclarationContext) {
                JavaParser.GenericMethodDeclarationContext declarationChild = (JavaParser.GenericMethodDeclarationContext) child;
                var name = declarationChild.methodDeclaration().identifier().getText();
                if (methodPublic) publicMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodStatic = false;
            }
            for (int i = 0; i < child.getChildCount(); i++) {
                children.add(child.getChild(i));
            }
        }
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        out.print("type ");
        className = ctx.identifier().getText();
        packageName = "otterop" + className.toLowerCase();
        purePackageName = className.toLowerCase();
        this.visitIdentifier(ctx.identifier());
        out.print(" interface ");
        out.print("{\n");
        indents++;
        visitInterfaceBody(ctx.interfaceBody());
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}\n");
        return null;
    }

    @Override
    public Void visitAnnotation(JavaParser.AnnotationContext ctx) {
        return null;
    }

    private void wrapMethod() {
        out.print(INDENT.repeat(indents));
        out.print("func Wrap(otterop ");
        out.print(fullClassName);
        out.print(") ");
        out.print(pureFullClassName);
        out.print(" {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("ret := new(");
        out.print(className);
        out.print(")\n");
        out.print(INDENT.repeat(indents));
        out.print("ret.otterop = otterop\n");
        out.print(INDENT.repeat(indents));
        out.print("return ret\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    private void unwrapMethod() {
        out.print(INDENT.repeat(indents));
        out.print("func ");
        out.print("(" + THIS + " *" + className);
        out.print(") ");
        out.print("Unwrap() ");
        out.print(fullClassName);
        out.print(" {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("return this.otterop\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        detectMethods(ctx);
        out.print("type ");
        className = ctx.identifier().getText();
        javaFullClassNames.put(className, javaFullPackageName + "." + className);
        packageName = "otterop" + className.toLowerCase();
        purePackageName = className.toLowerCase();
        fullClassName = "*" + packageName + "." + className;
        pureFullClassName = "*" + className;
        pureClassNames.put(className, pureFullClassName);
        fullClassNames.put(className, fullClassName);
        classPackage.put(pureFullClassName, purePackageName);
        classPackage.put(fullClassName, packageName);

        var importString = "otterop" + purePackageName + " \"" + fullPackageName + "/" + purePackageName + "\"";
        imports.add(importString);
        this.visitIdentifier(ctx.identifier());
        this.classTypeParametersContext = ctx.typeParameters();
        printTypeParameters(this.classTypeParametersContext, true, false);
        out.print(" struct {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("otterop *" + packageName + ".");
        out.println(className);
        indents--;
        out.println("}\n\n");
        wrapMethod();
        super.visitClassBody(ctx.classBody());
        unwrapMethod();
        return null;
    }

    private void printTypeParameters(JavaParser.TypeParametersContext typeParametersContext, boolean classDeclaration, boolean methodDeclaration) {
        if (typeParametersContext != null) {
            out.print("[");
            boolean rest = false;
            if (methodDeclaration) {
                currentMethodTypeParameters.clear();
            }
            for (var t: typeParametersContext.typeParameter()) {
                if(rest)
                    out.print(", ");
                else
                    rest = true;
                if (classDeclaration) currentTypeParameters.add(t.identifier().getText());
                else if (methodDeclaration) {
                    currentMethodTypeParameters.add(t.identifier().getText());
                }
                visitIdentifier(t.identifier());
                out.print(" any");
            }
            out.print("]");
        }
    }

    private void printTypeArguments(List<JavaParser.TypeArgumentsContext> ctx) {
        if (ctx != null && !ctx.isEmpty()) {
            insideTypeArgument = true;
            out.print("[");
            boolean rest = false;
            for (var t: ctx) {
                for (var t1: t.typeArgument()) {
                    if (rest)
                        out.print(", ");
                    else
                        rest = true;
                    visitTypeType(t1.typeType());
                }
            }
            out.print("]");
            insideTypeArgument = false;
        }
    }

    private void printTypeArguments(JavaParser.TypeParametersContext ctx) {
        if (ctx != null && !ctx.isEmpty()) {
            out.print("[");
            boolean rest = false;
            for (var t1: ctx.typeParameter()) {
                if (rest)
                    out.print(", ");
                else
                    rest = true;
                visitIdentifier(t1.identifier());
            }
            out.print("]");
        }
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        if (!memberPublic) {
            return null;
        }

        out.print("\n\nfunc ");
        var name = ctx.identifier().getText();
        if (memberPublic && !isMain) name = camelCaseToPascalCase(name);
        constructorPublic = true;
        out.print("New");
        out.print(name);
        printTypeParameters(this.classTypeParametersContext, false, false);
        visitFormalParameters(ctx.formalParameters());
        out.print(" *" + className);
        printTypeArguments(classTypeParametersContext);
        out.print(" {\n");
        indents++;
        mapArguments();
        out.print(INDENT.repeat(indents));
        out.print("ret := new(");
        out.print(className);
        out.print(")\n");
        out.print(INDENT.repeat(indents));
        out.print("ret.otterop = " + packageName + ".New");
        out.print(className);
        insideMethodCall = true;
        visitFormalParameters(ctx.formalParameters());
        insideMethodCall = false;
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("return ret\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n\n");
        return null;
    }

    @Override
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = false;
        return super.visitClassBodyDeclaration(ctx);
    }

    @Override
    public Void visitInterfaceCommonBodyDeclaration(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        if (ctx.typeTypeOrVoid().VOID() == null) {
            out.print(" ");
            visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        }
        out.print(";\n");
        return null;
    }

    private String wrapArgName(String clazz, String argName) {
        if (pureFullClassName.equals(clazz)) {
            return "Wrap(" + argName + ")";
        }

        var importName = classPackage.get(clazz);
        unusedImports.remove(importName);
        return importName + ".Wrap(" + argName + ")";
    }

    private String mapArgument(String argName, String mappedClass) {
        if (unwrappedClassName.containsKey(mappedClass)) {
            return wrapArgName(mappedClass, argName);
        } else {
            return argName + ".Unwrap()";
        }
    }

    private String unmapArgument(String argName, String originalClass, String mappedClass) {
        if (unwrappedClassName.containsKey(originalClass)) {
            return argName + ".Unwrap()";
        } else {
            return wrapArgName(mappedClass, argName);
        }
    }

    private void mapArguments() {
        if (!mappedArguments.isEmpty()) {
            for (var entry: mappedArguments.entrySet()) {
                out.print(INDENT.repeat(indents));
                var paramName = entry.getKey();
                var mapperParamName = entry.getValue();
                var mappedClass = mappedArgumentClass.get(paramName);
                if (mappedArgumentArray.getOrDefault(paramName, false)) {
                    var mappedArrayName = mapperParamName + "Array";
                    out.print("var ");
                    out.print(mappedArrayName);
                    out.print(" = ");
                    out.print("make([]");
                    out.print(mappedClass);
                    out.print(", len(");
                    out.print(paramName);
                    out.print("))");
                    out.print("\n");
                    out.print(INDENT.repeat(indents));
                    out.print("for i := 0; i < len(");
                    out.print(paramName);
                    out.print("); i++ {\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print(mappedArrayName);
                    out.print("[i] = ");
                    out.print(mapArgument(paramName + "[i]", mappedClass));
                    out.print("\n");
                    indents--;
                    out.print(INDENT.repeat(indents));
                    out.print("}\n");
                    out.print(INDENT.repeat(indents));
                    out.print("var ");
                    out.print(entry.getValue());
                    out.print(" = otteroparray.Wrap(");
                    unusedImports.remove("otteroparray");
                    out.print(mappedArrayName);
                    out.print(");\n");
                } else {
                    out.print("var ");
                    out.print(paramName);
                    out.print(" = ");
                    out.print(mapArgument(entry.getKey(), mappedClass));
                    out.print("\n");
                }
            }
        }
    }

    @Override
    public Void visitGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        this.methodTypeParametersContext = ctx.typeParameters();
        return super.visitGenericMethodDeclaration(ctx);
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (!memberPublic) return null;

        var hasReturn = ctx.typeTypeOrVoid().VOID() == null;
        var returnTypeArray = false;
        String returnTypePure = null;
        String returnTypeString = null;
        if (hasReturn) {
            var returnType = ctx.typeTypeOrVoid().typeType().classOrInterfaceType();
            if (returnType != null) {
                returnTypeArray = returnType.identifier(0).getText().equals("Array");
                if (!returnTypeArray) {
                    returnTypeString = returnType.identifier().stream().map(i -> i.getText())
                            .collect(Collectors.joining("."));
                } else {
                    returnTypeString = returnType.typeArguments().get(0).typeArgument(0).typeType().getText();
                }
                returnTypePure = pureClassNames.get(returnTypeString);
            }
        }
        if (returnTypeString != null) {
            returnTypeString = fullClassNames.get(returnTypeString);
        }

        mappedArguments.clear();
        mappedArgumentArray.clear();
        mappedArgumentClass.clear();
        out.print(INDENT.repeat(indents));
        variableType.clear();
        out.print("\n\nfunc ");
        if (!memberStatic) {
            out.print("(" + THIS + " *" + className);
            printTypeArguments(classTypeParametersContext);
            out.print(") ");
        }
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        out.print(" ");
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" {\n");
        indents++;
        mapArguments();
        out.print(INDENT.repeat(indents));
        if (hasReturn)
            out.print("retOtterop := ");
        out.print("this.otterop.");
        out.print(name);
        this.insideMethodCall = true;
        visitFormalParameters(ctx.formalParameters());
        this.insideMethodCall = false;
        out.print("\n");
        if (hasReturn) {
            if (!returnTypeArray) {
                if (returnTypePure == null) {
                    out.print(INDENT.repeat(indents));
                    out.print("ret := retOtterop\n");
                } else {
                    if (returnTypePure.equals(pureClassNames.get(className))) {
                        out.print(INDENT.repeat(indents));
                        out.print("if retOtterop == this.otterop { return this }\n");
                    }
                    out.print(INDENT.repeat(indents));
                    out.print("ret := ");
                    out.print(
                            this.unmapArgument("retOtterop", returnTypeString, returnTypePure)
                    );
                    out.print(";\n");
                }
            } else {
                out.print(INDENT.repeat(indents));
                out.print("ret := make([]");
                out.print(returnTypePure);
                out.print(", retOtterop.Size())\n");
                out.print(INDENT.repeat(indents));
                out.print("for i := 0; i < retOtterop.Size(); i++ {\n");
                indents++;
                out.print(INDENT.repeat(indents));
                out.print("retI := retOtterop.Get(i)\n");
                if (returnTypePure.equals(pureClassNames.get(className))) {
                    out.print(INDENT.repeat(indents));
                    out.print("if retI == this.otterop {\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print("ret[i] = this\n");
                    out.print(INDENT.repeat(indents));
                    out.print("continue\n");
                    indents--;
                    out.print(INDENT.repeat(indents));
                    out.print("}\n");
                }
                out.print(INDENT.repeat(indents));
                out.print("ret[i] = ");
                out.print(this.unmapArgument("retI", returnTypeString, returnTypePure));
                out.print("\n");
                indents--;
                out.print(INDENT.repeat(indents));
                out.print("}\n");
            }
            out.print(INDENT.repeat(indents));
            out.print("return ret\n");
        }
        indents--;
        out.print("}\n\n");

        this.isMain = false;
        variableType.clear();
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        if (isMain || ctx.formalParameterList() == null) {
            out.print("()");
        } else {
            out.print("(");
            visitFormalParameterList(ctx.formalParameterList());
            out.print(")");
        }
        return null;
    }

    @Override
    public Void visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        boolean isLast = ctx.getParent().children.get(ctx.getParent().getChildCount()-1) == ctx;
        var parameterName = ctx.variableDeclaratorId().getText();
        var parameterType = ctx.typeType();
        lastTypeWrapped = null;
        lastTypeArray = false;
        this.out.startCapture();
        visitTypeType(ctx.typeType());
        if (lastTypeWrapped != null) {
            mappedArguments.put(parameterName, "_" + parameterName);
            mappedArgumentArray.put(parameterName, lastTypeArray);
            mappedArgumentClass.put(parameterName, lastTypeWrapped);
        }
        String type = this.out.endCapture();

        variableType.put(parameterName, parameterType);

        if (insideMethodCall && mappedArguments.containsKey(parameterName)) {
            out.print(mappedArguments.get(parameterName));
        } else {
            out.print(parameterName);
        }

        if (!insideMethodCall) {
            out.print(" ");
            out.print(type);
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
        super.visitModifier(ctx);
        return null;
    }

    @Override
    public Void visitBlock(JavaParser.BlockContext ctx) {
        out.print(" {\n");
        indents++;
        if (isMain) {
            imports.add("\"os\"");
            out.print(INDENT.repeat(indents) + "args := os.Args[1:]\n");
            isMain = false;
        } else if (insideConstructor) {
            out.print(INDENT.repeat(indents) + "this := new(");
            out.print(className);
            printTypeArguments(classTypeParametersContext);
            out.print(")\n");
        }
        super.visitBlock(ctx);
        if (insideConstructor) {
            out.print(INDENT.repeat(indents) + "return this\n");
        }
        indents--;
        out.print(INDENT.repeat(indents) + "}");
        return null;
    }

    private void checkElseStatement(JavaParser.StatementContext ctx) {
        if (ctx.ELSE() != null) {
            if (ctx.statement(1).IF() != null) {
                out.print(" else if ");
                visitParExpression(ctx.statement(1).parExpression());
                visitStatement(ctx.statement(1).statement(0));
                checkElseStatement(ctx.statement(1));
            } else {
                out.print(" else ");
                visitStatement(ctx.statement(1));
            }
        }
    }

    private void usedImport(String fileName) {
        unusedImports.remove(fileName);
    }

    @Override
    public Void visitStatement(JavaParser.StatementContext ctx) {
        if (ctx.IF() != null || ctx.WHILE() != null) {
            out.print("\n");
            out.print(INDENT.repeat(indents));
            if(ctx.IF() != null) out.print("if ");
            if(ctx.WHILE() != null) out.print("for ");
            super.visit(ctx.parExpression());
            visitStatement(ctx.statement(0));
            checkElseStatement(ctx);
        } else if (ctx.FOR() != null) {
            var forControl = ctx.forControl();
            out.print("for ");
            this.insideForControl = true;
            visitChildren(forControl.forInit());
            out.print("; ");
            if (forControl.expression() != null)
                visitExpression(forControl.expression());
            out.print("; ");
            if (forControl.forUpdate != null) {
                visitChildren(forControl.forUpdate);
            }
            this.insideForControl = false;
            visitStatement(ctx.statement(0));
        } else {
            if (ctx.RETURN() != null) {
                out.print("return ");
            }
            super.visitStatement(ctx);
        }
        return null;
    }

    @Override
    public Void visitVariableDeclarator(JavaParser.VariableDeclaratorContext ctx) {
        var variableName = ctx.variableDeclaratorId().getText();
        variableType.put(variableName, currentType);
        if (ctx.variableInitializer() != null) {
            if (insideForControl) {
                visitVariableDeclaratorId(ctx.variableDeclaratorId());
                out.print(" := ");
                super.visitVariableInitializer(ctx.variableInitializer());
            } else {
                out.print("var ");
                visitVariableDeclaratorId(ctx.variableDeclaratorId());
                out.print(" ");
                visitTypeType(currentType);
                out.print(" = ");
                super.visitVariableInitializer(ctx.variableInitializer());
            }
        }
        return null;
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        if (memberStatic) {
            currentType = ctx.typeType();
            visitVariableDeclarators(ctx.variableDeclarators());
            currentType = null;
            out.print("\n");
        }
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        var calledOn = ctx.getParent().getChild(0).getText();
        var currentClass = calledOn.equals(className);
        var isThis = calledOn.equals(THIS);
        var isLocal = currentClass || isThis;
        if (!isLocal && !variableType.containsKey(calledOn)) {
            usedImport(calledOn.toLowerCase());
        }
        var methodName = ctx.identifier().getText();
        var changeCase = !isLocal || publicMethods.contains(methodName);
        if (changeCase) methodName = camelCaseToPascalCase(methodName);
        out.print(methodName);
        out.print("(");
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

    private void addClassToCurrentPackageImports(String className, String fileName) {
        if (fileName == null) fileName = className.toLowerCase();
        modules.put(className, fileName);
        fromImports.put(fileName, "\"" + this.fullPackageName + "/" + fileName + "\"");
    }

    @Override
    public Void visitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.prefix != null) {
            var prefixText = ctx.prefix.getText();
            out.print(prefixText);
        }
        if (ctx.bop != null) {
            boolean localMethodCall = false;
            if (ctx.methodCall() != null && ctx.expression(0) != null) {
                var name = ctx.expression(0).getText();
                var fileName = name.toLowerCase();
                if (THIS.equals(name) || className.equals(name)) {
                    localMethodCall = true;
                    if (THIS.equals(name)) out.print("this.");
                    visitMethodCall(ctx.methodCall());
                } else if (isClassName(name) && !fromImports.containsKey(fileName) ) {
                    addClassToCurrentPackageImports(name, fileName);
                }
            }
            if (!localMethodCall) {
                var name = ctx.expression(0).getText();
                var bop = ctx.bop.getText();
                var isDot = bop.equals(".");
                if (!isDot || !className.equals(name)) {
                    if (ctx.identifier() != null) {
                        out.print(name.toLowerCase());
                    } else {
                        visitExpression(ctx.expression(0));
                    }
                    if (!isDot) bop = " " + bop + " ";
                    out.print(bop);
                }
                if (ctx.expression(1) != null) visitExpression(ctx.expression(1));
                else if (ctx.methodCall() != null) visitMethodCall(ctx.methodCall());
                else if (ctx.identifier() != null) visitIdentifier(ctx.identifier());
            }
        } else if (ctx.RPAREN() != null) {
            visitExpression(ctx.expression(0));
            out.print(".(");
            visitTypeType(ctx.typeType(0));
            out.print(")");
        } else {
            super.visitExpression(ctx);
        }
        if (ctx.postfix != null) {
            var postfixText = ctx.postfix.getText();
            out.print(postfixText);
        }
        return null;
    }

    private AbstractMap.SimpleEntry<String,String> replaceBasePackage(String packageName) {
        var replacement = "";
        for(var replacePackage : importDomainMapping.keySet()) {
            if (packageName.startsWith(replacePackage)) {
                replacement = importDomainMapping.get(replacePackage);
                if (packageName.equals(replacePackage)) packageName = "";
                else packageName = packageName.replace(replacePackage + ".", "");
                break;
            }
        }
        return new AbstractMap.SimpleEntry(packageName, replacement);
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier().stream().map(
                identifier -> identifier.getText()
        ).collect(Collectors.toList());

        var packageName = String.join(".", identifiers);
        javaFullClassNames.put(identifiers.get(identifiers.size() - 1), packageName);
        var packageReplacement = replaceBasePackage(packageName);
        packageName = packageReplacement.getKey();
        var replacement = packageReplacement.getValue();
        identifiers = List.of(packageName.split("\\."));

        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        String className = identifiers.get(classNameIdx);
        var packageStr = String.join("/",
                identifiers.subList(0, classNameIdx).stream()
                        .collect(Collectors.toList())
        );
        if (!replacement.isEmpty()) {
            packageStr = replacement + "/" + packageStr;
        }
        var fileName = className.toLowerCase();
        modules.put(className, fileName);
        var otteropImport = "otterop" + fileName;
        var importName = fileName;
        var goClassName = "*" + otteropImport + "." + className;
        var goPureClassName = "*" + importName + "." + className;
        var importStatement = otteropImport + " \"" + packageStr + "/"+ fileName + "\"";
        var pureImportStatement = importName + " \"" + packageStr + "/"+ fileName + "/pure\"";
        fromImports.put(otteropImport, importStatement);
        fromImports.put(importName, pureImportStatement);
        if (isStatic) {
            var methodName = camelCaseToPascalCase(identifiers.get(classNameIdx + 1));
            var staticImportStatement = "var " + methodName + " = " + fileName + "." + methodName;
            staticImports.add(staticImportStatement);
        } else {
            unusedImports.put(otteropImport, importStatement);
            unusedImports.put(importName, pureImportStatement);
        }
        classPackage.put(goClassName, otteropImport);
        classPackage.put(goPureClassName, importName);
        pureClassNames.put(className, goPureClassName);
        fullClassNames.put(className, goClassName);
        if (unwrappedClassName.containsKey(goClassName)) {
            pureClassNames.put(className, unwrappedClassName.get(goClassName));
        } else {
            pureClassNames.put(className, goPureClassName);
        }
        return null;
    }

    @Override
    public Void visitBlockStatement(JavaParser.BlockStatementContext ctx) {
        out.print(INDENT.repeat(indents));
        super.visitBlockStatement(ctx);
        if (skipNewlines > 0) skipNewlines--;
        else out.print("\n");
        return null;
    }

    @Override
    public Void visitLiteral(JavaParser.LiteralContext ctx) {
        if (ctx.NULL_LITERAL() != null) out.print("nil");
        else out.print(ctx.getText());
        super.visitLiteral(ctx);
        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        var text = ctx.getText();
        if (modules.containsKey(text)) text = modules.get(text);
        out.print(text);
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
        visitVariableDeclarators(ctx.variableDeclarators());
        currentType = null;
        return null;
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        var createdName = ctx.createdName().identifier(0).getText();
        var currentClassCreated = className.equals(createdName);
        var currentCreatorPublic = !currentClassCreated || constructorPublic;
        if (!currentClassCreated) {
            var filename = createdName.toLowerCase();
            if (!fromImports.containsKey(filename)) {
                addClassToCurrentPackageImports(createdName, null);
            } else {
                usedImport(filename);
            }
            out.print(createdName.toLowerCase() + ".");
        }
        if (currentCreatorPublic)
            out.print("New");
        else
            out.print("new");
        out.print(createdName);
        var typeArguments = ctx.createdName().typeArgumentsOrDiamond();
        if (!typeArguments.isEmpty()) {
            printTypeArguments(List.of(typeArguments.get(0).typeArguments()));
        }
        visitClassCreatorRest(ctx.classCreatorRest());
        return null;
    }

    @Override
    public Void visitTypeParameters(JavaParser.TypeParametersContext ctx) {
        return null;
    }

    @Override
    public Void visitTypeType(JavaParser.TypeTypeContext ctx) {
        if (ctx.RBRACK().size() > 0) {
            out.print("[]");
        }
        return super.visitTypeType(ctx);
    }

    @Override
    public Void visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        var identifier = ctx.identifier(0).IDENTIFIER().getText();
        if ("Object".equals(identifier)) {
            out.print("any");
        } else if (currentTypeParameters.contains(identifier) ||
                   currentMethodTypeParameters.contains(identifier)) {
            if (!insideTypeArgument) out.print("*");
            out.print(identifier);
        } else if ("Array".equals(identifier)) {
            out.print("[]");
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
            lastTypeArray = true;
        } else if ("String".equals(identifier) || "java.lang.String".equals(identifier)) {
            out.print("*string");
            lastTypeWrapped = "*otteropstring.String";
        } else {
            var fileName = identifier.toLowerCase();
            usedImport(fileName);
            var isInterface = classReader.getClass(javaFullClassNames.get(identifier))
                    .orElseThrow(() ->
                            new RuntimeException("cannot find class " + identifier))
                    .isInterface();
            if (!isInterface) out.print("*");
            if (!identifier.equals(className)) {
                out.print(fileName);
                out.print(".");
                lastTypeWrapped = fullClassNames.get(identifier);
            } else {
                lastTypeWrapped = fullClassName;
            }

            out.print(identifier);
            printTypeArguments(ctx.typeArguments());
        }
        return null;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        String name = ctx.getText();
        if ("double".equals(name)) out.print("float64");
        else if ("float".equals(name)) out.print("float32");
        else if ("boolean".equals(name)) out.print("bool");
        else out.print(name);
        return null;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        var identifiers = ctx.qualifiedName().identifier();
        this.javaFullPackageName = identifiers.stream().map(
                identifier -> identifier.getText()
        ).collect(Collectors.joining("."));
        var packageReplacement = this.replaceBasePackage(this.javaFullPackageName);
        this.fullPackageName = packageReplacement.getValue();
        if (!packageReplacement.getKey().isEmpty())
            this.fullPackageName += "/" +
                    String.join("/", packageReplacement.getKey().split("\\."));
        return null;
    }

    public void printTo(PrintStream ps) {
        ps.print("package pure\n\n");

        for (String fileName : unusedImports.keySet()) {
            fromImports.remove(fileName);
        }
        var total = imports.size() + fromImports.size();
        if (total > 0) {
            ps.print("import (\n");
            indents++;
            for (String importStatement : imports) {
                ps.println(INDENT.repeat(indents) + importStatement);
            }
            for (String importStatement : fromImports.values()) {
                ps.println(INDENT.repeat(indents) + importStatement);
            }
            indents--;
            ps.print(")\n");
        }
        for (String staticImport : staticImports) {
            ps.println(INDENT.repeat(indents) + staticImport);
        }
        ps.println("");
        ps.print(outStream.toString());
    }
}
