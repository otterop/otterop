package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;

import java.util.concurrent.Future;

public interface Transpiler {
    Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext);
}
