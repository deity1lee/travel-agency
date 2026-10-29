# Python / FastAPI — Unit Test Generation Reference

## Table of contents

1. [Pre-flight checks](#1-pre-flight-checks)
2. [Configure coverage tooling if missing](#2-configure-coverage-tooling-if-missing)
3. [Run coverage and parse the report](#3-run-coverage-and-parse-the-report)
4. [Select files for test generation](#4-select-files-for-test-generation)
5. [Analyse each file](#5-analyse-each-file)
6. [Generate the test file](#6-generate-the-test-file)
7. [Write tests to disk](#7-write-tests-to-disk)
8. [Verify](#8-verify)
9. [Best-practice rules](#9-best-practice-rules)

---

## 1. Pre-flight checks

Check for a virtual environment directory (`.venv`, `venv`, or `env`) at the project root.

If a virtual environment exists, activate it and install dependencies:

```bash
source ./.venv/Scripts/activate   # Windows / bash
# or: source ./.venv/bin/activate  # Linux / macOS
pip install -r requirements.txt
pip install -r test-requirements.txt
```

If no virtual environment exists, create one first:

```bash
python -m venv .venv
source ./.venv/Scripts/activate
pip install -r requirements.txt
pip install -r test-requirements.txt
```

After installation, verify the project is importable — run `python -c "import app"` (or the main entry point).

- If the import **fails**: stop immediately and tell the user. Do not generate tests against broken code.
- If the import **succeeds**: continue.

---

## 2. Configure coverage tooling if missing

### 2.1 Install coverage

```bash
pip install coverage httpx
```

### 2.2 Ensure `test-requirements.txt` exists

Check for `test-requirements.txt` at the project root. If it is missing or does not contain `coverage`, `pytest`, and `pytest-mock`, create or update it to include at minimum:

```
pytest
pytest-asyncio
pytest-mock
coverage
httpx
```

If AWS services are used (look for `boto3`, `dynamodb`, `s3`, `sqs` in the source code), also add:

```
moto[all]
boto3
```

### 2.3 Ensure `.coveragerc` exists

Check for `.coveragerc` at the project root. If it is missing, create it:

```ini
[run]
source = .
omit =
    */tests/*
    */test_*
    */__pycache__/*
    */venv/*
    */.venv/*
    */test_venv/*
    setup.py

[report]
skip_empty = False
```

This ensures files with 0% coverage still appear in the report.

### 2.4 Ensure `tests/` directory and `conftest.py` exist

Check for the `tests/` directory. If it is missing, create it along with an empty `__init__.py`:

```bash
mkdir -p tests
touch tests/__init__.py
```

Check for `tests/conftest.py`. If it is missing, create a minimal one to configure `pytest-asyncio`:

```python
import pytest

# Allows all async test functions to run without per-test @pytest.mark.asyncio
pytest_plugins = ["pytest_asyncio"]
```

---

## 3. Run coverage and parse the report

Run:

```bash
coverage run -m pytest
coverage json -o coverage.json
```

Then read `coverage.json`.

**Structure of `coverage.json`:**

```json
{
  "meta": { "version": "7.x.x", ... },
  "files": {
    "services/object_service.py": {
      "executed_lines": [1, 3, 4, ...],
      "missing_lines": [28, 35, 42],
      "excluded_lines": [],
      "summary": {
        "covered_lines": 15,
        "num_statements": 20,
        "percent_covered": 75.0,
        "missing_lines": 5
      }
    }
  },
  "totals": {
    "covered_lines": 45,
    "num_statements": 60,
    "percent_covered": 75.0
  }
}
```

A file is **incomplete** when `summary.missing_lines > 0` (i.e., `percent_covered < 100`).

**Determine the coverage target:**

- Read `totals.percent_covered` as the **baseline overall coverage**.
- If the user stated an explicit target (e.g. "reach 80%", "get to 90% coverage"), record it as **target**.
- If no target was stated, set **target = baseline + 5 percentage points** (capped at 100%).
- The project prompt recommends ~70–75% as a practical minimum; default target should not be set below 70%.

---

## 4. Select files for test generation

**Exclude** any file whose path or name contains (case-insensitive):

`router`, `model`, `entity`, `mapper`, `config`, `constant`, `exception`, `logger`, `__init__`, `main.py`, `app.py`, `setup.py`

**Also exclude** files that are already at 100% coverage (`summary.missing_lines == 0`).

**Prioritise** files in this order:

1. `services/` — business logic, highest value
2. `repositories/` — data-access layer
3. `schemas/` or `validators/` — complex validation and transformation logic
4. Utility / helper modules
5. Everything else not excluded

Process files in priority order, one at a time.

---

## 5. Analyse each file

Before writing a test, read the source file and understand:

- What dependencies does the class have? (constructor parameters, `Depends(...)`, injected repositories)
- What are the happy-path flows for each public method?
- What are the error paths? (look for `raise HTTPException`, `raise ValueError`, conditional `if not …:` guards)
- What branches exist? (`if/else`, ternary, `match/case`)
- Are methods `async`? (any `async def`)
- Are AWS services involved? (`boto3`, DynamoDB, S3, SQS calls)

Map each **missing line** (from `summary.missing_lines` in `coverage.json`) back to specific lines in the source so you target those exact paths.

---

## 6. Generate the test file

### 6.1 Framework and tooling

| Scenario | Approach |
|---|---|
| Service / util (no FastAPI context) | Plain pytest + `unittest.mock.AsyncMock` / `MagicMock` |
| Service with AWS calls | pytest + `moto` decorators to mock AWS, or `unittest.mock.patch` |
| Repository (in-memory / async) | Plain pytest, instantiate directly, `pytest.mark.asyncio` |
| Schemas / validators | Plain pytest, call functions directly with sample data |

**Do NOT test:**
- Router / endpoint handlers (HTTP layer)
- Logger calls
- Constants modules
- Custom exception class definitions

### 6.2 Mandatory best-practice rules

#### Async tests
- Mark every async test function with `@pytest.mark.asyncio`.
- Use `pytest-asyncio` in `asyncio_mode = "auto"` (via `pytest.ini` or `pyproject.toml`) **or** mark individually — be consistent with whatever the project already uses.

#### Mock setup
- For repository / service dependencies, create mock objects with `unittest.mock.AsyncMock` for async methods and `MagicMock` for sync methods.
- Inject mocks directly into the class constructor — do not rely on FastAPI's `Depends()` DI in unit tests.
- For AWS calls, use `moto` library decorators (`@mock_aws`) so no real credentials are needed.
- Never use real AWS credentials or real external services.

#### Test structure — Arrange / Act / Assert
Every test function must have three clearly separated phases:

```python
async def test_should_raise_404_when_object_not_found(object_service):
    # Arrange
    object_service.repo.get = AsyncMock(return_value=None)

    # Act / Assert
    with pytest.raises(HTTPException) as exc_info:
        await object_service.get_object("missing-id")

    assert exc_info.value.status_code == 404
    object_service.repo.get.assert_called_once_with("missing-id")
```

#### Test naming
- Use `test_should_<expected_behaviour>_when_<condition>` or `test_<method>_<scenario>`.
- Names must be self-explanatory without reading the body.

#### Independence
- Each test sets up its own state — never rely on execution order.
- Use `@pytest.fixture` for shared setup that is truly common (e.g. constructing the class under test), not for shared mutable assertion data.
- Use `autouse=False` (default) — opt in to fixtures explicitly.

#### Assertions
- Use plain `assert` statements (pytest rewrites them for clear failure messages).
- For exception assertions use `pytest.raises` as a context manager.
- After the call, assert both the **result** and **mock interactions** (`assert_called_once_with`, `assert_called_with`, `call_count`).

#### Coverage breadth (RIGHT-BICEP)
Ensure tests cover:
- **R**ight — the expected happy-path result
- **B**oundary — `None` inputs, empty lists, zero values
- **E**rror — every `raise HTTPException`, every `raise` in the source
- **I**nverse — if there is a `create`, also test `delete` where relevant

### 6.3 Exception testing pattern

```python
import pytest
from unittest.mock import AsyncMock
from fastapi import HTTPException

@pytest.mark.asyncio
async def test_get_object_raises_404_when_not_found(object_service):
    # Arrange
    object_service.repo.get = AsyncMock(return_value=None)

    # Act / Assert
    with pytest.raises(HTTPException) as exc_info:
        await object_service.get_object("nonexistent")

    assert exc_info.value.status_code == 404
    assert "not found" in exc_info.value.detail.lower()
```

### 6.4 AWS mocking pattern

```python
import boto3
import pytest
from moto import mock_aws

@mock_aws
def test_should_store_item_in_dynamodb(my_service):
    # Arrange — moto intercepts all boto3 calls; no real AWS needed
    dynamodb = boto3.resource("dynamodb", region_name="us-east-1")
    table = dynamodb.create_table(
        TableName="MyTable",
        KeySchema=[{"AttributeName": "id", "KeyType": "HASH"}],
        AttributeDefinitions=[{"AttributeName": "id", "AttributeType": "S"}],
        BillingMode="PAY_PER_REQUEST",
    )

    # Act
    my_service.save_item({"id": "item-1", "value": "test"})

    # Assert
    item = table.get_item(Key={"id": "item-1"})["Item"]
    assert item["value"] == "test"
```

### 6.5 Template — service unit test

```python
import pytest
from unittest.mock import AsyncMock, MagicMock
from fastapi import HTTPException
from services.object_service import ObjectService
from entities.object_entity import ObjectEntity
from models.object_models import ObjectCreate
from datetime import datetime


@pytest.fixture
def mock_repo():
    repo = MagicMock()
    repo.get = AsyncMock()
    repo.list = AsyncMock()
    repo.add = AsyncMock()
    repo.update = AsyncMock()
    repo.delete = AsyncMock()
    return repo


@pytest.fixture
def object_service(mock_repo):
    return ObjectService(repo=mock_repo)


@pytest.mark.asyncio
async def test_get_object_returns_object_when_found(object_service, mock_repo):
    # Arrange
    entity = ObjectEntity(id="1", name="Test", data=None, createdAt=datetime.now())
    mock_repo.get.return_value = entity

    # Act
    result = await object_service.get_object("1")

    # Assert
    assert result is not None
    mock_repo.get.assert_called_once_with("1")


@pytest.mark.asyncio
async def test_get_object_raises_404_when_not_found(object_service, mock_repo):
    # Arrange
    mock_repo.get.return_value = None

    # Act / Assert
    with pytest.raises(HTTPException) as exc_info:
        await object_service.get_object("missing")

    assert exc_info.value.status_code == 404
    mock_repo.get.assert_called_once_with("missing")


@pytest.mark.asyncio
async def test_delete_object_raises_404_when_not_found(object_service, mock_repo):
    # Arrange
    mock_repo.delete.return_value = False

    # Act / Assert
    with pytest.raises(HTTPException) as exc_info:
        await object_service.delete_object("ghost")

    assert exc_info.value.status_code == 404


@pytest.mark.asyncio
async def test_list_objects_returns_empty_list_when_no_objects(object_service, mock_repo):
    # Arrange
    mock_repo.list.return_value = []

    # Act
    result = await object_service.list_objects()

    # Assert
    assert result == []
    mock_repo.list.assert_called_once_with(None)
```

### 6.6 Template — repository unit test

```python
import pytest
from repositories.object_repository import ObjectRepository
from entities.object_entity import ObjectEntity
from datetime import datetime


@pytest.fixture
def repo():
    return ObjectRepository()


@pytest.mark.asyncio
async def test_get_returns_none_for_missing_id(repo):
    # Act
    result = await repo.get("nonexistent-id")

    # Assert
    assert result is None


@pytest.mark.asyncio
async def test_add_then_get_returns_same_entity(repo):
    # Arrange
    entity = ObjectEntity(id="new-1", name="New Item", data=None, createdAt=datetime.now())

    # Act
    await repo.add(entity)
    result = await repo.get("new-1")

    # Assert
    assert result is not None
    assert result.id == "new-1"
    assert result.name == "New Item"


@pytest.mark.asyncio
async def test_delete_returns_false_for_missing_id(repo):
    # Act
    result = await repo.delete("does-not-exist")

    # Assert
    assert result is False
```

---

## 7. Write tests to disk

Tests are placed in the `tests/` directory, mirroring the source module path:

```
services/object_service.py       → tests/test_object_service.py
repositories/object_repository.py → tests/test_object_repository.py
services/user_service.py         → tests/test_user_service.py
```

Rules:
- Name the file `test_<module_name>.py`.
- If a test file already exists, **append** new test functions to it rather than overwriting. Do not duplicate functions that already exist.
- Ensure `tests/__init__.py` exists (even if empty) so pytest discovers the directory.

---

## 8. Verify

After writing all test files, re-run:

```bash
coverage run -m pytest
coverage json -o coverage.json
```

Read the updated `coverage.json` and report a before/after summary:

| File | Coverage before | Coverage after |
|---|---|---|
| `services/object_service.py` | 0% | 88% |
| `services/user_service.py` | 0% | 90% |

If any newly written test **fails**, diagnose and fix it before reporting success.

**Check against coverage target:**

1. Read `totals.percent_covered` from the updated `coverage.json`.
2. Show the overall delta: **baseline → current**.
3. If **new coverage ≥ target**: report success and stop.
4. If **new coverage < target** and the user specified an explicit goal:
   - Return to **Step 4**, select the next batch of eligible files (missing_lines > 0, not yet processed).
   - Repeat the generate → write → verify cycle until the target is reached or no eligible files remain.
   - If no eligible files remain but the target is still not met, inform the user and stop.
5. If **new coverage < target** and no explicit goal was set (increment mode): report the delta achieved and stop — do not loop further.

Common failure causes:

- `ModuleNotFoundError` — add missing package to `test-requirements.txt` and `pip install` it
- `RuntimeError: no running event loop` — add `@pytest.mark.asyncio` to the test or use `asyncio_mode = "auto"` in config
- `AttributeError` on `AsyncMock` — use `unittest.mock.AsyncMock` (Python 3.8+); for older Python use `asynctest` library
- `boto3` calls reaching real AWS — ensure `@mock_aws` (moto) decorator is applied before `boto3` client/resource creation
- Import error for project modules — check that virtual environment is active and `pip install -r requirements.txt` has been run

---

## 9. Best-practice rules (quick reference)

| # | Rule | Why |
|---|---|---|
| 1 | pytest only — no `unittest.TestCase` classes | Modern pytest fixtures and parametrize are more concise |
| 2 | `@pytest.mark.asyncio` on every `async` test | Required for async test execution |
| 3 | `AsyncMock` for async dependencies | Prevents `coroutine was never awaited` errors |
| 4 | Arrange-Act-Assert with inline comments | Immediate readability of intent |
| 5 | `test_should_<result>_when_<condition>` naming | Failures are self-explanatory |
| 6 | `pytest.raises` for exception assertions | Readable, catches only the expected exception |
| 7 | Assert mock interactions after the call | Verifies collaborator contracts were fulfilled |
| 8 | `@pytest.fixture` for repeated setup | Keeps tests DRY without shared mutable state |
| 9 | Never use real AWS credentials | Tests must be portable and CI-safe |
| 10 | `moto` for AWS service mocking | Simulates full AWS API without network calls |
| 11 | Skip routers, loggers, constants, exceptions | Business logic is the highest-value target |
| 12 | Tests self-contained and order-independent | Reliable in parallel execution |
