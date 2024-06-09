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

import otterop.transpiler.Otterop;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.util.CaseUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeScriptParserVisitor extends JavaParserBaseVisitor<Void> {
    private final String basePackage;
    private final String currentPackage;
    private final String[] currentPackageIdentifiers;
    private boolean classPublic = false;
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean memberPrivate = false;
    private String className = null;
    private boolean hasMain = false;
    private boolean insideFieldDeclaration = false;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private Set<String> imports = new LinkedHashSet<>();
    private Set<String> importedClasses = new LinkedHashSet<>();
    private Map<String,String> staticImports = new LinkedHashMap<>();
    private Map<String,String> javaFullClassNames = new LinkedHashMap<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private PrintStream out = new PrintStream(outStream);
    private JavaParser.TypeParametersContext classTypeParametersContext;
    private JavaParser.TypeParametersContext methodTypeParametersContext;
    private Set<String> currentTypeParameters = new HashSet<>();
    private Set<String> currentMethodTypeParameters = new HashSet<>();
    private Map<String, JavaParser.TypeTypeContext> variableType = new LinkedHashMap<>();
    private Set<String> attributePrivate = new LinkedHashSet<>();
    private Set<String> methodPrivate = new LinkedHashSet<>();
    private JavaParser.TypeTypeContext currentType;
    private boolean makePure = false;
    private boolean isTestMethod = false;
    private List<JavaParser.MethodDeclarationContext> testMethods = new LinkedList<>();


    private static final Set<String> NUMERIC_TYPES = new LinkedHashSet<String>() {{
        add("int");
        add("float");
        add("double");
    }};

    public TypeScriptParserVisitor(String basePackage, String currentPackage) {
        this.basePackage = basePackage;
        this.currentPackage = currentPackage;
        this.currentPackageIdentifiers = currentPackage.split("\\.");
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        out.print("export interface ");
        className = ctx.identifier().getText();
        this.visitIdentifier(ctx.identifier());
        this.classTypeParametersContext = ctx.typeParameters();
        printTypeParameters(this.classTypeParametersContext, true, false);
        if (ctx.EXTENDS() != null) {
            out.print(" extends ");
            visitTypeList(ctx.typeList(0));
        }
        out.print(" {\n");
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
        this.classPublic = ctx.PUBLIC() != null;
        super.visitClassOrInterfaceModifier(ctx);
        return null;
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        if (!classPublic) {
            out.print(INDENT.repeat(indents));
            out.println("/** @internal */");
        }
        out.print(INDENT.repeat(indents));
        out.print("export class ");
        className = ctx.identifier().getText();
        this.visitIdentifier(ctx.identifier());
        this.classTypeParametersContext = ctx.typeParameters();
        printTypeParameters(this.classTypeParametersContext, true, false);
        if (ctx.EXTENDS() != null) {
            out.print(" extends ");
            checkCurrentPackageImports(ctx.typeType().getText());
            visitTypeType(ctx.typeType());
        }
        if (ctx.IMPLEMENTS() != null) {
            out.print(" implements ");
            boolean first = true;
            for (var type : ctx.typeList().get(0).typeType()) {
                if (!first)
                    out.print(", ");
                first = false;
                visitTypeType(type);
            }
        }
        out.print(" {\n");
        indents++;
        super.visitClassBody(ctx.classBody());
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}\n");
        return null;
    }

    private void printTypeParameters(JavaParser.TypeParametersContext typeParametersContext, boolean classDeclaration, boolean methodDeclaration) {
        if (typeParametersContext != null) {
            if (methodDeclaration) {
                currentMethodTypeParameters.clear();
            }
            out.print("<");
            boolean rest = false;
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
            }
            out.print(">");
        }
    }

    private void printTypeArguments(List<JavaParser.TypeArgumentsContext> ctx) {
        if (ctx != null && !ctx.isEmpty()) {
            out.print("<");
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
            out.print(">");
        }
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        out.print("\n");
        out.print(INDENT.repeat(indents));
        if (memberPublic) {
            out.print("public ");
        } else if (memberPrivate) {
            out.print("private ");
        } else {
            out.println("/** @internal */");
            out.print(INDENT.repeat(indents));
        }
        out.print("constructor");
        visitFormalParameters(ctx.formalParameters());
        visitBlock(ctx.block());
        out.print("\n");
        return null;
    }

    @Override
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = false;
        this.memberPrivate = false;
        this.isTestMethod = false;
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

    @Override
    public Void visitGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        this.methodTypeParametersContext = ctx.typeParameters();
        return super.visitGenericMethodDeclaration(ctx);
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (isTestMethod)
            testMethods.add(ctx);
        variableType.clear();
        out.print("\n");
        var name = ctx.identifier().getText();
        var isIteratorMethod = "iterator".equals(name);

        out.print(INDENT.repeat(indents));
        if (!isIteratorMethod) {
            if (!memberPublic && !memberPrivate) {
                out.println("/** @internal */");
                out.print(INDENT.repeat(indents));
            }
            if (memberPublic) {
                out.print("public ");
                if (memberStatic) {
                    out.print("static ");
                }
            } else {
                if (memberStatic) {
                    out.print("static ");
                }
                if (memberPrivate) {
                    out.print("#");
                    methodPrivate.add(name);
                }
            }
        } else {
            name = "[Symbol.iterator]";
        }
        if ("main".equals(name)) {
            hasMain = true;
        }
        out.print(name);
        if (methodTypeParametersContext != null) {
            printTypeParameters(methodTypeParametersContext, false, true);
        }
        visitFormalParameters(ctx.formalParameters());
        out.print(" : ");
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        visitMethodBody(ctx.methodBody());
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
        boolean isLast = ctx.getParent().children.get(ctx.getParent().getChildCount()-1) == ctx;
        var parameterName = ctx.variableDeclaratorId().getText();
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        out.print(" : ");
        visitTypeType(ctx.typeType());
        variableType.put(parameterName, ctx.typeType());
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

    @Override
    public Void visitMemberDeclaration(JavaParser.MemberDeclarationContext ctx) {
        Void ret = super.visitMemberDeclaration(ctx);
        this.memberPrivate = false;
        this.memberPublic = false;
        this.memberStatic = false;
        return ret;
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

    private boolean checkSingleLineStatement(JavaParser.StatementContext ctx) {
        if (ctx.SEMI() != null) {
            out.println();
            indents++;
            out.print(INDENT.repeat(indents));
        }
        visitStatement(ctx);
        if (ctx.SEMI() != null) {
            out.println();
            indents--;
            out.print(INDENT.repeat(indents));
            return true;
        }
        return false;
    }

    private void checkElseStatement(JavaParser.StatementContext ctx, boolean singleLine) {
        if (ctx.ELSE() != null) {
            JavaParser.StatementContext nextCtx = ctx.statement(1);
            if (nextCtx.IF() != null) {
                if (!singleLine)
                    out.print(" ");
                out.print("else if (");
                visitParExpression(nextCtx.parExpression());
                out.print(")");
                boolean singleLineNext = checkSingleLineStatement(nextCtx.statement(0));
                checkElseStatement(nextCtx, singleLineNext);
            } else {
                if (!singleLine)
                    out.print(" ");
                out.print("else");
                checkSingleLineStatement(nextCtx);
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
            boolean singleLine = checkSingleLineStatement(ctx.statement(0));
            checkElseStatement(ctx, singleLine);
        } else if (ctx.FOR() != null) {
            var forControl = ctx.forControl();
            if (forControl.enhancedForControl() != null) {
                var enhancedForControl = forControl.enhancedForControl();
                out.print("for (let ");
                visitVariableDeclaratorId(enhancedForControl.variableDeclaratorId());
                out.print(" of ");
                visitExpression(enhancedForControl.expression());
                out.print(")");
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
            text = "#" + text;
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
        insideFieldDeclaration = true;
        out.print("\n");
        out.print(INDENT.repeat(indents));
        if (memberPublic)
            out.print("public ");
        if (memberStatic)
            out.print("static ");
        this.currentType = ctx.typeType();
        visitVariableDeclarators(ctx.variableDeclarators());
        this.currentType = null;
        out.print(";\n");
        insideFieldDeclaration = false;
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        var methodName = "super";
        if (ctx.SUPER() == null) {
            methodName = ctx.identifier().getText();
        }

        var staticImport = staticImports.containsKey(methodName);
        var isFirst = ctx.getParent().getChild(0) == ctx;
        var calledOn = ctx.getParent().getChild(0).getText();
        var currentClass = calledOn.equals(className);
        var isThis = calledOn.equals(THIS);
        var isLocal = currentClass || isThis || isFirst;

        if (isFirst && !staticImport) {
            out.print("this.");
        }

        if (isLocal && methodPrivate.contains(methodName))
            out.print("#");

        if (staticImport)
            out.print(staticImports.get(methodName));
        else
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
        if (CaseUtil.isClassName(name) && !className.equals(name) &&
                !importedClasses.contains(name) &&
                !currentTypeParameters.contains(name) &&
                !currentMethodTypeParameters.contains(name)) {
            if ("Iterable".equals(name) || "Iterator".equals(name))
                return;
            imports.add("import { " + name + " } from './" + name + "';");
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
                    out.print("#");
                }
                visitIdentifier(ctx.identifier());
            }

            if (isIntDivision)
                out.print(")");
        } else if (ctx.LPAREN() != null) {
            // this is a cast
            out.print("(");
            visitExpression(ctx.expression().get(0));
            out.print(" as unknown as ");
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
        return Otterop.WRAPPED_CLASS.equals(javaFullClassName) ||
               Otterop.TEST_ANNOTATION.equals(javaFullClassName) ||
               Otterop.ITERABLE.equals(javaFullClassName) ||
               Otterop.ITERATOR.equals(javaFullClassName);
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier().
                stream().map(identifier -> identifier.getText()).collect(Collectors.toList());
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        var javaFullClassName = identifiers.subList(0, classNameIdx + 1).stream()
                .collect(Collectors.joining("."));

        String className = identifiers.get(classNameIdx);
        javaFullClassNames.put(className, javaFullClassName);

        if (!excludeImports(javaFullClassName)) {
            importedClasses.add(className);
            String methodName = null;
            if (isStatic) {
                methodName = identifiers.get(classNameIdx + 1);
            }
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

            imports.add(
                    "import { " + className + " } from '" + fileStr + "';"
            );
            if (isStatic) {
                staticImports.put(
                        methodName,
                        className + "." + methodName
                );
            }
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
        if(ctx.SUPER() != null) out.print("super");
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
        var className = ctx.createdName().identifier().get(ctx.createdName().identifier().size()-1).getText();
        checkCurrentPackageImports(className);
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
        if ("Object".equals(identifier)) {
            out.print("object");
        } else if (Otterop.ITERABLE.equals(javaFullClassNames.get(identifier))) {
            out.print("Iterable<");
            visitTypeArguments(ctx.typeArguments(0));
            out.print(">");
        } else if ("java.lang.String".equals(identifier)) {
            out.print("string");
        } else {
            checkCurrentPackageImports(identifier);
            out.print(identifier);
        }
        if (!ctx.typeArguments().isEmpty()) {
            printTypeArguments(ctx.typeArguments());
        }
        return null;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        String primitiveType = ctx.getText();
        if(NUMERIC_TYPES.contains(primitiveType)) {
            out.print("number");
        } else {
            out.print(primitiveType);
        }
        return null;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        return null;
    }

    public boolean makePure() {
        return this.makePure && !testClass();
    }

    public boolean testClass() {
        return !testMethods.isEmpty();
    }

    public void printTo(PrintStream ps) {
        for (String importStatement : imports) {
            ps.println(importStatement);
        }
        ps.println("");
        ps.print(outStream.toString());
        if (!testMethods.isEmpty()) {
            for (var testMethod : testMethods) {
                var methodName = testMethod.identifier().getText();
                ps.print("\ntest('");
                ps.print(methodName);
                ps.print("', () => {\n");
                indents++;
                ps.print(INDENT.repeat(indents));
                ps.print("new ");
                ps.print(className);
                ps.print("().");
                ps.print(methodName);
                ps.print("();\n");
                indents--;
                ps.print("});\n");
            }
        }
        if (hasMain) {
            ps.print(className + ".main(process.argv.slice(2));");
        }
    }
}
