package otterop.transpiler.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CaseUtil {
    private static Pattern p = Pattern.compile("((?<=\\p{Lower})(?=\\p{Upper})|(?<=\\p{Upper})(?=\\p{Upper}\\p{Lower}))");

    public static String toPascalCase(List<String> pieces) {
        return pieces.stream().map(piece -> toFirstUppercase(piece))
                .collect(Collectors.joining());
    }

    public static String toSnakeCase(List<String> pieces) {
        return pieces.stream()
                .map(piece -> piece.toLowerCase())
                .collect(Collectors.joining("_"));
    }

    public static boolean isClassName(String name) {
        var first = name.substring(0,1);
        return !first.toLowerCase().equals(first);
    }

    public static String camelCaseToPascalCase(String in) {
        return toPascalCase(fromCamelCase(in));
    }

    public static String camelCaseToSnakeCase(String in) {
        return toSnakeCase(fromCamelCase(in));
    }

    private static String toFirstUppercase(String in) {
        if (in.length() == 0) return in;
        else return in.substring(0, 1).toUpperCase() + in.substring(1);
    }

    public static List<String> fromCamelCase(String in) {
        return Arrays.asList(p.split(in)).stream()
                .collect(Collectors.toList());
    }
}
