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


package otterop.transpiler;

import otterop.transpiler.ignore.IgnoreFile;
import otterop.transpiler.language.CSharpTranspiler;
import otterop.transpiler.language.CTranspiler;
import otterop.transpiler.language.GoTranspiler;
import otterop.transpiler.language.PythonTranspiler;
import otterop.transpiler.language.Transpiler;
import otterop.transpiler.language.TypeScriptTranspiler;
import otterop.transpiler.parser.OtteropParser;
import otterop.transpiler.reader.ClassReader;
import otterop.transpiler.reader.FileReader;
import otterop.transpiler.writer.FileWriter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Otterop {

    private Map<String, Transpiler> transpilers;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private FileReader fileReader = new FileReader(executor);
    private FileWriter fileWriter = new FileWriter();
    private ClassReader classReader = new ClassReader();
    private OtteropParser parser = new OtteropParser(executor);
    private CTranspiler cTranspiler;
    private IgnoreFile ignoreFile;
    private long start;

    public static final String WRAPPED_CLASS = "otterop.lang.MakePure";

    public Otterop() {
        TypeScriptTranspiler tsTranspiler = new TypeScriptTranspiler(
                "./ts",
                fileWriter,
                executor,
                classReader,
                "example.sort");
        CSharpTranspiler csTranspiler = new CSharpTranspiler(
                "./dotnet",
                fileWriter,
                executor,
                classReader);
        cTranspiler = new CTranspiler(
                "./c",
                fileWriter,
                executor,
                classReader,
                "example.sort");
        PythonTranspiler pythonTranspiler = new PythonTranspiler(
                "./python",
                fileWriter,
                executor,
                classReader);
        GoTranspiler goTranspiler = new GoTranspiler(
                "./go",
                fileWriter,
                executor,
                classReader,
                Map.of("otterop", "github.com/otterop/otterop/go",
                        "example.sort", "github.com/otterop/example-sort/go/example/sort"));

        this.transpilers = Map.of(
                "typescript", tsTranspiler,
                "csharp", csTranspiler,
                "c", cTranspiler,
                "python", pythonTranspiler,
                "go", goTranspiler);
        this.ignoreFile = new IgnoreFile(".");
    }

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
        var finishOps = transpilers.values().stream().map(transpiler ->
                transpiler.finish()
        ).collect(Collectors.toList());
        for (var finishOp : finishOps) {
            finishOp.get();
        }
    }

    public void transpile(String basePath) throws InterruptedException, ExecutionException {
        this.start = Instant.now().toEpochMilli();
        AtomicBoolean complete = new AtomicBoolean(false);
        BlockingQueue<String> classes = fileReader.walkClasses(basePath, complete);
        List<Future<Void>> futures = new LinkedList<>();
        while (!complete.get() || !classes.isEmpty()) {
            String clazz = classes.poll(100, TimeUnit.MILLISECONDS);
            if (clazz != null && !this.ignoreFile.ignores(clazz)) {
                System.out.println(clazz);
                futures.add(executor.submit(() ->
                        this.transpile(basePath, clazz).get()
                ));
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
        return this.transpile(basePath, classFile, null);
    }

    public Future<Void> transpile(String basePath, String classFile, Set<String> languages) {
        Set<String> availableLanguages;
        if (languages == null || languages.isEmpty()) {
            availableLanguages = transpilers.keySet();
        } else {
            availableLanguages = new LinkedHashSet<>(languages);
            availableLanguages.retainAll(transpilers.keySet());
        }

        var clazzParts = pathToClassParts(classFile);
        var in = fileReader.readFile(basePath, classFile);
        var context = parser.parse(in);
        var futures = availableLanguages.stream().map(transpiler ->
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
            otterop.transpile(args[0]);
        } finally {
            otterop.shutdown();
        }
    }
}
