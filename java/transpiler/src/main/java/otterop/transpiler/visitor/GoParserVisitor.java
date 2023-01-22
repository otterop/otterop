package otterop.transpiler.visitor;

import org.antlr.v4.runtime.tree.ParseTree;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToPascalCase;
import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;

public class GoParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean methodStatic = false;
    private boolean methodPublic = false;
    private boolean isMain = false;
    private boolean hasMain = false;
    private boolean insideConstructor = false;
    private String className = null;
    private String lastPackageName = null;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private Set<String> imports = new LinkedHashSet<>();
    private Map<String,String> modules = new LinkedHashMap<>();
    private Set<String> fromImports = new LinkedHashSet<>();
    private Set<String> staticImports = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Set<String> publicMethods = new LinkedHashSet<>();
    private Set<String> staticMethods = new LinkedHashSet<>();
    private List<JavaParser.FieldDeclarationContext> fields = new LinkedList<>();
    private PrintStream out = new PrintStream(outStream);
    private Map<String,String> importDomainMapping;
    private JavaParser.TypeParameterContext typeParameterContext;

    public GoParserVisitor(Map<String,String> importDomainMapping) {
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
            if (child instanceof JavaParser.ClassOrInterfaceModifierContext) {
                JavaParser.ClassOrInterfaceModifierContext modifierChild = (JavaParser.ClassOrInterfaceModifierContext) child;
                if (!methodPublic) methodPublic = modifierChild.PUBLIC() != null;
                if (!methodStatic) methodStatic = modifierChild.STATIC() != null;
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
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        detectMethods(ctx);
        out.print("type ");
        className = ctx.identifier().getText();
        this.visitIdentifier(ctx.identifier());
        out.print(" struct {\n");
        indents++;
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

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        out.print("\n\nfunc ");
        var name = ctx.identifier().getText();
        if (methodPublic && !isMain) name = camelCaseToPascalCase(name);
        out.print("New" + name);
        if (typeParameterContext != null) {
            out.print("[");
            visitIdentifier(typeParameterContext.identifier());
            out.print(" any]");
            typeParameterContext = null;
        }
        visitFormalParameters(ctx.formalParameters());
        out.print(" *" + className);
        insideConstructor = true;
        visitBlock(ctx.block());
        insideConstructor = false;
        this.methodStatic = false;
        this.methodPublic = false;
        return null;
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        out.print("\n\nfunc ");
        if (!methodStatic) {
            out.print("(" + THIS + " *" + className + ") ");
        }
        var name = ctx.identifier().getText();
        isMain = name.equals("main");
        if (isMain) hasMain = true;
        if (methodPublic && !isMain) name = camelCaseToPascalCase(name);
        out.print(name);
        if (typeParameterContext != null) {
            out.print("[");
            visitIdentifier(typeParameterContext.identifier());
            out.print(" any]");
            typeParameterContext = null;
        }
        visitFormalParameters(ctx.formalParameters());
        out.print(" ");
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        visitMethodBody(ctx.methodBody());
        this.methodStatic = false;
        this.methodPublic = false;
        this.isMain = false;
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
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        out.print(" ");
        visitTypeType(ctx.typeType());
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
        out.print(" {\n");
        indents++;
        if (isMain) {
            imports.add("\"os\"");
            out.print(INDENT.repeat(indents) + "args := os.Args[1:]\n");
            isMain = false;
        } else if (insideConstructor) {
            out.print(INDENT.repeat(indents) + "this := new(" + className + ")\n");
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
            out.print(" := ");
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
        var calledOn = ctx.getParent().getChild(0).getText();
        var thisClass = calledOn.equals(className);
        var methodName = ctx.identifier().getText();
        var changeCase = !thisClass || publicMethods.contains(methodName);
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
        var rootPackage = identifiers.get(0).getText();
        var packageStr = String.join("/",
                identifiers.subList(1, classNameIdx).stream().map(identifier -> identifier.getText())
                        .collect(Collectors.toList())
        );
        if (importDomainMapping.containsKey(rootPackage)) {
            packageStr = importDomainMapping.get(rootPackage) + "/" + packageStr;
        }
        var fileName = className.toLowerCase();
        modules.put(className, fileName);
        var importStatement = "\"" + packageStr + "/"+ fileName + "\"";
        fromImports.add(importStatement);
        if (isStatic) {
            var methodName = camelCaseToPascalCase(identifiers.get(classNameIdx + 1).getText());
            var staticImportStatement = "var " + methodName + " = " + fileName + "." + methodName;
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
        visitVariableDeclarators(ctx.variableDeclarators());
        return null;
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        out.print("New" + ctx.createdName().getText());
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
        } else {
            out.print("*" + identifier.toLowerCase() + "." + identifier);
        }
        if (ctx.typeArguments().size() > 0) {
            out.print("[");
            visitTypeArguments(ctx.typeArguments(0));
            out.print("]");
        }
        return null;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        String name = ctx.getText();
        if ("double".equals(name)) out.print("float64");
        else if ("float".equals(name)) out.print("float32");
        else out.print(name);
        return null;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        var identifiers = ctx.qualifiedName().identifier();
        lastPackageName = camelCaseToSnakeCase(identifiers.get(identifiers.size() - 1).getText());
        return null;
    }

    public void printTo(PrintStream ps) {
        if (hasMain) {
            ps.print("package main\n\n");
        } else if (lastPackageName != null) {
            ps.print("package ");
            ps.print(lastPackageName);
            ps.print("\n\n");
        }

        ps.print("import (\n");
        indents++;
        for (String importStatement : imports) {
            ps.println(INDENT.repeat(indents) + importStatement);
        }
        for (String importStatement : fromImports) {
            ps.println(INDENT.repeat(indents) + importStatement);
        }
        indents--;
        ps.print(")\n");
        for (String staticImport : staticImports) {
            ps.println(INDENT.repeat(indents) + staticImport);
        }
        ps.println("");
        ps.print(outStream.toString());
    }
}
