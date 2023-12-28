package otterop.transpiler.visitor.clazz;

import otterop.transpiler.visitor.CParserVisitor;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static otterop.transpiler.util.CaseUtil.camelCaseToSnakeCase;

public class CClassVisitor extends BaseClassVisitor {
    private CParserVisitor parserVisitor;
    private PrintStream out;
    private boolean insideMethodCall = false;

    public CClassVisitor(CParserVisitor parserVisitor) {
        this.parserVisitor = parserVisitor;
        this.out = parserVisitor.getPrintStream();
    }

    @Override
    public void visitMethod(Method m) {
        super.visitMethod(m);
        if (parserVisitor.isHeader()) {
            out.print(";\n");
            return;
        }
        out.print(" {\n");
        parserVisitor.increaseIndents();
        parserVisitor.printIndents();
        if (!m.getReturnType().equals(Void.class))
            out.print("return ");
        parserVisitor.setCurrentTypePointer(false);
        parserVisitor.visitMethodName(m.getName(), true);
        insideMethodCall = true;
        visitParameters(m.getParameters());
        insideMethodCall = false;
        out.print(";\n");
        parserVisitor.decreaseIndents();
        out.print("}\n");
    }

    @Override
    public void visitParameters(Parameter[] parameters) {
        boolean printTypes = true;
        boolean useSuper = false;
        if (insideMethodCall) {
            printTypes = false;
            useSuper = true;
        }
        parserVisitor.visitFormalParametersAnd(printTypes, useSuper, (hasComma) -> {
            if (parameters.length > 0) {
                if (hasComma) out.print(", ");
                Parameter lastParameter = parameters[parameters.length - 1];
                for (Parameter parameter : parameters) {
                    if (!insideMethodCall) {
                        if (!parameter.getType().isPrimitive())
                            parserVisitor.addToIncludes(List.of(parameter.getType().getName().split("\\.")));
                        visitTypeType(parameter.getType());
                        out.print(" ");
                    }
                    parserVisitor.visitVariableDeclaratorId();
                    out.print(camelCaseToSnakeCase(parameter.getName()));
                    if (parameter != lastParameter)
                        out.print(", ");
                }
            }
        });
    }

    @Override
    public void visitParameter(Parameter p) {
        super.visitParameter(p);
    }

    @Override
    public void visitMethodName(String name) {
        parserVisitor.visitMethodName(name, false);
    }

    private void visitTypeType(Class<?> typeType) {
        if (!typeType.isPrimitive()) {
            parserVisitor.visitClassOrInterfaceType(typeType.getSimpleName());
        } else
            parserVisitor.visitPrimitiveType(typeType.getSimpleName().toLowerCase());
    }

    @Override
    public void visitReturnType(Class<?> returnType) {
        visitTypeType(returnType);
    }
}
