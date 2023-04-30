package otterop.transpiler.util;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;

public class FileUtil {
    private static void rmdir(File file, Function<File, Boolean> filter) {
        if (!file.isDirectory()) {
            if (filter.apply(file)) {
                file.delete();
            }
        }
        else {
            for (var child : file.listFiles()) {
                rmdir(child, filter);
            }
            if (file.listFiles().length == 0) {
                file.delete();
            }
        }
    }

    public static void clean(String folder, Function<File, Boolean> filter) {
        rmdir(Path.of(folder).toFile(), filter);
    }
}
