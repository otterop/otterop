package otterop.transpiler.visitor;

import otterop.transpiler.Otterop;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.antlr.JavaParserBaseVisitor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JavaParserVisitor extends JavaParserBaseVisitor<Void>  {

    private Map<String,String> javaFullClassName = new LinkedHashMap<>();
    private boolean makePure = false;

    @Override
    public Void visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        var qualifiedName = ctx.qualifiedName();
        boolean isStatic = ctx.STATIC() != null;
        var identifiers = qualifiedName.identifier();
        int classNameIdx = identifiers.size() - (isStatic ?  2 : 1);
        String className = identifiers.get(classNameIdx).getText();
        var javaFullClassName = identifiers.subList(0, classNameIdx + 1).stream()
                .map(identifier -> identifier.getText())
                .collect(Collectors.joining("."));

        this.javaFullClassName.put(className, javaFullClassName);
        return null;
    }

    @Override
    public Void visitAnnotation(JavaParser.AnnotationContext ctx) {
        var annotationName = ctx.qualifiedName().identifier().get(0).getText();
        var fullAnnotationName = javaFullClassName.get(annotationName);
        makePure |= Otterop.WRAPPED_CLASS.equals(fullAnnotationName);
        return null;
    }

    public boolean makePure() {
        return this.makePure;
    }
}
