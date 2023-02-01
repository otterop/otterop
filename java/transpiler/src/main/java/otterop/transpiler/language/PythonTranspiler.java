package otterop.transpiler.language;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import otterop.transpiler.antlr.JavaLexer;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.util.CaseUtil;
import otterop.transpiler.visitor.PythonParserVisitor;
import otterop.transpiler.visitor.TypeScriptParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class PythonTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private FileWriter fileWriter;

    public PythonTranspiler(String outFolder, FileWriter fileWriter, ExecutorService executorService) {
        this.outFolder = outFolder;
        this.fileWriter = fileWriter;
        this.executorService = executorService;
    }

    private String getCodePath(String[] clazzParts) {
        clazzParts = Arrays.copyOf(clazzParts, clazzParts.length);
        clazzParts[clazzParts.length - 1] = CaseUtil.camelCaseToSnakeCase(clazzParts[clazzParts.length - 1])
                .replaceAll("$", ".py");
        return String.join(File.separator, clazzParts);
    }

    @Override
    public Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext) {
        return this.executorService.submit(() -> {
            String outCodePath = Paths.get(
                    this.outFolder,
                    getCodePath(clazzParts)
            ).toString();

            PythonParserVisitor visitor = new PythonParserVisitor();
            visitor.visit(compilationUnitContext.get());
            visitor.printTo(fileWriter.getPrintStream(outCodePath));
            return null;
        });
    }
}
