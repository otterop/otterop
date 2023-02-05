package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.visitor.GoParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class GoTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private FileWriter fileWriter;
    private Map<String,String> importDomainMapping;

    public GoTranspiler(String outFolder,
                        FileWriter fileWriter,
                        ExecutorService executorService,
                        Map<String,String> importDomainMapping) {
        this.outFolder = outFolder;
        this.fileWriter = fileWriter;
        this.executorService = executorService;
        this.importDomainMapping = importDomainMapping;
    }

    private String getCodePath(String[] clazzParts) {
        String clazzName = clazzParts[clazzParts.length - 1].toLowerCase();
        clazzParts = Arrays.copyOf(clazzParts, clazzParts.length + 1);
        clazzParts[clazzParts.length - 2] = clazzName;
        clazzParts[clazzParts.length - 1] = clazzName.replaceAll("$", ".go");
        return String.join(File.separator, clazzParts);
    }

    @Override
    public Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext) {
        return this.executorService.submit(() -> {
            String outCodePath = Paths.get(
                    this.outFolder,
                    getCodePath(clazzParts)
            ).toString();

            GoParserVisitor visitor = new GoParserVisitor(importDomainMapping);
            visitor.visit(compilationUnitContext.get());
            visitor.printTo(fileWriter.getPrintStream(outCodePath));
            return null;
        });
    }
}
