# AGENTS.md

## Purpose
Java library that indexes files and searches them. It combines an inverted index on lexemes with binary trees for special symbols. Indexing and search are multithreaded.

## Requirements
- Java source and target 1.7 (`maven-compiler-plugin` in `pom.xml`). Source encoding UTF-8.
- Maven (`pom.xml` at the root; no Maven wrapper).
- Main libraries: Guice 3.0 (with assistedinject), Guava 14.0.1, commons-io, commons-lang3, commons-configuration, commons-pool, logback, slf4j.
- Test libraries: JUnit 4.11, Mockito 1.9.5, junice 1.3. `pom.xml` declares `version.junit` twice (4.10, then 4.11); the later 4.11 is used.

## Build and test
- Compile: `mvn compile`
- Jar: `mvn package`
- All tests: `mvn test`
- One test class (standard surefire option): `mvn test -Dtest=LexemeAnalyzerTest`

## Code map
Paths are under `src/main/java/ru/bona/fileindex/`.
- `Configuration.java`: `getTokenSize`, `getSimIndexThreads`, `getSimSearchThreads`, `getIndexPath`. `Configuration.DEFAULT` gives 3, 10, 50 and `<user.dir>/index/`.
- `IndexProvider.java`: entry point. `IndexProvider.newInstance(Configuration)` returns a singleton; use `getIndexService()` and `getSearchService()`.
- `guice/BaseModule.java`: Guice bindings, factories via `FactoryModuleBuilder`. Overridable hooks: `bindChannelProvider()`, `initSourceParser()`.
- `index/`: `IndexService`, `IndexTask`, `IndexingListener`, `builder/IndexBuilder` and its factory.
- `filesholder/`: `FilesHolder`, `FileInfo`; track indexed files.
- `search/`: `SearchService`, `SearchTask`, `SearchCompleteListener`, result types, `searcher/IndexSearcher`.
- `model/`: shared abstractions `FrameManager`, `Registry`, `ControlFile`, `SourceAnalyzer`, `SourceParser`, `Loadable`, `Storable`.
  - `model/lexem/`: lexeme index (`LexemeAnalyzer`, `LexemeFrameManager`, `LexemeRegistry`, `LexemControlFile`).
  - `model/spec/`: special-symbol index (`SpecAnalyzer`, `SpecFrameManager`, `SpecRegistry`, `SpecControlFile`, `synctree/*Tree`).
  - `model/range/`: range types.
  - `model/fileparser/`: `FileParser`, the bound `SourceParser` implementation.
- `channelworker/`: I/O over channels. `file/` is the real filesystem; `bytes/` is the in-memory version used by tests.
- `utils/`, `exceptions/`: `Helper`, `SplittedRunnable`, `StoppedException`.
- `src/main/resources/logback.xml`: logging configuration.

## Index files
Stored in the `index` directory: `files.info`, `spec.ctr`, `spec.idx`, `spec.idx.bak`, `lexem.ctr`, `lexem.idx`, `lexem.idx.bak`. The `.ctr` files exist only during backup. After an abnormal stop the index is restored from the `.bak` files.

## Conventions
- Package `ru.bona.fileindex`.
- Dependencies are injected with Guice (`@Inject`). Services are singletons bound in `BaseModule`. Runtime objects come from assisted-inject factories (`*Factory` interfaces).
- Classes have a Javadoc header with author and date, and `/*====[ SECTION ]====*/` banners for static variables, instance variables, constructors and methods.
- Java 7 style: diamond operator; no lambdas, no streams.
- Tests use JUnit 4 with `@RunWith(GuiceTestRunner.class)`; `AbstractIndexTest` is the base class. `TestModule` binds `BytesWorkerProvider`, so tests run in memory.

## Do not change
- Java 1.7 source and target in `pom.xml`.
- Index file names and formats (existing indexes and backup/restore depend on them).
- The public `Configuration` interface and the `IndexProvider` API.
- `.gitignore` entries (`target/`, `logs/`, `.idea/`). Do not commit `target/`, `logs/` or `index/`.
