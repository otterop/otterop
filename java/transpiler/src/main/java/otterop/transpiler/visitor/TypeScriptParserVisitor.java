package otterop.transpiler.visitor;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.util.CaseUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeScriptParserVisitor extends JavaParserBaseVisitor<Void> {
    private final String basePackage;
    private final String currentPackage;
    private final String[] currentPackageIdentifiers;
    private boolean methodStatic = false;
    private boolean methodPublic = false;
    private boolean classPublic = false;
    private String className = null;
    private boolean hasMain = false;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private Set<String> imports = new LinkedHashSet<>();
    private Set<String> importedClasses = new LinkedHashSet<>();
    private Set<String> staticImports = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private PrintStream out = new PrintStream(outStream);
    private JavaParser.TypeParametersContext classTypeParametersContext;
    private JavaParser.TypeParametersContext methodTypeParametersContext;

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
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        if (classPublic)
            out.print("export ");
        out.print("class ");
        className = ctx.identifier().getText();
        this.visitIdentifier(ctx.identifier());
        this.classTypeParametersContext = ctx.typeParameters();
        printTypeParameters(this.classTypeParametersContext);
        out.print(" {\n");
        indents++;
        super.visitClassBody(ctx.classBody());
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}\n");
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
        out.print("\n");
        out.print(INDENT.repeat(indents));
        if (methodPublic) {
            out.print("public ");
        } else {
            out.print("private ");
        }
        out.print("constructor");
        visitFormalParameters(ctx.formalParameters());
        visitBlock(ctx.block());
        this.methodStatic = false;
        this.methodPublic = false;
        out.print("\n");
        return null;
    }

    @Override
    public Void visitGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        this.methodTypeParametersContext = ctx.typeParameters();
        return super.visitGenericMethodDeclaration(ctx);
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        out.print("\n");
        out.print(INDENT.repeat(indents));
        if (methodPublic) {
            out.print("public ");
        }
        if (methodStatic) {
            out.print("static ");
        }
        var name = ctx.identifier().getText();
        if ("main".equals(name)) {
            hasMain = true;
        }
        out.print(name);
        if (methodTypeParametersContext != null) {
            printTypeParameters(methodTypeParametersContext);
        }
        visitFormalParameters(ctx.formalParameters());
        out.print(" : ");
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
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
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        out.print(" : ");
        visitTypeType(ctx.typeType());
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
        if (ctx.getText().equals("static")) methodStatic = true;
        if (ctx.getText().equals("public")) methodPublic = true;
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
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("private ");
        visitVariableDeclarators(ctx.variableDeclarators());
        out.print(" : ");
        visitTypeType(ctx.typeType());
        out.print(";\n");
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

    @Override
    public Void visitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.prefix != null) {
            var prefixText = ctx.prefix.getText();
            out.print(prefixText);
        }
        if (ctx.bop != null) {
            var name = ctx.expression(0).getText();
            if (CaseUtil.isClassName(name) && !className.equals(name) &&
                !importedClasses.contains(name)) {
                imports.add("import { " + name + " } from './" + name + "';");
            }
            if (ctx.expression(0) != null) visitExpression(ctx.expression(0));
            var bop = ctx.bop.getText();
            if (!bop.equals(".")) bop = " " + bop + " ";
            out.print(bop);
            if (ctx.expression(1) != null) visitExpression(ctx.expression(1));
            else if (ctx.methodCall() != null) visitMethodCall(ctx.methodCall());
            else if (ctx.identifier() != null) visitIdentifier(ctx.identifier());
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

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier().
                stream().map(identifier -> identifier.getText()).collect(Collectors.toList());
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        String className = identifiers.get(classNameIdx);
        importedClasses.add(className);
        String methodName = null;
        if (isStatic) {
            methodName =  identifiers.get(classNameIdx + 1);
        }
        var isCurrentPackage = String.join(".", identifiers).startsWith(basePackage);
        var fileStr = "";
        if (isCurrentPackage) {
            fileStr = relativePath(currentPackageIdentifiers, identifiers
                                                                .toArray(new String[0]));
        } else {
            fileStr = "@" + String.join("/",
                    identifiers.subList(0, classNameIdx + 1).stream().map(
                                    identifier -> identifier.toLowerCase())
                            .collect(Collectors.toList())
            );
        }

        imports.add(
                "import { " + className + " } from '" + fileStr + "';"
        );
        if (isStatic) {
            staticImports.add(
                    "const { " + methodName + " } = " + className + ";"
            );
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
        if ("Object".equals(identifier)) {
            out.print("object");
        } else if ("java.lang.String".equals(identifier)) {
            out.print("string");
        } else {
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

    public void printTo(PrintStream ps) {
        for (String importStatement : imports) {
            ps.println(importStatement);
        }
        for (String staticImport : staticImports) {
            ps.println(staticImport);
        }
        ps.println("");
        ps.print(outStream.toString());
        if (hasMain) {
            ps.print(className + ".main(process.argv.slice(2));");
        }
    }
}
