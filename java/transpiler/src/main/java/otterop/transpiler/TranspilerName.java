package otterop.transpiler;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum TranspilerName {
    JAVA("java"),
    C("c"),
    TYPESCRIPT("ts"),
    CSHARP("csharp"),
    GO("go"),
    PYTHON("python");

    private String name;
    private static Map<String,TranspilerName> transpilerNames = new HashMap<>();

    TranspilerName(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

    static {
        for (TranspilerName tn : TranspilerName.values())
            transpilerNames.put(tn.name(), tn);
        transpilerNames = Collections.unmodifiableMap(transpilerNames);
    }

    public static TranspilerName fromName(String name) {
        return transpilerNames.get(name);
    }
}
