package otterop.transpiler.language;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import otterop.transpiler.antlr.JavaLexer;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.util.CaseUtil;
import otterop.transpiler.visitor.CParserVisitor;
import otterop.transpiler.visitor.TypeScriptParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private FileWriter fileWriter;

    private enum FileType {
        SOURCE,
        HEADER,
        DEPS
    }

    public CTranspiler(String outFolder, FileWriter fileWriter, ExecutorService executorService) {
        this.outFolder = outFolder;
        this.fileWriter = fileWriter;
        this.executorService = executorService;
    }

    private String getCodePath(String[] clazzParts, FileType fileType) {
        String replacement;
        switch (fileType) {
            case SOURCE:
                replacement = ".c";
                break;
            case HEADER:
                replacement = ".h";
                break;
            case DEPS:
                replacement = ".mk";
            default:
                throw new IllegalArgumentException();
        }
        clazzParts = Arrays.copyOf(clazzParts, clazzParts.length);
        clazzParts[clazzParts.length - 1] = clazzParts[clazzParts.length - 1]
                .toLowerCase()
                .replaceAll("$", replacement);
        return String.join(File.separator, clazzParts);
    }

    @Override
    public Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext) {
        return this.executorService.submit(() -> {
            String sourceCodePath = Paths.get(
                    this.outFolder,
                    getCodePath(clazzParts,FileType.SOURCE)
            ).toString();
            String headerCodePath = Paths.get(
                    this.outFolder,
                    getCodePath(clazzParts,FileType.HEADER)
            ).toString();

            CParserVisitor sourceVisitor = new CParserVisitor(false);
            CParserVisitor headerVisitor = new CParserVisitor(true);
            sourceVisitor.visit(compilationUnitContext.get());
            sourceVisitor.printTo(fileWriter.getPrintStream(sourceCodePath));
            headerVisitor.visit(compilationUnitContext.get());
            headerVisitor.printTo(fileWriter.getPrintStream(headerCodePath));
            return null;
        });
    }
}
