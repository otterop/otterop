package otterop.transpiler.ignore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class IgnoreFile {
    private File ignoreFile;
    private boolean ignoreFileExists;
    private List<Pattern> patterns;

    public IgnoreFile(String basePath) {
        this.ignoreFile = Path.of(basePath, ".oopignore").toFile();
        this.ignoreFileExists = ignoreFile.exists();
    }

    private synchronized List<Pattern> getPatterns() {
        if (patterns!=null) return patterns;
        try {
            Pattern stars = Pattern.compile("\\*\\*|\\*");
            var patterns = new ArrayList<Pattern>();
            List<String> lines = Files.readAllLines(ignoreFile.toPath());
            lines.forEach(line -> {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') return;
                var starsMatcher = stars.matcher(line);
                StringBuilder sb = new StringBuilder();
                sb.append("\\Q");
                int i = 0;
                while (starsMatcher.find()) {
                    sb.append(line, i, starsMatcher.start());
                    var group = starsMatcher.group();
                    sb.append("\\E");
                    if ("**".equals(group)) sb.append(".*?");
                    else sb.append("[^/]*?");
                    i = starsMatcher.end();
                    if (i < line.length()) sb.append("\\Q");
                }
                if (i < line.length()) {
                    sb.append(line, i, line.length());
                    sb.append("\\E");
                }
                patterns.add(Pattern.compile(sb.toString()));
            });
            this.patterns = patterns;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return patterns;
    }

    public boolean ignores(String relativePath) {
        if (!this.ignoreFileExists) return false;
        return getPatterns().parallelStream()
                .map(p -> p.matcher(relativePath).matches())
                .anyMatch(c -> c);
    }
}
