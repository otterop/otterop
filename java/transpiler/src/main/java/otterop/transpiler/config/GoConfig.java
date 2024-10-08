package otterop.transpiler.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GoConfig {
    private static final Map<String,String> DEFAULT_MAPPING = Map.of(
            "otterop", "github.com/otterop/otterop/go"
    );

    private Map<String,String> packageMapping = DEFAULT_MAPPING;
    private String outPath = "./go";

    public Map<String, String> packageMapping() {
        return packageMapping;
    }

    public void setPackageMapping(Map<String, String> packageMapping) {
        this.packageMapping = new LinkedHashMap<>(packageMapping);
        this.packageMapping.putAll(DEFAULT_MAPPING);
    }

    public String outPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }
}
