package otterop.transpiler.config;

public class JavaConfig {
    private String outPath = "./src/main/java";
    private String testOutPath = "./src/test/java";

    public String outPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }

    public String testOutPath() {
        return testOutPath;
    }

    public void setTestOutPath(String testOutPath) {
        this.testOutPath = testOutPath;
    }
}
