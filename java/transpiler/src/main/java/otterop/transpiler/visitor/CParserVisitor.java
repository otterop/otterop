package otterop.transpiler.visitor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;
import otterop.transpiler.reader.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;
import static otterop.transpiler.util.CaseUtil.isClassName;

public class CParserVisitor extends JavaParserBaseVisitor<Void> {
    private boolean methodStatic = false;
    private boolean methodPublic = false;
    private boolean isMain = false;
    private boolean hasMain = false;
    private boolean insideConstructor = false;
    private boolean insideMethodDefinition = false;
    private boolean insideFormalParameters = false;
    private String className = null;
    private String fullClassName = null;
    private String fullClassNameType = null;
    private int indents = 0;
    private int skipNewlines = 0;
    private static final String INDENT = "    ";
    private static final String THIS = "this";
    private static final String MALLOC = "GC_malloc";
    private String packagePrefix = null;
    private String javaPackagePrefix = null;
    private JavaParser.TypeTypeContext currentType = null;
    private boolean currentTypePointer = false;
    private String currentInstanceName;
    private Set<String> includes = new LinkedHashSet<>();
    private OutputStream outStream = new ByteArrayOutputStream();
    private Set<String> publicMethods = new LinkedHashSet<>();
    private Set<String> staticMethods = new LinkedHashSet<>();
    private List<JavaParser.FieldDeclarationContext> fields = new LinkedList<>();
    private List<JavaParser.MethodDeclarationContext> methods = new LinkedList<>();
    private List<JavaParser.InterfaceCommonBodyDeclarationContext> interfaceMethods = new LinkedList<>();
    private List<JavaParser.GenericMethodDeclarationContext> genericMethods = new LinkedList<>();
    private Map<String, JavaParser.MethodDeclarationContext> methodsMap = new LinkedHashMap<>();
    private Map<String, JavaParser.GenericMethodDeclarationContext> genericMethodsMap = new LinkedHashMap<>();
    private boolean interfaceImplementationThis = false;
    private boolean printParameterNames = true;
    private boolean constructorPublic;
    private JavaParser.ConstructorDeclarationContext constructor;
    private Map<String,String> fullClassNames = new LinkedHashMap<>();
    private Map<String,String> javaFullClassNames = new LinkedHashMap<>();
    private Map<String,String> methodNameFull = new LinkedHashMap<>();
    private Set<String> arrayArgs = new LinkedHashSet<>();
    private Map<String, JavaParser.TypeTypeContext> variableType = new LinkedHashMap<>();
    private PrintStream out = new PrintStream(outStream);
    private boolean header = false;
    private Set<String> currentTypeParameters = new HashSet<>();
    private Set<String> currentMethodTypeParameters = new HashSet<>();
    private JavaParser.TypeParametersContext methodTypeParametersContext;
    private ClassReader classReader;

    public CParserVisitor(ClassReader classReader, boolean header) {
        this.header = header;
        this.classReader = classReader;
    }

    private void detectMethods(ParserRuleContext ctx) {
        List<ParseTree> children = new ArrayList<>(ctx.children);
        boolean methodPublic = false;
        boolean methodStatic = false;
        while (children.size() > 0) {
            boolean skipChildren = false;
            ParseTree child = children.remove(0);
            if (child instanceof JavaParser.FieldDeclarationContext) {
                fields.add((JavaParser.FieldDeclarationContext) child);
            }
            if (child instanceof JavaParser.ClassOrInterfaceModifierContext) {
                JavaParser.ClassOrInterfaceModifierContext modifierChild = (JavaParser.ClassOrInterfaceModifierContext) child;
                if (!methodPublic) methodPublic = modifierChild.PUBLIC() != null;
                if (!methodStatic) methodStatic = modifierChild.STATIC() != null;
            }
            if (child instanceof JavaParser.InterfaceCommonBodyDeclarationContext) {
                JavaParser.InterfaceCommonBodyDeclarationContext declarationChild = (JavaParser.InterfaceCommonBodyDeclarationContext) child;
                interfaceMethods.add(declarationChild);
                var name = declarationChild.identifier().getText();
                publicMethods.add(name);
                methodPublic = false;
                methodStatic = false;
            }
            if (child instanceof JavaParser.MethodDeclarationContext) {
                JavaParser.MethodDeclarationContext declarationChild = (JavaParser.MethodDeclarationContext) child;
                var name = declarationChild.identifier().getText();
                methods.add(declarationChild);
                methodsMap.put(name, declarationChild);
                if (methodPublic) publicMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodStatic = false;
            }
            if (child instanceof JavaParser.ConstructorDeclarationContext) {
                if (methodPublic) constructorPublic = true;
                constructor = (JavaParser.ConstructorDeclarationContext) child;
            }
            if (child instanceof JavaParser.GenericMethodDeclarationContext) {
                JavaParser.GenericMethodDeclarationContext declarationChild = (JavaParser.GenericMethodDeclarationContext) child;
                var name = declarationChild.methodDeclaration().identifier().getText();
                genericMethods.add(declarationChild);
                genericMethodsMap.put(name, declarationChild);
                if (methodPublic) publicMethods.add(name);
                if (methodStatic) staticMethods.add(name);
                methodPublic = false;
                methodStatic = false;
                skipChildren = true;
            }
            if (!skipChildren) {
                for (int i = 0; i < child.getChildCount(); i++) {
                    children.add(child.getChild(i));
                }
            }
        }
    }

    private void visitTypeParameters(JavaParser.TypeParametersContext typeParametersContext, boolean classDeclaration, boolean methodDeclaration) {
        if (typeParametersContext != null) {
            if (methodDeclaration) {
                currentMethodTypeParameters.clear();
            }
            for (var t: typeParametersContext.typeParameter()) {
                if (classDeclaration) currentTypeParameters.add(t.identifier().getText());
                else if (methodDeclaration) {
                    currentMethodTypeParameters.add(t.identifier().getText());
                }
            }
        }
    }

    private void addToIncludes(String fullClassName) {
        var includeStr = String.join("/",
                Arrays.stream(fullClassName.split("_"))
                        .map(identifier -> camelCaseToSnakeCase(identifier))
                        .collect(Collectors.toList()));
        includes.add("#include <" + includeStr + ".h>");
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        detectMethods(ctx);
        out.print(INDENT.repeat(indents));
        visitTypeParameters(ctx.typeParameters(), true, false);
        out.print("typedef struct ");
        className = ctx.identifier().getText();
        fullClassName = packagePrefix + "_" + className;
        fullClassNames.put(className, fullClassName);
        fullClassNameType = fullClassName + "_t";
        out.print(fullClassName);
        out.print("_s");
        if (header) {
            out.print(" ");
        } else {
            out.print(" {\n");
            indents++;
            out.print(INDENT.repeat(indents));
            out.print("void *implementation;\n");
            for (var method: interfaceMethods) {
                out.print(INDENT.repeat(indents));
                visitInterfaceMethodType(method);
                out.print(";\n");
            }
            indents--;
            out.print("} ");
        }
        out.print(fullClassNameType);
        out.println(";\n\n");
        interfaceConstructorImplementation();

        // Pre declare all methods in header
        // and in implementation
        // to avoid declaration order problems
        for(var method : interfaceMethods) {
            insideMethodDefinition = true;
            visitInterfaceCommonBodyDeclaration(method);
            insideMethodDefinition = false;
        }
        if (!header) {
            addToIncludes(fullClassName);
            super.visitInterfaceBody(ctx.interfaceBody());
        }

        return null;
    }

    private void implementInterfaces(List<JavaParser.TypeTypeContext> typeTypes) {
        for (JavaParser.TypeTypeContext typeType : typeTypes) {
            if (!typeType.classOrInterfaceType().isEmpty()) {
                var className = typeType.classOrInterfaceType().getText();
                var javaFullClassName = javaFullClassNames.getOrDefault(className, className);
                if (!javaFullClassName.contains(".")) {
                    javaFullClassName = javaPackagePrefix + "." + javaFullClassName;
                }
                var interfaceFullClassName = Arrays.stream(javaFullClassName.split("\\."))
                        .collect(Collectors.joining("_"));
                if (header) addToIncludes(interfaceFullClassName);

                Class<?> interfaceClass = classReader.getClass(javaFullClassName).orElseThrow();
                var methods = classReader.findMethods(interfaceClass);
                out.print("\n");
                out.print(interfaceFullClassName);
                out.print("_t\n");
                out.print("*");
                out.print(fullClassName + "__to_" + interfaceFullClassName);
                out.print("(");
                out.print(fullClassNameType);
                out.print(" *");
                out.print(THIS);
                out.print(")");
                if (header) {
                    out.print(";\n\n");
                    continue;
                }
                out.print(" {\n");
                indents++;
                out.print(INDENT.repeat(indents));
                out.print("return ");
                out.print(interfaceFullClassName);
                out.print("_new(");
                out.print(THIS);
                indents++;
                for (var method : methods) {
                    out.print(",\n");
                    out.print(INDENT.repeat(indents));
                    out.print("(");
                    if (methodsMap.containsKey(method)) {
                        var methodContext = methodsMap.get(method);
                        visitImplementationMethodType(methodContext);
                    }
                    out.print(") ");
                    out.print(fullClassName);
                    out.print("_");
                    out.print(method);
                }
                indents--;
                out.print(");\n");
                indents--;
                out.print("}\n\n");
            }
        }
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        detectMethods(ctx);
        visitTypeParameters(ctx.typeParameters(), true, false);
        out.print("typedef struct ");
        className = ctx.identifier().getText();
        fullClassName = packagePrefix + "_" + className;
        fullClassNames.put(className, fullClassName);
        fullClassNameType = fullClassName + "_t";

        out.print(fullClassName);
        out.print("_s");
        if (header) {
            out.print(" ");
        } else {
            out.print(" {\n");
            indents++;
            for (var field : fields) {
                out.print(INDENT.repeat(indents));
                visitTypeType(field.typeType());
                out.print(" ");
                visitVariableDeclaratorId(field.variableDeclarators()
                        .variableDeclarator(0).variableDeclaratorId());
                out.print(";\n");
            }
            indents--;
            out.print("} ");
        }
        out.print(fullClassNameType);
        out.println(";\n\n");
        // Pre declare all public methods in header
        // and all private methods in implementation
        // to avoid declaration order problems
        if (constructor != null) {
            insideMethodDefinition = true;
            if (header)
                visitConstructorDeclaration(constructor);
            insideMethodDefinition = false;
        }
        for(var method : methods) {
            insideMethodDefinition = true;
            var methodName = method.identifier().getText();
            methodPublic = publicMethods.contains(methodName);
            methodStatic = staticMethods.contains(methodName);
            if (header && methodPublic || !header && !methodPublic)
                visitMethodDeclaration(method);
            insideMethodDefinition = false;
        }
        for(var method : genericMethods) {
            insideMethodDefinition = true;
            var methodName = method.methodDeclaration().identifier().getText();
            methodPublic = publicMethods.contains(methodName);
            methodStatic = staticMethods.contains(methodName);
            if (header && methodPublic || !header && !methodPublic)
                visitGenericMethodDeclaration(method);
            insideMethodDefinition = false;
        }
        if (!header) {
            addToIncludes(fullClassName);
            super.visitClassBody(ctx.classBody());
        }

        if (ctx.IMPLEMENTS() != null) {
            out.println();
            implementInterfaces(ctx.typeList().get(0).typeType());
        }

        return null;
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        if (header && !constructorPublic)
            return null;
        insideConstructor = true;
        out.print("\n\n");
        out.print(fullClassNameType);
        out.print(" *");
        out.print(fullClassName);
        out.print("_new");
        visitFormalParameters(ctx.formalParameters());
        if (insideMethodDefinition) out.print(";");
        else visitBlock(ctx.block());
        insideConstructor = false;
        this.methodStatic = false;
        this.methodPublic = false;
        return null;
    }

    private void interfaceConstructorImplementation() {
        out.print(fullClassNameType);
        out.print(" *");
        out.print(fullClassName);
        out.print("_new(void *implementation, ");
        boolean rest = false;
        for (var method: interfaceMethods) {
            if (rest) out.print(", ");
            else rest = true;
            visitInterfaceMethodType(method);
        }
        out.print(")");
        if (header) {
            out.print(";\n");
            return;
        }

        out.print(" {\n");
        indents++;
        printMalloc(THIS, fullClassNameType);
        out.print(INDENT.repeat(indents));
        out.print("this->implementation = implementation;\n");
        for (var method: interfaceMethods) {
            out.print(INDENT.repeat(indents));
            out.print("this->");
            var name = method.identifier().getText();
            name = camelCaseToSnakeCase(name);
            out.print(name);
            out.print(" = ");
            out.print(name);
            out.print(";\n");
        }
        out.print(INDENT.repeat(indents));
        out.print("return this;\n");
        indents--;
        out.print("}\n");
    }

    private void interfaceMethodImplementation(JavaParser.InterfaceCommonBodyDeclarationContext ctx,
                                               String methodName) {
        out.print(" {\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("this->");
        out.print(methodName);
        out.print("(this->implementation");
        for(var arg: ctx.formalParameters().formalParameterList().formalParameter()) {
            out.print(", ");
            visitVariableDeclaratorId(arg.variableDeclaratorId());
        }
        out.print(");\n");
        indents--;
        out.print("}");
    }

    private void visitInterfaceMethodType(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        var name = ctx.identifier().getText();
        name = camelCaseToSnakeCase(name);
        out.print("(*");
        out.print(name);
        out.print(")");
        this.interfaceImplementationThis = true;
        this.printParameterNames = false;
        visitFormalParameters(ctx.formalParameters());
        this.printParameterNames = true;
        this.interfaceImplementationThis = false;
    }

    private void visitImplementationMethodType(JavaParser.MethodDeclarationContext ctx) {
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        var name = ctx.identifier().getText();
        name = camelCaseToSnakeCase(name);
        out.print("(*)");
        this.interfaceImplementationThis = true;
        this.printParameterNames = false;
        visitFormalParameters(ctx.formalParameters());
        this.printParameterNames = true;
        this.interfaceImplementationThis = false;
    }

    @Override
    public Void visitInterfaceCommonBodyDeclaration(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        visitTypeParameters(methodTypeParametersContext, false, true);
        currentTypePointer = false;
        variableType.clear();
        arrayArgs.clear();
        out.print("\n\n");
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        if (currentTypePointer) {
            out.print("*");
            currentTypePointer = false;
        }
        var name = ctx.identifier().getText();
        if (!isMain) name = camelCaseToSnakeCase(name);
        out.print(fullClassName + "_" + name);
        visitFormalParameters(ctx.formalParameters());
        if (insideMethodDefinition) out.print(";\n");
        else interfaceMethodImplementation(ctx, name);
        this.methodStatic = false;
        this.methodPublic = false;
        arrayArgs.clear();
        variableType.clear();
        return null;
    }

    @Override
    public Void visitGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        this.methodTypeParametersContext = ctx.typeParameters();
        return visitMethodDeclaration(ctx.methodDeclaration());
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        visitTypeParameters(methodTypeParametersContext, false, true);
        currentTypePointer = false;
        variableType.clear();
        arrayArgs.clear();
        out.print("\n\n");
        if (ctx.typeTypeOrVoid().VOID() != null) out.print("void");
        else visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        if (currentTypePointer) {
            out.print("*");
            currentTypePointer = false;
        }
        var name = ctx.identifier().getText();
        isMain = name.equals("main");
        if (isMain) hasMain = true;
        if (!isMain) name = camelCaseToSnakeCase(name);
        out.print(fullClassName + "_" + name);
        visitFormalParameters(ctx.formalParameters());
        if (insideMethodDefinition) out.print(";\n");
        else visitMethodBody(ctx.methodBody());
        this.methodStatic = false;
        this.methodPublic = false;
        this.isMain = false;
        arrayArgs.clear();
        variableType.clear();
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        insideFormalParameters = true;
        out.print("(");
        var addThis = !methodStatic && !insideConstructor && !interfaceImplementationThis;
        if (addThis) {
            out.print(fullClassNameType);
            out.print(" *");
            if (printParameterNames)
                out.print(THIS);
        }
        if (interfaceImplementationThis) {
            out.print("void *");
            if (printParameterNames)
                out.print(THIS);
        }
        var hasComma = addThis || interfaceImplementationThis;
        if (ctx.formalParameterList() != null) {
            if (hasComma) out.print(", ");
            visitFormalParameterList(ctx.formalParameterList());
        }
        out.print(")");
        insideFormalParameters = false;
        return null;
    }

    @Override
    public Void visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        boolean isLast = ctx.getParent().children.get(ctx.getParent().getChildCount()-1) == ctx;
        var parameterName = ctx.variableDeclaratorId().getText();
        var parameterType = ctx.typeType();
        variableType.put(parameterName, parameterType);
        var declaratorId = ctx.variableDeclaratorId();
        if (parameterType.LBRACK().size() > 0) {
            arrayArgs.add(parameterName);
            out.print("size_t ");
            visitVariableDeclaratorId(ctx.variableDeclaratorId());
            out.print("_cnt, ");
        }
        visitTypeType(parameterType);
        if (printParameterNames) {
            out.print(" ");
            visitVariableDeclaratorId(ctx.variableDeclaratorId());
        } else if (currentTypePointer)
            out.print(" *");
        currentTypePointer = false;
        if (parameterType.LBRACK().size() > 0) {
            out.print("[]");
        }
        currentTypePointer = false;
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

    private void printMalloc(String name, String type) {
        out.print(INDENT.repeat(indents));
        out.print(type);
        out.print(" *");
        out.print(name);
        out.print(" = ");
        out.print(MALLOC);
        includes.add("#include <gc.h>");
        out.print("(sizeof(");
        out.print(type);
        out.print("));\n");
    }

    @Override
    public Void visitBlock(JavaParser.BlockContext ctx) {
        out.print(" {\n");
        indents++;
        var insideConstructorFirst = this.insideConstructor;
        this.insideConstructor = false;
        if (insideConstructorFirst) {
            printMalloc(THIS, fullClassNameType);
        }
        super.visitBlock(ctx);
        if (insideConstructorFirst) {
            out.print(INDENT.repeat(indents));
            out.print("return this;\n");
        }
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
        var variableName = ctx.variableDeclaratorId().getText();
        variableType.put(variableName, currentType);
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        if (ctx.variableInitializer() != null) {
            out.print(" = ");
            var variableInitializerText = ctx.variableInitializer().getText();
            String className = null;
            if (variableType.containsKey(variableInitializerText)) {
                className = variableType.get(variableInitializerText).getText();
            }
            checkInterfaceCreation(className, () -> {
                super.visitVariableInitializer(ctx.variableInitializer());
            });
        }
        return null;
    }

    @Override
    public Void visitVariableDeclaratorId(JavaParser.VariableDeclaratorIdContext ctx) {
        if (currentTypePointer) out.print("*");
        return super.visitVariableDeclaratorId(ctx);
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        return null;
    }

    @Override
    public Void visitMethodCall(JavaParser.MethodCallContext ctx) {
        var first = ctx.getParent().getChild(0) == ctx;
        var methodName = ctx.identifier().getText();
        if (first && methodNameFull.containsKey(methodName))
            methodName = methodNameFull.get(methodName);
        else
            methodName = camelCaseToSnakeCase(methodName);
        out.print(methodName);
        out.print("(");
        if (currentInstanceName != null) {
            out.print(currentInstanceName);
            if (ctx.expressionList() != null) out.print(", ");
            currentInstanceName = null;
        }
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

    private void addClassToFullClassNames(String className) {
        fullClassNames.put(className, packagePrefix + "_" + className);
    }

    @Override
    public Void visitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.LPAREN() != null) {
            out.print("(");
            visitTypeType(ctx.typeType(0));
            if (currentTypePointer) {
                out.print(" *");
                currentTypePointer = false;
            }
            out.print(") ");
            visitExpression(ctx.expression(0));
            return null;
        }
        if (ctx.prefix != null) {
            var prefixText = ctx.prefix.getText();
            out.print(prefixText);
        }
        if (ctx.bop != null) {
            boolean methodCall = false;
            if (ctx.methodCall() != null && ctx.expression(0) != null) {
                var name = ctx.expression(0).getText();
                methodCall = true;
                if (THIS.equals(name) || className.equals(name)) {
                    if (THIS.equals(name)) {
                        currentInstanceName = THIS;
                    }
                    out.print(fullClassName + "_");
                } else if (variableType.containsKey(name)) {
                    currentInstanceName = camelCaseToSnakeCase(name);
                    JavaParser.TypeTypeContext type = variableType.get(name);
                    var typeName = type.primitiveType() != null ? type.getText()
                                          : type.classOrInterfaceType().identifier(0).getText();
                    var fullClassName = fullClassNames.get(typeName);
                    out.print(fullClassName + "_");
                } else if (fullClassNames.containsKey(name)) {
                    var fullClassName = fullClassNames.get(name);
                    out.print(fullClassName + "_");
                } else if (isClassName(name)) {
                    addClassToFullClassNames(name);
                    out.print(fullClassNames.get(name) + "_");
                }
                visitMethodCall(ctx.methodCall());
            }
            if (!methodCall) {
                if (ctx.expression(0) != null) visitExpression(ctx.expression(0));
                var bop = ctx.bop.getText();
                if (!bop.equals(".")) bop = " " + bop + " ";
                else bop = "->";
                out.print(bop);
                if (ctx.expression(1) != null) visitExpression(ctx.expression(1));
                else if (ctx.identifier() != null) visitIdentifier(ctx.identifier());
            }
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
        var fileName = camelCaseToSnakeCase(className);
        var includeStr = String.join("/",
                identifiers.subList(0,classNameIdx ).stream().map(identifier -> identifier.getText())
                        .collect(Collectors.toList())
        );
        var prefix =
                identifiers.subList(0,classNameIdx).stream().map(identifier -> identifier.getText())
                        .collect(Collectors.joining("_"))
         + "_" + className;
        var javaFullClassName = identifiers.stream().map(identifier -> identifier.getText())
                .collect(Collectors.joining("."));

        var includeStatement = "#include <" + includeStr + "/" + fileName + ".h>";
        if (header) includes.add(includeStatement);
        fullClassNames.put(className, prefix);
        javaFullClassNames.put(className, javaFullClassName);
        if (isStatic) {
            var methodName = identifiers.get(classNameIdx + 1).getText();
            var methodNameSnake = camelCaseToSnakeCase(methodName);
            methodNameFull.put(methodName, prefix + "_" + methodNameSnake);
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
        if (ctx.NULL_LITERAL() != null) {
            includes.add("#include <stdlib.h>");
            out.print("NULL");
        }
        else out.print(ctx.getText());
        super.visitLiteral(ctx);
        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        var text = ctx.getText();
        if (fullClassNames.containsKey(text))
            out.print(fullClassNames.get(text));
        else if (methodNameFull.containsKey(text))
            out.print(methodNameFull.get(text));
        else {
            var cName = camelCaseToSnakeCase(text);
            if (!insideFormalParameters && arrayArgs.contains(text)) {
                out.print(cName);
                out.print("_cnt, ");
            }
            out.print(cName);
        }
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
        currentType = ctx.typeType();
        var currentTypeName = currentType.getText();
        currentTypePointer = fullClassNames.containsKey(currentTypeName);
        visitTypeType(currentType);
        out.print(" ");
        visitVariableDeclarators(ctx.variableDeclarators());
        currentType = null;
        currentTypePointer = false;
        return null;
    }

    private void checkInterfaceCreation(String name, Runnable runnable) {
        if (name == null) {
            runnable.run();
            return;
        }

        var fullClassName = fullClassNames.get(name);
        var currentTypeName = currentType != null ? currentType.getText()
                : null;
        boolean interfaceCreation = currentTypeName != null && !name.equals(currentTypeName);
        if (interfaceCreation) {
            out.print(fullClassName);
            out.print("__to_");
            out.print(fullClassNames.get(currentTypeName));
            out.print("(");
        }
        runnable.run();
        if (interfaceCreation) {
            out.print(")");
        }
    }

    @Override
    public Void visitCreator(JavaParser.CreatorContext ctx) {
        var name = ctx.createdName().identifier(0).getText();
        var fullClassName = fullClassNames.get(name);
        checkInterfaceCreation(name, () -> {
            out.print(fullClassName + "_new");
            visitClassCreatorRest(ctx.classCreatorRest());
        });
        return null;
    }

    @Override
    public Void visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        var className = ctx.identifier().stream().map(i -> i.getText())
                    .collect(Collectors.joining("."));
        if (currentTypeParameters.contains(className) ||
            currentMethodTypeParameters.contains(className)) {
            currentTypePointer = true;
            out.print("void");
        } else if (fullClassNames.containsKey(className)){
            currentTypePointer = true;
            out.print(fullClassNames.get(className) + "_t");
        } else if ("Integer".equals(className)) {
            out.print("int");
        } else if ("Object".equals(className)) {
            currentTypePointer = true;
            out.print("void");
        } else if ("java.lang.String".equals(className)) {
            currentTypePointer = true;
            out.print("char");
        } else if ("Boolean".equals(className)) {
            out.print("int");
        }
        return null;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        if ("boolean".equals(ctx.getText()))
            out.print("int");
        else
            out.print(ctx.getText());
        currentTypePointer = false;
        return null;
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        var identifiers = ctx.qualifiedName().identifier();
        packagePrefix = identifiers.stream().map(
                identifier ->
                    camelCaseToSnakeCase(identifier.getText())
        ).collect(Collectors.joining("_"));
        javaPackagePrefix = identifiers.stream().map(
                identifier ->
                        identifier.getText()
        ).collect(Collectors.joining("."));
        return null;
    }

    public void printTo(PrintStream ps) {
        if (header) {
            ps.println("#ifndef __" + fullClassName);
            ps.println("#define __" + fullClassName);
        }
        for (String importStatement : includes) {
            ps.println(INDENT.repeat(indents) + importStatement);
        }
        ps.println("");
        ps.print(outStream.toString());
        if (hasMain) {
            ps.print("\n\nint main(int args_cnt, char *args_arr[])");
            if (!header) {
                ps.print(" {\n");
                ps.print(INDENT + "args_cnt = args_cnt - 1;\n");
                ps.print(INDENT + "char **args = args_arr + 1;\n");
                ps.print(INDENT + fullClassName + "_main(args_cnt, args);\n");
                ps.print("}");
            } else {
                ps.print(";\n");
            }
        }
        if (header) {
            ps.println("#endif");
        }
    }
}
