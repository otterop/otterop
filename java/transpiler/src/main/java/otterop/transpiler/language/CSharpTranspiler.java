/**
 * Copyright (c) 2023 The OtterOP Authors. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *    * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


package otterop.transpiler.language;

import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.config.OtteropConfig;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.CaseUtil;
import otterop.transpiler.visitor.CSharpParserVisitor;
import otterop.transpiler.visitor.pure.PureCSharpParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CSharpTranspiler extends AbstractTranspiler {

    public CSharpTranspiler(FileWriter fileWriter,
                            ExecutorService executorService, ClassReader classReader, OtteropConfig config) {
        super(config.csharp().outPath(), config.csharp().testOutPath(),
                fileWriter, executorService, classReader, config);
    }

    public String changePackageCase(String part) {
        return CaseUtil.camelCaseToPascalCase(part);
    }

    private String getCodePath(String[] clazzParts, boolean pure) {
        int len = !pure ? clazzParts.length : clazzParts.length + 1;
        String[] newClassParts = Arrays.copyOf(clazzParts, len);
        for(int i = 0; i < clazzParts.length - 1; i++) {
            newClassParts[i] = changePackageCase(clazzParts[i]);
        }

        newClassParts[newClassParts.length - 1] = clazzParts[clazzParts.length - 1]
                .replaceAll("$", ".cs");
        if (pure) {
            newClassParts[newClassParts.length - 2] = "Pure";
        }

        var codePath = String.join(File.separator, newClassParts);
        String ret = replaceBasePath(codePath);
        return ret;
    }

    private void checkMakePure(CSharpParserVisitor visitor,
                               String[] clazzParts,
                               JavaParser.CompilationUnitContext compilationUnitContext,
                               boolean isTest) throws IOException {
        if (visitor.makePure()) {
            var codePath = getCodePath(clazzParts, true);
            var outCodePath = getPath(codePath, isTest);
            PureCSharpParserVisitor pureVisitor = new PureCSharpParserVisitor();
            pureVisitor.visit(compilationUnitContext);
            pureVisitor.printTo(fileWriter().getPrintStream(outCodePath));
        }
    }

    @Override
    public Future<Void> transpile(String[] clazzParts,
                                  Future<JavaParser.CompilationUnitContext> compilationUnitContext,
                                  boolean isTest) {
        return this.executorService().submit(() -> {
            var codePath = getCodePath(clazzParts, false);
            var outCodePath = getPath(codePath, isTest);

            if (ignoreFile().ignores(codePath)) {
                System.out.println("C# ignored: " + codePath);
                return null;
            }

            CSharpParserVisitor visitor = new CSharpParserVisitor();
            visitor.visit(compilationUnitContext.get());
            visitor.printTo(fileWriter().getPrintStream(outCodePath));

            checkMakePure(visitor, clazzParts, compilationUnitContext.get(), isTest);
            return null;
        });
    }
}
