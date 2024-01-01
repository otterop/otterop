package otterop.transpiler.config;

public class CConfig {
    private String outPath = "./c";

    public String outPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }
}
