package otterop.transpiler.visitor.clazz;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class BaseClassVisitor {
    public void visitMethod(Method m) {
        this.visitReturnType(m.getReturnType());
        this.visitMethodName(m.getName());
        this.visitParameters(m.getParameters());
    }

    public void visitParameters(Parameter[] parameters) {
        for (Parameter p : parameters) {
            this.visitParameter(p);
        }
    }

    public void visitParameter(Parameter p) {
    }

    public void visitMethodName(String name) {
    }

    public void visitReturnType(Class<?> returnType) {
    }
}
