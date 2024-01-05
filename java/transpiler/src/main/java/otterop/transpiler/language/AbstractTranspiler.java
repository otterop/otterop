package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.config.OtteropConfig;
import otterop.transpiler.config.ReplaceBasePackage;
import otterop.transpiler.ignore.IgnoreFile;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.FileUtil;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

abstract class AbstractTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private String testOutFolder;
    private FileWriter fileWriter;
    private ClassReader classReader;
    private IgnoreFile ignoreFile;
    private OtteropConfig config;
    private Pattern searchBasePath;
    private String replaceBasePath;
    private Pattern reverseSearchBasePath;
    private String reverseReplaceBasePath;

    AbstractTranspiler(String outFolder,
                       String testOutFolder,
                       FileWriter fileWriter,
                       ExecutorService executorService, ClassReader classReader,
                       OtteropConfig config) {
        this.config = config;
        this.outFolder = outFolder;
        if (testOutFolder == null)
            this.testOutFolder = outFolder;
        else
            this.testOutFolder = testOutFolder;
        this.fileWriter = fileWriter;
        this.executorService = executorService;
        this.classReader = classReader;
        this.ignoreFile = new IgnoreFile(outFolder);

        if (this.config.replaceBasePackage() == null) {
            var replace = new ReplaceBasePackage();
            replace.setPkg(config.basePackage());
            replace.setReplacement("");
            this.config.setReplaceBasePackage(replace);
        }

        String pkg = this.config.replaceBasePackage().pkg();
        String replacement = this.config.replaceBasePackage().replacement();
        String path = Arrays.stream(pkg.split("\\.")).map(part -> changePackageCase(part))
                .collect(Collectors.joining(File.separator)) + File.separator;
        searchBasePath = Pattern.compile("^\\Q" + path + "\\E");
        replaceBasePath = replacement
                .replaceAll("/", File.separator);
        reverseSearchBasePath = Pattern.compile("^\\Q" + replacement + "\\E");
        reverseReplaceBasePath = path;
    }

    public String changePackageCase(String part) {
        return part;
    }

    protected ExecutorService executorService() {
        return executorService;
    }

    protected String outFolder() {
        return outFolder;
    }

    protected String testOutFolder() {
        return testOutFolder;
    }

    protected FileWriter fileWriter() {
        return fileWriter;
    }

    protected ClassReader classReader() {
        return classReader;
    }

    protected IgnoreFile ignoreFile() {
        return ignoreFile;
    }

    protected String getPath(String codePath, boolean isTest) {
        String outFolder;
        if (isTest)
            outFolder = this.testOutFolder();
        else
            outFolder = this.outFolder();
        return Paths.get(
                outFolder,
                codePath
        ).toString();
    }

    protected boolean ignored(String relativePath) {
        return ignoreFile().ignores(relativePath);
    }

    protected boolean alwaysIgnored(String relativePath) {
        return ignoreFile().alwaysIgnores(relativePath);
    }

    public abstract Future<Void> transpile(String[] clazzParts,
                                           Future<JavaParser.CompilationUnitContext> compilationUnitContext,
                                           boolean isTest);

    private void cleanFolder(String folder, long before) {
        var folderPath = Path.of(folder);
        var cleanPath = folderPath;
        Function<File, Boolean> filter = (File file) -> {
            var relativePath = folderPath.relativize(file.toPath()).toString();
            var ignored = this.ignored(relativePath);
            if (ignored && !this.alwaysIgnored(relativePath)) {
                System.out.println("Clean ignored: " + relativePath);
            }
            return !ignored && file.lastModified() < before;
        };
        FileUtil.clean(cleanPath.toString(), filter);
    }

    @Override
    public Future<Void> clean(long before) {
        return this.executorService().submit(() -> {
            cleanFolder(outFolder(), before);
            if (!testOutFolder().equals(outFolder()))
                cleanFolder(testOutFolder(), before);
            return null;
        });
    }

    public String replaceBasePath(String outPath) {
        return searchBasePath.matcher(outPath).replaceAll(replaceBasePath);
    }

    public String reverseReplaceBasePath(String outPath) {
        return reverseSearchBasePath.matcher(outPath).replaceAll(reverseReplaceBasePath);
    }

    @Override
    public Future<Void> finish() {
        return CompletableFuture.completedFuture(null);
    }
}
