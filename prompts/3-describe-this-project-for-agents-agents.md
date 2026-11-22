# Task 3: Describe this project for agents (AGENTS.md)

## Request
Write a root `AGENTS.md` (at most 8 KB) that tells an agent what the project is, what it needs, how to build and test it, the code map, conventions and what not to change. Documentation only; no code, test or build file changes.

## What was built
- `AGENTS.md` (new, root): sections Purpose, Requirements, Build and test, Code map, Index files, Conventions, Do not change. About 3.5 KB.
- This record.
- No nested `AGENTS.md`: `pom.xml` declares no modules.

## Decisions
- Commands come from `pom.xml` and the test sources. No build or test was run, so `mvn test -Dtest=LexemeAnalyzerTest` is labelled as the standard surefire option, not as verified.
- `pom.xml` sets `version.junit` twice (4.10, then 4.11); the file states that 4.11 wins.
- CI, linters, JDK availability and deployment are left out because the repository shows none of them.
- The control-file class is `LexemControlFile` (not `LexemeControlFile`); the file uses the real name.
- Paths and symbols were checked against the source tree: `Configuration.DEFAULT`, `IndexProvider.newInstance`, `BaseModule.bindChannelProvider` and `initSourceParser`, `@RunWith(GuiceTestRunner.class)` in `AbstractIndexTest`, and the `.gitignore` entries.

## For the next agent
- Both `Configuration.DEFAULT.getIndexPath()` and the tests use the `index` path under `user.dir`, so tests write there; `index/` is not in `.gitignore`. Do not commit it.
- `TestModule` overrides `bindChannelProvider()` to use in-memory bytes workers, which is why tests do not touch real files for the channel layer.
- The task's size check (`wc -c`) was refused by the harness; the size was judged from the content instead.
- If the file is edited, keep paths and class names in step with the source; the lexeme package is `model/lexem/` while the analyzer classes are named `Lexeme*`.
