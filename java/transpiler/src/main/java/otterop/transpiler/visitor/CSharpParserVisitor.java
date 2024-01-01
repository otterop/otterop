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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToPascalCase;

public class CSharpParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean classPublic = false;
    private boolean isGenericClass = false;
    private boolean insideConstructor = false;
    private boolean isNewMethod = false;
    private boolean hasStaticMethods = false;
    private boolean insideStaticNonGeneric = false;
    private String className = null;
    private String namespace = null;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private static String SUPER = "super";
    private Map<String,String> fullClassName = new LinkedHashMap<>();
    private Map<String,String> javaFullClassName = new LinkedHashMap<>();
    private Map<String,String> staticImports = new LinkedHashMap<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private PrintStream out = new PrintStream(outStream);
    private JavaParser.TypeParametersContext classTypeParametersContext;
    private JavaParser.TypeParametersContext methodTypeParametersContext;
    private boolean makePure = false;
    private boolean isInterface = false;

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        isInterface = true;
        out.print(INDENT.repeat(indents));
        if (classPublic) {
            out.print("public ");
        }
        out.print("interface ");
        className = ctx.identifier().getText();
        this.visitIdentifier(ctx.identifier());
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        visitInterfaceBody(ctx.interfaceBody());
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}\n");
        isInterface = false;
        return null;
    }

    @Override
    public Void visitTypeList(JavaParser.TypeListContext ctx) {
        boolean rest = false;
        for (JavaParser.TypeTypeContext typeType : ctx.typeType()) {
            if (rest) out.print(", ");
            else rest = true;
            visitTypeType(typeType);
        }
        return null;
    }

    @Override
    public Void visitAnnotation(JavaParser.AnnotationContext ctx) {
        var annotationName = ctx.qualifiedName().identifier().get(0).getText();
        var fullAnnotationName = javaFullClassName.get(annotationName);
        makePure |= Otterop.WRAPPED_CLASS.equals(fullAnnotationName);
        if ("Override".equals(annotationName))
            this.isNewMethod = true;
        return null;
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        if (classPublic) {
            out.print("public ");
        }
        out.print("class ");
        className = ctx.identifier().getText();
        this.visitIdentifier(ctx.identifier());
        this.classTypeParametersContext = ctx.typeParameters();
        if (this.classTypeParametersContext != null) {
            isGenericClass = true;
        }
        printTypeParameters(this.classTypeParametersContext);
        if (ctx.IMPLEMENTS() != null || ctx.EXTENDS() != null) {
            out.print(" : ");
            if (ctx.IMPLEMENTS() != null) {
                visitTypeList(ctx.typeList(0));
                if (ctx.EXTENDS() != null)
                    out.print(", ");
            }
            if (ctx.EXTENDS() != null) {
                visitTypeType(ctx.typeType());
            }
        }
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        super.visitClassBody(ctx.classBody());
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}\n");

        if (isGenericClass && hasStaticMethods) {
            insideStaticNonGeneric = true;
            out.print("\n");
            out.print(INDENT.repeat(indents));
            out.print("class ");
            this.visitIdentifier(ctx.identifier());
            out.print("\n");
            out.print(INDENT.repeat(indents));
            out.print("{\n");
            indents++;
            super.visitClassBody(ctx.classBody());
            indents--;
            out.print(INDENT.repeat(indents));
            out.println("}\n");
            insideStaticNonGeneric = false;
        }

        return null;
    }

    private void printTypeParameters(JavaParser.TypeParametersContext typeParametersContext) {
        if (typeParametersContext != null) {
            out.print("<");
            boolean rest = false;
            for (var t: typeParametersContext.typeParameter()) {
                if(rest)
                    out.print(", ");
                else
                    rest = true;
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
        if (insideStaticNonGeneric)
            return null;
        out.print(INDENT.repeat(indents));
        if (memberPublic) {
            out.print("public ");
        } else {
            out.print("private ");
        }
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        if (ctx.block() != null &&
            ctx.block().blockStatement(0) != null &&
            ctx.block().blockStatement(0).statement() != null &&
            ctx.block().blockStatement(0).statement().getText().startsWith("super(")) {
            out.print(" : ");
            visitExpression(ctx.block().blockStatement(0).statement().statementExpression);
        }
        insideConstructor = true;
        visitBlock(ctx.block());
        insideConstructor = false;
        out.print("\n");
        return null;
    }

    @Override
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = isInterface;
        return super.visitClassBodyDeclaration(ctx);
    }

    @Override
    public Void visitGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        if (memberStatic && !hasStaticMethods) {
            hasStaticMethods = true;
        }
        this.methodTypeParametersContext = ctx.typeParameters();
        super.visitGenericMethodDeclaration(ctx);
        this.methodTypeParametersContext = null;
        return null;
    }

    private void visitMethodDeclarationInsideStaticNonGeneric(JavaParser.MethodDeclarationContext ctx, String name) {
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        out.print(INDENT.repeat(indents));
        if (ctx.typeTypeOrVoid().VOID() == null)
            out.print("return ");
        out.print(className);
        boolean rest = false;
        out.print("<");
        for (var classTypeParameterContext : classTypeParametersContext.typeParameter()) {
            if (rest) out.print(", ");
            else rest = true;
            out.print("object");
        }
        out.print(">");
        out.print(".");
        out.print(name);
        printTypeParameters(methodTypeParametersContext);
        out.print("(");
        rest = false;
        for (var formalParameter : ctx.formalParameters().formalParameterList().formalParameter()) {
            if (rest) out.print(", ");
            else rest = true;
            visitVariableDeclaratorId(formalParameter.variableDeclaratorId());
        }
        out.print(");");
        out.print("\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    @Override
    public Void visitInterfaceCommonBodyDeclaration(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        out.print(";\n");
        return null;
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (memberStatic && !hasStaticMethods) {
            hasStaticMethods = true;
        }
        if (insideStaticNonGeneric && !memberStatic)
            return null;
        out.print(INDENT.repeat(indents));
        if (isNewMethod) {
            out.print("new ");
        }
        if (memberPublic) {
            out.print("public ");
        }
        if (memberStatic) {
            out.print("static ");
        }
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);
        out.print(name);
        if (methodTypeParametersContext != null) {
            printTypeParameters(methodTypeParametersContext);
        }
        visitFormalParameters(ctx.formalParameters());
        if (insideStaticNonGeneric) {
            visitMethodDeclarationInsideStaticNonGeneric(ctx, name);
        } else
            visitMethodBody(ctx.methodBody());
        this.isNewMethod = false;
        out.print("\n");
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
        visitTypeType(ctx.typeType());
        out.print(" ");
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        if (!isLast) out.print(", ");
        return null;
    }

    @Override
    public Void visitClassOrInterfaceModifier(JavaParser.ClassOrInterfaceModifierContext ctx) {
        if (ctx.annotation() != null) visitAnnotation(ctx.annotation());
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
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        super.visitBlock(ctx);
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
        return null;
    }

    private void checkElseStatement(JavaParser.StatementContext ctx) {
        if (ctx.ELSE() != null) {
            if (ctx.statement(1).IF() != null) {
                out.print(INDENT.repeat(indents));
                out.print("else if (");
                visitParExpression(ctx.statement(1).parExpression());
                out.print(")");
                super.visitStatement(ctx.statement(1).statement(0));
                checkElseStatement(ctx.statement(1));
            } else {
                out.print(INDENT.repeat(indents));
                out.print("else");
                super.visitStatement(ctx.statement(1));
            }
        }
    }

    @Override
    public Void visitStatement(JavaParser.StatementContext ctx) {
        if (ctx.IF() != null || ctx.WHILE() != null) {
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
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        if (ctx.variableInitializer() != null) {
            out.print(" = ");
            super.visitVariableInitializer(ctx.variableInitializer());
        }
        return null;
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        if (insideStaticNonGeneric)
            return null;
        out.print(INDENT.repeat(indents));
        if (memberPublic)
            out.print("public ");
        else
            out.print("private ");
        if (memberStatic)
            out.print("static ");

        visitTypeType(ctx.typeType());
        out.print(" ");
        visitVariableDeclarators(ctx.variableDeclarators());
        out.print(";\n");
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        var methodName = "base";
        if (ctx.SUPER() == null) {
            methodName = ctx.identifier().getText();
        }

        if (staticImports.containsKey(methodName)) {
            methodName = staticImports.get(methodName);
        }
        if (ctx.SUPER() == null) {
            methodName = camelCaseToPascalCase(methodName);
        }
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
                    visitMethodCall(ctx.methodCall());
                }
            }
            if (!localMethodCall) {
                if (ctx.expression(0) != null) visitExpression(ctx.expression(0));
                var bop = ctx.bop.getText();
                if (!bop.equals(".")) bop = " " + bop + " ";
                out.print(bop);
                if (ctx.expression(1) != null) visitExpression(ctx.expression(1));
                else if (ctx.methodCall() != null) visitMethodCall(ctx.methodCall());
                else if (ctx.identifier() != null) visitIdentifier(ctx.identifier());
            }
        } else if (ctx.RPAREN() != null) {
            visitExpression(ctx.expression(0));
            out.print(".(");
            visitTypeType(ctx.typeType(0));
            out.print(")");
        } else if (fullClassName.containsKey(ctx.getText())) {
            out.print(fullClassName.get(ctx.getText()));
        } else {
            super.visitExpression(ctx);
        }
        if (ctx.postfix != null) {
            var postfixText = ctx.postfix.getText();
            out.print(postfixText);
        }
        return null;
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier();
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        String className = identifiers.get(classNameIdx).getText();
        String methodName = null;
        if (isStatic) {
            methodName =  identifiers.get(classNameIdx + 1).getText();
        }
        var classStr = String.join(".",
                identifiers.subList(0,classNameIdx + 1).stream().map(
                        identifier -> camelCaseToPascalCase(identifier.getText()))
                        .collect(Collectors.toList())
        );
        var javaClassStr = String.join(".",
                identifiers.subList(0,classNameIdx + 1).stream().map(
                                identifier -> identifier.getText())
                        .collect(Collectors.toList())
        );
        fullClassName.put(className, classStr);
        javaFullClassName.put(className, javaClassStr);
        if (isStatic) {
            staticImports.put(methodName, classStr + "." + camelCaseToPascalCase(methodName));
        }
        return null;
    }

    @Override
    public Void visitBlockStatement(JavaParser.BlockStatementContext ctx) {
        if (ctx.statement() != null && ctx.statement().getText().startsWith("super") && insideConstructor) {
            return null;
        }

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
        if(ctx.SUPER() != null) out.print("base");
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
        visitTypeType(ctx.typeType());
        out.print(" ");
        visitVariableDeclarators(ctx.variableDeclarators());
        return null;
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        var name = ctx.createdName().identifier(0).getText();
        out.print("new ");
        out.print(fullClassName.get(name));
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
        } else if ("java.lang.String".equals(identifier)) {
            out.print("string");
        } else {
            if (fullClassName.containsKey(identifier))
                identifier = fullClassName.get(identifier);
            out.print(identifier);
        }
        if (ctx.typeArguments().size() > 0) {
            printTypeArguments(ctx.typeArguments());
        }
        return null;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        String primitiveType = ctx.getText();
        if("boolean".equals(primitiveType)) {
            out.print("bool");
        } else {
            out.print(primitiveType);
        }
        return null;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        var identifiers = ctx.qualifiedName().identifier();
        namespace = identifiers.stream().map(
                identifier -> camelCaseToPascalCase(identifier.getText())
        ).collect(Collectors.joining("."));
        indents++;
        return null;
    }

    public boolean makePure() {
        return this.makePure;
    }

    public void printTo(PrintStream ps) {
        ps.print("namespace ");
        ps.print(namespace);
        ps.print("\n{\n");
        ps.print(outStream.toString());
        ps.print("}\n");
    }
}
