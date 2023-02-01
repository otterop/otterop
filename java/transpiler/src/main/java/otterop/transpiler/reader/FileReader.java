package otterop.transpiler.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileReader {

    private ExecutorService executorService;

    public FileReader(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public BlockingQueue<String> walkClasses(String basePath, AtomicBoolean complete) {
        BlockingQueue<String> ret = new LinkedBlockingQueue<>();
        Path base = Path.of(basePath);
        executorService.submit(() -> {
            Files.walkFileTree(base, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        String clazz = base.relativize(file).toString();
                        ret.add(clazz);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            complete.set(true);
            return null;
        });
        return ret;
    }

    public Future<InputStream> readFile(String basePath, String path) {
        return this.executorService.submit(() ->
            new FileInputStream(Paths.get(basePath, path).toString())
        );
    }
}
