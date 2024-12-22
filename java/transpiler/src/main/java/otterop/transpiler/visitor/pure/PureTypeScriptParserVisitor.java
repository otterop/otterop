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

import otterop.transpiler.Otterop;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.util.CaseUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PureTypeScriptParserVisitor extends JavaParserBaseVisitor<Void> {
    private final String basePackage;
    private final String currentPackage;
    private final String[] currentPackageIdentifiers;
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean classPublic = false;
    private String className = null;
    private String pureClassName = null;
    private boolean hasMain = false;
    private boolean insideFieldDeclaration = false;
    private boolean insideMethodCall = false;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private Map<String, String> imports = new LinkedHashMap<>();
    private Map<String, String> pureImports = new LinkedHashMap<>();
    private Set<String> importedClasses = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private PrintStream out = new PrintStream(outStream);
    private Map<String, JavaParser.TypeTypeContext> variableType = new LinkedHashMap<>();
    private Set<String> attributePrivate = new LinkedHashSet<>();
    private JavaParser.TypeTypeContext currentType;
    private String lastTypeWrapped = null;
    private boolean lastTypeArray = false;
    private boolean lastTypeIterable = false;
    private Map<String,String> specialClasses = new LinkedHashMap<>();
    private Map<String,String> wrapperClassName = new LinkedHashMap<>();
    private Map<String,String> mappedArguments = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentArray = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentIterable = new LinkedHashMap<>();
    private Map<String,String> mappedArgumentClass = new LinkedHashMap<>();
    private Map<String,String> pureClassNames = new LinkedHashMap<>();
    private Set<String> unusedImports = new LinkedHashSet<>();

    private static final Set<String> NUMERIC_TYPES = new LinkedHashSet<String>() {{
        add("int");
        add("short");
        add("float");
        add("double");
    }};

    public PureTypeScriptParserVisitor(String basePackage, String currentPackage) {
        this.basePackage = basePackage;
        this.currentPackage = currentPackage;
        this.currentPackageIdentifiers = currentPackage.split("\\.");
        //this.specialClasses.put("String", "string");
        //this.specialClasses.put("Array", "Array");
        this.specialClasses.put("StringOtterOP", "string");
        this.specialClasses.put("ArrayOtterOP", "Array");
        this.specialClasses.put("WrapperOOPIterableOtterOP", "WrapperOOPIterable");
        this.wrapperClassName.put("StringOtterOP", "StringOtterOP");
        this.wrapperClassName.put("OOPIterableOtterOP", "WrapperOOPIterableOtterOP");
        this.wrapperClassName.put("String", "StringOtterOP");
        this.wrapperClassName.put("OOPIterable", "WrapperOOPIterableOtterOP");
        addImport(Arrays.asList("otterop", "lang", "String"), false);
        addImport(Arrays.asList("otterop", "lang", "Array"), false);
        addImport(Arrays.asList("otterop", "lang", "WrapperOOPIterable"), false);
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        if (classPublic) {
            out.print("export ");
        }
        out.print("interface ");
        pureClassName = ctx.identifier().getText();
        className = ctx.identifier().getText() + "OtterOP";

        this.visitIdentifier(ctx.identifier());
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
        out.print("public static wrap(otterop :");
        out.print(className);
        out.print(") : ");
        out.print(pureClassName);
        out.print(" {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("return new ");
        out.print(pureClassName);
        out.print("(otterop);\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    private void unwrapMethod() {
        out.print(INDENT.repeat(indents));
        out.print("public unwrap() : ");
        out.print(className);
        out.print(" {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("return this.otterop;\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        if (classPublic)
            out.print("export ");
        out.print("class ");
        pureClassName = ctx.identifier().getText();
        className = ctx.identifier().getText() + "OtterOP";
        imports.put(pureClassName, "import { " + pureClassName + " as " + className + " } from '../" + pureClassName + "';");

        this.visitIdentifier(ctx.identifier());
        if (ctx.IMPLEMENTS() != null) {
            out.print(" implements ");
            boolean first = true;
            for (var type : ctx.typeList().get(0).typeType()) {
                visitTypeType(type);
                if (!first)
                    out.print(", ");
                first = false;
            }
        }
        out.print(" {\n");
        indents++;
        out.println();
        out.print(INDENT.repeat(indents));
        out.print("private otterop : ");
        out.print(className);
        out.println(";\n");
        wrapMethod();
        super.visitClassBody(ctx.classBody());
        unwrapMethod();
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}\n");
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        out.print("\n");
        mappedArguments.clear();
        mappedArgumentArray.clear();
        mappedArgumentIterable.clear();
        mappedArgumentClass.clear();

        if (memberPublic) {
            out.print(INDENT.repeat(indents));
            out.print("public constructor");
            visitFormalParameters(ctx.formalParameters());
            out.println(";");

            out.print(INDENT.repeat(indents));
            out.print("public constructor(wrapped : ");
            out.print(className);
            out.println(");");
            out.print(INDENT.repeat(indents));
            out.println("public constructor(...__args: any[]) {");
            indents++;
            out.print(INDENT.repeat(indents));
            out.print("if (__args.length === 1 && __args[0] instanceof ");
            out.print(className);
            out.println(") {");
            indents++;
            out.print(INDENT.repeat(indents));
            out.println("this.otterop = __args[0];");
            out.print(INDENT.repeat(indents));
            out.println("return;");
            indents--;
            out.print(INDENT.repeat(indents));
            out.println("}");
            if (ctx.formalParameters().formalParameterList() != null) {
                out.print(INDENT.repeat(indents));
                out.print("const [");
                out.print(ctx.formalParameters().formalParameterList().formalParameter().stream().map(
                        fp -> fp.variableDeclaratorId().identifier().getText()
                ).collect(Collectors.joining(", ")));
                out.println("] = __args;");
            }
            mapArguments();
            out.print(INDENT.repeat(indents));
            out.print("this.otterop = new ");
            out.print(className);
            insideMethodCall = true;
            visitFormalParameters(ctx.formalParameters());
            insideMethodCall = false;
            out.print(";\n");
        } else {
            out.print(INDENT.repeat(indents));
            out.print("public constructor(wrapped : ");
            out.print(className);
            out.println(") {");
            indents++;
            out.print(INDENT.repeat(indents));
            out.println("this.otterop = wrapped;");
        }
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}");
        out.print("\n");
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
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        out.print(" : ");
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(";\n");
        return null;
    }

    private String mapArgument(String argName, String mappedClass) {
        if (wrapperClassName.containsKey(mappedClass)) {
            return wrapperClassName.get(mappedClass) + ".wrap(" + argName + ")";
        } else {
            return argName + ".unwrap()";
        }
    }

    private String unmapArgument(String argName, String mappedClass) {
        if (wrapperClassName.containsKey(mappedClass)) {
            return wrapperClassName.get(mappedClass) + ".unwrap(" + argName + ")";
        } else {
            return mappedClass + ".wrap(" + argName + ")";
        }
    }

    private void mapArguments() {
        if (!mappedArguments.isEmpty()) {
            for (var entry: mappedArguments.entrySet()) {
                out.print(INDENT.repeat(indents));
                var paramName = entry.getKey();
                var mapperParamName = entry.getValue();
                var mappedClass = mappedArgumentClass.get(paramName);
                var pureClass = specialClasses.getOrDefault(mappedClass,
                        pureClassNames.get(mappedClass));

                if (mappedArgumentArray.getOrDefault(paramName, false)) {
                    var mappedArrayName = mapperParamName + "Array";
                    out.print("const ");
                    out.print(mappedArrayName);
                    out.print(" = new Array(");
                    out.print(paramName);
                    out.print(".length);\n");
                    out.print(INDENT.repeat(indents));
                    out.print("for (let i = 0; i < ");
                    out.print(paramName);
                    out.print(".length; i++) {\n");
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
                    out.print("const ");
                    out.print(entry.getValue());
                    out.print(" = ArrayOtterOP.wrap(");
                    out.print(mappedArrayName);
                    out.print(");\n");
                } else if (mappedArgumentIterable.getOrDefault(paramName, false)) {
                    out.print("const ");
                    out.print(mapperParamName);
                    out.print(" = WrapperOOPIterableOtterOP.wrap(");
                    out.print(entry.getKey());
                    out.print(", ");
                    if (!"unknown".equals(mappedClass)) {
                        out.print("(el : ");
                        out.print(pureClass);
                        out.print(") : ");
                        out.print(mappedClass);
                        out.print(" => { return ");
                        out.print(mapArgument("el", mappedClass));
                        out.print("; }");
                    } else {
                        out.print("null");
                    }
                    out.print(");\n");
                } else {
                    out.print("const ");
                    out.print(mapperParamName);
                    out.print(" = ");
                    out.print(mapArgument(entry.getKey(), mappedClass));
                    out.print(";\n");
                }
            }
        }
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (!memberPublic)
            return null;

        var hasReturn = ctx.typeTypeOrVoid().VOID() == null;
        var returnTypeArray = false;
        var returnTypeIterable = false;
        String returnTypePure = null;
        String returnTypeString = null;
        if (hasReturn) {
            var returnType = ctx.typeTypeOrVoid().typeType().classOrInterfaceType();
            if (returnType != null) {
                var identifier = returnType.identifier().stream().map(i -> i.getText())
                        .collect(Collectors.joining("."));
                returnTypeArray = returnType.identifier(0).getText().equals("Array");
                returnTypeIterable = returnType.identifier(0).getText().equals("OOPIterable");
                if (!returnTypeArray && !returnTypeIterable) {
                    returnTypeString = identifier;
                } else {
                    returnTypeString = returnType.typeArguments().get(0).typeArgument(0).typeType().getText();
                }
                returnTypePure = returnTypeString;
            }
        }

        variableType.clear();
        mappedArguments.clear();
        mappedArgumentArray.clear();
        mappedArgumentIterable.clear();
        mappedArgumentClass.clear();

        out.print("\n");
        out.print(INDENT.repeat(indents));
        if (memberPublic) {
            out.print("public ");
        }
        if (memberStatic) {
            out.print("static ");
        }
        var name = ctx.identifier().getText();
        if ("main".equals(name)) {
            hasMain = true;
        }
        if (name.equals("OOPString")) {
            out.print("toString");
        } else {
            out.print(name);
        }
        visitFormalParameters(ctx.formalParameters());
        out.print(" : ");
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.println(" {");
        indents++;
        mapArguments();
        out.print(INDENT.repeat(indents));
        if (hasReturn)
            out.print("const retOtterop = ");
        if (!memberStatic) {
            out.print("this.otterop.");
        } else {
            out.print(className);
            out.print(".");
        }
        out.print(name);
        insideMethodCall = true;
        visitFormalParameters(ctx.formalParameters());
        insideMethodCall = false;
        out.print(";\n");
        if (hasReturn) {
            if (returnTypeIterable) {
                out.print(INDENT.repeat(indents));
                out.print("const ret = WrapperOOPIterableOtterOP.unwrap(retOtterop, ");
                if (!"Object".equals(returnTypeString)) {
                    out.print("(el : ");
                    out.print(returnTypeString);
                    out.print(") : ");
                    out.print(returnTypeIterable);
                    out.print(" => { return ");
                    out.print(unmapArgument("el", returnTypeString));
                    out.print("; }");
                } else {
                    out.print("null");
                }
                out.print(");\n");
            } else if (returnTypeArray) {
                out.print(INDENT.repeat(indents));
                out.print("const ret = new Array(retOtterop.size());\n");
                out.print(INDENT.repeat(indents));
                out.print("for (let i = 0; i < ret.length; i++) {\n");
                indents++;
                out.print(INDENT.repeat(indents));
                out.print("const retI = retOtterop.get(i);\n");
                if (!memberStatic && returnTypePure.equals(pureClassName)) {
                    out.print(INDENT.repeat(indents));
                    out.print("if (Object.is(retI, this.otterop)) {\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print("ret[i] = this;\n");
                    out.print(INDENT.repeat(indents));
                    out.print("continue;\n");
                    indents--;
                    out.print(INDENT.repeat(indents));
                    out.print("}\n");
                }
                out.print(INDENT.repeat(indents));
                out.print("ret[i] = ");
                out.print(this.unmapArgument("retI", returnTypePure));
                out.print(";\n");
                indents--;
                out.print(INDENT.repeat(indents));
                out.print("}\n");
            } else {
                if (returnTypePure == null) {
                    out.print(INDENT.repeat(indents));
                    out.print("const ret = retOtterop;\n");
                } else {
                    if (!memberStatic && returnTypePure.equals(pureClassName)) {
                        out.print(INDENT.repeat(indents));
                        out.print("if (Object.is(retOtterop, this.otterop)) {\n");
                        indents++;
                        out.print(INDENT.repeat(indents));
                        out.print("return this;\n");
                        indents--;
                        out.print(INDENT.repeat(indents));
                        out.print("}\n");
                    }
                    out.print(INDENT.repeat(indents));
                    out.print("const ret = ");
                    out.print(
                            this.unmapArgument("retOtterop", returnTypePure)
                    );
                    out.print(";\n");
                }
            }
            out.print(INDENT.repeat(indents));
            out.print("return ret;\n");
        }
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}");
        out.print("\n");
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
        lastTypeArray = false;
        lastTypeIterable = false;
        lastTypeWrapped = null;

        boolean isLast = ctx.getParent().children.get(ctx.getParent().getChildCount()-1) == ctx;
        var parameterName = ctx.variableDeclaratorId().getText();
        if (insideMethodCall && mappedArguments.containsKey(parameterName)) {
            out.print(mappedArguments.get(parameterName));
        } else {
            out.print(parameterName);
        }

        if (!insideMethodCall) {
            out.print(" : ");
            visitTypeType(ctx.typeType());
        }
        variableType.put(parameterName, ctx.typeType());
        if (lastTypeWrapped != null) {
            mappedArguments.put(parameterName, "_" + parameterName);
            mappedArgumentArray.put(parameterName, lastTypeArray);
            mappedArgumentIterable.put(parameterName, lastTypeIterable);
            mappedArgumentClass.put(parameterName, lastTypeWrapped);
        }
        if (!isLast) out.print(", ");
        return null;
    }

    @Override
    public Void visitClassOrInterfaceModifier(JavaParser.ClassOrInterfaceModifierContext ctx) {
        if (ctx.PUBLIC() != null) classPublic = true;
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
        super.visitBlock(ctx);
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
        var text = ctx.variableDeclaratorId().getText();
        if (insideFieldDeclaration && !memberPublic) {
            attributePrivate.add(text);
            text = "_" + text;
        }
        variableType.put(text, currentType);
        out.print(text);
        if (this.currentType != null) {
            out.print(" : ");
            visitTypeType(currentType);
        }
        if (ctx.variableInitializer() != null) {
            out.print(" = ");
            super.visitVariableInitializer(ctx.variableInitializer());
        }
        return null;
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        var methodName = ctx.identifier().getText();
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

    private boolean isIntPrimary(JavaParser.PrimaryContext primaryContext) {
        if (primaryContext != null && primaryContext.identifier() != null) {
            var identifierName = primaryContext.identifier().getText();
            var typeContext = variableType.get(identifierName);
            if (typeContext != null && typeContext.primitiveType() != null) {
                return typeContext.primitiveType().INT() != null;
            }
            return false;
        }
        return (
                primaryContext != null &&
                        primaryContext.literal() != null &&
                        primaryContext.literal().integerLiteral() != null
        );
    }

    private void checkCurrentPackageImports(String name) {
        if (CaseUtil.isClassName(name) && !className.equals(name) && !pureClassName.equals(name) &&
                !importedClasses.contains(name)) {
            unusedImports.remove(name);
            pureImports.put(name, "import { " + name + " } from './" + name + "';");
        }
    }

    @Override
    public Void visitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.prefix != null) {
            var prefixText = ctx.prefix.getText();
            out.print(prefixText);
        }
        if (ctx.bop != null) {
            var name = ctx.expression(0).getText();
            var bop = ctx.bop.getText();
            boolean isIntDivision = false;
            if (bop.equals("/") &&
                    isIntPrimary(ctx.expression(0).primary()) &&
                    isIntPrimary(ctx.expression(1).primary())) {
                isIntDivision = true;
                out.print("Math.floor(");
            }

            checkCurrentPackageImports(name);
            if (ctx.expression(0) != null) visitExpression(ctx.expression(0));

            if (!bop.equals(".")) bop = " " + bop + " ";
            out.print(bop);

            if (ctx.expression(1) != null) {
                visitExpression(ctx.expression(1));
            }
            else if (ctx.methodCall() != null) visitMethodCall(ctx.methodCall());
            else if (ctx.identifier() != null) {
                if (bop.equals(".")
                        && attributePrivate.contains(ctx.identifier().getText())) {
                    out.print("_");
                }
                visitIdentifier(ctx.identifier());
            }

            if (isIntDivision)
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

    private String relativePath(String[] currentPath, String[] destinationPath) {
        int i;
        for(i = 0; i < currentPath.length && i <destinationPath.length; i++) {
            if (!currentPath[i].equals(destinationPath[i])) break;
        }
        StringBuffer sb = new StringBuffer();
        for (int j = i; j < currentPath.length; j++) {
            sb.append("..");
            if(j <currentPath.length - 1) sb.append("/");
        }
        if (sb.length() == 0) sb.append(".");
        sb.append("/");
        for (int j = i; j < destinationPath.length; j++) {
            sb.append(destinationPath[j]);
            if(j <destinationPath.length - 1) sb.append("/");
        }
        return sb.toString();
    }

    private boolean excludeImports(String javaFullClassName) {
        return Otterop.WRAPPED_CLASS.equals(javaFullClassName);
    }

    private void addImport(List<String> identifiers, boolean isStatic) {
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        String className = identifiers.get(classNameIdx);

        var javaFullClassName = identifiers.subList(0, classNameIdx + 1).stream()
                .collect(Collectors.joining("."));

        if (!excludeImports(javaFullClassName)) {
            importedClasses.add(className);

            var isCurrentPackage = String.join(".", identifiers).startsWith(basePackage);
            var fileStr = "";
            if (isCurrentPackage) {
                fileStr = relativePath(currentPackageIdentifiers, identifiers
                        .toArray(new String[0]));
            } else {
                var parts = identifiers.subList(0, classNameIdx).stream().map(
                                identifier -> identifier.toLowerCase())
                        .collect(Collectors.toList());
                parts.add(className);
                fileStr = "@" + String.join("/", parts);
            }

            var fullClassName = className + "OtterOP";
            var aliasString = " as " + fullClassName;

            pureClassNames.put(fullClassName, className);
            if (!specialClasses.containsKey(fullClassName) &&
                !wrapperClassName.containsKey(fullClassName)) {
                var pureStr = fileStr.replaceAll("/" + className + "$", "/pure/" + className);
                pureImports.put(className, "import { " + className + " } from '" + pureStr + "';");
            }
            imports.put(
                    className,
                    "import { " + className + aliasString + " } from '" + fileStr + "';"
            );
            unusedImports.add(className);
        }
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier().
                stream().map(identifier -> identifier.getText()).collect(Collectors.toList());

        addImport(identifiers, isStatic);
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
        if (ctx.NULL_LITERAL() != null) out.print("null");
        else out.print(ctx.getText());
        super.visitLiteral(ctx);
        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        var text = ctx.getText();
        out.print(text);
        return null;
    }

    @Override
    public Void visitPrimary(JavaParser.PrimaryContext ctx) {
        if(ctx.THIS() != null) out.print("this");
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
        out.print("let ");
        this.currentType = ctx.typeType();
        visitVariableDeclarators(ctx.variableDeclarators());
        return null;
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        var name = ctx.createdName().getText();
        out.print("new ");
        out.print(name);
        visitClassCreatorRest(ctx.classCreatorRest());
        return null;
    }

    @Override
    public Void visitTypeParameters(JavaParser.TypeParametersContext ctx) {
        return null;
    }

    @Override
    public Void visitTypeType(JavaParser.TypeTypeContext ctx) {
        super.visitTypeType(ctx);
        if (ctx.RBRACK().size() > 0) {
            out.print("[]");
        }
        return null;
    }

    @Override
    public Void visitTypeTypeOrVoid(JavaParser.TypeTypeOrVoidContext ctx) {
        if (ctx.VOID() != null) {
            out.print("void");
            return null;
        }
        return super.visitTypeTypeOrVoid(ctx);
    }

    @Override
    public Void visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        var identifier = ctx.identifier().stream().map(i -> i.getText())
                .collect(Collectors.joining("."));
        var checkImport = true;
        if ("Object".equals(identifier)) {
            out.print("unknown");
            checkImport = false;
            if (lastTypeArray || lastTypeIterable) {
                lastTypeWrapped = "unknown";
            }
        } else if ("OOPIterable".equals(identifier)) {
            lastTypeIterable = true;
            unusedImports.remove("WrapperOOPIterable");
            out.print("Iterable<");
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
            out.print(">");
        } else if ("Array".equals(identifier)) {
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
            out.print("[]");
            lastTypeArray = true;
        } else if ("String".equals(identifier) || "java.lang.String".equals(identifier)) {
            out.print("string");
            lastTypeWrapped = "StringOtterOP";
        } else {
            lastTypeWrapped = identifier + "OtterOP";
            out.print(identifier);
        }
        unusedImports.remove(identifier);
        if (checkImport)
            checkCurrentPackageImports(identifier);
        return null;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        String primitiveType = ctx.getText();
        if(NUMERIC_TYPES.contains(primitiveType)) {
            out.print("number");
        } else if ("long".equals(primitiveType)) {
            out.print("BigInt");
        } else {
            out.print(primitiveType);
        }
        return null;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        return null;
    }

    public void printTo(PrintStream ps) {
        for (String className : imports.keySet()) {
            if (!unusedImports.contains(className))
                ps.println(imports.get(className));
        }
        for (String className : pureImports.keySet()) {
            if (!unusedImports.contains(className))
                ps.println(pureImports.get(className));
        }

        ps.println("");
        ps.print(outStream.toString());
        if (hasMain) {
            ps.print(className + ".main(process.argv.slice(2));");
        }
    }
}
