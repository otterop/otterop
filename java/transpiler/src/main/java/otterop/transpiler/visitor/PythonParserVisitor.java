package otterop.transpiler.visitor;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.util.CaseUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;

public class PythonParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean hasMain = false;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private String className;
    private String currentPythonPackage;
    private Set<String> imports = new LinkedHashSet<>();
    private Set<String> fromImports = new LinkedHashSet<>();
    private Set<String> staticImports = new LinkedHashSet<>();
    private final Set<String> importedClasses = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Map<String, JavaParser.TypeTypeContext> variableType = new LinkedHashMap<>();
    private JavaParser.TypeTypeContext currentType;
    private PrintStream out = new PrintStream(outStream);

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        out.print("class ");
        className = ctx.identifier().getText();
        out.print(className);
        out.println(":");
        indents++;
        out.print(INDENT.repeat(indents));
        out.println("pass");
        indents++;
        return null;
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print("class ");
        className = ctx.identifier().getText();
        importedClasses.add(className);
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
        visitBlock(ctx.block());
        return null;
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        variableType.clear();
        out.print("\n");
        if (memberStatic) {
            out.print(INDENT.repeat(indents) + "@staticmethod\n    def ");
        } else {
            out.print(INDENT.repeat(indents) + "def ");
        }
        var name = camelCaseToSnakeCase(ctx.identifier().getText());
        if (name.equals("main")) hasMain = true;

        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        visitMethodBody(ctx.methodBody());
        this.memberStatic = false;
        variableType.clear();
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        if (!memberStatic) {
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
        var parameterName = ctx.variableDeclaratorId().getText();
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        variableType.put(parameterName, ctx.typeType());
        if (!isLast) out.print(", ");
        return null;
    }

    @Override
    public Void visitModifier(JavaParser.ModifierContext ctx) {
        memberStatic = ctx.getText().equals("static");
        memberPublic = ctx.getText().equals("public");
        super.visitModifier(ctx);
        return null;
    }

    @Override
    public Void visitBlock(JavaParser.BlockContext ctx) {
        indents++;
        if (ctx.children.size() <= 2) {
            out.print(INDENT.repeat(indents));
            out.print("pass");
        } else super.visitBlock(ctx);
        indents--;
        return null;
    }

    private void checkElseStatement(JavaParser.StatementContext ctx) {
        if (ctx.ELSE() != null) {
            if (ctx.statement(1).IF() != null) {
                out.print(INDENT.repeat(indents) + "elif ");
                visitParExpression(ctx.statement(1).parExpression());
                out.print(":\n");
                visitStatement(ctx.statement(1).statement(0));
                checkElseStatement(ctx.statement(1));
            } else {
                out.print(INDENT.repeat(indents) + "else:\n");
                visitStatement(ctx.statement(1));
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
            visitStatement(ctx.statement(0));
            checkElseStatement(ctx);
            skipNewlines++;
        } else if (ctx.FOR() != null) {
            var forControl = ctx.forControl();
            visitChildren(forControl.forInit());
            out.print("\n");
            out.print(INDENT.repeat(indents));
            out.print("while ");
            if (forControl.expression() != null)
                visitExpression(forControl.expression());
            else
                out.print("True");
            out.print(":\n");
            visitStatement(ctx.statement(0));
            if (forControl.forUpdate != null) {
                indents++;
                out.print(INDENT.repeat(indents));
                visitChildren(forControl.forUpdate);
                indents--;
            }
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
        variableType.put(ctx.variableDeclaratorId().getText(), currentType);
        if (ctx.variableInitializer() != null) {
            visitVariableDeclaratorId(ctx.variableDeclaratorId());
            out.print(" = ");
            super.visitVariableInitializer(ctx.variableInitializer());
        }
        return null;
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        if (memberStatic) {
            visitVariableDeclarators(ctx.variableDeclarators());
            out.print("\n");
        }
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

    @Override
    public Void visitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.prefix != null) {
            var prefixText = ctx.prefix.getText();
            if (prefixText.equals("!")) prefixText = "not ";
            out.print(prefixText);
        }
        if (ctx.bop != null) {
            var name = ctx.expression(0).getText();
            if (CaseUtil.isClassName(name) && !className.equals(name) &&
                    !importedClasses.contains(name)) {
                var importStatement = "from " + currentPythonPackage + "." + camelCaseToSnakeCase(name) + " import "+ name;
                fromImports.add(importStatement);
                importedClasses.add(name);
            }
            if (ctx.expression(0) != null) visit(ctx.expression(0));
            var bop = ctx.bop.getText();
            if (bop.equals("&&")) bop = "and";
            if (bop.equals("||")) bop = "or";

            if (bop.equals("/") &&
                    isIntPrimary(ctx.expression(0).primary()) &&
                    isIntPrimary(ctx.expression(1).primary())) {
                bop = "//";
            }

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
        if (ctx.postfix != null) {
            var postfixText = ctx.postfix.getText();
            if (postfixText.equals("++")) postfixText = " += 1";
            if (postfixText.equals("--")) postfixText = " -= 1";
            out.print(postfixText);
        }
        return null;
    }

    private String pythonPackage(List<String> identifiers) {
        return String.join(".",
                identifiers.stream().map(identifier ->
                                camelCaseToSnakeCase(identifier))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier();
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        String className = identifiers.get(classNameIdx).getText();
        var pythonPackage = pythonPackage(
                identifiers.subList(0, classNameIdx + 1).stream()
                        .map(id -> id.getText()).collect(Collectors.toList())
        );
        var importStatement = "from " + pythonPackage + " import "+ className;
        importedClasses.add(className);
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
        var skipSnakeCase = importedClasses.contains(ctxText);
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
        currentType = ctx.typeType();
        visitVariableDeclarators(ctx.variableDeclarators());
        return null;
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        out.print(ctx.createdName().identifier(0).getText());
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
        currentPythonPackage = pythonPackage(ctx.qualifiedName().identifier().stream().map(
                identifier -> identifier.getText()
        ).collect(Collectors.toList()));
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
