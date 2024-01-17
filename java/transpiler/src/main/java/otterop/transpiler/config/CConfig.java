package otterop.transpiler.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CConfig {
    private String outPath = "./c";
    private String testOutPath;
    private static final Map<String,String> DEFAULT_MAPPING = Collections.emptyMap();
    private Map<String,String> packageMapping = DEFAULT_MAPPING;

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

    public Map<String, String> packageMapping() {
        return packageMapping;
    }

    public void setPackageMapping(Map<String, String> packageMapping) {
        this.packageMapping = new HashMap<>(DEFAULT_MAPPING);
        this.packageMapping.putAll(packageMapping);
    }
}
