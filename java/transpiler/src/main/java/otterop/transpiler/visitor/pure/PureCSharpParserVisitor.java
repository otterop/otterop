package otterop.transpiler.visitor.pure;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static otterop.transpiler.util.CaseUtil.camelCaseToPascalCase;

public class PureCSharpParserVisitor extends JavaParserBaseVisitor<Void> {

    private int indents = 0;
    private String namespace = null;
    private String pureNamespace = null;
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
    private boolean isInterface = false;

    public PureCSharpParserVisitor() {
        this.specialClasses.put("Otterop.Lang.String", "string");
        this.wrapperClassName.put("Otterop.Lang.String", "Otterop.Lang.String");
        this.wrapperClassName.put("Otterop.Lang.OOPIterable", "Otterop.Lang.WrapperOOPIterable");
    }

    @Override
    public Void visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        var identifiers = ctx.qualifiedName().identifier();
        namespace = identifiers.stream().map(
                identifier -> camelCaseToPascalCase(identifier.getText())
        ).collect(Collectors.joining("."));

        pureNamespace = namespace + ".Pure";
        indents++;
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
            return wrapperClassName.get(mappedClass) + ".Wrap(" + argName + ")";
        } else {
            return argName + ".Unwrap()";
        }
    }

    private String unmapArgument(String argName, String originalClass, String mappedClass) {
        if ("object".equals(originalClass)) {
            return argName;
        } else if (wrapperClassName.containsKey(originalClass)) {
            return wrapperClassName.get(originalClass) + ".Unwrap(" + argName + ")";
        } else {
            return mappedClass + ".Wrap(" + argName + ")";
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
                    var mappedArrayName = mapperParamName + "Array";
                    out.print("var ");
                    out.print(mappedArrayName);
                    out.print(" = ");
                    out.print("new ");
                    out.print(mappedClass);
                    out.print("[");
                    out.print(paramName);
                    out.print(".Count()];\n");
                    out.print(INDENT.repeat(indents));
                    out.print("for (var i = 0; i < ");
                    out.print(paramName);
                    out.print(".Count(); i++)\n");
                    out.print(INDENT.repeat(indents));
                    out.print("{\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print(mappedArrayName);
                    out.print("[i] = ");
                    out.print(mapArgument(paramName + "[i]", mappedClass));
                    out.print(";\n");
                    indents--;
                    out.print(INDENT.repeat(indents));
                    out.print("}\n");
                    out.print(INDENT.repeat(indents));
                    out.print("var ");
                    out.print(entry.getValue());
                    out.print(" = Otterop.Lang.Array.Wrap(");
                    out.print(mappedArrayName);
                    out.print(");\n");
                } else if (mappedArgumentIterable.getOrDefault(paramName, false)) {
                    out.print("var ");
                    out.print(mappedArguments.get(paramName));
                    out.print(" = Otterop.Lang.WrapperOOPIterable.Wrap(");
                    out.print(entry.getKey());
                    out.print(", ");
                    if (!"object".equals(mappedClass)) {
                        out.print("(el) => ");
                        out.print(mapArgument("el", mappedClass));
                        out.print(", null");
                    } else {
                        out.print("null, null as object");
                    }
                    out.print(");\n");
                } else {
                    out.print("var ");
                    out.print(mapperParamName);
                    out.print(" = ");
                    out.print(mapArgument(entry.getKey(), mappedClass));
                    out.print(";\n");
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
        out.print(INDENT.repeat(indents));
        out.print("public ");
        if (memberStatic)
            out.print("static ");
        var javaName = ctx.identifier().getText();
        var isOOPString = javaName.equals("OOPString");
        if (isOOPString)
            out.print("new ");
        visitTypeTypeOrVoid(ctx.typeTypeOrVoid());
        out.print(" ");
        var name = camelCaseToPascalCase(javaName);
        if (isOOPString)
            out.print("ToString");
        else
            out.print(name);

        visitFormalParameters(ctx.formalParameters());
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        mapArguments();
        out.print(INDENT.repeat(indents));
        if (hasReturn)
            out.print("var retOtterop = ");
        if (!memberStatic)
            out.print("otterop.");
        else {
            out.print(fullClassNames.get(className));
            out.print(".");
        }
        out.print(name);
        insideMethodCall = true;
        visitFormalParameters(ctx.formalParameters());
        insideMethodCall = false;
        out.print(";\n");
        if (hasReturn) {
            if (returnTypeIterable) {
                out.print(INDENT.repeat(indents));
                out.print("var ret = Otterop.Lang.WrapperOOPIterable.Unwrap(retOtterop, ");
                if (!"Object".equals(returnTypeString)) {
                    out.print("(el) => ");
                    out.print(unmapArgument("el", returnTypeString, returnTypePure));
                    out.print(", null");
                } else {
                    out.print("null, null as object");
                }
                out.print(");\n");
            } else if (returnTypeArray) {
                out.print(INDENT.repeat(indents));
                out.print("var ret = new ");
                out.print(returnTypePure);
                out.print("[retOtterop.Size()];\n");
                out.print(INDENT.repeat(indents));
                out.print("for (var i = 0; i < retOtterop.Size(); i++)\n");
                out.print(INDENT.repeat(indents));
                out.print("{\n");
                indents++;
                out.print(INDENT.repeat(indents));
                out.print("var retI = retOtterop.Get(i);\n");
                if (!memberStatic && returnTypePure.equals(pureClassNames.get(className))) {
                    out.print(INDENT.repeat(indents));
                    out.print("if (retI == this.otterop)\n");
                    out.print(INDENT.repeat(indents));
                    out.print("{\n");
                    indents++;
                    out.print(INDENT.repeat(indents));
                    out.print("ret[i] = this;\n");
                    out.print(INDENT.repeat(indents));
                    out.print("continue;\n");
                    indents--;
                    out.print(INDENT.repeat(indents));
                    out.print("}\n");
                }
                out.print(INDENT.repeat(indents));
                out.print("ret[i] = ");
                out.print(this.unmapArgument("retI", returnTypeString, returnTypePure));
                out.print(";\n");
                indents--;
                out.print(INDENT.repeat(indents));
                out.print("}\n");
            } else {
                if (returnTypePure == null) {
                    out.print(INDENT.repeat(indents));
                    out.print("var ret = retOtterop;\n");
                } else {
                    if (!memberStatic && returnTypePure.equals(pureClassNames.get(className))) {
                        out.print(INDENT.repeat(indents));
                        out.print("if (retOtterop == this.otterop) return this;\n");
                    }
                    out.print(INDENT.repeat(indents));
                    out.print("var ret = ");
                    out.print(
                            this.unmapArgument("retOtterop", returnTypeString, returnTypePure)
                    );
                    out.print(";\n");
                }
            }
            out.print(INDENT.repeat(indents));
            out.print("return ret;\n");
        }
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
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
            out.print("void");
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
        out.print("public ");
        var name = ctx.identifier().getText();
        name = camelCaseToPascalCase(name);
        out.print(name);
        visitFormalParameters(ctx.formalParameters());
        this.memberStatic = false;
        this.memberPublic = false;
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        mapArguments();
        out.print(INDENT.repeat(indents));
        out.print("this.otterop = new ");
        out.print(fullClassNames.get(className));
        insideMethodCall = true;
        visitFormalParameters(ctx.formalParameters());
        insideMethodCall = false;
        out.print(";\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n\n");
        return null;
    }

    @Override
    public Void visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        this.memberStatic = false;
        this.memberPublic = isInterface;
        return super.visitClassBodyDeclaration(ctx);
    }

    @Override
    public Void visitAnnotation(JavaParser.AnnotationContext ctx) {
        return null;
    }

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier();
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        String className = identifiers.get(classNameIdx).getText();
        if (isStatic) {
            return null;
        }
        var csharpIdentifiers = identifiers.subList(0,classNameIdx + 1).stream().map(
                        identifier -> camelCaseToPascalCase(identifier.getText()))
                        .collect(Collectors.toList());
        var classStr = String.join(".",csharpIdentifiers);
        csharpIdentifiers.add(csharpIdentifiers.size() - 1, "Pure");

        var pureClassStr = String.join(".", csharpIdentifiers);

        fullClassNames.put(className, classStr);
        if (specialClasses.containsKey(classStr)) {
            pureClassNames.put(className, specialClasses.get(classStr));
        } else {
            pureClassNames.put(className, pureClassStr);
        }
        return null;
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
    public Void visitIdentifier(JavaParser.IdentifierContext ctx) {
        var text = ctx.getText();
        out.print(text);
        return null;
    }

    private void wrapMethod() {
        var currentPure = pureClassNames.get(className);
        var currentOtterop = fullClassNames.get(className);
        out.print(INDENT.repeat(indents));
        out.print("public static ");
        out.print(currentPure);
        out.print(" Wrap(");
        out.print(currentOtterop);
        out.print(" wrapped)\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("return new ");
        out.print(currentPure);
        out.print("(wrapped);\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    private void unwrapMethod() {
        var currentOtterop = fullClassNames.get(className);
        out.print(INDENT.repeat(indents));
        out.print("public ");
        out.print(currentOtterop);
        out.print(" Unwrap()\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("return this.otterop;\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    private void wrapperConstructor() {
        var currentOtterop = fullClassNames.get(className);
        var currentPure = className;
        out.print(INDENT.repeat(indents));
        out.print("private ");
        out.print(currentPure);
        out.print("(");
        out.print(currentOtterop);
        out.print(" wrapped)\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("this.otterop = wrapped;\n");
        indents--;
        out.print(INDENT.repeat(indents));
        out.print("}\n");
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        out.print(INDENT.repeat(indents));
        out.print("public ");
        out.print("class ");
        className = ctx.identifier().getText();
        var currentFullClassName = namespace + "." + className;
        var currentPureClassName = pureNamespace + "." + className;
        fullClassNames.put(className, currentFullClassName);
        pureClassNames.put(className, currentPureClassName);
        this.visitIdentifier(ctx.identifier());
        if (ctx.IMPLEMENTS() != null) {
            out.print(" : ");
            visitTypeList(ctx.typeList(0));
        }
        out.print("\n");
        out.print(INDENT.repeat(indents));
        out.print("{\n");
        indents++;
        out.print(INDENT.repeat(indents));
        out.print("private ");
        out.print(currentFullClassName);
        out.print(" otterop;\n\n");
        wrapperConstructor();
        super.visitClassBody(ctx.classBody());
        this.unwrapMethod();
        out.println();
        this.wrapMethod();
        indents--;
        out.print(INDENT.repeat(indents));
        out.println("}\n");

        return null;
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        isInterface = true;
        out.print(INDENT.repeat(indents));
        out.print("public ");
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
    public Void visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        var identifier = ctx.identifier().stream().map(i -> i.getText())
                .collect(Collectors.joining("."));
        if ("Object".equals(identifier)) {
            out.print("object");
            if (insideFormalParameters && lastTypeIterable) {
                lastTypeWrapped = "object";
            }
        } else if ("OOPIterable".equals(identifier)) {
            if (insideFormalParameters) {
                lastTypeIterable = true;
            }
            out.print("IEnumerable<");
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
            out.print(">");
        } else if ("Array".equals(identifier)) {
            visitTypeType(ctx.typeArguments().get(0).typeArgument().get(0).typeType());
            out.print("[]");
            if (insideFormalParameters) {
                lastTypeArray = true;
            }
        } else if ("String".equals(identifier)) {
            out.print("string");
            if (insideFormalParameters) {
                lastTypeWrapped = "Otterop.Lang.String";
            }
        } else {
            var pureClass = pureClassNames.get(identifier);
            if (pureClass == null) pureClass = pureNamespace + "." + identifier;
            out.print(pureClass);
            if (insideFormalParameters) {
                lastTypeWrapped = fullClassNames.get(identifier);
            }
        }
        return null;
    }

    @Override
    public Void visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        if (ctx.formalParameterList() == null) {
            out.print("()");
        } else {
            out.print("(");
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
            out.print(" ");
        }
        var parameterName = ctx.variableDeclaratorId().identifier().getText();
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
        String primitiveType = ctx.getText();
        if("boolean".equals(primitiveType)) {
            out.print("bool");
        } else {
            out.print(primitiveType);
        }
        return null;
    }


    public void printTo(PrintStream ps) {
        ps.print("namespace ");
        ps.print(pureNamespace);
        ps.print("\n{\n");
        ps.print(outStream.toString());
        ps.print("}\n");
    }
}
