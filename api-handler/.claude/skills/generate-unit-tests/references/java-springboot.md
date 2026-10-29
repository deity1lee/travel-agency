# Java / Spring Boot — Unit Test Generation Reference

## Table of contents

1. [Pre-flight checks](#1-pre-flight-checks)
2. [Configure JaCoCo if missing](#2-configure-jacoco-if-missing)
3. [Run coverage and parse the report](#3-run-coverage-and-parse-the-report)
4. [Select classes for test generation](#4-select-classes-for-test-generation)
5. [Analyse each class](#5-analyse-each-class)
6. [Generate the test class](#6-generate-the-test-class)
7. [Write tests to disk](#7-write-tests-to-disk)
8. [Verify](#8-verify)
9. [Best-practice rules](#9-best-practice-rules)

---

## 1. Pre-flight checks

Run `mvn compile` (or `./mvnw compile`) before doing anything else.

- If compilation **fails**: stop immediately and tell the user. Do not generate tests against broken code.
- If compilation **succeeds**: continue.

---

## 2. Configure JaCoCo if missing

Check `pom.xml` for the `jacoco-maven-plugin`. If it is absent, add the following inside
`<build><plugins>` before proceeding:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

Tell the user you added JaCoCo and will now run the report.

---

## 3. Run coverage and parse the report

Run:

```bash
mvn -DskipITs org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report -B
```

Then read the CSV report at `target/site/jacoco/jacoco.csv`.

**CSV columns that matter:**

| Column | Meaning |
|---|---|
| `CLASS_NAME` | Simple class name |
| `PACKAGE` | Package path (slashes) |
| `INSTRUCTION_MISSED` | Bytecode instructions not covered |
| `INSTRUCTION_COVERED` | Bytecode instructions covered |
| `BRANCH_MISSED` | Branches not covered |
| `BRANCH_COVERED` | Branches covered |

Compute coverage percentage per class:

```
instruction_coverage = INSTRUCTION_COVERED / (INSTRUCTION_COVERED + INSTRUCTION_MISSED)
```

A class is **incomplete** when `instruction_coverage < 1.0`.

**Determine the coverage target:**

- Compute the **baseline overall instruction coverage**: divide the sum of all `INSTRUCTION_COVERED` values by the sum of all `(INSTRUCTION_COVERED + INSTRUCTION_MISSED)` values across every row in the CSV.
- If the user stated an explicit target (e.g. "reach 80%", "get to 90% coverage"), record it as **target**.
- If no target was stated, set **target = baseline + 5 percentage points** (capped at 100%).

---

## 4. Select classes for test generation

**Exclude** any class whose simple name contains (case-insensitive):
`dto`, `enum`, `model`, `config`, `mapper`, `application` (main entry points)

**Also exclude** classes that are already at 100% coverage.

**Prioritise** classes in this order:
1. `service` / `service.impl` packages — pure business logic, highest value
2. `controller` packages — request handling and response mapping
3. `repository` packages — data access layer
4. Everything else

Process classes in priority order, one at a time.

---

## 5. Analyse each class

Before writing a test, read the source file and understand:

- What dependencies does the class have? (fields annotated `@Autowired`, `@Resource`, constructor params)
- What are the happy-path flows?
- What are the error/exception paths? (look for `throw`, `orElseThrow`, `if` guards)
- What branches exist? (ternary, `if/else`, `switch`)
- Are there any `null` checks?

Map each **uncovered branch or instruction** (as reported by JaCoCo) to specific lines in the source to target those exact paths.

---

## 6. Generate the test class

### 6.1 Framework selection

| Class type | Test approach |
|---|---|
| Service / util (no Spring context needed) | Plain JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) |
| Controller | `@WebMvcTest` + `@MockBean` for service layer |
| Repository (JPA) | `@DataJpaTest` |
| Repository (in-memory / custom) | Plain JUnit 5, instantiate directly |

**Prefer plain unit tests** (no Spring context) for service and util classes — they are faster and more
focused. Only load the Spring context when it is genuinely necessary (e.g., controller slice tests).

### 6.2 Mandatory best-practice rules

#### Annotations
- Use **JUnit 5** (`org.junit.jupiter.api.*`): `@Test`, `@BeforeEach`, `@AfterEach`, `@DisplayName`,
  `@Nested`, `@ParameterizedTest`. Do NOT use JUnit 4 (`org.junit.Test`, `@RunWith`).
- Annotate the test class with `@ExtendWith(MockitoExtension.class)` for Mockito-backed tests.

#### Assertions
- Use **AssertJ** (`org.assertj.core.api.Assertions.assertThat`, `assertThatThrownBy`).
- Do NOT use JUnit's `assertEquals` / `assertTrue` / `assertThrows`.
- Chain multiple assertions fluently: `assertThat(result).isNotNull().isEqualTo(expected)`.

#### Test structure
- Follow **Given-When-Then** with inline comments separating the three phases.
- Each test method tests exactly **one** logical scenario (single responsibility).
- Test names follow `should_<expectedBehavior>_when_<scenario>` or use `@DisplayName` for natural language.

#### Visibility
- Test class: **package-private** (no `public`).
- Test methods: **package-private** (no `public`).

#### Mocking (Mockito)
- Declare dependencies as `@Mock` fields.
- Inject into the class under test with `@InjectMocks`.
- Use `when(...).thenReturn(...)` for stubbing.
- Always call `verify(mock, times(N)).method(args)` to confirm expected interactions.
- Call `verifyNoMoreInteractions(mock)` when the test must confirm no unexpected calls were made.

#### Independence
- Each test sets up its own state — never rely on execution order.
- Use `@BeforeEach` only for common fixture initialization, not for shared mutable state.
- Never use `static` mutable fields shared across tests.

#### Coverage breadth (RIGHT-BICEP principles)
Ensure tests cover:
- **R**ight — the expected happy-path result
- **B**oundary — empty collections, zero, null inputs, max values
- **I**nverse — if there's a `create`, also test `delete`
- **C**ross-check — verify the result through a different code path if possible
- **E**rror conditions — every `throw` / `orElseThrow` in the source
- **P**erformance — not required for unit tests, skip

#### Parameterized tests
When testing the same logic with several input variations, use `@ParameterizedTest` with
`@CsvSource`, `@ValueSource`, or `@MethodSource` instead of writing duplicate test methods.

#### Null safety
Add `@NullMarked` (from `org.jspecify.annotations`) to test classes that explicitly test null-handling
scenarios.

#### Avoid reflection
Never use `getDeclaredMethod(...).setAccessible(true)` to access private members. Instead:
- Test behaviour through the public API.
- Or extract private logic to a package-private helper class and test that directly.

### 6.3 Exception testing pattern

```java
// Good — AssertJ assertThatThrownBy
assertThatThrownBy(() -> service.findById("missing-id"))
    .isInstanceOf(RuntimeException.class)
    .hasMessageContaining("not found");
```

### 6.4 Mockito verification pattern

```java
// Good
verify(repository, times(1)).findById("123");
verifyNoMoreInteractions(repository);
```

### 6.5 Template — service unit test

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MyService")
class MyServiceTest {

    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyServiceImpl service;

    @Test
    @DisplayName("should return entity when found by id")
    void should_returnEntity_when_idExists() {
        // Given
        MyEntity entity = new MyEntity("id-1", "data");
        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        // When
        MyEntity result = service.findById("id-1");

        // Then
        assertThat(result).isEqualTo(entity);
        verify(repository, times(1)).findById("id-1");
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("should throw RuntimeException when id is not found")
    void should_throwException_when_idNotFound() {
        // Given
        when(repository.findById("missing")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.findById("missing"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }
}
```

### 6.6 Template — controller slice test

```java
@WebMvcTest(MyController.class)
@DisplayName("MyController")
class MyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MyService service;

    @Test
    @DisplayName("GET /items/{id} returns 200 and item JSON when item exists")
    void should_return200_when_itemExists() throws Exception {
        // Given
        ItemDto dto = new ItemDto("id-1", "name");
        when(service.findById("id-1")).thenReturn(dto);

        // When / Then
        mockMvc.perform(get("/items/id-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("id-1"))
            .andExpect(jsonPath("$.name").value("name"));
    }
}
```

---

## 7. Write tests to disk

Determine the test file path from the class under test:

```
src/main/java/com/example/service/impl/MyServiceImpl.java
→ src/test/java/com/example/service/impl/MyServiceImplTest.java
```

Rules:
- Mirror the **same package path** under `src/test/java/`.
- Name the file `<ClassName>Test.java`.
- If a test file already exists for the class, **append** new test methods to the existing class rather
  than overwriting it. Do not duplicate methods that already exist.

---

## 8. Verify

After writing all test files, re-run:

```bash
mvn -DskipITs org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report -B
```

Read the updated `target/site/jacoco/jacoco.csv` and report:

| Class | Coverage before | Coverage after |
|---|---|---|
| `ApiObjectServiceImpl` | 12% | 85% |
| `AuthServiceImpl` | 0% | 92% |

If any newly written test **fails**, diagnose and fix it before reporting success.

**Check against coverage target:**

1. Compute the new overall instruction coverage from the updated `target/site/jacoco/jacoco.csv`.
2. Show the overall delta: **baseline → current**.
3. If **new coverage ≥ target**: report success and stop.
4. If **new coverage < target** and the user specified an explicit goal:
   - Return to **Step 4**, select the next batch of eligible classes (instruction_coverage < 1.0, not yet processed).
   - Repeat the generate → write → verify cycle until the target is reached or no eligible classes remain.
   - If no eligible classes remain but the target is still not met, inform the user and stop.
5. If **new coverage < target** and no explicit goal was set (increment mode): report the delta achieved and stop — do not loop further.

---

## 9. Best-practice rules (quick reference)

| # | Rule | Why |
|---|---|---|
| 1 | JUnit 5 only — no JUnit 4 | Modern lifecycle, better extensions |
| 2 | AssertJ for all assertions | Fluent, descriptive failure messages |
| 3 | Given-When-Then structure | Readability as living documentation |
| 4 | Descriptive test names | Failures are self-explanatory |
| 5 | Single responsibility per test | Easy to pinpoint failures |
| 6 | Independent tests (no shared state) | Reliable in any order |
| 7 | `@ParameterizedTest` for data variations | Reduces duplication |
| 8 | Mockito for all external dependencies | Isolation, speed |
| 9 | Verify mock interactions | Catches unexpected calls |
| 10 | Package-private visibility | Idiomatic Java test convention |
| 11 | No reflection to access privates | Avoids brittle implementation coupling |
| 12 | Test all exception paths | Completeness, RIGHT-BICEP E |
