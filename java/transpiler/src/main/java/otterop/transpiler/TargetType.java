package otterop.transpiler;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum TargetType {
    EXECUTABLE("executable"),
    LIBRARY("library");

    private String name;
    private static Map<String, TargetType> targetTypes = new HashMap<>();

    TargetType(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

    static {
        for (TargetType tn : TargetType.values())
            targetTypes.put(tn.name(), tn);
        targetTypes = Collections.unmodifiableMap(targetTypes);
    }

    public static TargetType fromName(String name) {
        return targetTypes.get(name);
    }
}
