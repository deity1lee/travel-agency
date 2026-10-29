---
name: generate-unit-tests
description: >
  Use this skill whenever the user wants to generate, improve, or increase unit test coverage for a project.
  Trigger for requests like: "generate unit tests", "improve coverage", "add tests for my service/controller/class",
  "run JaCoCo", "increase test coverage", "write tests for untested code", "boost code coverage",
  "create unit tests automatically", or any mention of coverage gaps, missing tests, or test automation.
  This skill runs coverage analysis, identifies under-tested classes, and writes high-quality unit tests following
  language/framework best practices. Start this skill proactively whenever the user asks about or is working on
  improving their test coverage, even if they don't explicitly say "generate tests".
---

# Generate Unit Tests

You are an expert test automation engineer. Your job is to analyze code coverage metrics and generate
high-quality, production-ready unit tests that close the coverage gaps.

## How to use this skill

### Step 1: Detect project type

Examine the project root to determine the language and framework:

| File present | Project type |
|---|---|
| `pom.xml` with Spring Boot parent | Java / Spring Boot → load [references/java-springboot.md](references/java-springboot.md) |
| `pom.xml` (no Spring Boot) | Java / Maven → load [references/java-springboot.md](references/java-springboot.md) |
| `build.gradle` or `build.gradle.kts` | Java / Gradle → load [references/java-springboot.md](references/java-springboot.md) |
| `package.json` containing `@nestjs/core` | TypeScript / NestJS → load [references/nestjs-jest.md](references/nestjs-jest.md) |
| `requirements.txt` containing `fastapi` | Python / FastAPI → load [references/python-fastapi-pytest.md](references/python-fastapi-pytest.md) |
| `*.csproj` or `*.sln` present | .NET 8 / ASP.NET Core → load [references/dotnet-xunit.md](references/dotnet-xunit.md) |

> **Note:** If the project type is not listed above, tell the user this skill does not yet support their stack
> and ask if they'd like to add support for it.

### Step 2: Follow the reference

Load the matching reference file and follow its instructions end-to-end. The reference covers:
- How to check and configure coverage tooling
- How to run the coverage report
- How to read and parse the report
- Which classes to include / exclude
- How to generate tests that meet best-practice standards
- How to write tests to the correct directories

### Step 3: Verify

After writing tests, re-run coverage to confirm the new tests actually improve coverage for the targeted classes.
Report a brief summary: which classes were covered, what the coverage delta was.

## General constraints (apply to all project types)

- Never generate tests for classes/modules whose names include: `dto`, `enum`, `model`, `entity`, `config`, `mapper`, `router`, `constant`, `exception`, `logger`
- Only generate tests for classes with **incomplete** coverage — skip fully-covered classes
- Ensure each generated test class compiles before moving on to the next
- Do not modify production source code to make tests pass — fix the tests instead
- Tests must be self-contained and runnable independently of each other
- All file writes go into the project's standard test source directory
