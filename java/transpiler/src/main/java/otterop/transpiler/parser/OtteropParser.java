package otterop.transpiler.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import otterop.transpiler.antlr.JavaLexer;
import otterop.transpiler.antlr.JavaParser;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class OtteropParser {
    private ExecutorService executorService;

    public OtteropParser(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Future<JavaParser.CompilationUnitContext> parse(Future<InputStream> inputStreamFuture) {
        return this.executorService.submit(() -> {
            InputStream in = inputStreamFuture.get();
            Lexer lexer = new JavaLexer(CharStreams.fromStream(in));
            TokenStream tokenStream = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokenStream);
            return parser.compilationUnit();
        });
    }
}
