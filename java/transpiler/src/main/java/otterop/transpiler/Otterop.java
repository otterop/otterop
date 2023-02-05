package otterop.transpiler;

import otterop.transpiler.language.CSharpTranspiler;
import otterop.transpiler.language.CTranspiler;
import otterop.transpiler.language.GoTranspiler;
import otterop.transpiler.language.PythonTranspiler;
import otterop.transpiler.language.Transpiler;
import otterop.transpiler.language.TypeScriptTranspiler;
import otterop.transpiler.parser.OtteropParser;
import otterop.transpiler.reader.FileReader;
import otterop.transpiler.writer.FileWriter;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Otterop {

    private Map<String, Transpiler> transpilers;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private FileReader fileReader = new FileReader(executor);
    private FileWriter fileWriter = new FileWriter();
    private OtteropParser parser = new OtteropParser(executor);

    public Otterop() {
        TypeScriptTranspiler tsTranspiler = new TypeScriptTranspiler(
                "./ts",
                fileWriter,
                executor,
                "example.quicksort");
        CSharpTranspiler csTranspiler = new CSharpTranspiler(
                "./dotnet",
                fileWriter,
                executor);
        CTranspiler cTranspiler = new CTranspiler(
                "./c",
                fileWriter,
                executor);
        PythonTranspiler pythonTranspiler = new PythonTranspiler(
                "./python",
                fileWriter,
                executor);
        GoTranspiler goTranspiler = new GoTranspiler(
                "./go",
                fileWriter,
                executor,
                Map.of("otterop", "github.com/otterop/otterop/go",
                        "example.quicksort", "github.com/otterop/example-quicksort/go/example/quicksort"));

        this.transpilers = Map.of(
                "typescript", tsTranspiler,
                "csharp", csTranspiler,
                "c", cTranspiler,
                "python", pythonTranspiler,
                "go", goTranspiler);
    }

    private String[] pathToClassParts(String path) {
        String[] parts = path.split("/");
        parts[parts.length - 1] = parts[parts.length - 1].split("\\.")[0];
        return parts;
    }

    public void transpile(String basePath) throws InterruptedException, ExecutionException {
        AtomicBoolean complete = new AtomicBoolean(false);
        BlockingQueue<String> classes = fileReader.walkClasses(basePath, complete);
        List<Future<Void>> futures = new LinkedList<>();
        while (!complete.get() || !classes.isEmpty()) {
            String clazz = classes.poll(100, TimeUnit.MILLISECONDS);
            System.out.println(clazz);
            if (clazz != null) {
                futures.add(executor.submit(() ->
                        this.transpile(basePath, clazz).get()
                ));
            }
        }
        for (Future f: futures) f.get();
    }

    public Future<Void> transpile(String basePath, String classFile) {
        return this.transpile(basePath, classFile, null);
    }

    public Future<Void> transpile(String basePath, String classFile, Set<String> languages) {
        Set<String> availableLanguages;
        if (languages == null || languages.isEmpty()) {
            availableLanguages = transpilers.keySet();
        } else {
            availableLanguages = new LinkedHashSet<>(languages);
            availableLanguages.retainAll(transpilers.keySet());
        }

        var clazzParts = pathToClassParts(classFile);
        var in = fileReader.readFile(basePath, classFile);
        var context = parser.parse(in);
        var futures = availableLanguages.stream().map(transpiler ->
                executor.submit(() ->
                        transpilers.get(transpiler).transpile(clazzParts, context).get()
                )
        ).collect(Collectors.toList());
        return executor.submit(() -> {
            for (var future : futures) {
                future.get();
            }
            return null;
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    public static void main(String[] args) throws Exception {
        var otterop = new Otterop();
        otterop.transpile(args[0]);
        otterop.shutdown();
    }
}
