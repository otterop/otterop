package otterop.transpiler.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Pattern;

public class ReplaceBasePackage {
    @JsonProperty("package")
    private String pkg;
    private String replacement;
    private Pattern replacementPattern;

    public String pkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String replacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}
