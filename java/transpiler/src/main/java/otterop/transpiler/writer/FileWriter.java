package otterop.transpiler.writer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileWriter {

    private Map<File,File> directories = new ConcurrentHashMap<>();

    public PrintStream getPrintStream(String path) throws IOException {
        File f = new File(path);
        directories.computeIfAbsent(f.getParentFile(), (dirPath) -> {
            if (!dirPath.exists()) dirPath.mkdirs();
            return dirPath;
        });
        return new PrintStream(f);
    }
}
