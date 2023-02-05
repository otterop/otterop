package otterop.transpiler.visitor;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToPascalCase;

public class CSharpParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean methodStatic = false;
    private boolean methodPublic = false;
    private String className = null;
    private String namespace = null;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private Map<String,String> fullClassName = new LinkedHashMap<>();
    private Map<String,String> staticImports = new LinkedHashMap<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private PrintStream out = new PrintStream(outStream);
    private JavaParser.TypeParameterContext typeParameterContext;

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        out.print("class ");
        className = ctx.identifier().getText();
        this.visitIdentifier(ctx.identifier());
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        super.visitClassBody(ctx.classBody());
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}\n");
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        if (methodPublic) {
            out.print("public ");
        }
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        visitBlock(ctx.block());
        this.methodStatic = false;
        this.methodPublic = false;
        out.print("\n");
        return null;
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        if (methodPublic) {
            out.print("public ");
        }
        if (methodStatic) {
            out.print("static ");
        }
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);
        out.print(name);
//        if (typeParameterContext != null) {
//            out.print("[");
//            visitIdentifier(typeParameterContext.identifier());
//            out.print(" any]");
//            typeParameterContext = null;
//        }
        visitFormalParameters(ctx.formalParameters());
        visitMethodBody(ctx.methodBody());
        this.methodStatic = false;
        this.methodPublic = false;
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
    public Void visitModifier(JavaParser.ModifierContext ctx) {
        if (ctx.getText().equals("static")) methodStatic = true;
        if (ctx.getText().equals("public")) methodPublic = true;
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
        } else {
            if (ctx.RETURN() != null) {
                out.print("return ");
            }
            super.visitStatement(ctx);
            if (ctx.expression() != null) {
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
        out.print(INDENT.repeat(indents));
        visitTypeType(ctx.typeType());
        out.print(" ");
        visitVariableDeclarators(ctx.variableDeclarators());
        out.print(";\n");
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        var methodName = ctx.identifier().getText();
        if (staticImports.containsKey(methodName)) {
            methodName = staticImports.get(methodName);
        }
        methodName = camelCaseToPascalCase(methodName);
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
        fullClassName.put(className, classStr);
        if (isStatic) {
            staticImports.put(methodName, classStr + "." + camelCaseToPascalCase(methodName));
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
        if (ctx.NULL_LITERAL() != null) out.print("nil");
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
        visitTypeType(ctx.typeType());
        out.print(" ");
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
    public Void visitTypeParameter(JavaParser.TypeParameterContext ctx) {
        this.typeParameterContext = ctx;
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
            out.print("<");
            visitTypeArguments(ctx.typeArguments(0));
            out.print(">");
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

    public void printTo(PrintStream ps) {
        ps.print("namespace ");
        ps.print(namespace);
        ps.print("\n{\n");
        ps.print(outStream.toString());
        ps.print("}\n");
    }
}
