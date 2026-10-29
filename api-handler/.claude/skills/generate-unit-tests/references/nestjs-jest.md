# TypeScript / NestJS — Unit Test Generation Reference

## Table of contents

1. [Pre-flight checks](#1-pre-flight-checks)
2. [Configure coverage output if needed](#2-configure-coverage-output-if-needed)
3. [Run coverage and parse the report](#3-run-coverage-and-parse-the-report)
4. [Select files for test generation](#4-select-files-for-test-generation)
5. [Analyse each file](#5-analyse-each-file)
6. [Generate the test file](#6-generate-the-test-file)
7. [Write tests to disk](#7-write-tests-to-disk)
8. [Verify](#8-verify)
9. [Best-practice rules](#9-best-practice-rules)

---

## 1. Pre-flight checks

Run `npm run build` (or `npx tsc --noEmit`) before doing anything else.

- If compilation **fails**: stop immediately and tell the user. Do not generate tests against broken code.
- If compilation **succeeds**: continue.

---

## 2. Configure coverage output if needed

Read `jest.config.ts` (or `jest.config.js`). Find the `coverageDirectory` setting.

- If it is set to `'../coverage'` (outside the project root), change it to `'coverage'` so the HTML report and JSON land inside the project directory where you can read them.
- If it is already `'coverage'` or another in-project path, leave it alone.

Tell the user if you made this change.

---

## 3. Run coverage and parse the report

Run:

```bash
npm run test:coverage
```

Then read the JSON report at `coverage/coverage-final.json`.

**Structure of coverage-final.json:**

Each top-level key is an absolute file path. Each value has:

```json
{
  "s": { "0": 1, "1": 0, ... },   // statement hit counts
  "b": { "0": [1, 0], ... },       // branch hit counts (array per branch point)
  "f": { "0": 1, "1": 0, ... }    // function hit counts
}
```

A file is **incomplete** when **any** value in `s`, any element in any `b` array, or any value in `f` is `0`.

Compute a rough coverage % per file for display:

```
statements_covered / (statements_covered + statements_missed)
```

**Determine the coverage target:**

- Compute the **baseline overall statement coverage**: sum all hit counts across every file's `s` map, divide by total statement count (covered + missed) across all files.
- If the user stated an explicit target (e.g. "reach 80%", "get to 90% coverage"), record it as **target**.
- If no target was stated, set **target = baseline + 5 percentage points** (capped at 100%).

---

## 4. Select files for test generation

**Exclude** any file whose path contains (case-insensitive):

`dto`, `entity`, `model`, `config`, `mapper`, `interface`, `.type.ts`, `index.ts`, `main.ts`, `.module.ts`, `.spec.ts`

**Also exclude** files whose statement coverage is already at or above the **skip threshold** (default: **80%**). This focuses effort on the biggest gaps and avoids spending tokens on files that are already well-covered. Raise the threshold only if the user explicitly requests higher coverage.

**Prioritise** files in this order:

1. `services/` — business logic, highest value
2. `controllers/` — request handling and response mapping
3. `guards/` — permission and authentication logic
4. `interceptors/` — request/response transformation
5. Everything else

Process files in priority order, one at a time.

---

## 5. Analyse each file

Before writing a test, read the source file and understand:

- What dependencies does the class have? (constructor parameters decorated with `@Injectable`, `@InjectRepository`, etc.)
- What are the happy-path flows for each public method?
- What are the error paths? (look for `throw new`, `Promise.reject`, conditional `null` checks that lead to throws)
- What branches exist? (`if/else`, ternary, `switch`, optional chaining `?.`)
- Are there async methods? (`async`/`await`, returned `Observable` or `Promise`)

Check whether a stub file exists for each dependency (e.g. `my.service.stub.ts`) — if so, import and use it rather than creating a fresh mock object.

Map each **uncovered statement, branch, or function** back to specific lines in the source so you target those exact paths.

---

## 6. Generate the test file

### 6.1 Framework selection

| File type | Test approach |
|---|---|
| Service / util | `Test.createTestingModule({ providers: [MyService, ...mocks] })` |
| Controller | `Test.createTestingModule({ controllers: [MyController], providers: [...mocks] })` |
| Guard / interceptor | Instantiate directly with mocked dependencies, call `canActivate` / `intercept` |

Always use `@nestjs/testing`'s `Test.createTestingModule()` for services and controllers — it correctly wires NestJS dependency injection and makes the test representative of real runtime behaviour.

### 6.2 Mandatory best-practice rules

#### Test structure
- Use **`describe/it`** blocks — never bare `test()` at module level.
- **Outer** `describe` names the class: `describe('MyService', () => { ... })`.
- **Inner** `describe` groups by method: `describe('findById', () => { ... })`.
- Each `it` tests exactly **one** logical scenario.
- Test names follow `'should <expected behaviour> when <condition>'`.

#### Arrange-Act-Assert
Every `it` block must have three clearly separated phases marked with inline comments:

```typescript
it('should return the user when id exists', async () => {
  // Arrange
  const userId = 'user-1';
  const mockUser = { id: userId, name: 'Alice' };
  jest.spyOn(userRepository, 'findOne').mockResolvedValue(mockUser);

  // Act
  const result = await service.findById(userId);

  // Assert
  expect(result).toEqual(mockUser);
  expect(userRepository.findOne).toHaveBeenCalledTimes(1);
  expect(userRepository.findOne).toHaveBeenCalledWith({ where: { id: userId } });
});
```

#### Mocking rules
- If a stub file exists for a dependency (`*.stub.ts`), import and use it.
- Otherwise, provide a mock object with `jest.fn()` for each method you need to control:

  ```typescript
  { provide: MyService, useValue: { findById: jest.fn(), save: jest.fn() } }
  ```

- After resolving the module, get the dependency instance via `module.get(MyService)` so you can spy on it.
- Use `jest.spyOn(dep, 'method').mockReturnValue(value)` for synchronous methods.
- Use `jest.spyOn(dep, 'method').mockResolvedValue(value)` for async methods.
- Use `jest.spyOn(dep, 'method').mockRejectedValue(new Error('msg'))` for error paths.
- Always assert interaction: `expect(spy).toHaveBeenCalledTimes(N)` and `expect(spy).toHaveBeenCalledWith(...args)`.
- Call `jest.clearAllMocks()` or use `afterEach(() => jest.clearAllMocks())` to prevent spy state leaking between tests.

#### Independence
- Each `it` sets up its own state — never rely on execution order.
- Use `beforeEach` only for common module setup, never for shared mutable assertion data.
- Reset mocks in `afterEach` (or use `jest.clearAllMocks()` globally in jest config).

#### Coverage breadth
Ensure tests cover:
- **R**ight — the expected happy-path result
- **B**oundary — empty arrays, `null`/`undefined` inputs, zero, empty string
- **E**rror — every `throw new`, every `Promise.reject`, every guard rejection path

### 6.3 Exception testing pattern

```typescript
it('should throw NotFoundException when user is not found', async () => {
  // Arrange
  jest.spyOn(userRepository, 'findOne').mockResolvedValue(null);

  // Act & Assert
  await expect(service.findById('missing-id'))
    .rejects.toThrow(NotFoundException);
});
```

For synchronous throws:

```typescript
it('should throw Error when input is invalid', () => {
  expect(() => service.validate(null)).toThrow('Input must not be null');
});
```

### 6.4 Template — service unit test

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { NotFoundException } from '@nestjs/common';
import { UserService } from './user.service';
import { UserRepository } from './user.repository';

describe('UserService', () => {
  let service: UserService;
  let userRepository: UserRepository;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UserService,
        {
          provide: UserRepository,
          useValue: { findOne: jest.fn(), save: jest.fn() },
        },
      ],
    }).compile();

    service = module.get(UserService);
    userRepository = module.get(UserRepository);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('findById', () => {
    it('should return user when id exists', async () => {
      // Arrange
      const mockUser = { id: 'user-1', name: 'Alice' };
      jest.spyOn(userRepository, 'findOne').mockResolvedValue(mockUser);

      // Act
      const result = await service.findById('user-1');

      // Assert
      expect(result).toEqual(mockUser);
      expect(userRepository.findOne).toHaveBeenCalledTimes(1);
      expect(userRepository.findOne).toHaveBeenCalledWith({ where: { id: 'user-1' } });
    });

    it('should throw NotFoundException when user is not found', async () => {
      // Arrange
      jest.spyOn(userRepository, 'findOne').mockResolvedValue(null);

      // Act & Assert
      await expect(service.findById('missing'))
        .rejects.toThrow(NotFoundException);
    });
  });
});
```

### 6.5 Template — controller unit test

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { AppController } from './app.controller';
import { AppService } from '../services/app.service';

describe('AppController', () => {
  let controller: AppController;
  let appService: AppService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [AppController],
      providers: [
        {
          provide: AppService,
          useValue: { getHello: jest.fn() },
        },
      ],
    }).compile();

    controller = module.get(AppController);
    appService = module.get(AppService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('getHello', () => {
    it('should return message object when called', () => {
      // Arrange
      const message = 'Hello World!';
      const spy = jest.spyOn(appService, 'getHello').mockReturnValue(message);

      // Act
      const result = controller.getHello();

      // Assert
      expect(result).toEqual({ message });
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith();
    });
  });
});
```

### 6.6 Template — guard unit test

```typescript
import { ExecutionContext } from '@nestjs/common';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authService: jest.Mocked<AuthService>;

  beforeEach(() => {
    authService = { validateToken: jest.fn() } as any;
    guard = new AuthGuard(authService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('canActivate', () => {
    it('should return true when token is valid', () => {
      // Arrange
      const mockContext = {
        switchToHttp: () => ({
          getRequest: () => ({ headers: { authorization: 'Bearer valid-token' } }),
        }),
      } as ExecutionContext;
      jest.spyOn(authService, 'validateToken').mockReturnValue(true);

      // Act
      const result = guard.canActivate(mockContext);

      // Assert
      expect(result).toBe(true);
      expect(authService.validateToken).toHaveBeenCalledWith('valid-token');
    });

    it('should return false when token is missing', () => {
      // Arrange
      const mockContext = {
        switchToHttp: () => ({
          getRequest: () => ({ headers: {} }),
        }),
      } as ExecutionContext;

      // Act
      const result = guard.canActivate(mockContext);

      // Assert
      expect(result).toBe(false);
    });
  });
});
```

---

## 7. Write tests to disk

NestJS tests are **co-located** with the source file they test:

```
src/services/app/app.service.ts
→ src/services/app/app.service.spec.ts

src/controllers/app/app.controller.ts
→ src/controllers/app/app.controller.spec.ts
```

Rules:
- Place the `*.spec.ts` file in the **same directory** as the source file.
- Name the file `<source-name>.spec.ts`.
- If a spec file already exists, **append** new `describe`/`it` blocks rather than overwriting. Do not duplicate tests that already exist.
- Check `jest.config.ts` for `moduleNameMapper` path aliases (e.g. `@app/services`) and use those aliases in imports where the example project does — this keeps import paths consistent with the rest of the codebase.

---

## 8. Verify

After writing all test files, re-run:

```bash
npm run test:coverage
```

Read the updated `coverage/coverage-final.json` and report a before/after summary:

| File | Coverage before | Coverage after |
|---|---|---|
| `app.service.ts` | 0% | 100% |
| `user.service.ts` | 34% | 87% |

If any newly written test **fails**, diagnose and fix it before reporting success.

**Check against coverage target:**

1. Compute the new overall statement coverage from the updated `coverage/coverage-final.json`.
2. Show the overall delta: **baseline → current**.
3. If **new coverage ≥ target**: report success and stop.
4. If **new coverage < target** and the user specified an explicit goal:
   - Return to **Step 4**, select the next batch of eligible files (statement coverage below the skip threshold, not yet processed).
   - Repeat the generate → write → verify cycle until the target is reached or no eligible files remain.
   - If no eligible files remain but the target is still not met, inform the user and stop.
5. If **new coverage < target** and no explicit goal was set (increment mode): report the delta achieved and stop — do not loop further.

Common failure causes:

- Path alias not resolved — check `moduleNameMapper` in `jest.config.ts`
- Missing `async/await` on async tests
- Mock not providing all required methods — add missing `jest.fn()` entries to the provider value
- NestJS DI token mismatch — use `module.get(Token)` with the exact token used in `@Inject()`

---

## 9. Best-practice rules (quick reference)

| # | Rule | Why |
|---|---|---|
| 1 | `describe/it` structure — no bare `test()` | Reads as living documentation |
| 2 | Co-locate spec files next to source | Easy to find; matches NestJS CLI convention |
| 3 | `Test.createTestingModule()` for services & controllers | Tests real DI wiring, not just the class in isolation |
| 4 | Stub files > inline `jest.fn()` when stubs exist | Consistency; stubs encode correct shapes |
| 5 | `jest.spyOn` for interaction assertions | Verifies collaborator contracts were fulfilled |
| 6 | `afterEach(() => jest.clearAllMocks())` | Prevents spy state leaking across tests |
| 7 | Arrange-Act-Assert with inline comments | Immediate readability of intent |
| 8 | `should <result> when <condition>` naming | Failures are self-explanatory |
| 9 | Test all exception/rejection paths | RIGHT-BICEP E — completeness |
| 10 | Never call private methods directly | Avoids brittle coupling to implementation |
| 11 | Fix `coverageDirectory` to `'coverage'` before first run | Report appears inside project; readable by tools |
| 12 | Use `moduleNameMapper` aliases in imports | Consistent with the rest of the codebase |
