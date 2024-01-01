package otterop.transpiler.config;

public class TypeScriptConfig {
    private String outPath = "./ts";

    public String outPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }
}
