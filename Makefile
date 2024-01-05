build: transpile build-java build-dotnet \
build-python build-go build-ts

VERSION=0.2.0-SNAPSHOT
BLUE=\033[0;34m
RESET_COLOR=\e[0m

.PHONY: init-submodules transpile transpiler \
transpile-lang transpile-io transpile-test \
build-java build-python build-c build-ts \
build-dotnet build-go clean

init: init-submodules

init-submodules:
	@git submodule update --init --recursive

check-submodules:
	@if git submodule status | grep --quiet '^-'; then \
		echo "A git submodule is not initialized." && exit 1; \
	fi

transpiler:
	@echo "$(BLUE)Build OOP Transpiler ...${RESET_COLOR}"
	@(cd java && ./gradlew :transpiler:jar) > /dev/null

transpile-lang: transpiler
	@echo "$(BLUE)Transpile OOP Lang ...${RESET_COLOR}"
	@(cd java && ./gradlew :lang:build) > /dev/null
	@java -cp java/transpiler/build/libs/transpiler-$(VERSION).jar:java/lang/build/classes/java/main:java/lang/build/classes/java/test otterop.transpiler.Otterop config/lang/oopconfig.yml
	@(cd java && ./gradlew :lang:jar) > /dev/null

transpile-io: transpiler
	@echo "$(BLUE)Transpile OOP IO ...${RESET_COLOR}"
	@(cd java && ./gradlew :io:build) > /dev/null
	@java -cp java/transpiler/build/libs/transpiler-$(VERSION).jar:java/lang/build/libs/lang-$(VERSION).jar:java/io/build/classes/java/main:java/io/build/classes/java/test otterop.transpiler.Otterop config/io/oopconfig.yml
	@(cd java && ./gradlew :io:jar) > /dev/null

transpile-test: transpiler
	@echo "$(BLUE)Transpile OOP Test ...${RESET_COLOR}"
	@(cd java && ./gradlew :test:build) > /dev/null
	@java -cp java/transpiler/build/libs/transpiler-$(VERSION).jar:java/lang/build/libs/lang-$(VERSION).jar:java/test/build/classes/java/main:java/test/build/classes/java/test otterop.transpiler.Otterop config/test/oopconfig.yml
	@(cd java && ./gradlew :test:jar) > /dev/null

transpile: transpile-lang transpile-io transpile-test
	@echo "$(BLUE)Transpiled OOP Libraries${RESET_COLOR}"

build-java: transpile

build-python:
	@echo "$(BLUE)Build Python ...${RESET_COLOR}"
	@(cd python && \
	if [ ! -d _venv ]; then \
	python -m venv _venv; \
	fi && \
	. ./_venv/bin/activate && \
	python -m pip install ./lang ./io ./test ) > /dev/null

build-ts:
	@echo "$(BLUE)Build TypeScript ...${RESET_COLOR}"
	@(cd ts/lang && \
	rm -rf node_modules && \
	pnpm i && \
	pnpm build) > /dev/null
	@(cd ts/io && \
	rm -rf node_modules && \
	pnpm i && \
	pnpm build) > /dev/null
	@(cd ts/test && \
	rm -rf node_modules && \
	pnpm i && \
	pnpm build) > /dev/null

build-dotnet:
	@echo "$(BLUE)Build .NET ...${RESET_COLOR}"
	@(cd dotnet && \
	dotnet build) > /dev/null

build-go:
	@echo "$(BLUE)Build Go ...${RESET_COLOR}"
	@(cd go && go build ./...) > /dev/null

clean: check-submodules
	@echo "$(BLUE)Cleaning ...${RESET_COLOR}"
	@(rm -rf ./python/_venv \
	ts/lang/node_modules \
	ts/io/node_modules \
	ts/test/node_modules && \
	(cd java && ./gradlew clean)) > /dev/null

