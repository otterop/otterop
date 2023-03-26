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
 *    * Neither the name of Confluent Inc. nor the names of its
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
import otterop.transpiler.util.CaseUtil;
import otterop.transpiler.visitor.CParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CTranspiler implements Transpiler {

    private ExecutorService executorService;
    private String outFolder;
    private FileWriter fileWriter;
    private final ClassReader classReader;
    private List<String[]> sources = Collections.synchronizedList(new LinkedList<>());

    private enum FileType {
        SOURCE,
        HEADER,
        CMAKELISTS
    }

    public CTranspiler(String outFolder, FileWriter fileWriter,
                       ExecutorService executorService, ClassReader classReader) {
        this.outFolder = outFolder;
        this.fileWriter = fileWriter;
        this.executorService = executorService;
        this.classReader = classReader;
    }

    private String getCodePath(String[] clazzParts, FileType fileType) {
        String replacement;
        switch (fileType) {
            case SOURCE:
                replacement = ".c";
                break;
            case HEADER:
                replacement = ".h";
                break;
            case CMAKELISTS:
                replacement = "/CMakeLists.txt";
                break;
            default:
                throw new IllegalArgumentException();
        }
        clazzParts = Arrays.copyOf(clazzParts, clazzParts.length);
        clazzParts[clazzParts.length - 1] = CaseUtil.camelCaseToSnakeCase(clazzParts[clazzParts.length - 1])
                .replaceAll("$", replacement);
        return String.join(File.separator, clazzParts);
    }

    private String getPath(String[] clazzParts, FileType type) {
        return Paths.get(
                this.outFolder,
                getCodePath(clazzParts, type)
        ).toString();
    }

    public void writeCMakeLists(String[] packageParts) {
        var cmakeListsFile = getPath(packageParts, FileType.CMAKELISTS);
        OutputStream out = null;
        PrintStream ps = null;
        sources.sort(Comparator.comparing(a -> String.join(".", a)));
        try {
            out = new FileOutputStream(cmakeListsFile);
            ps = new PrintStream(out);
            var execName = String.join("_", packageParts);

            ps.print("add_executable(");
            ps.print(execName);
            ps.print("\n");
            for (var s : sources) {
                var sourceCodePath = getCodePath(s, FileType.SOURCE);
                ps.println(sourceCodePath);
            }
            ps.println(")");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (ps != null) {
                ps.close();
            }
        }
    }

    @Override
    public Future<Void> transpile(String[] clazzParts, Future<JavaParser.CompilationUnitContext> compilationUnitContext) {
        sources.add(clazzParts);
        return this.executorService.submit(() -> {
            String sourceCodePath = getPath(clazzParts, FileType.SOURCE);
            String headerCodePath = getPath(clazzParts, FileType.HEADER);

            CParserVisitor sourceVisitor = new CParserVisitor(classReader, false);
            CParserVisitor headerVisitor = new CParserVisitor(classReader, true);
            sourceVisitor.visit(compilationUnitContext.get());
            sourceVisitor.printTo(fileWriter.getPrintStream(sourceCodePath));
            headerVisitor.visit(compilationUnitContext.get());
            headerVisitor.printTo(fileWriter.getPrintStream(headerCodePath));
            return null;
        });
    }
}
