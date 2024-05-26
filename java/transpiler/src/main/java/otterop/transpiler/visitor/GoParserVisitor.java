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

import org.antlr.v4.runtime.tree.ParseTree;
import otterop.transpiler.Otterop;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.CaseUtil;
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
import static otterop.transpiler.util.CaseUtil.toFirstLowercase;

public class GoParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean memberStatic = false;
    private boolean classPublic = false;
    private boolean memberPublic = false;
    private boolean constructorPublic = false;
    private boolean isMain = false;
    private boolean hasMain = false;
    private boolean insideConstructor = false;
    private boolean insideTypeArgument = false;
    private boolean isIterableMethod = false;
    private boolean insideForControl = false;
    private String enhancedForControlVariable = null;
    private JavaParser.TypeTypeContext currentType = null;
    private String className = null;
    private String classIdentifier;
    private String javaFullPackageName = null;
    private String fullPackageName = null;
    private String packageIdentifier = null;
    private String superClassName;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private Set<String> imports = new LinkedHashSet<>();
    private Map<String,String> classIdentifiers = new LinkedHashMap<>();
    private Map<String,String> classIdentifiersPublic = new LinkedHashMap<>();
    private Map<String,String> packageIdentifiers = new LinkedHashMap<>();
    private Map<String,String> packageIdentifierFullNames = new LinkedHashMap<>();
    private Map<String,String> unusedImports = new LinkedHashMap<>();
    private Map<String,String> fromImports = new LinkedHashMap<>();
    private Map<String,String> javaFullClassNames = new LinkedHashMap<>();
    private Map<String,String> staticImports = new LinkedHashMap<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Set<String> privateMethods = new LinkedHashSet<>();
    private Set<String> publicMethods = new LinkedHashSet<>();
    private Set<String> staticMethods = new LinkedHashSet<>();
    private Map<String, JavaParser.TypeTypeContext> variableType = new LinkedHashMap<>();
    private List<JavaParser.FieldDeclarationContext> fields = new LinkedList<>();
    private PrintStreamStack out = new PrintStreamStack(outStream);
    private Map<String,String> importDomainMapping;
    private JavaParser.TypeParametersContext classTypeParametersContext;
    private JavaParser.TypeParametersContext methodTypeParametersContext;
    private Set<String> currentTypeParameters = new HashSet<>();
    private Set<String> currentMethodTypeParameters = new HashSet<>();
    private ClassReader classReader;
    private boolean makePure = false;
    private boolean isTestClass = false;
    private boolean isTestMethod = false;
    private boolean isInterface = false;
    private boolean hasConstructor = false;
    private List<JavaParser.MethodDeclarationContext> testMethods = new LinkedList<>();

    public GoParserVisitor(ClassReader classReader, Map<String,String> importDomainMapping) {
        this.classReader = classReader;
        this.importDomainMapping = new HashMap<>(importDomainMapping);
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
            if (child instanceof JavaParser.AnnotationContext) {
                JavaParser.AnnotationContext annotationContext = (JavaParser.AnnotationContext) child;
                var annotationName = annotationContext.qualifiedName().identifier().get(0).getText();
                var fullAnnotationName = javaFullClassNames.get(annotationName);
                boolean isTestAnnotation = Otterop.TEST_ANNOTATION.equals(fullAnnotationName);
                if (isTestAnnotation)
                    this.isTestClass = true;
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
                else privateMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodStatic = false;
            }
            if (child instanceof JavaParser.GenericMethodDeclarationContext) {
                JavaParser.GenericMethodDeclarationContext declarationChild = (JavaParser.GenericMethodDeclarationContext) child;
                var name = declarationChild.methodDeclaration().identifier().getText();
                if (methodPublic) publicMethods.add(name);
                else privateMethods.add(name);
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
        isInterface = true;
        out.print(INDENT.repeat(indents));
        out.print("type ");
        className = ctx.identifier().getText();
        javaFullClassNames.put(className, javaFullPackageName + "." + className);
        putClassIdentifier(packageIdentifier, javaFullPackageName, className);
        this.classIdentifier = getClassIdentifier(className);
        out.print(classIdentifier);
        this.classTypeParametersContext = ctx.typeParameters();
        printTypeParameters(this.classTypeParametersContext, true, false);
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
        var annotationName = ctx.qualifiedName().identifier().get(0).getText();
        var fullAnnotationName = javaFullClassNames.get(annotationName);
        makePure |= Otterop.WRAPPED_CLASS.equals(fullAnnotationName);
        boolean isTestAnnotation = Otterop.TEST_ANNOTATION.equals(fullAnnotationName);
        if (isTestAnnotation)
            this.isTestMethod = true;
        return null;
    }

    @Override
    public Void visitClassOrInterfaceModifier(JavaParser.ClassOrInterfaceModifierContext ctx) {
        if (ctx.PUBLIC() != null) {
            this.classPublic = !testClass();
        }
        return super.visitClassOrInterfaceModifier(ctx);
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        detectMethods(ctx);
        out.print("\ntype ");
        className = ctx.identifier().getText();
        javaFullClassNames.put(className, javaFullPackageName + "." + className);
        putClassIdentifier(packageIdentifier, javaFullPackageName, className);
        classIdentifier = getClassIdentifier(className);
        out.print(classIdentifier);
        this.classTypeParametersContext = ctx.typeParameters();
        printTypeParameters(this.classTypeParametersContext, true, false);
        out.print(" struct {\n");
        indents++;
        if (ctx.EXTENDS() != null) {
            out.print(INDENT.repeat(indents));
            superClassName = ctx.typeType().getText();
            checkCurrentPackageImports(superClassName);
            out.print("*");
            out.println(classIdentifiersPublic.get(superClassName));
        }
        for(var field: fields) {
            out.print(INDENT.repeat(indents));
            visitVariableDeclaratorId(field.variableDeclarators()
                    .variableDeclarator(0).variableDeclaratorId());
            out.print(" ");
            visitTypeType(field.typeType());
            out.print("\n");
        }
        indents--;
        out.println("}\n\n");
        super.visitClassBody(ctx.classBody());
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
        hasConstructor = true;
        out.print("\n\nfunc ");
        var name = ctx.identifier().getText();
        if (memberPublic && !isMain) name = camelCaseToPascalCase(name);

        if(memberPublic) {
            out.print(name);
            constructorPublic = true;
        } else {
            out.print(toFirstLowercase(name));
        }
        out.print("New");
        printTypeParameters(this.classTypeParametersContext, false, false);
        visitFormalParameters(ctx.formalParameters());
        out.print(" *" + classIdentifier);
        printTypeArguments(classTypeParametersContext);
        insideConstructor = true;
        visitBlock(ctx.block());
        insideConstructor = false;
        return null;
    }

    @Override
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = false;
        this.isTestMethod = false;
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

    @Override
    public Void visitGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        this.methodTypeParametersContext = ctx.typeParameters();
        return super.visitGenericMethodDeclaration(ctx);
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (isTestMethod) {
            if (testMethods.isEmpty())
                imports.add("\"testing\"");
            testMethods.add(ctx);
        }
        out.startCapture();
        printTypeParameters(this.methodTypeParametersContext, false, true);
        String typeParametersString = out.endCapture();

        out.startCapture();
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        String typeTypeString = out.endCapture();
        if (isIterableMethod) {
            isIterableMethod = false;
            return null;
        }


        variableType.clear();
        out.print("\n\nfunc ");
        if (!memberStatic) {
            out.print("(" + THIS + " *" + classIdentifier);
            printTypeArguments(classTypeParametersContext);
            out.print(") ");
        } else {
            out.print(classIdentifier);
        }
        var name = ctx.identifier().getText();
        isMain = name.equals("main");
        if (isMain) hasMain = true;
        if ((memberPublic || memberStatic) && !isMain) name = camelCaseToPascalCase(name);
        out.print(name);
        out.print(typeParametersString);
        visitFormalParameters(ctx.formalParameters());
        out.print(" ");
        out.print(typeTypeString);
        visitMethodBody(ctx.methodBody());
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
        variableType.put(parameterName, parameterType);
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        out.print(" ");
        visitTypeType(ctx.typeType());
        if (!isLast) out.print(", ");
        return null;
    }

    @Override
    public Void visitModifier(JavaParser.ModifierContext ctx) {
        if (ctx.getText().equals("static"))
            memberStatic = true;
        if (ctx.getText().equals("public"))
            memberPublic = !testClass();
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
            out.print(classIdentifier);
            printTypeArguments(classTypeParametersContext);
            out.print(")\n");
        }
        if (enhancedForControlVariable != null) {
            out.print(INDENT.repeat(indents));
            out.print(enhancedForControlVariable);
            out.println();
            this.enhancedForControlVariable = null;
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
                checkSingleLineStatement(ctx.statement(1).statement(0));
                checkElseStatement(ctx.statement(1));
            } else {
                out.print(" else ");
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
                out.println();
                out.print(INDENT.repeat(indents));
                this.enhancedForControlVariable = null;
            }
        }
        visitStatement(ctx);
        if (ctx.SEMI() != null) {
            out.println();
            indents--;
            out.print(INDENT.repeat(indents));
            out.print("}");
        }
    }

    @Override
    public Void visitStatement(JavaParser.StatementContext ctx) {
        if (ctx.IF() != null || ctx.WHILE() != null) {
            out.print("\n");
            out.print(INDENT.repeat(indents));
            if(ctx.IF() != null) out.print("if ");
            if(ctx.WHILE() != null) out.print("for ");
            super.visit(ctx.parExpression());
            checkSingleLineStatement(ctx.statement(0));
            checkElseStatement(ctx);
        } else if (ctx.FOR() != null) {
            var forControl = ctx.forControl();
            out.print("for ");
            this.insideForControl = true;
            if (forControl.enhancedForControl() != null) {
                var enhancedForControl = forControl.enhancedForControl();
                out.print("it := (");
                visitExpression(enhancedForControl.expression());
                out.print(").OOPIterator(); ");
                out.print("it.HasNext();");

                out.startCapture();
                visitVariableDeclaratorId(enhancedForControl.variableDeclaratorId());
                out.print(" := it.Next()");
                this.enhancedForControlVariable = out.endCapture();
            } else {
                visitChildren(forControl.forInit());
                out.print("; ");
                if (forControl.expression() != null)
                    visitExpression(forControl.expression());
                out.print("; ");
                if (forControl.forUpdate != null) {
                    visitChildren(forControl.forUpdate);
                }
            }
            this.insideForControl = false;
            checkSingleLineStatement(ctx.statement(0));
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
        } else {
            out.print("var ");
            visitVariableDeclaratorId(ctx.variableDeclaratorId());
            out.print(" ");
            visitTypeType(currentType);
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
        if (ctx.SUPER() != null) {
            out.print("this.");
            out.print(this.superClassName);
            out.print(" = ");
            out.print(classIdentifiersPublic.get(this.superClassName));
            out.print("New");
        } else {
            var methodInternal = false;
            var staticImport = false;
            var isFirst = ctx.getParent().getChild(0) == ctx;
            var calledOn = ctx.getParent().getChild(0).getText();
            var currentClass = calledOn.equals(className);
            var isThis = calledOn.equals(THIS);
            var isLocal = currentClass || isThis || isFirst;
            var hasVariable = variableType.containsKey(calledOn);

            var methodName = ctx.identifier().getText();

            if (isFirst && staticImports.containsKey(methodName)) {
                isLocal = false;
                staticImport = true;
                methodName = staticImports.get(methodName);
            }
            if (!isLocal && !staticImport && !hasVariable) {
                checkCurrentPackageImports(calledOn);
            }
            if (!isLocal && !staticImport && hasVariable) {
                methodInternal = this.isMethodInternal(calledOn, ctx.identifier().getText());
            }

            var changeCase =
                             !isLocal && !methodInternal && !staticImport ||
                                     isLocal && publicMethods.contains(methodName) ||
                                     isLocal && !privateMethods.contains(methodName);
            var addThis = isFirst && isLocal;
            if (changeCase) methodName = camelCaseToPascalCase(methodName);
            if (addThis)
                out.print("this.");
            out.print(methodName);

        }
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

    private boolean checkCurrentPackageImports(String className) {
        if (CaseUtil.isClassName(className) && !this.className.equals(className)) {
            if (!fromImports.containsKey(className)) {
                putClassIdentifier(packageIdentifier, javaFullPackageName, className);
                javaFullClassNames.put(className, javaFullPackageName + "." + className);
                return true;
            } else {
                unusedImports.remove(className);
            }
        }
        return false;
    }

    private String getClassIdentifier(String className) {
        var classIdentifier = classIdentifiers.get(className);

        var javaFullClassName = javaFullClassNames.get(className);
        if (this.classReader.isPublicClass(javaFullClassName)) {
            classIdentifier = classIdentifiersPublic.get(className);
        }
        return classIdentifier;
    }

    private String getClassIdentifierConstructor(String className) {
        var classIdentifier = classIdentifiers.get(className);

        var javaFullClassName = javaFullClassNames.get(className);
        if (this.classReader.hasPublicConstructor(javaFullClassName)) {
            classIdentifier = classIdentifiersPublic.get(className);
        }
        return classIdentifier;
    }

    private String getClassIdentifierCallingMethod(String className, String methodName) {
        var classIdentifier = classIdentifiers.get(className);
        var javaFullClassName = javaFullClassNames.get(className);
        if (this.classReader.isPublicMethod(javaFullClassName, methodName)) {
            classIdentifier = classIdentifiersPublic.get(className);
        }
        return classIdentifier;
    }

    private boolean isMethodInternal(String variableName, String methodName) {
        var variableType = this.variableType.get(variableName);
        if (variableType.classOrInterfaceType() != null) {
            var className = variableType.classOrInterfaceType().identifier(0).getText();
            var javaFullClassName = javaFullClassNames.get(className);
            return !this.classReader.isPublicMethod(javaFullClassName, methodName);
        }
        return false;
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
                if (THIS.equals(name) || className.equals(name)) {
                    localMethodCall = true;
                    if (THIS.equals(name)) out.print("this.");
                    visitMethodCall(ctx.methodCall());
                } else {
                    checkCurrentPackageImports(name);
                }
            }
            if (!localMethodCall) {
                var name = ctx.expression(0).getText();
                var isClass = isClassName(name);
                var bop = ctx.bop.getText();
                var isDot = bop.equals(".");
                if (!isDot || !isClass) {
                    visitExpression(ctx.expression(0));
                    if (!isDot) bop = " " + bop + " ";
                    out.print(bop);
                } else if (isDot && isClass && ctx.methodCall() != null) {
                    var methodName = ctx.methodCall().identifier().getText();
                    var classIdentifier = getClassIdentifierCallingMethod(name, methodName);
                    out.print(classIdentifier);
                }
                if (ctx.expression(1) != null) visitExpression(ctx.expression(1));
                else if (ctx.methodCall() != null) visitMethodCall(ctx.methodCall());
                else if (ctx.identifier() != null) visitIdentifier(ctx.identifier());
            }
        } else if (ctx.LPAREN() != null) {
            // cast case
            out.print("(");
            visitTypeType(ctx.typeType(0));
            out.print(")(");
            visitExpression(ctx.expression(0));
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
            if (packageName.equals(replacePackage) || packageName.startsWith(replacePackage + ".")) {
                replacement = importDomainMapping.get(replacePackage);
                if (packageName.equals(replacePackage)) packageName = "";
                else packageName = packageName.replace(replacePackage + ".", "");
                break;
            }
        }
        return new AbstractMap.SimpleEntry(packageName, replacement);
    }

    private void putClassIdentifier(String packageIdentifier, String packageIdentifierFullName,
                                    String className) {
        packageIdentifierFullNames.put(packageIdentifier, packageIdentifierFullName);
        packageIdentifiers.put(className, packageIdentifier);

        String classIdentifier, classIdentifierPublic;
        if (this.packageIdentifier.equals(packageIdentifier)) {
            classIdentifier = CaseUtil.toFirstLowercase(className);
            classIdentifierPublic = className;
        } else {
            classIdentifier = packageIdentifier + "." + className;
            classIdentifierPublic = packageIdentifier + "." + className;
        }
        classIdentifiers.put(className, classIdentifier);
        classIdentifiersPublic.put(className, classIdentifierPublic);
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier().stream().map(
                identifier -> identifier.getText()
        ).collect(Collectors.toList());

        String methodName = null;
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        var className = identifiers.get(classNameIdx);
        var lastPackagePart = identifiers.get(classNameIdx - 1);
        var classIdentifiers = identifiers.subList(0, classNameIdx + 1).stream()
                .collect(Collectors.toList());

        var javaFullClassName = String.join(".", classIdentifiers);
        var javaFullPackageName = identifiers.subList(0, classNameIdx).stream()
                .collect(Collectors.joining("."));
        javaFullClassNames.put(className, javaFullClassName);
        var packageReplacement = replaceBasePackage(javaFullPackageName);
        var packageName = packageReplacement.getKey();
        var replacement = packageReplacement.getValue();
        var packageIdentifiers = List.of(packageName.split("\\."));

        var packageStr = String.join("/",
                packageIdentifiers.stream()
                        .collect(Collectors.toList())
        );
        if (!replacement.isEmpty()) {
            packageStr = replacement + "/" + packageStr;
        }

        putClassIdentifier(lastPackagePart, javaFullPackageName, className);
        lastPackagePart = this.packageIdentifiers.get(className);
        var importStatement = lastPackagePart + " \"" + packageStr + "\"";

        if (this.javaFullPackageName.equals(javaFullPackageName)) {
            return null;
        }

        fromImports.put(className, importStatement);
        if (isStatic) {
            methodName = identifiers.get(classNameIdx + 1);
            var classIdentifier = this.classIdentifiers.get(className);
            var staticImportStatement = classIdentifier + camelCaseToPascalCase(methodName);
            staticImports.put(methodName, staticImportStatement);
        } else {
            unusedImports.put(className, importStatement);
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
        else {
            if (ctx.STRING_LITERAL() != null) {
                out.print("lang.StringLiteral(");
            }
            out.print(ctx.getText());
            if (ctx.STRING_LITERAL() != null) {
                out.print(")");
            }
        }
        super.visitLiteral(ctx);
        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        out.print(ctx.getText());
        return null;
    }

    @Override
    public Void visitPrimary(JavaParser.PrimaryContext ctx) {
        if (ctx.THIS() != null)  out.print("this");
        if (ctx.SUPER() != null)  out.print("this." + this.superClassName);
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
        checkCurrentPackageImports(createdName);
        out.print(getClassIdentifierConstructor(createdName));
        out.print("New");
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
        } else if (Otterop.ITERATOR.equals(javaFullClassNames.get(identifier))) {
            this.isIterableMethod = true;
        } else if (currentTypeParameters.contains(identifier) ||
                   currentMethodTypeParameters.contains(identifier)) {
            out.print(identifier);
        } else {
            var className = identifier;
            checkCurrentPackageImports(className);
            var isInterface = classReader.getClass(javaFullClassNames.get(className))
                    .orElseThrow(() ->
                            new RuntimeException("cannot find class " + className))
                    .isInterface();
            if (!isInterface) out.print("*");
            out.print(getClassIdentifier(className));
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
        var identifiersString = identifiers.stream().map(
                identifier -> identifier.getText()
        ).collect(Collectors.toList());
        this.packageIdentifier = identifiersString.get(identifiersString.size() - 1);
        this.javaFullPackageName = identifiersString.stream().collect(Collectors.joining("."));
        var packageReplacement = this.replaceBasePackage(this.javaFullPackageName);
        this.fullPackageName = packageReplacement.getValue();
        if (!packageReplacement.getKey().isEmpty())
            this.fullPackageName += "/" +
                    String.join("/", packageReplacement.getKey().split("\\."));
        return null;
    }

    private void printDefaultConstructor(PrintStream ps) {
        if (hasConstructor || isInterface)
            return;
        ps.print("\nfunc ");
        if (classPublic)
            ps.print(className);
        else
            ps.print(classIdentifiers.get(className));
        ps.print("New");
        ps.print("() *");
        ps.print(classIdentifier);
        ps.print(" {\n");
        indents++;
        ps.print(INDENT.repeat(indents));
        ps.print("this := new(");
        ps.print(classIdentifier);
        ps.print(")\n");
        if (this.superClassName != null) {
            ps.print(INDENT.repeat(indents));
            ps.print("this.");
            ps.print(this.superClassName);
            ps.print(" = ");
            ps.print(classIdentifiers.get(superClassName));
            ps.print("New()\n");
        }
        ps.print(INDENT.repeat(indents));
        ps.print("return this\n");
        indents--;
        ps.print("}\n");
    }

    public boolean makePure() {
        return this.makePure && !testClass();
    }

    public boolean testClass() {
        return isTestClass;
    }

    public void printTo(PrintStream ps) {
        if (hasMain) {
            ps.print("package main\n\n");
        } else if (packageIdentifier != null) {
            ps.print("package ");
            ps.print(packageIdentifier);
            ps.print("\n\n");
        }

        for (String className : unusedImports.keySet()) {
            fromImports.remove(className);
        }
        var total = imports.size() + fromImports.size();
        if (total > 0) {
            ps.print("import (\n");
            indents++;
            for (String importStatement : imports) {
                ps.println(INDENT.repeat(indents) + importStatement);
            }
            for (String importStatement : new LinkedHashSet<>(fromImports.values())) {
                ps.println(INDENT.repeat(indents) + importStatement);
            }
            indents--;
            ps.print(")\n");
        }
        ps.println();
        this.printDefaultConstructor(ps);
        ps.print(outStream.toString());
        if (!testMethods.isEmpty()) {
            ps.print("\n\nfunc Test");
            ps.print(className);
            ps.print("(t *testing.T) {\n");
            indents++;
            for (var testMethod : testMethods) {
                var methodName = testMethod.identifier().getText();
                ps.print(INDENT.repeat(indents));
                ps.print("t.Run(\"");
                ps.print(methodName);
                ps.print("\", func(t *testing.T) {\n");
                indents++;
                ps.print(INDENT.repeat(indents));
                ps.print("test := ");
                ps.print(toFirstLowercase(className));
                ps.print("New");
                ps.print("()\n");
                ps.print(INDENT.repeat(indents));
                ps.print("test.SetGoTestingT(t)\n");
                ps.print(INDENT.repeat(indents));
                ps.print("test.");
                ps.print(methodName);
                ps.print("()\n");
                indents--;
                ps.print(INDENT.repeat(indents));
                ps.print("})\n");
            }
            indents--;
            ps.print("}\n");
        }
    }
}
