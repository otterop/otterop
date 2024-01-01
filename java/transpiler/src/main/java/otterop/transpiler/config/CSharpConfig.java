package otterop.transpiler.config;

public class CSharpConfig {
    private String outPath = "./dotnet";

    public String outPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }
}
