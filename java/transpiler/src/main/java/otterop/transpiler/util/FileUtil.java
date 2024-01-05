package otterop.transpiler.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.function.Consumer;
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

    public static void withPrintStream(String file, Consumer<PrintStream> consumer) {
        OutputStream out = null;
        PrintStream ps = null;
        try {
            out = new FileOutputStream(file);
            ps = new PrintStream(out);
            consumer.accept(ps);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (ps != null) {
                ps.close();
            }
        }
    }
}
