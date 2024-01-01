package otterop.transpiler.ignore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class IgnoreFile {
    private File ignoreFile;
    private boolean ignoreFileExists;
    private List<Pattern> patterns;
    private List<Pattern> alwaysIgnorePatterns;
    private List<Pattern> antiPatterns;
    private static final String OOPIGNORE = ".oopignore";
    private static final Pattern stars = Pattern.compile("\\*\\*|\\*");

    public IgnoreFile(String basePath) {
        this.alwaysIgnorePatterns = new LinkedList<>();
        this.ignoreFile = Path.of(basePath, OOPIGNORE).toFile();
        this.ignoreFileExists = ignoreFile.exists();
        this.addPattern(OOPIGNORE);
    }

    private Pattern getPattern(String line) {
        var starsMatcher = stars.matcher(line);
        StringBuilder sb = new StringBuilder();
        sb.append("^\\Q");
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
        sb.append("$");
        return Pattern.compile(sb.toString());
    }

    private void initPatterns() {
        this.patterns = new ArrayList<>();;
        this.antiPatterns = new ArrayList<>();;
        try {
            var patterns = new ArrayList<Pattern>();
            var antiPatterns = new ArrayList<Pattern>();
            List<String> lines = Files.readAllLines(ignoreFile.toPath());
            lines.forEach(line -> {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') return;
                boolean antiPattern = line.startsWith("!");
                if (antiPattern) {
                    line = line.substring(1);
                }
                Pattern p = getPattern(line);
                if (antiPattern)
                    antiPatterns.add(p);
                else
                    patterns.add(p);

            });
            this.patterns = patterns;
            this.antiPatterns = antiPatterns;
        } catch (IOException e) {
            // ignore
        }
    }

    public void addPattern(String pattern) {
        Pattern p = getPattern(pattern);
        this.getPatterns().add(p);
        alwaysIgnorePatterns.add(p);
    }

    private synchronized List<Pattern> getAntiPatterns() {
        if (antiPatterns!=null) return antiPatterns;
        initPatterns();
        return antiPatterns;
    }

    private synchronized List<Pattern> getPatterns() {
        if (patterns!=null) return patterns;
        initPatterns();
        return patterns;
    }

    private boolean matchesPatterns(Collection<Pattern> patterns, String path) {
        return patterns.parallelStream()
            .anyMatch(p -> p.matcher(path).matches());
    }

    public boolean alwaysIgnores(String relativePath) {
        return matchesPatterns(alwaysIgnorePatterns, relativePath);
    }

    public boolean ignores(String relativePath) {
        if (!this.ignoreFileExists)
            return false;
        if (alwaysIgnores(relativePath))
            return true;
        boolean ret = matchesPatterns(getPatterns(), relativePath);
        return ret && !matchesPatterns(getAntiPatterns(), relativePath);
    }
}
