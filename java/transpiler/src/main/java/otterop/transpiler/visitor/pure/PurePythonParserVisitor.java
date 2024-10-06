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
    private boolean insideMethodCall = false;
    private boolean insideFormalParameters = false;
    private String lastTypeWrapped = null;
    private boolean lastTypeArray = false;
    private boolean lastTypeIterable = false;
    private Map<String,String> mappedArguments = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentArray = new LinkedHashMap<>();
    private Map<String,Boolean> mappedArgumentIterable = new LinkedHashMap<>();
    private Map<String,String> mappedArgumentClass = new LinkedHashMap<>();
    private Map<String,String> fullClassNames = new LinkedHashMap<>();
    private Map<String,String> pureClassNames = new LinkedHashMap<>();
    private Map<String,String> specialClasses = new LinkedHashMap<>();
    private Map<String,String> wrapperClassName = new LinkedHashMap<>();
    private Map<String,String> fromImportsOOP = new LinkedHashMap<>();
    private Map<String,String> fromImports = new LinkedHashMap<>();
    private Set<String> usedImports = new LinkedHashSet<>();

    public PurePythonParserVisitor() {
        this.specialClasses.put("String", "str");
        this.specialClasses.put("Array", "list");
        this.wrapperClassName.put("_OOPString", "_OOPString");
        this.wrapperClassName.put("_OOPOOPIterable", "_OOPWrapperOOPIterable");
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
        if (ctx.getText().equals("static"))
            memberStatic = true;
        if (ctx.getText().equals("public"))
            memberPublic = true;
        super.visitModifier(ctx);
        return null;
    }

    private String mapArgument(String argName, String mappedClass) {
        if ("object".equals(mappedClass)) {
            return argName;
        } else if (wrapperClassName.containsKey(mappedClass)) {
            return wrapperClassName.get(mappedClass) + ".wrap(" + argName + ")";
        } else {
            return argName + ".unwrap()";
        }
    }

    private String unmapArgument(String argName, String mappedClass) {
        if ("object".equals(mappedClass)) {
            return argName;
        } else if (wrapperClassName.containsKey(mappedClass)) {
            return wrapperClassName.get(mappedClass) + ".unwrap(" + argName + ")";
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
                    out.print(" = _OOPArray.wrap(");
                    out.print(mappedArrayName);
                    out.print(")\n");
                } else if (mappedArgumentIterable.getOrDefault(paramName, false)) {
                    out.print(mappedArguments.get(paramName));
                    fromImports.put("WrapperOOPIterable", "from otterop.lang.wrapper_oop_iterable import WrapperOOPIterable as _OOPWrapperOOPIterable");
                    usedImports.add("WrapperOOPIterable");
                    out.print(" = _OOPWrapperOOPIterable.wrap(");
                    out.print(entry.getKey());
                    out.print(", ");
                    if (!"object".equals(mappedClass)) {
                        out.print("lambda el: ");
                        out.print(mapArgument("el", mappedClass));
                    } else {
                        out.print("None");
                    }
                    out.print(")\n");
                } else {
                    out.print(mapperParamName);
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
        var returnTypeIterable = false;
        String returnTypePure = null;
        String returnTypeString = null;
        if (hasReturn) {
            var returnType = ctx.typeTypeOrVoid().typeType().classOrInterfaceType();
            if (returnType != null) {
                returnTypeString = returnType.identifier().stream().map(i -> i.getText())
                        .collect(Collectors.joining("."));
                returnTypeArray = returnType.identifier(0).getText().equals("Array");
                returnTypeIterable = returnType.identifier(0).getText().equals("OOPIterable");
                if (!returnTypeArray && !returnTypeIterable) {
                    returnTypePure = pureClassNames.get(returnTypeString);
                } else {
                    returnTypeString = returnType.typeArguments().get(0).typeArgument(0).typeType().getText();
                    returnTypePure = pureClassNames.get(returnTypeString);
                }
            }
        }

        if (returnTypeString != null) {
            returnTypeString = fullClassNames.getOrDefault(returnTypeString, returnTypeString);
        }

        mappedArguments.clear();
        mappedArgumentArray.clear();
        mappedArgumentIterable.clear();
        mappedArgumentClass.clear();
        out.print("\n");
        out.print(INDENT.repeat(indents));
        if (memberStatic) {
            out.print("@staticmethod\n");
            out.print(INDENT.repeat(indents));
        }
        out.print("def ");
        var name = ctx.identifier().getText();
        name = camelCaseToSnakeCase(name);
        if (ctx.identifier().getText().equals("OOPString"))
            out.print("__str__");
        else
            out.print(name);
        visitFormalParameters(ctx.formalParameters());
        out.print(":\n");
        indents++;
        mapArguments();
        out.print(INDENT.repeat(indents));
        if (hasReturn)
            out.print("ret_otterop = ");
        if (!memberStatic)
            out.print("self.otterop.");
        else {
            out.print(fullClassNames.get(className));
            out.print(".");
        }
        out.print(name);
        insideMethodCall = true;
        visitFormalParameters(ctx.formalParameters());
        insideMethodCall = false;
        out.print("\n");
        if (hasReturn) {
            if (returnTypeIterable) {
                out.print(INDENT.repeat(indents));
                out.print("ret = _OOPWrapperOOPIterable.unwrap(ret_otterop, ");
                if (!"Object".equals(returnTypeString)) {
                    out.print("lambda el: ");
                    out.print(unmapArgument("el", returnTypeString));
                } else {
                    out.print("None");
                }
                out.print(")\n");
            } else if (returnTypeArray) {
                out.print(INDENT.repeat(indents));
                out.print("ret = [None] * ret_otterop.size()\n");
                out.print(INDENT.repeat(indents));
                out.print("for i in range(len(ret)):\n");
                indents++;
                out.print(INDENT.repeat(indents));
                out.print("ret_i = ret_otterop.get(i)\n");
                if (!memberStatic && returnTypePure.equals(pureClassNames.get(className))) {
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
            } else {
                if (returnTypePure == null) {
                    out.print(INDENT.repeat(indents));
                    out.print("ret = ret_otterop\n");
                } else {
                    if (!memberStatic && returnTypePure.equals(pureClassNames.get(className))) {
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
            }
            out.print(INDENT.repeat(indents));
            out.print("return ret\n");
        }
        indents--;
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
        indents++;
        out.print(":\n");
        mapArguments();
        out.print(INDENT.repeat(indents));
        out.print("self.otterop = ");
        out.print(fullClassNames.get(className));
        insideMethodCall = true;
        visitFormalParameters(ctx.formalParameters());
        insideMethodCall = false;
        out.print("\n");
        indents--;
        return null;
    }

    @Override
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = false;
        return super.visitClassBodyDeclaration(ctx);
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
        var classStr = "_OOP" + className;

        if (this.excludeImports(javaClassStr))
            return null;

        var pureLanguageIdentifies = new LinkedList<>(languageIdentifiers);
        pureLanguageIdentifies.add(pureLanguageIdentifies.size() - 1, "pure");

        var purePackageStr = String.join(".", pureLanguageIdentifies);
        var pureClassStr = "_" + className;

        fullClassNames.put(className, classStr);
        fromImportsOOP.put(className, "from " + packageString + " import " + className + " as _OOP" + className);

        if (specialClasses.containsKey(className)) {
            pureClassNames.put(className, classStr);
        } else {
            pureClassNames.put(className, pureClassStr);
            fromImports.put(className, "from " + purePackageStr + " import " + className + " as _" + className);
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
        var currentPure = pureClassNames.get(className);
        var currentOtterop = fullClassNames.get(className);
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
        var currentFullClassName = "_OOP" + className;
        fromImportsOOP.put(className, "from " + module + "." + camelCaseToSnakeCase(className) + " import " + className + " as " + currentFullClassName);
        fullClassNames.put(className, currentFullClassName);
        pureClassNames.put(className, className);
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
        var checkImport = true;

        if ("Object".equals(identifier)) {
            if (insideFormalParameters && lastTypeIterable) {
                lastTypeWrapped = "object";
            }
        } else if ("OOPIterable".equals(identifier)) {
            if (insideFormalParameters) {
                lastTypeIterable = true;
            }
            checkImport = false;
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
        } else if ("Array".equals(identifier)) {
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
            if (insideFormalParameters) {
                lastTypeArray = true;
            }
        } else if ("String".equals(identifier)) {
            if (insideFormalParameters) {
                lastTypeWrapped = "_OOPString";
            }
        } else {
            var pureClass = pureClassNames.get(identifier);
            if (pureClass == null) pureClass = pureModule + "." + identifier;
            if (insideFormalParameters) {
                lastTypeWrapped = fullClassNames.get(identifier);
            }
        }
        if (checkImport)
            usedImports.add(identifier);
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        if (ctx.formalParameterList() == null) {
            if (insideMethodCall || memberStatic) out.print("()");
            else out.print("(self)");
        } else {
            if (insideMethodCall || memberStatic) out.print("(");
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
        lastTypeIterable = false;
        if (!insideMethodCall) {
            visitTypeType(ctx.typeType());
        }
        var parameterName = ctx.variableDeclaratorId().identifier().getText();
        parameterName = camelCaseToSnakeCase(parameterName);
        if (lastTypeWrapped != null) {
            mappedArguments.put(parameterName, "_" + parameterName);
            mappedArgumentArray.put(parameterName, lastTypeArray);
            mappedArgumentIterable.put(parameterName, lastTypeIterable);
            mappedArgumentClass.put(parameterName, lastTypeWrapped);
        }
        if (insideMethodCall && mappedArguments.containsKey(parameterName)) {
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
        for (Map.Entry<String,String> importStatementEntry : fromImportsOOP.entrySet()) {
            if (usedImports.contains(importStatementEntry.getKey()))
                ps.println(importStatementEntry.getValue());
        }
        for (Map.Entry<String,String> importStatementEntry : fromImports.entrySet()) {
            if (usedImports.contains(importStatementEntry.getKey()))
                ps.println(importStatementEntry.getValue());
        }
        ps.println("");
        ps.print(outStream.toString());
    }
}
