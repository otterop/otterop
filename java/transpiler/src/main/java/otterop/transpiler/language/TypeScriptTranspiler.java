package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.visitor.TypeScriptParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TypeScriptTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private String basePackage;
    private FileWriter fileWriter;

    public TypeScriptTranspiler(String outFolder, FileWriter fileWriter, ExecutorService executorService,
                                String basePackage) {
        this.outFolder = outFolder;
        this.fileWriter = fileWriter;
        this.basePackage = basePackage;
        this.executorService = executorService;
    }

    private String getCodePath(String[] clazzParts) {
        clazzParts = Arrays.copyOf(clazzParts, clazzParts.length);
        clazzParts[clazzParts.length - 1] = clazzParts[clazzParts.length - 1]
                .replaceAll("$", ".ts");
        return String.join(File.separator, clazzParts);
    }

    @Override
    public Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext) {
        return this.executorService.submit(() -> {
            String outCodePath = Paths.get(
                    this.outFolder,
                    getCodePath(clazzParts)
            ).toString();

            TypeScriptParserVisitor visitor = new TypeScriptParserVisitor(basePackage, basePackage);
            visitor.visit(compilationUnitContext.get());
            visitor.printTo(fileWriter.getPrintStream(outCodePath));
            return null;
        });
    }
}
