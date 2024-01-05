package otterop.transpiler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import otterop.transpiler.TargetType;
import otterop.transpiler.TranspilerName;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class OtteropConfig {
    private String basePackage;
    private String basePath = "src/main/java";
    private String testBasePath = "src/test/java";
    private GoConfig go = new GoConfig();
    private CConfig c = new CConfig();
    private CSharpConfig csharp = new CSharpConfig();
    private PythonConfig python = new PythonConfig();
    private TypeScriptConfig ts = new TypeScriptConfig();
    private JavaConfig java = new JavaConfig();
    private TargetType targetType;
    private List<TranspilerName> transpilers;
    private ReplaceBasePackage replaceBasePackage;

    public String basePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String basePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String testBasePath() {
        return testBasePath;
    }

    public void setTestBasePath(String testBasePath) {
        this.testBasePath = testBasePath;
    }

    public GoConfig go() {
        return go;
    }

    public void setGo(GoConfig go) {
        this.go = go;
    }

    public CConfig c() {
        return c;
    }

    public void setC(CConfig c) {
        this.c = c;
    }

    public CSharpConfig csharp() {
        return csharp;
    }

    public void setCsharp(CSharpConfig csharp) {
        this.csharp = csharp;
    }

    public PythonConfig python() {
        return python;
    }

    public void setPython(PythonConfig python) {
        this.python = python;
    }

    public TypeScriptConfig ts() {
        return ts;
    }

    public void setTs(TypeScriptConfig ts) {
        this.ts = ts;
    }

    public JavaConfig java() {
        return java;
    }

    public void setJava(JavaConfig java) {
        this.java = java;
    }

    public List<TranspilerName> transpilers() {
        return transpilers;
    }

    public void setTranspilers(List<TranspilerName> transpilers) {
        this.transpilers = transpilers;
    }

    public TargetType targetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public ReplaceBasePackage replaceBasePackage() {
        return replaceBasePackage;
    }

    public void setReplaceBasePackage(ReplaceBasePackage replaceBasePackage) {
        this.replaceBasePackage = replaceBasePackage;
    }

    public static OtteropConfig fromYAML(String path){
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(new File(path), OtteropConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
