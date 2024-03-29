package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.config.OtteropConfig;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.visitor.JavaParserVisitor;
import otterop.transpiler.visitor.pure.PureJavaParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class JavaTranspiler extends AbstractTranspiler {
    public JavaTranspiler(FileWriter fileWriter,
                          ExecutorService executorService, ClassReader classReader, OtteropConfig config) {
        super(config.java().outPath(), config.java().testOutPath(),
                fileWriter, executorService, classReader, config);
    }

    private String getCodePath(String[] clazzParts, boolean pure) {
        int len = !pure ? clazzParts.length : clazzParts.length + 1;
        String[] newClassParts = Arrays.copyOf(clazzParts, len);
        newClassParts[newClassParts.length - 1] = clazzParts[clazzParts.length - 1]
                .replaceAll("$", ".java");

        if (pure) {
            newClassParts[newClassParts.length - 2] = "pure";
        }
        return String.join(File.separator, newClassParts);
    }

    private void checkMakePure(JavaParserVisitor visitor,
                               String[] clazzParts,
                               JavaParser.CompilationUnitContext compilationUnitContext,
                               boolean isTest) throws IOException {
        if (visitor.makePure()) {
            var codePath = getCodePath(clazzParts, true);
            var outCodePath = getPath(codePath, isTest);
            var pureVisitor = new PureJavaParserVisitor();
            pureVisitor.visit(compilationUnitContext);
            pureVisitor.printTo(fileWriter().getPrintStream(outCodePath));
        }
    }

    @Override
    public Future<Void> transpile(String[] clazzParts,
                                  Future<JavaParser.CompilationUnitContext> compilationUnitContext,
                                  boolean isTest) {
        return this.executorService().submit(() -> {
            var codePath = getCodePath(clazzParts, false);

            if (ignoreFile().ignores(codePath)) {
                System.out.println("Java pure ignored: " + codePath);
                return null;
            }

            JavaParserVisitor visitor = new JavaParserVisitor();
            visitor.visit(compilationUnitContext.get());

            checkMakePure(visitor, clazzParts, compilationUnitContext.get(), isTest);
            return null;
        });
    }

    @Override
    public Future<Void> clean(long before) {
        return CompletableFuture.completedFuture(null);
    }
}
