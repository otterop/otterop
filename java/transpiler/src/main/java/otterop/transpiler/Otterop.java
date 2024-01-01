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


package otterop.transpiler;

import otterop.transpiler.config.OtteropConfig;
import otterop.transpiler.ignore.IgnoreFile;
import otterop.transpiler.language.CSharpTranspiler;
import otterop.transpiler.language.CTranspiler;
import otterop.transpiler.language.GoTranspiler;
import otterop.transpiler.language.JavaTranspiler;
import otterop.transpiler.language.PythonTranspiler;
import otterop.transpiler.language.Transpiler;
import otterop.transpiler.language.TypeScriptTranspiler;
import otterop.transpiler.parser.OtteropParser;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.reader.FileReader;
import otterop.transpiler.writer.FileWriter;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Otterop {

    private Map<TranspilerName, Transpiler> transpilers;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private FileReader fileReader = new FileReader(executor);
    private FileWriter fileWriter = new FileWriter();
    private ClassReader classReader = new ClassReader();
    private OtteropParser parser = new OtteropParser(executor);
    private CTranspiler cTranspiler;
    private JavaTranspiler javaTranspiler;
    private IgnoreFile ignoreFile;
    private OtteropConfig config;
    private long start;
    private Set<TranspilerName> selectedLanguages;

    public static final String WRAPPED_CLASS = "otterop.lang.MakePure";
    public static final Pattern PURE_CLASSES = Pattern.compile("^.*\\/pure\\/[^\\/\\.]*\\.java$");

    private String[] pathToClassParts(String path) {
        String[] parts = path.split("/");
        parts[parts.length - 1] = parts[parts.length - 1].split("\\.")[0];
        return parts;
    }

    private void clean() throws ExecutionException, InterruptedException {
        var cleanOps = transpilers.values().stream().map(transpiler ->
                transpiler.clean(start)
        ).collect(Collectors.toList());
        for (var cleanOp : cleanOps) {
            cleanOp.get();
        }
    }
    private void finish() throws ExecutionException, InterruptedException {
        var finishOps = transpilers.entrySet().stream().map(transpilerEntry -> {
            if (selectedLanguages.contains(transpilerEntry.getKey()))
                return transpilerEntry.getValue().finish();
            return CompletableFuture.completedFuture(null);
        }).collect(Collectors.toList());
        for (var finishOp : finishOps) {
            finishOp.get();
        }
    }

    private boolean ignored(String clazz) {
        if (PURE_CLASSES.matcher(clazz).matches())
            return true;
        return this.ignoreFile.ignores(clazz);
    }

    private OtteropConfig readConfig(String configPath) {
        if (configPath == null)
            return new OtteropConfig();
        return OtteropConfig.fromYAML(configPath);
    }

    private void init() {
        var basePackage = config.basePackage();
        if (basePackage == null)
            throw new RuntimeException("basePackage is a required configuration property");
        if (config.targetType() == null)
            throw new RuntimeException("targetType is a required configuration property");

        TypeScriptTranspiler tsTranspiler = new TypeScriptTranspiler(
                config.ts().outPath(),
                fileWriter,
                executor,
                classReader,
                config);
        CSharpTranspiler csTranspiler = new CSharpTranspiler(
                config.csharp().outPath(),
                fileWriter,
                executor,
                classReader,
                config);
        cTranspiler = new CTranspiler(
                config.c().outPath(),
                fileWriter,
                executor,
                classReader,
                config);
        PythonTranspiler pythonTranspiler = new PythonTranspiler(
                config.python().outPath(),
                fileWriter,
                executor,
                classReader,
                config);
        GoTranspiler goTranspiler = new GoTranspiler(
                config.go().outPath(),
                fileWriter,
                executor,
                classReader,
                config);
        JavaTranspiler javaTranspiler = new JavaTranspiler(
                config.java().outPath(),
                fileWriter,
                executor,
                classReader,
                config
        );

        this.transpilers = Map.of(
                TranspilerName.TYPESCRIPT, tsTranspiler,
                TranspilerName.CSHARP, csTranspiler,
                TranspilerName.C, cTranspiler,
                TranspilerName.PYTHON, pythonTranspiler,
                TranspilerName.GO, goTranspiler,
                TranspilerName.JAVA, javaTranspiler);
        this.ignoreFile = new IgnoreFile(".");

        Set<TranspilerName> languages = null;
        if (config.transpilers() != null) {
            languages = new LinkedHashSet<>(config.transpilers());
        }
        if (languages == null) {
            selectedLanguages = transpilers.keySet();
        } else {
            selectedLanguages = languages;
            selectedLanguages.retainAll(transpilers.keySet());
        }

        if (config.basePath() != null && config.java().outPath() == null) {
            config.java().setOutPath(config.basePath());
        }
    }

    public void transpile(String configPath) throws InterruptedException, ExecutionException {
        this.config = readConfig(configPath);
        init();
        if (selectedLanguages.isEmpty()) {
            System.out.println("No language selected for transpiling, exiting");
            return;
        }

        System.out.println("Transpiling to languages: " + selectedLanguages.stream()
                .map(language -> language.getName()).collect(Collectors.joining(",")));
        this.start = Instant.now().toEpochMilli();
        AtomicBoolean complete = new AtomicBoolean(false);
        var basePath = config.basePath();
        BlockingQueue<String> classes = fileReader.walkClasses(basePath, complete);
        List<Future<Void>> futures = new LinkedList<>();
        while (!complete.get() || !classes.isEmpty()) {
            String clazz = classes.poll(100, TimeUnit.MILLISECONDS);
            if (clazz != null) {
                if (!this.ignored(clazz)) {
                    System.out.print("Transpiled: ");
                    futures.add(executor.submit(() ->
                            this.transpile(basePath, clazz).get()
                    ));
                } else {
                    System.out.print("Transpiler ignored: ");
                }
                System.out.println(clazz);
            }
            while(!futures.isEmpty() && futures.get(0).isDone()) {
                futures.remove(0).get();
            }
        }
        for (Future f: futures) f.get();
        clean();
        finish();
    }

    public Future<Void> transpile(String basePath, String classFile) {
        return this.transpile(basePath, classFile, selectedLanguages);
    }

    public Future<Void> transpile(String basePath, String classFile, Set<TranspilerName> selectedLanguages) {
        var clazzParts = pathToClassParts(classFile);
        var in = fileReader.readFile(basePath, classFile);
        var context = parser.parse(in);
        var futures = selectedLanguages.stream().map(transpiler ->
                executor.submit(() ->
                        transpilers.get(transpiler).transpile(clazzParts, context).get()
                )
        ).collect(Collectors.toList());
        return executor.submit(() -> {
            for (var future : futures) {
                future.get();
            }
            return null;
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    public static void main(String[] args) throws Exception {
        var otterop = new Otterop();
        try {
            if (args.length > 0)
                otterop.transpile(args[0]);
            else
                otterop.transpile(null);
        } finally {
            otterop.shutdown();
        }
    }
}
