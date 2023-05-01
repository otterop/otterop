package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.ignore.IgnoreFile;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.FileUtil;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

abstract class AbstractTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private FileWriter fileWriter;
    private ClassReader classReader;
    private String firstClassPart;
    private IgnoreFile ignoreFile;

    AbstractTranspiler(String outFolder, FileWriter fileWriter,
                       ExecutorService executorService, ClassReader classReader) {
        this.outFolder = outFolder;
        this.fileWriter = fileWriter;
        this.executorService = executorService;
        this.classReader = classReader;
        this.ignoreFile = new IgnoreFile(outFolder);
    }

    protected ExecutorService executorService() {
        return executorService;
    }

    protected String outFolder() {
        return outFolder;
    }

    protected FileWriter fileWriter() {
        return fileWriter;
    }

    protected ClassReader classReader() {
        return classReader;
    }

    public String firstClassPart() {
        return firstClassPart;
    }

    protected void setFirstClassPart(String firstClassPart) {
        this.firstClassPart = firstClassPart;
    }

    protected IgnoreFile ignoreFile() {
        return ignoreFile;
    }

    protected String getPath(String codePath) {
        return Paths.get(
                this.outFolder(),
                codePath
        ).toString();
    }

    protected boolean ignored(String relativePath) {
        return ignoreFile().ignores(relativePath);
    }

    public abstract Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext);

    @Override
    public Future<Void> clean(long before) {
        return this.executorService().submit(() -> {
            if (firstClassPart() == null) return null;
            var outFolderPath = Path.of(outFolder());
            var cleanPath = Path.of(outFolder(), firstClassPart());
            Function<File, Boolean> filter = (File file) -> {
                var relativePath = outFolderPath.relativize(file.toPath()).toString();
                var ignored = this.ignored(relativePath);
                if (ignored) {
                    System.out.println("Clean ignored: " + relativePath);
                }
                return !ignored && file.lastModified() < before;
            };
            FileUtil.clean(cleanPath.toString(), filter);
            return null;
        });
    }

    @Override
    public Future<Void> finish() {
        return CompletableFuture.completedFuture(null);
    }
}
