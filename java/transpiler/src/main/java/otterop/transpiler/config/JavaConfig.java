package otterop.transpiler.config;

public class JavaConfig {
    private String outPath = "./src/main/java";

    public String outPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }
}
