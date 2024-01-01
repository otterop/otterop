package otterop.transpiler.config;

public class PythonConfig {
    private String outPath = "./python";

    public String outPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }
}
