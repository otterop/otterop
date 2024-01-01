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
import otterop.transpiler.visitor.TypeScriptParserVisitor;
import otterop.transpiler.visitor.pure.PureTypeScriptParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TypeScriptTranspiler extends AbstractTranspiler {

    private String basePackage;

    public TypeScriptTranspiler(String outFolder, FileWriter fileWriter,
                                ExecutorService executorService,
                                ClassReader classReader,
                                OtteropConfig config) {
        super(outFolder, fileWriter, executorService, classReader, config);
        this.basePackage = config.basePackage();
        this.ignoreFile().addPattern("**.js");
    }

    private String[] pureClazzParts(String[] clazzParts) {
        var pureClazzParts = Arrays.copyOf(clazzParts, clazzParts.length + 1);
        pureClazzParts[pureClazzParts.length - 1] = pureClazzParts[pureClazzParts.length - 2];
        pureClazzParts[pureClazzParts.length - 2] = "pure";
        return pureClazzParts;
    }

    private String getCodePath(String[] clazzParts) {
        clazzParts = Arrays.copyOf(clazzParts, clazzParts.length);
        clazzParts[clazzParts.length - 1] = clazzParts[clazzParts.length - 1]
                .replaceAll("$", ".ts");

        var codePath = String.join(File.separator, clazzParts);
        String ret = replaceBasePath(codePath);
        return ret;
    }

    private String getCurrentPackage(String[] clazzParts) {
        clazzParts = Arrays.copyOf(clazzParts, clazzParts.length - 1);
        return String.join(".", clazzParts);
    }

    private void checkMakePure(TypeScriptParserVisitor visitor,
                               String[] clazzParts,
                               JavaParser.CompilationUnitContext compilationUnitContext) throws IOException {
        if (visitor.makePure()) {
            var pureClazzParts = pureClazzParts(clazzParts);
            var codePath = getCodePath(pureClazzParts);
            var purePackage = getCurrentPackage(pureClazzParts);
            var outCodePath = getPath(codePath);
            PureTypeScriptParserVisitor pureVisitor = new PureTypeScriptParserVisitor(
                    basePackage, purePackage);
            pureVisitor.visit(compilationUnitContext);
            pureVisitor.printTo(fileWriter().getPrintStream(outCodePath));
        }
    }
    @Override
    public Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext) {
        return this.executorService().submit(() -> {
            var codePath = getCodePath(clazzParts);
            String outCodePath = getPath(codePath);
            String currentPackage = getCurrentPackage(clazzParts);

            if (ignoreFile().ignores(codePath)) {
                System.out.println("TypeScript ignored: " + codePath);
                return null;
            }

            TypeScriptParserVisitor visitor = new TypeScriptParserVisitor(basePackage, currentPackage);
            visitor.visit(compilationUnitContext.get());
            visitor.printTo(fileWriter().getPrintStream(outCodePath));
            checkMakePure(visitor, clazzParts, compilationUnitContext.get());
            return null;
        });
    }
}
