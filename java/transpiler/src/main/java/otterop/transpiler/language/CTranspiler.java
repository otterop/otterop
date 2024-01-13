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

import otterop.transpiler.TargetType;
import otterop.transpiler.antlr.JavaParser;
import otterop.transpiler.config.OtteropConfig;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.util.CaseUtil;
import otterop.transpiler.visitor.CParserVisitor;
import otterop.transpiler.visitor.pure.PureCParserVisitor;
import otterop.transpiler.writer.FileWriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import static otterop.transpiler.util.FileUtil.withPrintStream;

public class CTranspiler extends AbstractTranspiler {

    private String mainClass;
    private String testMainClass;
    private List<String> sources = Collections.synchronizedList(new LinkedList<>());
    private List<String> testSources = Collections.synchronizedList(new LinkedList<>());
    private List<String> testClasses = Collections.synchronizedList(new LinkedList<>());
    private String[] packageParts;
    private TargetType targetType;

    private enum FileType {
        SOURCE,
        HEADER,
        CMAKELISTS
    }

    public CTranspiler(FileWriter fileWriter,
                       ExecutorService executorService, ClassReader classReader,
                       OtteropConfig config) {
        super(config.c().outPath(),
                config.c().testOutPath(),
                fileWriter, executorService, classReader, config);
        packageParts = config.basePackage().split("\\.");
        this.targetType = config.targetType();
        this.ignoreFile().addPattern("CMakeLists.manual.txt");
    }

    private String getCodePath(String[] clazzParts, FileType fileType, boolean pure) {
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

        String codePath;
        if (!pure)
            codePath = String.join(File.separator, clazzParts);
        else {
            String[] clazzPartsPure = Arrays.copyOf(clazzParts, clazzParts.length + 1);
            clazzPartsPure[clazzPartsPure.length - 1] = clazzPartsPure[clazzPartsPure.length - 2];
            clazzPartsPure[clazzPartsPure.length - 2] = "pure";
            codePath = String.join(File.separator, clazzPartsPure);
        }
        String ret = replaceBasePath(codePath);
        return ret;
    }

    public void writeCMakeLists(boolean isTest) {
        if (isTest && (this.testOutFolder().equals(outFolder()) || !new File(this.testOutFolder()).exists()))
            return;

        var cmakeListsCodePath = getCodePath(packageParts, FileType.CMAKELISTS, false);
        var cmakeListsFile = getPath(cmakeListsCodePath, isTest);
        String cmakeListsManualFile;
        if (isTest)
            cmakeListsManualFile = cmakeListsCodePath.replaceAll("\\.txt$", ".manual.tests.txt");
        else
            cmakeListsManualFile = cmakeListsCodePath.replaceAll("\\.txt$", ".manual.txt");

        var sources = isTest ? this.testSources : this.sources;
        Collections.sort(sources);
        withPrintStream(cmakeListsFile, (ps) -> {
            var targetName = String.join("_", packageParts);

            boolean isLibrary = targetType == TargetType.LIBRARY;
            if (!isLibrary) {
                ps.print("add_executable(");
                ps.print(targetName);
                if (isTest)
                    ps.print("_tests");
                ps.println();
                for (var sourceCodePath : sources) {
                    ps.print("${CMAKE_CURRENT_LIST_DIR}/");
                    ps.println(sourceCodePath);
                }
                if (!isTest && mainClass != null) {
                    ps.print("${CMAKE_CURRENT_LIST_DIR}/");
                    ps.println(mainClass);
                }
                else if (isTest && testMainClass != null) {
                    ps.print("${CMAKE_CURRENT_LIST_DIR}/");
                    ps.println(testMainClass);
                }
                ps.println(")");
            }
            if (!isTest) {
                ps.print("add_library(");
                ps.print(targetName);
                if (!isLibrary)
                    ps.print("__lib");
                ps.print(" STATIC\n");
                for (var sourceCodePath : sources) {
                    ps.print("${CMAKE_CURRENT_LIST_DIR}/");
                    ps.println(sourceCodePath);
                }
                ps.println(")");
            }
            ps.println("include(${CMAKE_CURRENT_LIST_DIR}/" + cmakeListsManualFile + " OPTIONAL)");
        });
    }

    public void writeTestsMain() {
        if (this.testOutFolder().equals(outFolder()) || !new File(this.testOutFolder()).exists())
            return;

        testMainClass = "__tests_main.c";

        Collections.sort(this.testClasses);
        withPrintStream(getPath(testMainClass, true), (ps) -> {
            ps.print("#include \"unity_fixture.h\"\n\n");
            ps.print("static void __run_all_tests(void) {\n");
            for (var testClass : this.testClasses) {
                ps.print("    RUN_TEST_GROUP(");
                ps.print(testClass);
                ps.print(");\n");
            }
            ps.print("}\n");
            ps.print("\nint main(int argc, const char *argv[]) {\n");
            ps.print("    return UnityMain(argc, argv, __run_all_tests);\n");
            ps.print("}\n");
        });
    }

    private void addToSources(String path, boolean isTest) {
        if (isTest)
            testSources.add(path);
        else
            sources.add(path);
    }

    private void checkMakePure(CParserVisitor visitor,
                               String[] clazzParts,
                               JavaParser.CompilationUnitContext compilationUnitContext,
                               boolean isTest) throws IOException {
        if (visitor.makePure()) {
            var sourceCodePath = getCodePath(clazzParts,  FileType.SOURCE, true);
            var headerCodePath = getCodePath(clazzParts,  FileType.HEADER, true);
            String sourcePath = getPath(sourceCodePath, isTest);
            String headerPath = getPath(headerCodePath, isTest);
            PureCParserVisitor headerVisitor = new PureCParserVisitor(classReader(), true, null);
            headerVisitor.visit(compilationUnitContext);
            headerVisitor.printTo(fileWriter().getPrintStream(headerPath));
            PureCParserVisitor sourceVisitor = new PureCParserVisitor(classReader(), false, headerVisitor);
            sourceVisitor.visit(compilationUnitContext);
            sourceVisitor.printTo(fileWriter().getPrintStream(sourcePath));
            addToSources(sourceCodePath, isTest);
        }
    }

    @Override
    public Future<Void> transpile(String[] clazzParts,
                                  Future<JavaParser.CompilationUnitContext> compilationUnitContext,
                                  boolean isTest) {
        return this.executorService().submit(() -> {
            var sourceCodePath = getCodePath(clazzParts, FileType.SOURCE, false);
            var headerCodePath = getCodePath(clazzParts, FileType.HEADER, false);
            String sourcePath = getPath(sourceCodePath, isTest);
            String headerPath = getPath(headerCodePath, isTest);

            if (ignoreFile().ignores(sourceCodePath)) {
                System.out.println("C ignored: " + sourceCodePath);
                return null;
            }

            CParserVisitor headerVisitor = new CParserVisitor(classReader(), true, null);
            headerVisitor.visit(compilationUnitContext.get());
            headerVisitor.printTo(fileWriter().getPrintStream(headerPath));
            CParserVisitor sourceVisitor = new CParserVisitor(classReader(), false, headerVisitor);
            sourceVisitor.visit(compilationUnitContext.get());
            sourceVisitor.printTo(fileWriter().getPrintStream(sourcePath));
            if (!sourceVisitor.hasMain())
                addToSources(sourceCodePath, isTest);
            else if (!isTest)
                mainClass = sourceCodePath;

            if (sourceVisitor.testClass()) {
                testClasses.add(sourceVisitor.fullClassName());
            }

            checkMakePure(sourceVisitor, clazzParts, compilationUnitContext.get(), isTest);
            return null;
        });
    }

    @Override
    public Future<Void> finish() {
        return this.executorService().submit(() -> {
            writeCMakeLists(false);
            writeTestsMain();
            writeCMakeLists(true);
            return null;
        });
    }
}
