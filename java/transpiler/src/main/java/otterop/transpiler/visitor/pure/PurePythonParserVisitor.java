package otterop.transpiler.visitor.pure;

import otterop.transpiler.Otterop;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;

public class PurePythonParserVisitor extends JavaParserBaseVisitor<Void> {

    private int indents = 0;
    private String module = null;
    private String pureModule = null;
    private static String INDENT = "    ";
    private OutputStream outStream = new ByteArrayOutputStream();
    private PrintStream out = new PrintStream(outStream);
    private String className = null;
    private boolean memberStatic = false;
    private boolean memberPublic = false;
    private boolean printArguments = false;
    private boolean insideFormalParameters = false;
    private String lastTypeWrapped = null;
    private boolean lastTypeArray = false;
    private Map<String,String> mappedArguments = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentArray = new LinkedHashMap<>();
    private Map<String,String> mappedArgumentClass = new LinkedHashMap<>();
    private Map<String,String> fullClassName = new LinkedHashMap<>();
    private Map<String,String> pureClassName = new LinkedHashMap<>();
    private Map<String,String> unwrappedClassName = new LinkedHashMap<>();
    private Set<String> imports = new LinkedHashSet<>();

    public PurePythonParserVisitor() {
        this.unwrappedClassName.put("otterop.lang.string.String", "str");
        this.unwrappedClassName.put("otterop.lang.array.Array", "list");
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        var identifiers = ctx.qualifiedName().identifier();
        var identifiersString = identifiers.stream().map(
                identifier -> camelCaseToSnakeCase(identifier.getText())
        ).collect(Collectors.toList());
        var pureIdentifiersString = new LinkedList<>(identifiersString);
        pureIdentifiersString.add("pure");
        module = identifiersString.stream().collect(Collectors.joining("."));
        pureModule = pureIdentifiersString.stream().collect(Collectors.joining("."));
        return null;
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        return null;
    }

    @Override
    public Void visitModifier(JavaParser.ModifierContext ctx) {
        memberStatic = ctx.getText().equals("static");
        memberPublic = ctx.getText().equals("public");
        super.visitModifier(ctx);
        return null;
    }

    private String mapArgument(String argName, String mappedClass) {
        if (unwrappedClassName.containsKey(mappedClass)) {
            return mappedClass + ".wrap(" + argName + ")";
        } else {
            return argName + ".unwrap()";
        }
    }

    private String unmapArgument(String argName, String mappedClass) {
        if (unwrappedClassName.containsKey(mappedClass)) {
            return argName + ".unwrap()";
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
                if (mappedArgumentArray.getOrDefault(paramName, false)) {
                    var mappedArrayName = mapperParamName + "_array";
                    out.print(mappedArrayName);
                    out.print(" = [None] * len(");
                    out.print(paramName);
                    out.print(")\n");
                    out.print(INDENT.repeat(indents));
                    out.print("for i in range(len(");
                    out.print(paramName);
                    out.print(")):\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print(mappedArrayName);
                    out.print("[i] = ");
                    out.print(mapArgument(paramName + "[i]", mappedClass));
                    out.print("\n");
                    indents--;
                    out.print(INDENT.repeat(indents));
                    out.print(entry.getValue());
                    out.print(" = otterop.lang.array.Array.wrap(");
                    out.print(mappedArrayName);
                    out.print(")\n");
                } else {
                    out.print(paramName);
                    out.print(" = ");
                    out.print(mapArgument(entry.getKey(), mappedClass));
                    out.print("\n");
                }
            }
        }
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (!memberPublic) return null;

        var hasReturn = ctx.typeTypeOrVoid().VOID() == null;
        var returnTypeArray = false;
        String returnTypePure = null;
        if (hasReturn) {
            var returnType = ctx.typeTypeOrVoid().typeType().classOrInterfaceType();
            if (returnType != null) {
                var identifier = returnType.identifier().stream().map(i -> i.getText())
                        .collect(Collectors.joining("."));
                returnTypeArray = returnType.identifier(0).getText().equals("Array");
                if (!returnTypeArray) {
                    returnTypePure = pureClassName.get(identifier);
                } else {
                    var typeArgumentName = returnType.typeArguments().get(0).typeArgument(0).typeType().getText();
                    returnTypePure = pureClassName.get(typeArgumentName);
                }
            }
        }

        mappedArguments.clear();
        mappedArgumentArray.clear();
        mappedArgumentClass.clear();
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("def ");
        var name = ctx.identifier().getText();
        name = camelCaseToSnakeCase(name);
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        out.print(":\n");
        indents++;
        mapArguments();
        out.print(INDENT.repeat(indents));
        if (hasReturn)
            out.print("ret_otterop = ");
        out.print("self.otterop.");
        out.print(name);
        printArguments = true;
        visitFormalParameters(ctx.formalParameters());
        printArguments = false;
        out.print("\n");
        if (hasReturn) {
            if (!returnTypeArray) {
                if (returnTypePure == null) {
                    out.print(INDENT.repeat(indents));
                    out.print("ret = ret_otterop\n");
                } else {
                    if (returnTypePure.equals(pureClassName.get(className))) {
                        out.print(INDENT.repeat(indents));
                        out.print("if ret_otterop is self.otterop:\n");
                        indents++;
                        out.print(INDENT.repeat(indents));
                        out.print("return self\n");
                        indents--;
                    }
                    out.print(INDENT.repeat(indents));
                    out.print("ret = ");
                    out.print(
                            this.unmapArgument("ret_otterop", returnTypePure)
                    );
                    out.print("\n");
                }
            } else {
                out.print(INDENT.repeat(indents));
                out.print("ret = [None] * ret_otterop.size()\n");
                out.print(INDENT.repeat(indents));
                out.print("for i in range(len(ret)):\n");
                indents++;
                out.print(INDENT.repeat(indents));
                out.print("ret_i = ret_otterop.get(i)\n");
                if (returnTypePure.equals(pureClassName.get(className))) {
                    out.print(INDENT.repeat(indents));
                    out.print("if ret_i is self.otterop:\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print("ret[i] = self\n");
                    out.print(INDENT.repeat(indents));
                    out.print("continue\n");
                    indents--;
                }
                out.print(INDENT.repeat(indents));
                out.print("ret[i] = ");
                out.print(this.unmapArgument("ret_i", returnTypePure));
                out.print("\n");
                indents--;
            }
            out.print(INDENT.repeat(indents));
            out.print("return ret\n");
        }
        indents--;
        this.memberStatic = false;
        this.memberPublic = false;
        out.print("\n");
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
            return null;
        }
        return super.visitTypeTypeOrVoid(ctx);
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        if (!memberPublic) {
            return null;
        }
        out.print(INDENT.repeat(indents));
        out.print("def __init__");
        visitFormalParameters(ctx.formalParameters());
        this.memberStatic = false;
        this.memberPublic = false;
        indents++;
        out.print(":\n");
        mapArguments();
        out.print(INDENT.repeat(indents));
        out.print("self.otterop = ");
        out.print(fullClassName.get(className));
        printArguments = true;
        visitFormalParameters(ctx.formalParameters());
        printArguments = false;
        out.print("\n");
        indents--;
        return null;
    }

    @Override
    public Void visitAnnotation(JavaParser.AnnotationContext ctx) {
        return null;
    }

    private boolean excludeImports(String javaFullClassName) {
        return Otterop.WRAPPED_CLASS.equals(javaFullClassName);
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        boolean isStatic = ctx.STATIC() != null;
        if (isStatic)
            return null;

        var qualifiedName = ctx.qualifiedName();
        var identifiers = qualifiedName.identifier();
        int classNameIdx = identifiers.size() - 1;
        String className = identifiers.get(classNameIdx).getText();

        var languageIdentifiers = identifiers.subList(0,classNameIdx + 1).stream().map(
                        identifier -> camelCaseToSnakeCase(identifier.getText()))
                .collect(Collectors.toList());
        var javaClassStr = identifiers.subList(0,classNameIdx + 1).stream().map(
                        identifier -> identifier.getText())
                .collect(Collectors.joining("."));
        var packageString = String.join(".",languageIdentifiers);
        var classStr = packageString + "." + className;

        if (this.excludeImports(javaClassStr))
            return null;

        var pureLanguageIdentifies = new LinkedList<>(languageIdentifiers);
        pureLanguageIdentifies.add(pureLanguageIdentifies.size() - 1, "pure");

        var purePackageStr = String.join(".", pureLanguageIdentifies);
        var pureClassStr = purePackageStr + "." + className;

        fullClassName.put(className, classStr);
        imports.add("import " + packageString);

        if (unwrappedClassName.containsKey(classStr)) {
            pureClassName.put(className, classStr);
        } else {
            pureClassName.put(className, pureClassStr);
            imports.add("import " + purePackageStr);
        }

        return null;
    }

    @Override
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        var text = ctx.getText();
        out.print(text);
        return null;
    }

    private void wrapMethod() {
        var currentPure = pureClassName.get(className);
        var currentOtterop = fullClassName.get(className);
        out.print(INDENT.repeat(indents));
        out.print("@staticmethod\n");
        out.print(INDENT.repeat(indents));
        out.print("def wrap(wrapped):\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("ret = ");
        out.print(currentPure);
        out.print(".__new__(");
        out.print(currentPure);
        out.print(")\n");
        out.print(INDENT.repeat(indents));
        out.print("ret.otterop = wrapped\n");
        out.print(INDENT.repeat(indents));
        out.print("return ret\n");
        indents--;
    }

    private void unwrapMethod() {
        out.print(INDENT.repeat(indents));
        out.print("def unwrap(self):\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("return self.otterop\n");
        indents--;
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        out.print("class ");
        className = ctx.identifier().getText();
        var currentFullPackageName = module + "." + camelCaseToSnakeCase(className);
        var currentPurePackageName = pureModule + "."+ camelCaseToSnakeCase(className);
        var currentFullClassName = currentFullPackageName + "." + className;
        var currentPureClassName = currentPurePackageName + "." + className;
        fullClassName.put(className, currentFullClassName);
        pureClassName.put(className, currentPureClassName);
        imports.add("import " + currentFullPackageName);
        this.visitIdentifier(ctx.identifier());
        out.print(":\n");
        indents++;
        super.visitClassBody(ctx.classBody());
        this.unwrapMethod();
        out.println();
        this.wrapMethod();
        indents--;
        return null;
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        out.print("class ");
        className = ctx.identifier().getText();
        this.visitIdentifier(ctx.identifier());
        out.print(":\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("pass\n");
        indents--;
        return null;
    }

    @Override
    public Void visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        var identifier = ctx.identifier().stream().map(i -> i.getText())
                .collect(Collectors.joining("."));
        if ("Object".equals(identifier)) {
        } else if ("Array".equals(identifier)) {
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
            if (insideFormalParameters) {
                lastTypeArray = true;
            }
        } else if ("String".equals(identifier)) {
            if (insideFormalParameters) {
                lastTypeWrapped = "otterop.lang.string.String";
            }
        } else {
            var pureClass = pureClassName.get(identifier);
            if (pureClass == null) pureClass = pureModule + "." + identifier;
            if (insideFormalParameters) {
                lastTypeWrapped = fullClassName.get(identifier);
            }
        }
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        if (ctx.formalParameterList() == null) {
            if (printArguments) out.print("()");
            else out.print("(self)");
        } else {
            if (printArguments) out.print("(");
            else out.print("(self, ");
            insideFormalParameters = true;
            visitFormalParameterList(ctx.formalParameterList());
            insideFormalParameters = false;
            out.print(")");
        }
        return null;
    }

    @Override
    public Void visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        boolean isLast = ctx.getParent().children.get(ctx.getParent().getChildCount()-1) == ctx;
        lastTypeWrapped = null;
        lastTypeArray = false;
        if (!printArguments) {
            visitTypeType(ctx.typeType());
        }
        var parameterName = ctx.variableDeclaratorId().identifier().getText();
        parameterName = camelCaseToSnakeCase(parameterName);
        if (lastTypeWrapped != null) {
            mappedArguments.put(parameterName, "_" + parameterName);
            mappedArgumentArray.put(parameterName, lastTypeArray);
            mappedArgumentClass.put(parameterName, lastTypeWrapped);
        }
        if (printArguments && mappedArguments.containsKey(parameterName)) {
            out.print(mappedArguments.get(parameterName));
        } else {
            out.print(parameterName);
        }
        if (!isLast) out.print(", ");
        return null;
    }

    @Override
    public Void visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        return null;
    }


    public void printTo(PrintStream ps) {
        for (String importStatement : imports) {
            ps.println(importStatement);
        }
        ps.println("");
        ps.print(outStream.toString());
    }
}
