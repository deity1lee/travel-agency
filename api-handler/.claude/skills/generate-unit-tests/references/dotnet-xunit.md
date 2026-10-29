# .NET 8 / ASP.NET Core — Unit Test Generation Reference

## Table of contents

1. [Pre-flight checks](#1-pre-flight-checks)
2. [Detect and configure the test project](#2-detect-and-configure-the-test-project)
3. [Run coverage and parse the report](#3-run-coverage-and-parse-the-report)
4. [Select classes for test generation](#4-select-classes-for-test-generation)
5. [Analyse each class](#5-analyse-each-class)
6. [Generate the test class](#6-generate-the-test-class)
7. [Write tests to disk](#7-write-tests-to-disk)
8. [Verify](#8-verify)
9. [Best-practice rules](#9-best-practice-rules)

---

## 1. Pre-flight checks

Read the `.csproj` file to determine:

- **Target framework** (`<TargetFramework>`) — must be `net8.0` or later to use this reference.
- **Project SDK** (`<Project Sdk="...">`) — `Microsoft.NET.Sdk.Web` means it is a Web / API project; plain `Microsoft.NET.Sdk` is a library or console.
- **Nullable** — note if `<Nullable>enable</Nullable>` is set; generated tests must be nullable-safe.

Run a clean restore and build:

```bash
dotnet clean && dotnet restore
dotnet build
```

- If the build **fails**: stop immediately and tell the user. Do not generate tests against broken code.
- If the build **succeeds**: continue to Step 2.

---

## 2. Detect and configure the test project

### 2.1 Check whether a test project exists

Look for a directory or `.csproj` file whose name ends with `.Tests` or contains `Tests` (e.g. `MyProject.Tests/MyProject.Tests.csproj`).

- **If a test project already exists**: skip to [2.5 Verify test project is wired up](#25-verify-test-project-is-wired-up).
- **If no test project exists**: inform the user and ask for approval before making any changes:

> "No test project was found. To generate unit tests I need to create one and make a few small configuration changes. May I proceed with the following steps?
> 1. Create `<MainProject>.Tests` xUnit project
> 2. Add a project reference from the test project to the main project
> 3. Add compile exclusions to the main `.csproj` so it does not pick up test files
> 4. (Web/API only) Expose the implicit `Program` class so `WebApplicationFactory` can reference it
> 5. Create or update the `.sln` solution file to include both projects
> 6. Install `coverlet.console` and `dotnet-reportgenerator-globaltool` globally (if not already installed)
>
> Proceed? (Yes / No)"

**Do not perform any of the steps below until the user confirms.**

### 2.2 Create the test project

```bash
dotnet new xunit -n <MainProject>.Tests
```

Open `<MainProject>.Tests/<MainProject>.Tests.csproj` and add `<IsPublishable>false</IsPublishable>` inside `<PropertyGroup>`:

```xml
<PropertyGroup>
  <TargetFramework>net8.0</TargetFramework>
  <IsPublishable>false</IsPublishable>
  ...
</PropertyGroup>
```

Add a project reference to the main project:

```xml
<ItemGroup>
  <ProjectReference Include="..\<MainProject>.csproj" />
</ItemGroup>
```

Add the required NuGet packages to the test project:

```bash
cd <MainProject>.Tests
dotnet add package Moq
dotnet add package FluentAssertions
dotnet add package coverlet.collector
```

For Web / API projects, also add:

```bash
dotnet add package Microsoft.AspNetCore.Mvc.Testing
```

### 2.3 Exclude test files from the main project

Open the main `.csproj` and add this `<ItemGroup>` if it is not already present:

```xml
<ItemGroup>
  <Compile Remove="**/*.Tests/**" />
  <EmbeddedResource Remove="**/*.Tests/**" />
  <None Remove="**/*.Tests/**" />
</ItemGroup>
```

### 2.4 Expose the Program class (Web / API projects only)

For projects using top-level statements (`Program.cs` with no explicit `class Program`), add this line at the very end of `Program.cs`:

```csharp
// Required to allow WebApplicationFactory<Program> to reference this assembly in tests
public partial class Program { }
```

### 2.5 Verify test project is wired up

If a test project already existed, confirm:

- It has a `<ProjectReference>` pointing to the main project.
- The main project excludes test files (add the `<ItemGroup>` from 2.3 if absent).
- For Web / API projects, `Program.cs` exposes the `Program` class (add the partial declaration from 2.4 if absent).

### 2.6 Create or update the solution file

Check whether a `.sln` file exists at the repository root.

**If absent**, create one:

```bash
dotnet new sln -n <SolutionName>
dotnet sln add <MainProject>.csproj
dotnet sln add <MainProject>.Tests/<MainProject>.Tests.csproj
```

**If present**, ensure both projects are listed:

```bash
dotnet sln add <MainProject>.Tests/<MainProject>.Tests.csproj   # safe to run even if already added
```

### 2.7 Install global coverage tools

```bash
dotnet tool install --global coverlet.console   2>/dev/null || true
dotnet tool install --global dotnet-reportgenerator-globaltool   2>/dev/null || true
```

The `|| true` prevents failure if the tool is already installed.

---

## 3. Run coverage and parse the report

Run the test suite with coverage collection:

```bash
dotnet test $(ls *.sln 2>/dev/null | head -1 || echo) \
  --collect:"XPlat Code Coverage" \
  --results-directory ./TestResults
```

Generate the CSV summary report:

```bash
reportgenerator \
  "-reports:TestResults/**/coverage.cobertura.xml" \
  "-targetdir:coverage-report" \
  "-reporttypes:CsvSummary"
```

Then read `coverage-report/Summary.csv`.

**CSV columns that matter:**

| Column | Meaning |
|---|---|
| `Name` | Fully-qualified class name (e.g. `RestfulApi.Services.ObjectService`) |
| `Line coverage` | Percentage of lines covered (e.g. `75%`) |
| `Branch coverage` | Percentage of branches covered |
| `Method coverage` | Percentage of methods covered |

A class is **incomplete** when `Line coverage < 100%`.

**Troubleshoot: 0 assemblies reported**

If the CSV shows 0 rows or the XML files are missing:

- Check that the test project has a `<ProjectReference>` to the main project.
- Check that `coverlet.collector` is installed in the test project.
- Re-run `dotnet build` and confirm it succeeds.

**Determine the coverage target:**

- Parse `Line coverage` for each row. Compute the **baseline overall line coverage** as the weighted average across all non-excluded classes.
- If the user stated an explicit target (e.g. "reach 80%"), record it as **target**.
- If no target was stated, set **target = baseline + 5 percentage points** (capped at 100%).

---

## 4. Select classes for test generation

**Exclude** any class whose fully-qualified name contains (case-insensitive):

`Dto`, `Entity`, `Model`, `Mapper`, `Config`, `Program`, `Controller`, `Middleware`, `Extension`, `Migration`

**Also exclude** interfaces (names starting with `I` followed by an uppercase letter, e.g. `IObjectService`) — test the concrete implementation, not the interface.

**Also exclude** classes already at 100% line coverage.

**Prioritise** classes in this order:

1. `Services` namespace — pure business logic, highest value
2. `Repositories` namespace — data-access layer
3. Utility / helper classes
4. Everything else

Process classes in priority order, one at a time.

---

## 5. Analyse each class

Before writing a test, read the source file and understand:

- What interfaces does the class depend on? (constructor parameters typed as `I...`)
- What are the happy-path flows for each public method?
- What are the error paths? (`throw new`, `if (x is null) throw`, early-return `null` / `false`)
- What branches exist? (`if/else`, ternary `?:`, null-coalescing `??`, `switch`)
- Are methods `async`? (any `async Task<...>`)
- Does the class use `DateTime.UtcNow`, `Guid.NewGuid()`, or other non-deterministic calls? — these need to be handled carefully in tests or injected via an abstraction.

Map each uncovered line (from the CSV) back to specific lines in the source to target those exact paths.

---

## 6. Generate the test class

### 6.1 Test framework selection

Detect which framework is used in the test project's `.csproj`:

| Package reference present | Framework |
|---|---|
| `xunit` | xUnit → use `[Fact]`, `[Theory]`, constructor setup |
| `NUnit` | NUnit → use `[Test]`, `[TestCase]`, `[SetUp]` |
| `MSTest` | MSTest → use `[TestMethod]`, `[DataRow]`, `[TestInitialize]` |

**Default to xUnit** if the test project was just created.

Detect assertion library:

| Package reference present | Assertions |
|---|---|
| `FluentAssertions` or `AwesomeAssertions` | Use fluent style: `result.Should().Be(expected)` |
| Neither | Use xUnit's `Assert.*` / NUnit's `Assert.That` / MSTest's `Assert.*` |

**Default to FluentAssertions** if the test project was just created (already added in Step 2.2).

### 6.2 Mandatory best-practice rules

#### Arrange-Act-Assert
Every test must have three clearly separated phases:

```csharp
// Arrange
var mockRepo = new Mock<IObjectRepository>();
mockRepo.Setup(r => r.GetByIdAsync("1")).ReturnsAsync(entity);
var service = new ObjectService(mockRepo.Object);

// Act
var result = await service.GetByIdAsync("1");

// Assert
result.Should().NotBeNull();
result!.Id.Should().Be("1");
```

#### Test naming — xUnit
Use descriptive method names that encode the scenario:

```
WhenObjectExists_GetByIdAsync_ReturnsDto
WhenObjectNotFound_GetByIdAsync_ReturnsNull
WhenUserAlreadyExists_SignUpAsync_ThrowsInvalidOperationException
```

Or the equally acceptable `MethodName_Condition_ExpectedResult` form.

#### Class structure — xUnit

```csharp
public class ObjectServiceTests
{
    private readonly Mock<IObjectRepository> _mockRepo;
    private readonly ObjectService _sut;   // sut = system under test

    public ObjectServiceTests()
    {
        _mockRepo = new Mock<IObjectRepository>();
        _sut = new ObjectService(_mockRepo.Object);
    }

    // test methods...
}
```

#### Independence
- Each test creates its own `Mock<T>` and constructs the system under test fresh — never share mutable state between tests.
- `Mock<T>` setup must be per-test (in the test body or `[Theory]` data) unless the setup is identical for every test in the class.

#### Mocking rules (Moq)
- Declare mocks as `Mock<IInterface>` (not `Mock<ConcreteClass>`).
- Use `Setup(...).ReturnsAsync(value)` for async methods.
- Use `Setup(...).ThrowsAsync(new Exception(...))` for async error paths.
- Use `Setup(...).Returns(value)` for sync methods.
- After the call, verify expected interactions: `_mockRepo.Verify(r => r.GetByIdAsync("1"), Times.Once())`.

#### Coverage breadth (RIGHT-BICEP)
Ensure tests cover:
- **R**ight — expected happy-path result
- **B**oundary — `null` inputs, empty collections, zero values
- **E**rror — every `throw` / exception path in the source
- **I**nverse — if there is an `Add`, also test that subsequent `Get` reflects the change (where useful)

#### Parameterized tests — xUnit `[Theory]`
When the same logic applies to several input variations, use `[Theory]` + `[InlineData]` instead of writing duplicate `[Fact]` methods:

```csharp
[Theory]
[InlineData("")]
[InlineData("   ")]
[InlineData(null)]
void WhenEmailIsInvalid_SignUpAsync_ThrowsArgumentException(string? email) { ... }
```

### 6.3 Exception testing pattern

```csharp
// FluentAssertions — recommended
[Fact]
async Task WhenUserExists_SignUpAsync_ThrowsInvalidOperationException()
{
    // Arrange
    _mockUserRepo.Setup(r => r.GetByEmailAsync("test@example.com"))
                 .ReturnsAsync(new User { Email = "test@example.com" });
    var dto = new SignUpRequestDto { Email = "test@example.com", Password = "pass", FirstName = "A", LastName = "B" };

    // Act
    Func<Task> act = () => _sut.SignUpAsync(dto);

    // Assert
    await act.Should().ThrowAsync<InvalidOperationException>()
             .WithMessage("*already exists*");
}
```

### 6.4 Moq async verification pattern

```csharp
// Verify the mock was called exactly once with expected args
_mockRepo.Verify(r => r.AddAsync(It.Is<User>(u => u.Email == "test@example.com")), Times.Once());
_mockRepo.VerifyNoOtherCalls();
```

### 6.5 Template — service unit test (xUnit + Moq + FluentAssertions)

```csharp
using FluentAssertions;
using Moq;
using RestfulApi.DTOs;
using RestfulApi.Entities;
using RestfulApi.Repositories;
using RestfulApi.Services;

namespace RestfulApi.Tests.Services;

public class ObjectServiceTests
{
    private readonly Mock<IObjectRepository> _mockRepo;
    private readonly ObjectService _sut;

    public ObjectServiceTests()
    {
        _mockRepo = new Mock<IObjectRepository>();
        _sut = new ObjectService(_mockRepo.Object);
    }

    [Fact]
    async Task WhenObjectExists_GetByIdAsync_ReturnsMappedDto()
    {
        // Arrange
        var entity = new ApiObject { Id = "1", Name = "Test", Data = null };
        _mockRepo.Setup(r => r.GetByIdAsync("1")).ReturnsAsync(entity);

        // Act
        var result = await _sut.GetByIdAsync("1");

        // Assert
        result.Should().NotBeNull();
        result!.Id.Should().Be("1");
        result.Name.Should().Be("Test");
        _mockRepo.Verify(r => r.GetByIdAsync("1"), Times.Once());
    }

    [Fact]
    async Task WhenObjectNotFound_GetByIdAsync_ReturnsNull()
    {
        // Arrange
        _mockRepo.Setup(r => r.GetByIdAsync("missing")).ReturnsAsync((ApiObject?)null);

        // Act
        var result = await _sut.GetByIdAsync("missing");

        // Assert
        result.Should().BeNull();
    }

    [Fact]
    async Task WhenObjectNotFound_DeleteAsync_ReturnsNull()
    {
        // Arrange
        _mockRepo.Setup(r => r.DeleteAsync("ghost")).ReturnsAsync((ApiObject?)null);

        // Act
        var result = await _sut.DeleteAsync("ghost");

        // Assert
        result.Should().BeNull();
        _mockRepo.Verify(r => r.DeleteAsync("ghost"), Times.Once());
    }

    [Fact]
    async Task WhenObjectsExist_GetAllAsync_ReturnsMappedList()
    {
        // Arrange
        var entities = new List<ApiObject>
        {
            new() { Id = "1", Name = "Item A" },
            new() { Id = "2", Name = "Item B" }
        };
        _mockRepo.Setup(r => r.GetAllAsync()).ReturnsAsync(entities);

        // Act
        var result = await _sut.GetAllAsync();

        // Assert
        result.Should().HaveCount(2);
        result[0].Id.Should().Be("1");
    }
}
```

### 6.6 Template — repository unit test (in-memory, no mocks needed)

```csharp
using FluentAssertions;
using RestfulApi.Entities;
using RestfulApi.Repositories;

namespace RestfulApi.Tests.Repositories;

public class InMemoryObjectRepositoryTests
{
    private readonly InMemoryObjectRepository _sut = new();

    [Fact]
    async Task WhenIdExists_GetByIdAsync_ReturnsEntity()
    {
        // Act
        var result = await _sut.GetByIdAsync("1");  // seeded in constructor

        // Assert
        result.Should().NotBeNull();
        result!.Id.Should().Be("1");
    }

    [Fact]
    async Task WhenIdMissing_GetByIdAsync_ReturnsNull()
    {
        // Act
        var result = await _sut.GetByIdAsync("nonexistent");

        // Assert
        result.Should().BeNull();
    }

    [Fact]
    async Task WhenIdMissing_DeleteAsync_ReturnsNull()
    {
        // Act
        var result = await _sut.DeleteAsync("nonexistent");

        // Assert
        result.Should().BeNull();
    }

    [Fact]
    async Task WhenObjectAdded_GetByIdAsync_ReturnsIt()
    {
        // Arrange
        var obj = new ApiObject { Id = "new-99", Name = "New Item" };

        // Act
        await _sut.AddAsync(obj);
        var result = await _sut.GetByIdAsync("new-99");

        // Assert
        result.Should().NotBeNull();
        result!.Name.Should().Be("New Item");
    }
}
```

---

## 7. Write tests to disk

Determine the test file path by mirroring the source structure under the test project root:

```
Services/ObjectService.cs
  → <MainProject>.Tests/Services/ObjectServiceTests.cs

Repositories/InMemoryObjectRepository.cs
  → <MainProject>.Tests/Repositories/InMemoryObjectRepositoryTests.cs
```

Rules:
- Name the file `<ClassName>Tests.cs`.
- Place it in the **same sub-folder name** under `<MainProject>.Tests/` as the source file lives in under the main project.
- Create the sub-folder if it does not exist.
- If a test file already exists, **append** new test methods to the existing class — do not overwrite or duplicate methods that already exist.
- Namespace in the test file: `<MainNamespace>.Tests.<SubFolder>` (e.g. `RestfulApi.Tests.Services`).

---

## 8. Verify

After writing all test files, re-run:

```bash
dotnet test $(ls *.sln 2>/dev/null | head -1 || echo) \
  --collect:"XPlat Code Coverage" \
  --results-directory ./TestResults

reportgenerator \
  "-reports:TestResults/**/coverage.cobertura.xml" \
  "-targetdir:coverage-report" \
  "-reporttypes:CsvSummary"
```

Read the updated `coverage-report/Summary.csv` and report:

| Class | Coverage before | Coverage after |
|---|---|---|
| `RestfulApi.Services.ObjectService` | 0% | 85% |
| `RestfulApi.Services.AuthService` | 0% | 90% |

If any newly written test **fails**, diagnose and fix it before reporting success.

**Check against coverage target:**

1. Compute the new overall line coverage from the updated `Summary.csv`.
2. Show the overall delta: **baseline → current**.
3. If **new coverage ≥ target**: report success and stop.
4. If **new coverage < target** and the user specified an explicit goal:
   - Return to **Step 4**, select the next batch of eligible classes (line coverage < 100%, not yet processed).
   - Repeat the generate → write → verify cycle until the target is reached or no eligible classes remain.
   - If no eligible classes remain but the target is still not met, inform the user and stop.
5. If **new coverage < target** and no explicit goal was set (increment mode): report the delta achieved and stop — do not loop further.

Common failure causes:

- `CS0246 type not found` — missing `using` statement or `<PackageReference>` in test csproj
- `cannot access internal class` — class visibility is `internal`; either add `[assembly: InternalsVisibleTo("<TestProject>")]` to the main project or change visibility to `public`
- `WebApplicationFactory` fails to start — check `public partial class Program { }` is present in `Program.cs`
- `0% coverage after run` — verify test project has `<ProjectReference>` to main project and `coverlet.collector` package
- `reportgenerator: command not found` — run `dotnet tool install --global dotnet-reportgenerator-globaltool` again

---

## 9. Best-practice rules (quick reference)

| # | Rule | Why |
|---|---|---|
| 1 | Use the test framework already in the project (default xUnit) | Consistency; avoid mixing frameworks |
| 2 | FluentAssertions for all assertions | Descriptive failure messages; fluent readability |
| 3 | Moq for all interface mocks | Standard .NET mocking library; type-safe setups |
| 4 | Arrange-Act-Assert with inline comments | Immediate readability of intent |
| 5 | Descriptive test names encoding the scenario | Failures are self-explanatory without reading the body |
| 6 | One behaviour per `[Fact]` | Easy to pinpoint which scenario failed |
| 7 | `[Theory]` + `[InlineData]` for data variations | Eliminates copy-paste test methods |
| 8 | Constructor setup for shared mock creation | Keeps test methods lean; avoids `[SetUp]` shared-state anti-pattern |
| 9 | `Mock<IInterface>` — always mock the interface | Decouples tests from implementation details |
| 10 | `Verify(...)` after every interaction | Catches unexpected or missing calls |
| 11 | `VerifyNoOtherCalls()` when interaction completeness matters | Guards against surplus calls |
| 12 | Never mock `DateTime.UtcNow` / `Guid.NewGuid()` inline — assert the shape, not exact value | Avoids brittle time-sensitive tests |
| 13 | Do not test DTOs, Entities, or Mappers | Pure data structures; no logic to break |
| 14 | Get user approval before creating/modifying test project or `Program.cs` | Project structure changes affect the whole team |
