package otterop.transpiler.reader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ClassReader {

    private final ExecutorService executorService;

    public ClassReader(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Future<Class<?>> getClass(String binaryName) {
        return executorService.submit(
                () -> getClass().getClassLoader().loadClass(binaryName));
    }
}
