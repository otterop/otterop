package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.util.CaseUtil;
import otterop.transpiler.visitor.CSharpParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CSharpTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private FileWriter fileWriter;

    public CSharpTranspiler(String outFolder, FileWriter fileWriter, ExecutorService executorService) {
        this.outFolder = outFolder;
        this.fileWriter = fileWriter;
        this.executorService = executorService;
    }

    private String getCodePath(String[] clazzParts) {
        clazzParts = Arrays.copyOf(clazzParts, clazzParts.length);
        for(int i = 0; i < clazzParts.length - 1; i++) {
            clazzParts[i] = CaseUtil.camelCaseToPascalCase(clazzParts[i]);
        }
        clazzParts[clazzParts.length - 1] = clazzParts[clazzParts.length - 1]
                .replaceAll("$", ".cs");
        return String.join(File.separator, clazzParts);
    }

    @Override
    public Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext) {
        return this.executorService.submit(() -> {
            String outCodePath = Paths.get(
                    this.outFolder,
                    getCodePath(clazzParts)
            ).toString();

            CSharpParserVisitor visitor = new CSharpParserVisitor();
            visitor.visit(compilationUnitContext.get());
            visitor.printTo(fileWriter.getPrintStream(outCodePath));
            return null;
        });
    }
}
