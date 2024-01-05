package otterop.transpiler.config;

public class CSharpConfig {
    private String outPath = "./dotnet";
    private String testOutPath;

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
