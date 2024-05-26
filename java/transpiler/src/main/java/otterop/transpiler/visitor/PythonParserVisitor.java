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
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.CaseUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;
import static otterop.transpiler.util.CaseUtil.isClassName;

public class PythonParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean insideFieldDeclaration = false;
    private boolean hasMain = false;
    private int indents = 0;
    private int skipNewlines = 0;
    private static String INDENT = "    ";
    private static String THIS = "this";
    private String className;
    private String currentPythonPackage;
    private String javaFullPackageName;
    private Set<String> imports = new LinkedHashSet<>();
    private Set<String> fromImports = new LinkedHashSet<>();
    private Map<String,String> staticImports = new LinkedHashMap<>();
    private final Set<String> importedClasses = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Map<String,String> javaFullClassNames = new LinkedHashMap<>();
    private Map<String, JavaParser.TypeTypeContext> variableType = new LinkedHashMap<>();
    private JavaParser.TypeTypeContext currentType;
    private Set<String> attributePrivate = new LinkedHashSet<>();
    private PrintStream out = new PrintStream(outStream);
    private boolean makePure = false;
    private boolean isTestMethod = false;
    private boolean classPublic = false;
    private List<JavaParser.MethodDeclarationContext> testMethods = new LinkedList<>();
    private ClassReader classReader;

    public PythonParserVisitor(ClassReader classReader) {
        this.classReader = classReader;
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        out.print("class ");
        className = ctx.identifier().getText();
        javaFullClassNames.put(className, javaFullPackageName + "." + className);
        out.print(className);
        out.println(":");
        indents++;
        out.print(INDENT.repeat(indents));
        out.println("pass");
        indents++;
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
        if (ctx.getParent() instanceof JavaParser.TypeDeclarationContext)
            this.classPublic = ctx.PUBLIC() != null;
        super.visitClassOrInterfaceModifier(ctx);
        return null;
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print("class ");
        className = ctx.identifier().getText();
        javaFullClassNames.put(className, javaFullPackageName + "." + className);
        importedClasses.add(className);
        out.print(className);
        if (ctx.EXTENDS() != null) {
            out.print("(");
            checkCurrentPackageImports(ctx.typeType().getText());
            visitTypeType(ctx.typeType());
            out.print(")");
        }
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
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = false;
        this.isTestMethod = false;
        return super.visitClassBodyDeclaration(ctx);
    }

    private boolean isMethodInternal(String variableName, String methodName) {
        var variableType = this.variableType.get(variableName);
        String className = null;
        if (variableType != null && variableType.classOrInterfaceType() != null) {
            className = variableType.classOrInterfaceType().identifier(0).getText();
            checkCurrentPackageImportsAndAdd(className, false);
        } else {
            className = this.className;
        }
        if (className != null) {
            var javaFullClassName = javaFullClassNames.get(className);
            return !this.classReader.isPublicMethod(javaFullClassName, methodName);
        }
        return false;
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (isTestMethod)
            testMethods.add(ctx);
        variableType.clear();
        out.print("\n");
        if (memberStatic) {
            out.print(INDENT.repeat(indents) + "@staticmethod\n    def ");
        } else {
            out.print(INDENT.repeat(indents) + "def ");
        }

        var name = ctx.identifier().getText();
        if (name.equals("iterator")) {
            name = "__iter__";
        } else {
            name = camelCaseToSnakeCase(name);
            if (name.equals("main")) hasMain = true;

            if (!memberPublic || "is".equals(name))
                out.print("_");
        }
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        visitMethodBody(ctx.methodBody());
        variableType.clear();
        memberPublic = false;
        memberStatic = false;
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
        if (ctx.getText().equals("static"))
            memberStatic = true;
        if (ctx.getText().equals("public"))
            memberPublic = true;
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

    private void checkSingleLineStatement(JavaParser.StatementContext ctx) {
        if (ctx.SEMI() != null) {
            indents++;
            out.print(INDENT.repeat(indents));
        }
        visitStatement(ctx);
        if (ctx.SEMI() != null) {
            out.println();
            indents--;
        }
    }

    private void checkElseStatement(JavaParser.StatementContext ctx) {
        if (ctx.ELSE() != null) {
            if (ctx.statement(1).IF() != null) {
                out.print(INDENT.repeat(indents) + "elif ");
                visitParExpression(ctx.statement(1).parExpression());
                out.print(":\n");
                checkSingleLineStatement(ctx.statement(1).statement(0));
                checkElseStatement(ctx.statement(1));
            } else {
                out.print(INDENT.repeat(indents) + "else:\n");
                checkSingleLineStatement(ctx.statement(1));
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
            checkSingleLineStatement(ctx.statement(0));
            checkElseStatement(ctx);
            skipNewlines++;
        } else if (ctx.FOR() != null) {
            var forControl = ctx.forControl();
            if (ctx.forControl().enhancedForControl() != null) {
                out.print("for ");
                var enhancedForControl = forControl.enhancedForControl();
                visitVariableDeclaratorId(enhancedForControl.variableDeclaratorId());
                out.print(" in ");
                visitExpression(enhancedForControl.expression());
                out.print(":\n");
                checkSingleLineStatement(ctx.statement(0));
            } else {
                visitChildren(forControl.forInit());
                out.print("\n");
                out.print(INDENT.repeat(indents));
                out.print("while ");
                if (forControl.expression() != null)
                    visitExpression(forControl.expression());
                else
                    out.print("True");

                out.print(":\n");
                checkSingleLineStatement(ctx.statement(0));
                if (forControl.forUpdate != null) {
                    indents++;
                    out.print(INDENT.repeat(indents));
                    visitChildren(forControl.forUpdate);
                    indents--;
                }
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
        var text = ctx.variableDeclaratorId().getText();
        if (insideFieldDeclaration && !memberPublic) {
            attributePrivate.add(text);
            text = "_" + text;
        }
        variableType.put(text, currentType);
        if (ctx.variableInitializer() != null) {
            visitVariableDeclaratorId(ctx.variableDeclaratorId());
            out.print(" = ");
            super.visitVariableInitializer(ctx.variableInitializer());
        }
        return null;
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        insideFieldDeclaration = true;
        out.print(INDENT.repeat(indents));
        if (memberStatic) {
            visitVariableDeclarators(ctx.variableDeclarators());
            out.print("\n");
        }
        insideFieldDeclaration = false;
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        if (ctx.SUPER() != null) {
            out.print("super().__init__");
        } else {
            var methodName = ctx.identifier().getText();
            var methodInternal = false;
            var staticImport = staticImports.containsKey(methodName);
            var isFirst = ctx.getParent().getChild(0) == ctx;
            var calledOn = ctx.getParent().getChild(0).getText();
            var calledOnClass = !isFirst && CaseUtil.isClassName(calledOn);
            var currentClass = calledOn.equals(className);
            var isThis = calledOn.equals(THIS);
            var hasVariable = variableType.containsKey(calledOn);

            if (calledOnClass) {
                var javaFullClassName = javaFullClassNames.get(calledOn);
                var isPublic = classReader.isPublicMethod(javaFullClassName, methodName);
                if (!isPublic)
                    out.print("_");
            }

            if (isFirst && !staticImport) {
                calledOn = THIS;
                out.print("self.");
            }
            var isLocal = currentClass || isThis || isFirst;
            if (!staticImport && (isLocal || hasVariable)) {
                methodInternal = this.isMethodInternal(calledOn, methodName);
            }
            if (methodInternal)
                out.print("_");
            if (staticImport)
                out.print(staticImports.get(methodName));
            else {
                out.print(camelCaseToSnakeCase(methodName));
            }
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
        checkCurrentPackageImportsAndAdd(name, true);
    }

    private void checkCurrentPackageImportsAndAdd(String name, boolean add) {
        if (CaseUtil.isClassName(name) && !className.equals(name) &&
                !importedClasses.contains(name)) {
            if ("Iterator".equals(name))
                return;

            var javaFullClassName = javaFullPackageName + "." + name;
            javaFullClassNames.put(name, javaFullPackageName + "." + name);
            if (add) {
                var pythonPackage = camelCaseToSnakeCase(name);
                if (!classReader.isPublicClass(javaFullClassName)) {
                    pythonPackage = "_" + pythonPackage;
                }
                var importStatement = "from " + currentPythonPackage + "." + pythonPackage + " import " + name + " as _" + name;
                fromImports.add(importStatement);
                importedClasses.add(name);
            }
        }
    }

    private boolean isPrivateAttribute(String attributeOf, String attribute) {
        return !isClassName(attributeOf) || (attributeOf.equals(className) &&
                attributePrivate.contains(attribute));
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
            checkCurrentPackageImports(name);
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
            if (ctx.expression(1) != null) {
                visitExpression(ctx.expression(1));
            } else if (ctx.methodCall() != null)
                visitMethodCall(ctx.methodCall());
            else if (ctx.identifier() != null) {
                if (bop.equals(".")
                        && isPrivateAttribute(name, ctx.identifier().getText())) {
                    out.print("_");
                }
                visitIdentifier(ctx.identifier());
            }
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

    private boolean excludeImports(String javaFullClassName) {
        return Otterop.WRAPPED_CLASS.equals(javaFullClassName) ||
               Otterop.TEST_ANNOTATION.equals(javaFullClassName);
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
        var javaFullClassName = identifiers.subList(0, classNameIdx + 1).stream()
                .map(identifier -> identifier.getText())
                .collect(Collectors.joining("."));
        this.javaFullClassNames.put(className, javaFullClassName);

        if (this.classReader.isInterface(javaFullClassName))
            return null;

        if (!excludeImports(javaFullClassName)) {
            if (!classReader.isPublicClass(javaFullClassName)) {
                pythonPackage = "_" + pythonPackage;
            }
            var importStatement = "from " + pythonPackage + " import " + className + " as _" + className;
            importedClasses.add(className);
            fromImports.add(importStatement);
            if (isStatic) {
                var methodName = identifiers.get(classNameIdx + 1).getText();
                var methodNameSnake = camelCaseToSnakeCase(methodName);
                if ("is".equals(methodNameSnake))
                    methodNameSnake = "_is";
                staticImports.put(methodName, className + "." + methodNameSnake);
            }
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
        else if (ctx.BOOL_LITERAL() != null)
            out.print(ctx.BOOL_LITERAL().getText().equals("true") ? "True": "False");
        else out.print(ctx.getText());
        super.visitLiteral(ctx);
        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        var ctxText = ctx.getText();
        var skipSnakeCase = importedClasses.contains(ctxText);
        if (skipSnakeCase) out.print("_" + ctxText);
        else out.print(camelCaseToSnakeCase(ctxText));
        return null;
    }

    @Override
    public Void visitPrimary(JavaParser.PrimaryContext ctx) {
        if(ctx.THIS() != null) out.print("self");
        if(ctx.SUPER() != null) out.print("super()");
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
        var className = ctx.createdName().identifier(0).getText();
        checkCurrentPackageImports(className);
        if (className.equals(this.className))
            out.print(className);
        else
            out.print("_" + className);
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
        var identifiers = ctx.qualifiedName().identifier();
        var identifiersString = identifiers.stream().map(
                identifier -> identifier.getText()
        ).collect(Collectors.toList());
        currentPythonPackage = pythonPackage(identifiersString);
        this.javaFullPackageName = identifiersString.stream().collect(Collectors.joining("."));
        return null;
    }

    public boolean makePure() {
        return this.makePure && !testClass();
    }

    public boolean testClass() {
        return !testMethods.isEmpty();
    }

    public boolean classPublic() {
        return this.classPublic;
    }

    public void printTo(PrintStream ps) {
        for (String importStatement : imports) {
            ps.println(importStatement);
        }
        for (String importStatement : fromImports) {
            ps.println(importStatement);
        }
        ps.println("");
        ps.print(outStream.toString());
        if (!testMethods.isEmpty()) {
            for (var testMethod : testMethods) {
                var methodName = camelCaseToSnakeCase(testMethod.identifier().getText());
                ps.print("\ndef test_");
                ps.print(methodName);
                ps.print("():\n");
                indents++;
                ps.print(INDENT.repeat(indents));
                ps.print(className);
                ps.print("().");
                ps.print(methodName);
                ps.print("()\n");
                indents--;
            }
        }
    }
}
