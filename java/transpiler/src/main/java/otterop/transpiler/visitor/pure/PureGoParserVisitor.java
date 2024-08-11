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
import static otterop.transpiler.util.CaseUtil.toFirstLowercase;

public class PureGoParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean insideConstructor = false;
    private boolean insideMethodCall = false;
    private boolean insideTypeArgument = false;
    private boolean insideForControl = false;
    private String lastTypeWrapped = null;
    private boolean lastTypeArray = false;
    private boolean lastTypeIterable = false;
    private boolean anyTypeIterable = false;
    private JavaParser.TypeTypeContext currentType = null;
    private String className = null;
    private String classIdentifier = null;
    private String otteropClassIdentifier = null;
    private String javaFullPackageName = null;
    private String fullPackageName = null;
    private String packageIdentifier = null;
    private String otteropPackageIdentifier = null;
    private int indents = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private Set<String> imports = new LinkedHashSet<>();
    private Map<String,String> classIdentifiers = new LinkedHashMap<>();
    private Map<String,String> classIdentifiersPublic = new LinkedHashMap<>();
    private Map<String,String> otteropClassIdentifiers = new LinkedHashMap<>();
    private Map<String,String> otteropClassIdentifiersPublic = new LinkedHashMap<>();
    private Map<String,String> packageIdentifiers = new LinkedHashMap<>();
    private Map<String,String> packageIdentifierFullNames = new LinkedHashMap<>();
    private Map<String,String> unusedImports = new LinkedHashMap<>();
    private Map<String,String> fromImports = new LinkedHashMap<>();
    private Map<String,String> pureImports = new LinkedHashMap<>();
    private Map<String,String> javaFullClassNames = new LinkedHashMap<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Set<String> publicMethods = new LinkedHashSet<>();
    private Set<String> staticMethods = new LinkedHashSet<>();
    private Map<String,String> mappedArguments = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentArray = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentIterable = new LinkedHashMap<>();
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
    private Map<String,String> specialClassName = new LinkedHashMap<>();
    private Map<String,String> wrapperClassName = new LinkedHashMap<>();

    public PureGoParserVisitor(ClassReader classReader, Map<String,String> importDomainMapping) {
        this.classReader = classReader;
        this.importDomainMapping = new HashMap<>(importDomainMapping);
        this.specialClassName.put("otteroplang.String", "string");
        this.specialClassName.put("otteroplang.Array", "[]");
        this.wrapperClassName.put("otteroplang.String", "otteroplang.String");
        this.wrapperClassName.put("otteroplang.OOPIterable", "otteroplang.WrapperOOPIterable");
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
        out.print(ctx.identifier().getText());
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
        out.print("func ");
        out.print(className);
        out.print("Wrap(otterop *");
        out.print(otteropClassIdentifier);
        out.print(") *");
        out.print(classIdentifier);
        out.print(" {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("ret := new(");
        out.print(classIdentifier);
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
        out.print("(" + THIS + " *" + classIdentifier);
        out.print(") ");
        out.print("Unwrap() *");
        out.print(otteropClassIdentifier);
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
        classIdentifier = className;
        otteropClassIdentifier = otteropPackageIdentifier + "." + className;
        javaFullClassNames.put(className, javaFullPackageName + "." + className);
        putClassIdentifier(packageIdentifier, javaFullPackageName, className);

        var importString = otteropPackageIdentifier + " \"" + fullPackageName + "\"";
        imports.add(importString);
        out.print(className);
        this.classTypeParametersContext = ctx.typeParameters();
        printTypeParameters(this.classTypeParametersContext, true, false);
        out.print(" struct {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("otterop *" + otteropClassIdentifier + "\n");
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
        if (memberPublic) name = camelCaseToPascalCase(name);

        if(memberPublic) {
            out.print(name);
        } else {
            out.print(toFirstLowercase(name));
        }
        out.print("New");
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
        out.print("ret.otterop = " + otteropClassIdentifier + "New");
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
        if (className.equals(clazz)) {
            return clazz + "Wrap(" + argName + ")";
        }

        var classIdentifier = classIdentifiers.get(clazz);
        var otteropClassIdentifier = otteropClassIdentifiersPublic.get(clazz);
        if (specialClassName.containsKey(otteropClassIdentifier)) {
            classIdentifier = otteropClassIdentifier;
        }
        unusedImports.remove(clazz);
        return classIdentifier + "Wrap(" + argName + ")";
    }

    private String mapArgument(String argName, String mappedClass) {
        var otteropClassIdentifier = otteropClassIdentifiersPublic.get(mappedClass);
        if (specialClassName.containsKey(otteropClassIdentifier)) {
            return wrapArgName(mappedClass, argName);
        } else {
            return argName + ".Unwrap()";
        }
    }

    private String unmapArgument(String argName, String originalClass, String mappedClass) {
        if (specialClassName.containsKey(originalClass)) {
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
                    out.print("make([]*");
                    out.print(otteropClassIdentifiersPublic.get(mappedClass));
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
                    out.print(" = otteroplang.ArrayWrap(");
                    unusedImports.remove("Array");
                    out.print(mappedArrayName);
                    out.print(");\n");
                } else if (mappedArgumentIterable.getOrDefault(paramName, false)) {
                    var otteropClass = otteropClassIdentifiersPublic.get(mappedClass);

                    out.print("var ");
                    out.print(mappedArguments.get(paramName));
                    out.print(" = otteroplang.WrapperOOPIterableWrap");
                    if (anyTypeIterable)
                        out.print("Slice");
                    out.print("(");
                    out.print(entry.getKey());
                    out.print(", ");
                    if (!"any".equals(mappedClass)) {
                        out.print("func(el *");
                        out.print(mappedClass);
                        out.print(") *");
                        out.print(otteropClass);
                        out.print(" { return ");
                        out.print(mapArgument("el", mappedClass));
                        out.print("}, nil");
                    } else {
                        out.print("nil, (any)(nil)");
                    }
                    out.print(");\n");
                } else {
                    out.print("var ");
                    out.print(mapperParamName);
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
        var returnTypeIterable = false;
        String returnTypePure = null;
        String returnTypeString = null;
        if (hasReturn) {
            var returnType = ctx.typeTypeOrVoid().typeType().classOrInterfaceType();
            if (returnType != null) {
                returnTypeArray = returnType.identifier(0).getText().equals("Array");
                returnTypeIterable = returnType.identifier(0).getText().equals("OOPIterable");
                if (!returnTypeArray && !returnTypeIterable) {
                    returnTypeString = returnType.identifier().stream().map(i -> i.getText())
                            .collect(Collectors.joining("."));
                } else {
                    returnTypeString = returnType.typeArguments().get(0).typeArgument(0).typeType().getText();
                }
                returnTypePure = classIdentifiersPublic.get(returnTypeString);
            }
        }
        if (returnTypeString != null) {
            returnTypeString = otteropClassIdentifiersPublic.getOrDefault(returnTypeString, returnTypeString);
        }

        mappedArguments.clear();
        mappedArgumentArray.clear();
        mappedArgumentIterable.clear();
        mappedArgumentClass.clear();
        anyTypeIterable = false;
        out.print(INDENT.repeat(indents));
        variableType.clear();
        out.print("\n\nfunc ");
        if (!memberStatic) {
            out.print("(" + THIS + " *" + className);
            printTypeArguments(classTypeParametersContext);
            out.print(") ");
        } else {
            out.print(classIdentifier);
        }
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);

        if (ctx.identifier().getText().equals("OOPString")) {
            out.print("ToString");
        } else {
            out.print(name);
            anyTypeIterable = false;
            out.startCapture();
            visitFormalParameters(ctx.formalParameters());
            visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
            out.endCapture();
            if (anyTypeIterable)
                out.print("Slice");
        }
        visitFormalParameters(ctx.formalParameters());
        out.print(" ");
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" {\n");
        indents++;
        mapArguments();
        out.print(INDENT.repeat(indents));
        if (hasReturn)
            out.print("retOtterop := ");
        if (!memberStatic)
            out.print("this.otterop.");
        else
            out.print(otteropClassIdentifier);

        out.print(name);
        this.insideMethodCall = true;
        visitFormalParameters(ctx.formalParameters());
        this.insideMethodCall = false;
        out.print("\n");
        if (hasReturn) {
            if (returnTypeIterable) {
                out.print(INDENT.repeat(indents));
                out.print("ret := otteroplang.WrapperOOPIterableUnwrap");
                if (anyTypeIterable)
                    out.print("Slice");
                out.print("(retOtterop, ");
                if (!"Object".equals(returnTypeString)) {
                    out.print("func (el ");
                    out.print(returnTypeString);
                    out.print(") ");
                    out.print(returnTypeIterable);
                    out.print(" { return ");
                    out.print(unmapArgument("el", returnTypeString, returnTypePure));
                    out.print("} , nil");
                } else {
                    out.print("nil, (any)(nil)");
                }
                out.print(")\n");
            } else if (returnTypeArray) {
                out.print(INDENT.repeat(indents));
                out.print("ret := make([]*");
                out.print(returnTypePure);
                out.print(", retOtterop.Size())\n");
                out.print(INDENT.repeat(indents));
                out.print("for i := 0; i < retOtterop.Size(); i++ {\n");
                indents++;
                out.print(INDENT.repeat(indents));
                out.print("retI := retOtterop.Get(i)\n");
                if (returnTypePure.equals(className) && !memberStatic) {
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
            } else {
                if (returnTypePure == null) {
                    out.print(INDENT.repeat(indents));
                    out.print("ret := retOtterop\n");
                } else {
                    if (returnTypePure.equals(className) && !memberStatic) {
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
            }
            out.print(INDENT.repeat(indents));
            out.print("return ret\n");
        }
        indents--;
        out.print("}\n\n");

        variableType.clear();
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        if (ctx.formalParameterList() == null) {
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
        lastTypeArray = false;
        lastTypeIterable = false;
        this.out.startCapture();
        visitTypeType(ctx.typeType());
        if (lastTypeWrapped != null) {
            mappedArguments.put(parameterName, "_" + parameterName);
            mappedArgumentArray.put(parameterName, lastTypeArray);
            mappedArgumentIterable.put(parameterName, lastTypeIterable);
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
        if (insideConstructor) {
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

    @Override
    public Void visitExpression(JavaParser.ExpressionContext ctx) {
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

        String classIdentifier, classIdentifierPublic,
            otteropClassIdentifier, otteropClassIdentifierPublic;
        if (this.packageIdentifier.equals(packageIdentifier)) {
            classIdentifier = CaseUtil.toFirstLowercase(className);
            classIdentifierPublic = className;
            otteropClassIdentifier = otteropPackageIdentifier + "." + CaseUtil.toFirstLowercase(className);
            otteropClassIdentifierPublic = otteropPackageIdentifier + "." + className;
        } else {
            classIdentifier = packageIdentifier + "." + className;
            classIdentifierPublic = classIdentifier;
            otteropClassIdentifier = "otterop" + classIdentifier;
            otteropClassIdentifierPublic = "otterop" + classIdentifierPublic;
            if (specialClassName.containsKey(otteropClassIdentifier)) {
                classIdentifier = specialClassName.get(otteropClassIdentifier);
                classIdentifierPublic = classIdentifier;
                otteropClassIdentifiers.put(classIdentifier, otteropClassIdentifier);
                otteropClassIdentifiersPublic.put(classIdentifier, otteropClassIdentifier);
            }
        }
        classIdentifiers.put(className, classIdentifier);
        otteropClassIdentifiers.put(className, otteropClassIdentifier);
        classIdentifiersPublic.put(className, classIdentifierPublic);
        otteropClassIdentifiersPublic.put(className, otteropClassIdentifierPublic);
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier().stream().map(
                identifier -> identifier.getText()
        ).collect(Collectors.toList());

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

        this.putClassIdentifier(lastPackagePart, javaFullPackageName, className);
        lastPackagePart = this.packageIdentifiers.get(className);
        var otteropLastPackagePart = "otterop" + lastPackagePart;

        var importStatement = otteropLastPackagePart + " \"" + packageStr + "\"";
        var pureImportStatement = lastPackagePart + " \"" + packageStr + "/pure\"";

        if (isStatic)
            return null;

        var otteropClassIdentifierPublic = otteropClassIdentifiersPublic.get(className);
        if (!this.javaFullPackageName.equals(javaFullPackageName) &&
            !specialClassName.containsKey(otteropClassIdentifierPublic) &&
            !wrapperClassName.containsKey(otteropClassIdentifierPublic)) {
            pureImports.put(className, pureImportStatement);
        }
        fromImports.put(className, importStatement);
        unusedImports.put(className, importStatement);

        return null;
    }

    @Override
    public Void visitBlockStatement(JavaParser.BlockStatementContext ctx) {
        return null;
    }

    @Override
    public Void visitLiteral(JavaParser.LiteralContext ctx) {
        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        return null;
    }

    @Override
    public Void visitPrimary(JavaParser.PrimaryContext ctx) {
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
        var checkImport = true;
        if ("Object".equals(identifier)) {
            out.print("any");
            checkImport = false;
            if (lastTypeIterable) {
                lastTypeWrapped = "any";
            }
        } else if ("OOPIterable".equals(identifier)) {
            lastTypeIterable = true;
            anyTypeIterable = true;
            out.print("[]");
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
        } else if (currentTypeParameters.contains(identifier) ||
                   currentMethodTypeParameters.contains(identifier)) {
            out.print(identifier);
        } else if ("Array".equals(identifier)) {
            out.print("[]");
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
            lastTypeArray = true;
        } else if ("String".equals(identifier) || "java.lang.String".equals(identifier)) {
            out.print("*string");
            lastTypeWrapped = "string";
        } else {
            var className = identifier;

            var isInterface = classReader.getClass(javaFullClassNames.get(className))
                    .isInterface();
            if (!isInterface) out.print("*");
            out.print(getClassIdentifier(className));
            printTypeArguments(ctx.typeArguments());
            lastTypeWrapped = className;
        }
        if (checkImport)
            checkCurrentPackageImports(identifier);
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
        this.otteropPackageIdentifier = "otterop" + identifiersString.get(identifiersString.size() - 1);
        this.javaFullPackageName = identifiersString.stream().collect(Collectors.joining("."));
        var packageReplacement = this.replaceBasePackage(this.javaFullPackageName);
        this.fullPackageName = packageReplacement.getValue();
        if (!packageReplacement.getKey().isEmpty())
            this.fullPackageName += "/" +
                    String.join("/", packageReplacement.getKey().split("\\."));
        return null;
    }

    public void printTo(PrintStream ps) {
        ps.print("package pure\n\n");

        for (String className : unusedImports.keySet()) {
            fromImports.remove(className);
            pureImports.remove(className);
        }
        var total = imports.size() + fromImports.size();
        if (total > 0) {
            ps.print("import (\n");
            indents++;
            for (String importStatement : new LinkedHashSet<>(imports)) {
                ps.println(INDENT.repeat(indents) + importStatement);
            }
            for (String importStatement : new LinkedHashSet<>(fromImports.values())) {
                ps.println(INDENT.repeat(indents) + importStatement);
            }
            for (String importStatement : new LinkedHashSet<>(pureImports.values())) {
                ps.println(INDENT.repeat(indents) + importStatement);
            }
            indents--;
            ps.print(")\n");
        }
        ps.println("");
        ps.print(outStream.toString());
    }
}
