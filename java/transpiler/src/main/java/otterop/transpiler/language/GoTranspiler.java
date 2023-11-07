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
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.visitor.GoParserVisitor;
import otterop.transpiler.visitor.pure.PureCSharpParserVisitor;
import otterop.transpiler.visitor.pure.PureGoParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class GoTranspiler extends AbstractTranspiler{

    private Map<String,String> importDomainMapping;

    public GoTranspiler(String outFolder,
                        FileWriter fileWriter,
                        ExecutorService executorService,
                        ClassReader classReader,
                        Map<String,String> importDomainMapping) {
        super(outFolder, fileWriter, executorService, classReader);
        this.importDomainMapping = importDomainMapping;
    }

    private String getCodePath(String[] clazzParts, boolean pure) {
        String clazzName = clazzParts[clazzParts.length - 1].toLowerCase();
        int classPartsLen = !pure ? clazzParts.length + 1 : clazzParts.length + 2;
        int startClazzName = !pure ? classPartsLen - 2 : classPartsLen - 3;
        int endClassName = classPartsLen - 1;
        clazzParts = Arrays.copyOf(clazzParts, classPartsLen);
        clazzParts[startClazzName] = clazzName;
        if (pure)
            clazzParts[startClazzName + 1] = "pure";
        clazzParts[endClassName] = clazzName.replaceAll("$", ".go");
        if (firstClassPart() == null) setFirstClassPart(clazzParts[0]);
        return String.join(File.separator, clazzParts);
    }

    private void checkMakePure(GoParserVisitor visitor,
                               String[] clazzParts,
                               JavaParser.CompilationUnitContext compilationUnitContext) throws IOException {
        if (visitor.makePure()) {
            var codePath = getCodePath(clazzParts, true);
            var outCodePath = getPath(codePath);
            PureGoParserVisitor pureVisitor = new PureGoParserVisitor(
                    this.classReader(), this.importDomainMapping);
            pureVisitor.visit(compilationUnitContext);
            pureVisitor.printTo(fileWriter().getPrintStream(outCodePath));
        }
    }

    @Override
    public Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext) {
        return this.executorService().submit(() -> {
            var codePath = getCodePath(clazzParts, false);
            var outCodePath = getPath(codePath);

            if (ignoreFile().ignores(codePath)) {
                System.out.println("Go ignored: " + codePath);
                return null;
            }

            GoParserVisitor visitor = new GoParserVisitor(classReader(), importDomainMapping);
            visitor.visit(compilationUnitContext.get());
            visitor.printTo(fileWriter().getPrintStream(outCodePath));
            checkMakePure(visitor, clazzParts, compilationUnitContext.get());
            return null;
        });
    }
}
