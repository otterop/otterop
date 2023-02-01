package otterop.transpiler.visitor;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;

public class PythonParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean methodStatic = false;
    private boolean hasMain = false;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private Set<String> imports = new LinkedHashSet<>();
    private Set<String> fromImports = new LinkedHashSet<>();
    private Set<String> staticImports = new LinkedHashSet<>();
    private final Set<String> clazzes = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private PrintStream out = new PrintStream(outStream);

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print("class ");
        var className = ctx.identifier().getText();
        clazzes.add(className);
        out.print(className);
        out.println(":");
        indents++;
        super.visitClassBody(ctx.classBody());
        indents--;
        if (hasMain) {
            out.println("\nif __name__ == \"__main__\":");
            imports.add("import sys");
            out.println(INDENT + ctx.identifier().getText() + ".main(sys.argv[1:])");
        }
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        out.print("\n");
        out.print(INDENT.repeat(indents) + "def __init__");
        visitFormalParameters(ctx.formalParameters());
        indents++;
        visitBlock(ctx.block());
        indents--;
        return null;
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        out.print("\n");
        if (methodStatic) {
            out.print(INDENT.repeat(indents) + "@staticmethod\n    def ");
        } else {
            out.print(INDENT.repeat(indents) + "def ");
        }
        var name = camelCaseToSnakeCase(ctx.identifier().getText());
        if (name.equals("main")) hasMain = true;

        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        visitMethodBody(ctx.methodBody());
        this.methodStatic = false;
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        if (!methodStatic) {
            out.print("(self");
            if (ctx.formalParameterList() != null &&
                    !ctx.formalParameterList().isEmpty()) out.print(", ");
        } else {
            out.print("(");
        }
        if (ctx.formalParameterList() != null)
            visitFormalParameterList(ctx.formalParameterList());
        out.print("):\n");
        return null;
    }

    @Override
    public Void visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        boolean isLast = ctx.getParent().children.get(ctx.getParent().getChildCount()-1) == ctx;
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        if (!isLast) out.print(", ");
        return null;
    }

    @Override
    public Void visitModifier(JavaParser.ModifierContext ctx) {
        if (ctx.getText().equals("static")) this.methodStatic = true;
        super.visitModifier(ctx);
        return null;
    }

    @Override
    public Void visitMethodBody(JavaParser.MethodBodyContext ctx) {
        indents++;
        super.visitMethodBody(ctx);
        indents--;
        return null;
    }

    private void checkElseStatement(JavaParser.StatementContext ctx) {
        if (ctx.ELSE() != null) {
            if (ctx.statement(1).IF() != null) {
                out.print(INDENT.repeat(indents) + "elif ");
                visitParExpression(ctx.statement(1).parExpression());
                out.print(":\n");
                indents++;
                visitStatement(ctx.statement(1).statement(0));
                indents--;
                checkElseStatement(ctx.statement(1));
            } else {
                out.print(INDENT.repeat(indents) + "else:\n");
                indents++;
                visitStatement(ctx.statement(1));
                indents--;
            }
        }
    }

    @Override
    public Void visitStatement(JavaParser.StatementContext ctx) {
        if (ctx.IF() != null || ctx.WHILE() != null) {
            if(ctx.IF() != null) out.print("if ");
            if(ctx.WHILE() != null) out.print("while ");
            super.visit(ctx.parExpression());
            out.print(":\n");
            indents++;
            visitStatement(ctx.statement(0));
            indents--;
            checkElseStatement(ctx);
            skipNewlines++;
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
        if (ctx.variableInitializer() != null) {
            visitVariableDeclaratorId(ctx.variableDeclaratorId());
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
        visitIdentifier(ctx.identifier());
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
            if (prefixText.equals("!")) prefixText = "not ";
            out.print(prefixText);
        }
        if (ctx.bop != null) {
            if (ctx.expression(0) != null) visit(ctx.expression(0));
            var bop = ctx.bop.getText();
            if (bop.equals("&&")) bop = "and";
            if (bop.equals("||")) bop = "or";
            if (!bop.equals(".")) bop = " " + bop + " ";
            out.print(bop);
            if (ctx.expression(1) != null) visitExpression(ctx.expression(1));
            else if (ctx.methodCall() != null) visitMethodCall(ctx.methodCall());
            else if (ctx.identifier() != null) visitIdentifier(ctx.identifier());
        } else if (ctx.RPAREN() != null) {
            visitExpression(ctx.expression(0));
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
        var pythonPackage = String.join(".",
                identifiers.subList(0, classNameIdx + 1).stream().map(identifier ->
                                camelCaseToSnakeCase(identifier.getText()))
                        .collect(Collectors.toList())
        );
        var importStatement = "from " + pythonPackage + " import "+ className;
        clazzes.add(className);
        fromImports.add(importStatement);
        if (isStatic) {
            var methodName = identifiers.get(classNameIdx + 1).getText();
            var methodNameSnake = camelCaseToSnakeCase(methodName);
            var staticImportStatement = methodNameSnake + " = " + className + "." + methodNameSnake;
            staticImports.add(staticImportStatement);
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
        if (ctx.NULL_LITERAL() != null) out.print("None");
        else out.print(ctx.getText());
        super.visitLiteral(ctx);
        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        var ctxText = ctx.getText();
        var skipSnakeCase = clazzes.contains(ctxText);
        if (skipSnakeCase) out.print(ctxText);
        else out.print(camelCaseToSnakeCase(ctxText));
        return null;
    }

    @Override
    public Void visitPrimary(JavaParser.PrimaryContext ctx) {
        if(ctx.THIS() != null) out.print("self");
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
        visitVariableDeclarators(ctx.variableDeclarators());
        return null;
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        out.print(ctx.createdName().getText());
        visitClassCreatorRest(ctx.classCreatorRest());
        return null;
    }

    @Override
    public Void visitTypeTypeOrVoid(JavaParser.TypeTypeOrVoidContext ctx) {
        return super.visitTypeTypeOrVoid(ctx);
    }

    @Override
    public Void visitTypeParameter(JavaParser.TypeParameterContext ctx) {
        return null;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        return null;
    }

    public void printTo(PrintStream ps) {
        for (String importStatement : imports) {
            ps.println(importStatement);
        }
        for (String importStatement : fromImports) {
            ps.println(importStatement);
        }
        for (String staticImport : staticImports) {
            ps.println(staticImport);
        }
        ps.println("");
        ps.print(outStream.toString());
    }
}
