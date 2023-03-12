package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.CaseUtil;
import otterop.transpiler.visitor.CParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private FileWriter fileWriter;
    private final ClassReader classReader;


    private enum FileType {
        SOURCE,
        HEADER,
        DEPS
    }

    public CTranspiler(String outFolder, FileWriter fileWriter,
                       ExecutorService executorService, ClassReader classReader) {
        this.outFolder = outFolder;
        this.fileWriter = fileWriter;
        this.executorService = executorService;
        this.classReader = classReader;
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
        clazzParts[clazzParts.length - 1] = CaseUtil.camelCaseToSnakeCase(clazzParts[clazzParts.length - 1])
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

            CParserVisitor sourceVisitor = new CParserVisitor(classReader, false);
            CParserVisitor headerVisitor = new CParserVisitor(classReader, true);
            sourceVisitor.visit(compilationUnitContext.get());
            sourceVisitor.printTo(fileWriter.getPrintStream(sourceCodePath));
            headerVisitor.visit(compilationUnitContext.get());
            headerVisitor.printTo(fileWriter.getPrintStream(headerCodePath));
            return null;
        });
    }
}
