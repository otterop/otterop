package otterop.transpiler;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import otterop.transpiler.antlr.JavaLexer;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.visitor.GoParserVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class GoTranspiler {

        public static void main(String[] args) throws IOException {
            try {
                /*
                 * get the input file as an InputStream
                 */
                InputStream inputStream = System.in;
                /*
                 * make Lexer
                 */
                Lexer lexer = new JavaLexer(CharStreams.fromStream(inputStream));
                /*
                 * get a TokenStream on the Lexer
                 */
                TokenStream tokenStream = new CommonTokenStream(lexer);
                /*
                 * make a Parser on the token stream
                 */
                JavaParser parser = new JavaParser(tokenStream);
                JavaParser.CompilationUnitContext cu = parser.compilationUnit();
                var visitor = new GoParserVisitor(new HashMap<>(){{
                    put("common", "emasab.com");
                }});
                visitor.visit(cu);
                visitor.printTo(System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
