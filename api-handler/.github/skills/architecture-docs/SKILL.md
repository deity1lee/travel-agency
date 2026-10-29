---
name: architecture-docs
description: Use this skill when creating, updating, or maintaining ARCHITECTURE.md files, when users ask about "my architecture documentation" or "architecture", when generating diagrams from architecture documentation, when validating/checking/auditing architecture (including BIAN alignment, META layers, standards compliance), or when answering questions about documented components, data structures, integrations, security, performance, deployment, technology stack, or architectural decisions
---

# Architecture Documentation Skill

This skill provides comprehensive guidelines for creating and maintaining ARCHITECTURE.md files using the standardized template from ARCHITECTURE_DOCUMENTATION_GUIDE.md. It enforces consistency across all documentation sections through the **Foundational Context Anchor Protocol** — a dependency-aware editing workflow that loads required upstream context before any downstream section edit, requires source attribution for derived claims, and detects downstream impact when any section changes.

## When This Skill is Invoked

Automatically activate when:
- User asks to create architecture documentation
- User asks to update or edit ARCHITECTURE.md
- User mentions documenting system architecture
- User requests architecture review, audit, or analysis (triggers Design Drivers calculation prompt)
- User explicitly asks to "calculate design drivers" or "update design drivers"
- User asks about architecture documentation structure or best practices
- User edits Section 1 Executive Summary Key Metrics (triggers metric consistency check)
- User edits any downstream section (S4–S11) → triggers Context Anchor load
- User requests metric consistency check, verify metrics, or audit metrics
- **User asks informational questions about the documented architecture** (if ARCHITECTURE.md exists)
  - "What is our [authentication/scaling/data flow/etc.] approach?"
  - "How does [component/system/integration] work?"
  - "What technologies do we use for [purpose]?"
  - "Tell me about the architecture of [system]"
- **User asks to generate, create, or add diagrams to architecture documentation** (triggers Workflow 8)
  - "Generate my architecture diagrams"
  - "Create Mermaid diagrams from ARCHITECTURE.md"
  - "Add diagrams to my architecture"

### Query Pattern Triggers

This skill automatically activates when users ask questions about documented architecture, including:

**Reference Patterns**:
- "According to my architecture documentation..."
- "Based on the architecture..."
- "What does the architecture use/require/implement for..."
- "My architecture documentation shows/says..."
- "The architecture specifies/defines..."
- "Check/Validate/Verify the architecture [aspect]..."
- "Audit the [architecture component/pattern]..."

**Technical Query Keywords**:
- **Components**: "components", "services", "modules", "microservices", "systems"
- **Data**: "data structures", "data flow", "database", "schema", "models", "entities"
- **Integration**: "APIs", "integrations", "external systems", "endpoints", "interfaces"
- **Security**: "authentication", "authorization", "encryption", "security", "compliance"
- **Performance**: "scaling", "performance", "SLA", "capacity", "throughput", "latency"
- **Deployment**: "deployment", "cloud provider", "infrastructure", "environments", "regions"
- **Technology**: "tech stack", "languages", "frameworks", "tools", "libraries", "versions"
- **Decisions**: "why choose", "decision", "trade-offs", "alternatives", "ADR", "rationale"
- **Validation**: "check", "validate", "verify", "audit", "alignment", "BIAN", "META", "service domain", "layer", "standards", "compliance check"

**Multi-section Queries**:
- Questions requiring synthesis across multiple sections
- Cross-cutting concerns (e.g., "How does authentication work with external systems?")
- Implementation details spanning components, data, and deployment

---

## 🎯 AUTOMATIC WORKFLOW DETECTION

**IMPORTANT**: Immediately upon skill invocation, analyze the user's request to detect their intent.

### Detection Logic

Check the user's original message (before `/architecture-docs` was invoked) for these patterns:

#### Workflow 8: Diagram Generation
**Triggers:**
- Keywords: "generate", "create", "add", "update", "make" + "diagram", "diagrams", "Mermaid diagram", "architecture diagram"
- Examples: "generate my architecture diagrams", "create diagrams from ARCHITECTURE.md", "add diagrams to my architecture"
- Section-specific: "generate diagrams for Section 4", "create data flow diagrams"
- Format mentions: "Mermaid diagrams", "visual diagrams", "architecture diagrams"
- Reconciliation: "reconcile diagrams", "move diagrams", "consolidate diagrams"
- Audit/coverage: "check diagram coverage", "audit diagrams", "diagram completeness", "diagram audit"
- Placement: "diagrams in wrong location", "fix diagram placement", "diagram location"
- External intake: user provides external file path and mentions diagrams

**Action when detected:**
1. Confirm: "I'll help you generate architecture diagrams."
2. Jump directly to **Workflow 8, Step 1** (Diagram Type Selection)
3. Do NOT ask which workflow - proceed automatically

#### Workflow 9: Migrate to docs/ Structure
**Triggers:**
- Keywords: "migrate", "restructure", "split", "reorganize", "convert" + "architecture", "ARCHITECTURE.md"
- Size complaints: "too large", "too long", "hard to navigate", "split into files"
- Explicit: "migrate my architecture to the new structure", "convert to docs/ layout"

**Action when detected:**
1. Confirm: "I'll help you migrate ARCHITECTURE.md to the multi-file docs/ structure."
2. Jump directly to **Workflow 9, Step 1**
3. Do NOT ask which workflow - proceed automatically

#### Other Workflows
If the user's request matches other documented workflows (1-9), follow their respective trigger patterns.

**Note**: Workflow 1 (new ARCHITECTURE.md creation) starts at Step 0 (PO Spec prerequisite check) before Step 1 (type selection).

### If No Pattern Matches

If the user's request doesn't match any workflow triggers:
1. Acknowledge the skill invocation
2. Ask which workflow they want to use
3. Provide brief description of available workflows

---

## File Naming Convention

**IMPORTANT**: All architecture documentation uses the multi-file `docs/` structure.

- **`ARCHITECTURE.md`** at the project root is the **navigation index only** (~130 lines)
- All section content lives under **`docs/`** as numbered Markdown files
- Component details (Section 5) live under **`docs/components/`** — one file per component

**File naming pattern**: `NN-kebab-case-section-name.md` (e.g., `01-system-overview.md`)

See **RESTRUCTURING_GUIDE.md** for the full directory structure and naming conventions.

### Location
- `ARCHITECTURE.md` — always at the **project root**
- `docs/` — always at the **project root** (sibling to `ARCHITECTURE.md`)
- `docs/components/` — inside `docs/`
- For multi-project repositories, each project subdirectory gets its own `ARCHITECTURE.md` + `docs/`

---

## Working with Architecture Documentation — Context Optimization

**IMPORTANT**: The multi-file structure makes context loading simple — individual `docs/` files are 50–400 lines each (full file fits in context). No line-offset tricks needed.

### Context-Efficient Workflow

1. **Find the target section**
   - Read `ARCHITECTURE.md` navigation table to identify which `docs/NN-name.md` file contains the target section
   - Example: "Edit security architecture" → navigate to `docs/07-security-architecture.md`

2. **Load Context Anchor** *(REQUIRED for downstream sections)*
   - **SKIP** this step when editing `docs/01-system-overview.md`, `docs/02-architecture-principles.md`, or for typo/formatting-only fixes
   - **REQUIRED** when editing any file from `docs/03-architecture-layers.md` through `docs/09-operational-considerations.md`, or any `docs/components/*.md` file
   - **Universal Foundation**: Always load `docs/01-system-overview.md` + `docs/02-architecture-principles.md`
   - **Relevant ADRs**: Match ADR titles from `ARCHITECTURE.md` navigation table against target section keywords; load matched ADRs
   - **Section-Specific Parents**: Load parent sections per the Foundational Context Anchor Protocol dependency table (see below)
   - Example for S9 (Security): load S1+2 (foundation) + S5/README (components) + S7 (integrations) + S8 (tech stack) + relevant ADRs
   - Context budget: 250–850 lines depending on section tier

3. **Read the entire target file**
   - Individual `docs/` files are small enough to read in full
   - `Read(file_path="docs/07-security-architecture.md")`  — no offset/limit needed

4. **Edit the target file directly**
   - Use the Edit tool on the specific `docs/NN-name.md` file
   - **Do NOT edit ARCHITECTURE.md** unless you are adding a new section/file to the navigation table
   - **Source Attribution** *(during editing)*: When writing derived content in downstream sections, insert cross-reference links to the source:
     - **Metrics**: When repeating a metric from S1 Key Metrics → `(see [Key Metrics](01-system-overview.md#key-metrics))`
     - **ADR decisions**: When content implements an ADR → `per [ADR-NNN](../adr/ADR-NNN-title.md)`
     - **Principles**: When invoking an S3 principle → `per [Principle Name](02-architecture-principles.md#anchor)`
     - **Parent section references**: When referencing components, layers, integrations, or tech → link to the specific file
     - **Unverifiable claims**: If a specific claim (metric, decision, constraint) cannot be traced to an existing section, user input, or ADR → insert `<!-- TODO: Add source reference -->` marker

5. **Post-Write Alignment & Traceability Audit**
   - **Check A — Principle traceability**: Written content does not contradict Section 3 principles
   - **Check B — Metric consistency**: Numeric values match Section 1 Key Metrics
   - **Check C — ADR alignment**: Content does not contradict loaded ADR decisions
   - **Check D — Parent section alignment**: Content references valid components (S5), integrations (S7), tech (S8) as loaded
   - **Check E — Source citation audit**: Scan the written content for:
     - Numeric values (TPS, latency, SLO, %) → must link to S1 Key Metrics or be marked as section-local
     - Technology names matching S8 → should link to tech stack or governing ADR
     - Architecture pattern references → should link to S3 or S4
     - `<!-- TODO: Add source reference -->` markers → count and report
   - **Silent pass** if no issues found; display alignment report **only when misalignment or missing citations are detected**

6. **Verification**
   - After edits, re-read the modified `docs/` file to verify changes
   - Use Grep to search for specific content without loading multiple files

### Discovering Available Files

When the target section is not obvious, read `ARCHITECTURE.md` first:

```python
# Step 1: Read navigation table
nav_content = Read(file_path="ARCHITECTURE.md")

# Step 2: Parse the Documentation table to find the target file
# Example row: | 8 | Security Architecture | [docs/07-security-architecture.md](docs/07-security-architecture.md) | ... |

# Step 3: Read that specific docs/ file in full
target_content = Read(file_path="docs/07-security-architecture.md")
```

### Updating the Navigation Index

Update `ARCHITECTURE.md` only when:
- A new section file is added to `docs/`
- A new component file is added to `docs/components/`
- A file is renamed or removed

**Do NOT update ARCHITECTURE.md** for content edits within existing `docs/` files.

---

## Architecture Type Selection Workflow

**PURPOSE**: When creating a new ARCHITECTURE.md, prompt the user to select their architecture type. This determines the structure and content of Section 4 (Meta Architecture) and Section 5 (Component Details).

### When to Trigger

**Activate this workflow when:**
- ✅ User asks to create a NEW ARCHITECTURE.md document
- ✅ User explicitly requests to "change architecture type" or "select architecture type"
- ✅ User is updating an existing ARCHITECTURE.md and mentions changing from one architecture type to another

**Prerequisite**: Step 0 (PO Spec check) runs before Step 1 for all new document creation triggers

**Skip this workflow when:**
- ❌ Editing an existing ARCHITECTURE.md (type already selected)
- ❌ User is only updating specific sections unrelated to architecture type
- ❌ Document type is already clear from context

### Available Architecture Types

1. **Microservices Architecture (Recommended)** - Cloud-native distributed systems with independent services
2. **META Architecture** - 6-layer enterprise model (Channels → UX → Business Scenarios → Business → Domain → Core) with [BIAN V12.0](https://bian.org/servicelandscape-12-0-0/views/view_51891.html) alignment for Layer 5 Domain service domains
3. **3-Tier Architecture** - Classic web application pattern (Presentation → Application/Business Logic → Data)
4. **N-Layer Architecture** - Customizable patterns (DDD, Clean Architecture, Hexagonal)
5. **BIAN Architecture** - 5-layer BIAN-compliant model (Channels → BIAN Business Scenarios → BIAN Business Capabilities → BIAN Service Domains → Core Systems) with full [BIAN V12.0](https://bian.org/servicelandscape-12-0-0/views/view_51891.html) compliance across layers 2-4

**BIAN Standard for META**: BIAN V12.0 is the default and recommended version for META architecture. Use the [BIAN Service Landscape V12.0](https://bian.org/servicelandscape-12-0-0/views/view_51891.html) to identify and define service domains for Layer 5 (Domain).

### Workflow Steps

#### Step 0: Product Owner Specification Prerequisite Check

Before proceeding with new ARCHITECTURE.md creation, verify that a Product Owner Specification exists.

**Detection procedure** (ordered — stop at first match):
1. Check for `PRODUCT_OWNER_SPEC.md` at project root
2. Check for `PO_SPEC.md` at project root
3. Glob search: `**/PRODUCT_OWNER_SPEC*.md`, `**/PO_SPEC*.md`
4. Grep fallback: search for header pattern `# .* - Product Owner Specification`

---

**If PO Spec FOUND → Gate passes:**

- Display: `✅ Product Owner Specification found: [filename]`
- Show scoring reminder: "If you haven't evaluated it yet, consider running `/skill architecture-readiness` to score it (recommended threshold: ≥7.5/10)."
- Load high-weight PO Spec sections into context for pre-populating ARCHITECTURE.md:
  - Section 1: Business Context
  - Section 3: Business Objectives
  - Section 4: Use Cases
  - Section 7: Business Constraints
- Record traceability metadata (embed in `docs/01-system-overview.md` during Step 5):
  ```
  <!-- PO_SPEC_SOURCE: [filename] (loaded YYYY-MM-DD) -->
  ```
- Proceed to Step 1

---

**If PO Spec NOT FOUND → Present 3-option menu:**

Display the following message:

```
🚫 No Product Owner Specification Found

The three-phase workflow requires business context before architecture design:

  Phase 1 → Product Owner Specification  ← MISSING
  Phase 2 → ARCHITECTURE.md              ← You are here
  Phase 3 → Compliance Contracts

How would you like to proceed?

  1️⃣  Run the full elicitation flow
      → Launches /skill architecture-readiness to create a complete PO Spec
      → Recommended for new projects with no documented requirements

  2️⃣  Provide business context now
      → Type your business context here and I'll evaluate it against the
        PO Spec scoring rubric, then interview you for any gaps
      → Best when you have context in mind but no formal document yet

  3️⃣  Skip (SKIP PO SPEC)
      → Proceed without any business context
      → A PO_SPEC_GATE: SKIPPED warning will be recorded

Enter 1, 2, or 3:
```

Wait for user response before proceeding.

---

**Option 1 — Full elicitation flow:**

- Invoke `/skill architecture-readiness`
- After the PO Spec is created, re-run Step 0 detection (the new file should now be found)
- Proceed with the standard "PO Spec FOUND" path

---

**Option 2 — Provide business context now (inline elicitation):**

**Step 0.2a: Capture initial context**

Prompt the user:

```
Please describe your business context — the problem you're solving, who it's for,
key objectives, constraints, and any other business requirements you have in mind.
```

Wait for user input.

**Step 0.2b: Evaluate against PO Spec scoring rubric**

- Load `skills/architecture-readiness/PO_SPEC_SCORING_GUIDE.md`
- Map the user's input to the 8 PO Spec sections
- Score each section using the weighted rubric:
  - Section 4: Use Cases (weight 2.5) — HIGH priority
  - Section 7: Business Constraints (weight 2.0) — HIGH priority
  - Section 3: Business Objectives (weight 1.5) — HIGH priority
  - Section 1: Business Context (weight 1.0)
  - Section 6: UX Requirements (weight 1.0)
  - Section 8: Success Metrics (weight 1.0)
  - Section 2: Stakeholders & Users (weight 0.5)
  - Section 5: User Stories (weight 0.5)
- Display the evaluation results:
  ```
  📊 Business Context Evaluation (scored against PO Spec rubric)

  Section                    | Score | Weight | Weighted
  ---------------------------|-------|--------|--------
  1. Business Context        | X%    | 1.0    | X.XX
  2. Stakeholders & Users    | X%    | 0.5    | X.XX
  3. Business Objectives     | X%    | 1.5    | X.XX
  4. Use Cases               | X%    | 2.5    | X.XX
  5. User Stories            | X%    | 0.5    | X.XX
  6. UX Requirements         | X%    | 1.0    | X.XX
  7. Business Constraints    | X%    | 2.0    | X.XX
  8. Success Metrics & KPIs  | X%    | 1.0    | X.XX
  ─────────────────────────────────────────────
  Total                                        X.XX/10.0
  ```

**Step 0.2c: Interview for gaps (if score < 7.5)**

- Identify sections scoring below 50% (Critical Gaps) and 50–74% (Moderate Gaps)
- Prioritize by weight: ask about HIGH-priority gaps first (Use Cases → Business Constraints → Business Objectives)
- For each gap, ask targeted questions derived from that section's evaluation criteria in the scoring guide
- After each answer, re-score the affected section
- Continue until score reaches ≥7.5 or all HIGH-priority sections are ≥75%
- If score still < 7.5 after addressing HIGH-priority sections, ask about MEDIUM-priority gaps
- Display updated scorecard after the interview round

**Step 0.2d: Generate inline PO Spec and proceed**

- Compile all gathered context into a structured PO Spec document
- Save as `PRODUCT_OWNER_SPEC.md` at project root (using the template from `skills/architecture-readiness/templates/PO_SPEC_TEMPLATE.md`)
- Display: `✅ Product Owner Specification created: PRODUCT_OWNER_SPEC.md (score: X.X/10.0)`
- Load high-weight sections (1, 3, 4, 7) into context for pre-populating ARCHITECTURE.md
- Record traceability (embed in `docs/01-system-overview.md` during Step 5):
  ```
  <!-- PO_SPEC_SOURCE: PRODUCT_OWNER_SPEC.md (inline-generated YYYY-MM-DD, score: X.X/10.0) -->
  ```
- Proceed to Step 1

---

**Option 3 — Skip:**

Display the following warning and proceed:

```
⚠️  PO Spec gate overridden. Proceeding without a Product Owner Specification.

   Consequences to be aware of:
   - Architecture decisions will not be traceable to validated business requirements
   - The compliance phase may flag gaps due to missing business context
   - A PO_SPEC_GATE: SKIPPED note will be embedded in the generated documentation

Proceeding to architecture type selection...
```

Record override metadata (embed in `docs/01-system-overview.md` during Step 5):
```
<!-- PO_SPEC_GATE: SKIPPED (user override at YYYY-MM-DD) -->
```

No PO Spec context is available for section pre-population — proceed to Step 1 without it.

---

**If multiple PO Spec files found:**

List all matches and ask the user to select which one applies before proceeding:

```
📋 Multiple Product Owner Specifications found. Which one applies to this architecture?

  1. [path/to/first/PRODUCT_OWNER_SPEC.md]
  2. [path/to/second/PO_SPEC.md]
  ...

Please enter the number of the specification to use.
```

---

#### Step 1: Present Architecture Type Options

When creating a new ARCHITECTURE.md, present the user with architecture type selection:

```markdown
📐 **Architecture Type Selection**

Before creating your ARCHITECTURE.md, please select the architecture type that best describes your system:

**1. Microservices Architecture (Recommended)** (Cloud-Native Distributed)
   - Best for: Cloud-native systems, independently deployable services, modern applications
   - Components: API Gateway → Services → Data Stores → Event Bus
   - Complexity: High
   - Team Size: Large (10+)
   - Why Recommended: Industry standard for scalable, resilient modern applications

**2. META Architecture** (6-Layer Enterprise)
   - Best for: Large enterprise systems, financial services, complex integrations
   - Layers: Channels → UX → Business Scenarios → Integration → Domain → Core
   - Complexity: Very High
   - Team Size: Large (10+)

**3. 3-Tier Architecture** (Classic Web Application)
   - Best for: Web apps, REST APIs, standard CRUD systems
   - Tiers: Presentation → Application/Business Logic → Data
   - Complexity: Low
   - Team Size: Small-Medium (2-8)

**4. N-Layer Architecture** (Customizable Patterns)
   - Best for: DDD, Clean Architecture, Hexagonal Architecture
   - Patterns: 4-Layer DDD, 5-Layer Extended, Clean Architecture, Hexagonal
   - Complexity: Medium-High
   - Team Size: Medium (4-10)

**5. BIAN Architecture** (5-Layer BIAN-Compliant)
   - Best for: Banking systems requiring BIAN V12.0 certification, full BIAN compliance
   - Layers: Channels → BIAN Business Scenarios → BIAN Business Capabilities → BIAN Service Domains → Core Systems
   - Complexity: Very High
   - Team Size: Large (10+)
   - BIAN Compliance: Full BIAN V12.0 across layers 2-4

For detailed comparison and decision guidance, refer to: `templates/ARCHITECTURE_TYPE_SELECTOR.md`

**Which architecture type best describes your system? (1-5, or type name)**

Note: Option 1 (Microservices) is recommended for most modern cloud-native applications.
```

#### Step 2: Capture User Selection

Wait for user response (1, 2, 3, 4, or 5) or architecture type name.

**Valid inputs:**
- Numeric: `1`, `2`, `3`, `4`, `5`
- Type names: `Microservices`, `META`, `3-Tier`, `N-Layer`, `BIAN`
- Variations: `microservices`, `meta`, `three-tier`, `n-layer`, `bian`

**If user is unsure:**
- Offer to load `templates/ARCHITECTURE_TYPE_SELECTOR.md` decision guide
- Provide quick decision tree questions
- Default to **Microservices (Recommended)** for modern cloud-native systems
- Default to **META** if user has enterprise requirements and regulatory compliance needs
- Default to **3-Tier** if user wants maximum simplicity and minimal operational complexity

#### Step 3: Load Type-Specific Templates

Based on user selection, load the appropriate templates:

| Selection | Section 4 Template | Section 5 Template |
|-----------|-------------------|-------------------|
| Microservices (1) | `templates/SECTION_4_MICROSERVICES.md` | `templates/SECTION_5_MICROSERVICES.md` |
| META (2) | `templates/SECTION_4_META.md` | `templates/SECTION_5_META.md` |
| 3-Tier (3) | `templates/SECTION_4_3TIER.md` | `templates/SECTION_5_3TIER.md` |
| N-Layer (4) | `templates/SECTION_4_NLAYER_PATTERNS.md` | *(Use generic component template)* |
| BIAN (5) | `templates/SECTION_4_BIAN.md` | `templates/SECTION_5_BIAN.md` |

**Loading Process:**
1. Read the appropriate Section 4 template file
2. Read the appropriate Section 5 template file (if exists)
3. Extract template content (excluding metadata comments)
4. Prepare for insertion into ARCHITECTURE.md at correct section boundaries

#### Step 4: Add Architecture Type Metadata

When creating the ARCHITECTURE.md, add an HTML comment metadata tag at the beginning of Section 4 to track the selected architecture type:

```html
<!-- ARCHITECTURE_TYPE: META -->
```

**Valid metadata values:**
- `<!-- ARCHITECTURE_TYPE: META -->`
- `<!-- ARCHITECTURE_TYPE: 3-TIER -->`
- `<!-- ARCHITECTURE_TYPE: MICROSERVICES -->`
- `<!-- ARCHITECTURE_TYPE: N-LAYER -->`
- `<!-- ARCHITECTURE_TYPE: BIAN -->`

**Purpose:**
- Enables type detection for future edits
- Used by validation rules
- Used by Design Drivers calculation
- Used by architecture compliance skill

#### Step 5: Create Multi-File Architecture Documentation

Instead of creating a single `ARCHITECTURE.md`, create the full multi-file `docs/` structure:

**Creation Order:**
1. Create `docs/` directory
2. Create `docs/components/` directory
3. Write each section to its corresponding `docs/NN-name.md` file (see RESTRUCTURING_GUIDE.md for file mapping):
   - `docs/01-system-overview.md` — Sections 1+2 (Executive Summary + System Overview)
     - Include PO Spec traceability metadata at the top of this file:
       - `<!-- PO_SPEC_SOURCE: [filename] (loaded YYYY-MM-DD) -->` if a PO Spec was found and used
       - `<!-- PO_SPEC_GATE: SKIPPED (user override at YYYY-MM-DD) -->` if the gate was overridden
   - `docs/02-architecture-principles.md` — Section 3 (Architecture Principles)
   - `docs/03-architecture-layers.md` — Section 4 (Architecture Layers, type-specific template)
   - `docs/04-data-flow-patterns.md` — Section 6 (Data Flow Patterns)
   - `docs/05-integration-points.md` — Section 7 (Integration Points)
   - `docs/06-technology-stack.md` — Section 8 (Technology Stack)
   - `docs/07-security-architecture.md` — Section 9 (Security Architecture)
   - `docs/08-scalability-and-performance.md` — Section 10 (Scalability & Performance)
   - `docs/09-operational-considerations.md` — Section 11 (Operational Considerations)
   - `docs/10-references.md` — References
4. **Section 5 — Create Individual Component Files** (MANDATORY)

   Every component identified during architecture design MUST get its own file. Do NOT write Section 5 content into a single monolithic file.

   **4a.** Identify all components from the architecture discussion (services, modules, infrastructure components, etc.)
   **4b.** For each component, create `docs/components/NN-<component-name>.md`:
      - `NN` = two-digit zero-padded index (01, 02, 03, ...)
      - Name = lowercase kebab-case (e.g., `01-api-gateway.md`, `02-order-service.md`)
      - Add breadcrumb: `[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > <Component Name>`
      - Add `# <Component Name>` heading
      - Add `**Type:** <type>` field (e.g., Microservice, Infrastructure, Module, Service Domain)
      - Fill in component details using the type-specific Section 5 template (loaded in Step 3)
      - Use placeholder values (e.g., `[To be defined]`) for fields the user hasn't specified yet
   **4c.** Create `docs/components/README.md` component index using the architecture-component-guardian format:
      - Line 1: `<!-- managed by solutions-architect-skills:architecture-component-guardian -- do not edit manually -->`
      - Breadcrumb: `[Architecture](../../ARCHITECTURE.md) > Components`
      - 4-column table: `#`, `Component`, `File`, `Type`
      - One row per component file created in 4b
   **4d.** Verify: count of component files created must match count of rows in README.md table

   > **HARD GATE**: Do NOT proceed to Step 5 (ARCHITECTURE.md index) until at least one component file exists in `docs/components/`. If no components were identified, ask the user to define at least the primary components before continuing.

5. Write `ARCHITECTURE.md` as the navigation index using the template from RESTRUCTURING_GUIDE.md
6. Add the `<!-- ARCHITECTURE_TYPE: TYPE -->` metadata comment at the top of `docs/03-architecture-layers.md`

> **Note — Context Anchor on Subsequent Edits**: The sequential creation order (S1 → S2 → S3 → ... → S11) inherently satisfies the context anchor during initial documentation creation, since each section is written with prior sections fresh in context. However, the **Foundational Context Anchor Protocol MUST be followed for all subsequent edits** to any downstream section (S4–S11), as the original creation context will no longer be available.

**Include:**
- All template content from the type-specific templates, properly formatted
- Placeholder values in each file for the user to customize
- Breadcrumbs in all `docs/components/NN-name.md` files
- Cross-references as relative Markdown links (never bare `Section X.Y`)
- At least one `docs/components/NN-name.md` file per identified component
- `docs/components/README.md` with 4-column index matching all component files
- Each component file has: breadcrumb, `# Heading`, `**Type:**` field

#### Step 6: Prompt for ADR Generation (Optional)

**Trigger**: Immediately after Step 5 completes successfully (docs/ structure created)

**Objective**: Offer to generate ADR files from the ADR table in the ARCHITECTURE.md navigation index

---

##### Step 6.0: Present ADR Generation Prompt

After ARCHITECTURE.md is successfully created, display the following prompt:

```
✅ Architecture documentation created successfully!

═══════════════════════════════════════════════════════════
📋 Architecture Decision Records (ADRs) Setup
═══════════════════════════════════════════════════════════

Your ARCHITECTURE.md navigation index includes an ADR table with placeholder
entries. I can automatically generate these ADR files using the standard
ADR template.

Would you like me to generate the ADR files now?

Options:
1. [Yes - Generate ADRs] - Create all ADR files listed in the navigation index
2. [Preview First] - Show me which ADRs will be created
3. [No Thanks] - I'll create them manually later
4. [Learn More] - Tell me about ADRs and the template

Recommended: Option 1 (Generate ADRs) - Saves time and ensures consistency
```

**Wait for user response**: `1`, `2`, `3`, `4`, or keywords like "yes", "preview", "no", "learn more"

---

##### Step 6.1: Handle User Selection

**If user selects Option 1 (Yes - Generate ADRs)**:
- Proceed to Step 6.2 (Locate ADR Table)

**If user selects Option 2 (Preview First)**:
- Proceed to Step 6.2 (Locate ADR Table)
- After Step 6.3 (Extract ADR List), show preview
- Re-prompt: "Proceed with generation? (yes/no)"
- If yes: Continue to Step 6.4
- If no: Skip to Step 6.8 (Complete)

**If user selects Option 3 (No Thanks)**:
- Skip to Step 6.8 (Complete with skip message)

**If user selects Option 4 (Learn More)**:
- Show ADR information:

```
Architecture Decision Records (ADRs) document important architectural choices,
providing context for future team members and explaining the "why" behind
technical decisions.

**What ADRs Document**:
- Technology selection rationale
- Architecture pattern choices
- Trade-offs and alternatives considered
- Consequences of decisions

**ADR Template Structure**:
- Context: Problem statement and requirements
- Decision: What was decided
- Rationale: Why this choice was made
- Consequences: Positive/negative outcomes
- Alternatives: Options that were considered but rejected

**Template Location**: skills/architecture-docs/adr/ADR-000-template.md
**Guide**: skills/architecture-docs/ADR_GUIDE.md

Would you like me to generate the ADR files now? (yes/no)
```

- Re-prompt with Options 1-3
- Wait for new response

---

##### Step 6.2: Locate ADR Table

The ADR table is in the `ARCHITECTURE.md` navigation index (not in a separate Section 12).

**Method**: Read `ARCHITECTURE.md` in full (it's ~130 lines) and parse the ADR table:

```bash
# Read the full navigation index
Read(file_path="ARCHITECTURE.md")

# Parse ADR table rows
grep -E "^\| \[ADR-" ARCHITECTURE.md
```

**Expected output**:
```
| [ADR-001](adr/ADR-001-name.md) | Title | Accepted |
```

**Error Handling**: If no ADR table found in ARCHITECTURE.md:

```
⚠️  ADR table not found in ARCHITECTURE.md

Would you like me to:
1. [Add ADR Table] - Add the ADR section to ARCHITECTURE.md
2. [Skip ADR Generation] - Continue without generating ADRs
3. [Manual Review] - Let me check the navigation index first

Recommended: Option 3 (Manual Review)
```

---

##### Step 6.3: Extract ADR List from Navigation Index

**Read navigation index content** (already loaded in Step 6.2 — no additional read needed):

**Parse ADR table rows**:

```bash
# Extract ADR table rows using grep
grep -E "^\| \[ADR-" ARCHITECTURE.md
```

**Expected output** (example):
```
| [ADR-001](adr/ADR-001.md) | [Title] | Accepted | YYYY-MM-DD | High/Medium/Low |
| [ADR-002](adr/ADR-002-database.md) | Database Choice | Proposed | 2024-01-20 | High |
```

**Parse each row**:

For each line matching the pattern `^\| \[ADR-`:
1. Extract ADR number: `001`, `002`, etc. (from `[ADR-001]`)
2. Extract file path from link: `adr/ADR-001.md` (from `(adr/ADR-001.md)`)
3. Extract slug (if present): `` (empty) or `database` (from `ADR-002-database.md`)
4. Extract title: Column 2 (e.g., `[Title]` or `Database Choice`)
5. Extract status: Column 3 (e.g., `Accepted`, `Proposed`)
6. Extract date: Column 4 (e.g., `YYYY-MM-DD` or `2024-01-20`)
7. Extract impact: Column 5 (e.g., `High/Medium/Low` or `High`)

**Regex Pattern** for parsing:
```regex
^\| \[ADR-(\d{3})\]\(adr\/ADR-\d{3}(-[a-z0-9-]+)?\.md\) \| (.+?) \| (.+?) \| (.+?) \| (.+?) \|
```

**Capture Groups**:
- Group 1: ADR number (e.g., `001`)
- Group 2: Optional slug (e.g., `-database` or empty)
- Group 3: Title
- Group 4: Status
- Group 5: Date
- Group 6: Impact

**Store in list**:
```python
# Example structure (pseudocode)
adrs = [
    {
        "number": "001",
        "slug": "",  # Empty means generate from title
        "title": "[Title]",
        "status": "Accepted",
        "date": "YYYY-MM-DD",
        "impact": "High/Medium/Low",
        "file_path": "adr/ADR-001.md"
    },
    {
        "number": "002",
        "slug": "database",
        "title": "Database Choice",
        "status": "Proposed",
        "date": "2024-01-20",
        "impact": "High",
        "file_path": "adr/ADR-002-database.md"
    }
]
```

**Error Handling**: If no ADRs found (empty table):

```
ℹ️  Section 12 ADR table is empty or contains only placeholder entries.

No ADRs found to generate. This is normal for newly created ARCHITECTURE.md files.

Would you like me to:
1. [Skip ADR Generation] - Continue without creating ADR files
2. [Learn About ADRs] - Understand when to create ADRs
3. [Add Sample ADR] - Create one example ADR to get started

Recommended: Option 1 (Skip) - Create ADRs as architectural decisions are made
```

**Validation**:
- Check for duplicate ADR numbers (same number appears multiple times)
- Check for malformed rows (wrong column count)

**If duplicates found**:
```
⚠️  Duplicate ADR numbers detected in Section 12 table

Duplicates found: ADR-002 (appears 2 times)

This will cause file overwrites. Please fix the table before generating ADRs.

Would you like me to:
1. [Show Duplicates] - Display the conflicting rows
2. [Auto-Renumber] - Automatically renumber ADRs sequentially
3. [Skip ADR Generation] - Fix manually and regenerate later

Recommended: Option 1 (Show Duplicates)
```

---

##### Step 6.3a: Preview ADRs (if user selected Option 2)

If user selected "Preview First" in Step 6.1:

**Display ADR list**:
```
Found {count} ADRs in Section 12 table:

1. ADR-001: [Title] (Status: Accepted, Impact: High/Medium/Low)
   → Will create: adr/ADR-001-untitled.md
   Note: Placeholder title - you can customize later

2. ADR-002: Database Choice (Status: Proposed, Impact: High)
   → Will create: adr/ADR-002-database.md

Proceed with generation? (yes/no)
```

**Wait for response**:
- If `yes` or `1`: Continue to Step 6.4
- If `no` or `2` or `skip`: Skip to Step 6.8

---

##### Step 6.4: Prepare ADR Generation

**Get ARCHITECTURE.md directory**:

```bash
# Extract directory path from ARCHITECTURE.md location
# If ARCHITECTURE.md is in current directory, use "."
# Otherwise, extract directory from file path
ARCH_DIR="."
```

**Define ADR directory path**:
```bash
ADR_DIR="${ARCH_DIR}/adr"
# Results in: "./adr" relative to ARCHITECTURE.md
```

**Create ADR directory if doesn't exist**:
```bash
mkdir -p adr
```

**Error Handling**: If directory creation fails:

```
❌ Error: Unable to create adr/ directory

Reason: [Error message from mkdir]

Possible causes:
- Insufficient file permissions
- Path does not exist
- Disk space full

Would you like me to:
1. [Retry with Different Path] - Try creating in user home directory
2. [Skip ADR Generation] - Continue without creating ADR files
3. [Manual Instructions] - Show me how to create manually

Recommended: Option 3 (Manual Instructions)
```

**Success message**:
```
Generating ADR files in ./adr/
```

---

##### Step 6.5: Load ADR Template

**Template path**: Resolve dynamically — `$PLUGIN_DIR/skills/architecture-docs/adr/ADR-000-template.md`

**Resolve plugin directory** (run once per session if not already known):
```bash
PLUGIN_DIR=$(find "$HOME" -maxdepth 10 -type d -name "solutions-architect-skills" ! -path "*/node_modules/*" 2>/dev/null | head -1)
echo "$PLUGIN_DIR"
```

**Load template** using the resolved path:
```
Read(file_path="$PLUGIN_DIR/skills/architecture-docs/adr/ADR-000-template.md")
```
(Replace `$PLUGIN_DIR` with the actual path returned by the Bash command above)

**Store template content** in memory for reuse across all ADR files

---

##### Step 6.6: Generate Each ADR File

For each ADR in the `adrs` list from Step 6.3:

**Step 6.6a: Generate File Path**

```python
# Determine slug (pseudocode)
if adr["slug"] == "":
    # Generate slug from title using Step 6.6b rules
    slug = generate_slug(adr["title"])
else:
    # Use slug from file path in table
    slug = adr["slug"]

# Construct file path
file_name = f"ADR-{adr['number']}-{slug}.md"
file_path = f"adr/{file_name}"
```

**Step 6.6b: Generate Slug from Title** (if needed)

**Slug Generation Rules**:
1. Convert title to lowercase
2. Replace spaces with hyphens
3. Remove special characters: `:?/*<>|"` (keep alphanumeric and hyphens)
4. Remove consecutive hyphens (replace `--` with `-`)
5. Trim leading/trailing hyphens
6. Limit to 50 characters
7. If title is `[Title]` or placeholder, use slug `untitled`

**Examples**:
- `Technology Stack Selection` → `technology-stack-selection`
- `REST vs. gRPC: Which API?` → `rest-vs-grpc-which-api`
- `PostgreSQL Database Choice` → `postgresql-database-choice`
- `[Title]` → `untitled`

**Step 6.6c: Check for Existing File**

```bash
# Check if file already exists
if [ -f "adr/ADR-${number}-*.md" ]; then
    # File exists - trigger conflict handling
fi
```

**If exists**:
```
⚠️  ADR file conflict detected

Existing file: adr/ADR-001-old-title.md
New file: adr/ADR-001-technology-stack.md

Would you like me to:
1. [Skip This ADR] - Don't overwrite, keep existing file
2. [Rename New ADR] - Create as ADR-{next_available} instead
3. [Overwrite] - Replace existing file (destructive)
4. [Review Existing] - Show me what's in the existing file first

Recommended: Option 4 (Review Existing)
```

**Conflict Resolution**:
- **Never auto-overwrite** existing ADRs
- Always require user confirmation
- Suggest next available ADR number if renaming

**Step 6.6d: Populate Template**

```python
# Copy template (pseudocode)
adr_content = template_content

# Replace placeholders
adr_content = adr_content.replace("ADR-XXX", f"ADR-{adr['number']}")
adr_content = adr_content.replace("[Short Decision Title]", adr["title"])

# Replace status line (keep full line format from template)
status_line_old = "**Status**: Proposed | Accepted | Deprecated | Superseded by ADR-XXX"
status_line_new = f"**Status**: {adr['status']}"
adr_content = adr_content.replace(status_line_old, status_line_new)

# Replace date
if adr["date"] == "YYYY-MM-DD" or adr["date"] == "[Date]":
    # Use current date
    current_date = datetime.now().strftime("%Y-%m-%d")
    adr_content = adr_content.replace("**Date**: YYYY-MM-DD", f"**Date**: {current_date}")
else:
    # Use date from table
    adr_content = adr_content.replace("**Date**: YYYY-MM-DD", f"**Date**: {adr['date']}")

# Replace authors (default to Architecture Team)
adr_content = adr_content.replace("**Authors**: [Author names or team name]", "**Authors**: Architecture Team")

# Replace related ADRs (empty by default)
adr_content = adr_content.replace("**Related**: [Links to related ADRs, e.g., ADR-001, ADR-005]", "**Related**: []")
```

**Special handling for placeholder titles**:
```python
# If title is placeholder, add TODO note
if adr["title"] in ["[Title]", "[title]", "Title"]:
    # Add note about placeholder
    note = "\n> **TODO**: This ADR was auto-generated with a placeholder title. Please update the title, filename, and content with your actual architectural decision.\n"
    # Insert after first header (after "# ADR-XXX: [Title]" line)
    lines = adr_content.split('\n')
    for i, line in enumerate(lines):
        if line.startswith('# ADR-'):
            lines.insert(i + 1, note)
            break
    adr_content = '\n'.join(lines)
```

**Step 6.6e: Write ADR File**

```bash
# Write the file using Write tool
Write(file_path=file_path, content=adr_content)
```

**Error Handling**: If write fails:
- Log error message
- Mark ADR as "failed" in tracking
- Continue with next ADR (don't abort entire batch)

**Step 6.6f: Report Progress**

```
✅ Created: adr/ADR-001-technology-stack.md
```

**Repeat** Steps 6.6a-6.6f for all ADRs in list

---

##### Step 6.7: Display Summary Report

After all ADRs processed:

**Count results**:
```python
total_adrs = len(adrs)
created_adrs = count(created successfully)
skipped_adrs = count(skipped due to conflicts)
failed_adrs = count(failed to write)
```

**Display summary**:
```
═══════════════════════════════════════════════════════════
✅ ADR Generation Complete
═══════════════════════════════════════════════════════════

Successfully generated {created_adrs} of {total_adrs} ADR files in ./adr/

Created ADRs:
- adr/ADR-001-technology-stack.md
- adr/ADR-002-database-choice.md
- adr/ADR-003-api-protocol.md

{If skipped_adrs > 0:}
Skipped ADRs:
- ADR-004 (file already exists: adr/ADR-004-old-title.md)

{If failed_adrs > 0:}
Failed ADRs:
- ADR-005 (write error: permission denied)

Next steps:
1. Review and customize each ADR file
2. Fill in Context, Decision, and Rationale sections
3. Update placeholder titles (marked with TODO)
4. Link related ADRs in the "Related" field
5. Update status to "Accepted" once reviewed and approved

ADR Template Guide: skills/architecture-docs/ADR_GUIDE.md
```

---

##### Step 6.8: Complete Workflow

**If user skipped ADR generation** (Option 3 in Step 6.1):

```
ℹ️  Skipped ADR generation - you can create ADR files manually later

To create ADRs manually:
1. Update Section 12 table in ARCHITECTURE.md with your ADRs
2. Copy template: cp skills/architecture-docs/adr/ADR-000-template.md adr/ADR-001-your-title.md
3. Customize the ADR content

For guidance, see: skills/architecture-docs/ADR_GUIDE.md
```

**Return to main workflow**: Architecture Type Selection Workflow completes successfully

---

**End of Step 6**

### Detecting Existing Architecture Type

When editing an existing ARCHITECTURE.md, detect the architecture type:

**Detection Method 1: Metadata Comment**
```bash
grep -n "<!-- ARCHITECTURE_TYPE:" ARCHITECTURE.md
```

If found, extract the type from the comment.

**Detection Method 2: Section 4 Header Analysis**

If no metadata comment, infer from Section 4 headers:

```bash
# Check for BIAN indicators (check FIRST - most specific)
grep -E "(Layer 2: BIAN Business Scenarios|Layer 4: BIAN Service Domains)" ARCHITECTURE.md

# Check for META indicators
grep -E "(Layer 1: Channels|Layer 5: Domain|Layer 6: Core)" ARCHITECTURE.md

# Check for 3-Tier indicators
grep -E "(Tier 1: Presentation|Tier 3: Data)" ARCHITECTURE.md

# Check for Microservices indicators
grep -E "(API Gateway|Service Mesh|Microservices Catalog)" ARCHITECTURE.md

# Check for N-Layer indicators
grep -E "(Clean Architecture|Hexagonal|Ports & Adapters)" ARCHITECTURE.md
```

**Inference Rules (in order of specificity):**
- Contains "Layer 2: BIAN Business Scenarios" OR "Layer 4: BIAN Service Domains" → **BIAN**
- Contains "Layer 1: Channels" AND "Layer 5: Domain" AND "Layer 6: Core" → **META**
- Contains "Tier 1: Presentation" OR "Tier 3: Data" → **3-Tier**
- Contains "API Gateway" AND "Service Mesh" → **Microservices**
- Contains "Clean Architecture" OR "Hexagonal" → **N-Layer**
- Cannot determine → Ask user or default to **Microservices (Recommended)**

### Changing Architecture Type (Existing Document)

If user requests to change architecture type of an existing ARCHITECTURE.md:

**Warning Steps:**
1. **Detect current type** using detection methods above
2. **Warn user** about potential data loss:
   ```
   ⚠️  **Architecture Type Change Warning**

   Current type: [DETECTED_TYPE]
   Requested type: [NEW_TYPE]

   Changing architecture type will:
   - Replace Section 4 (Meta Architecture) with new structure
   - Replace Section 5 (Component Details) with new organization
   - Require manual component remapping

   **Recommendation**: Review and backup current Sections 4 & 5 before proceeding.

   Continue with architecture type change? (yes/no)
   ```

3. **If user confirms:**
   - Load new type templates
   - Replace Section 4 content
   - Replace Section 5 content
   - Update metadata comment
   - Update Document Index
   - Report changes to user

4. **If user declines:**
   - Cancel operation
   - Suggest manual editing approach

### Type-Specific Validation

After selecting or detecting architecture type, apply type-specific validation rules:

**BIAN Architecture:**
- ✅ Must have all 5 layers (Channels, BIAN Business Scenarios, BIAN Business Capabilities, BIAN Service Domains, Core Systems)
- ✅ Layer 2 must map to BIAN Business Areas (5 areas)
- ✅ Layer 3 must map to BIAN Business Domains (30+ domains)
- ✅ Layer 4 must implement BIAN Service Domains from [BIAN V12.0](https://bian.org/servicelandscape-12-0-0/views/view_51891.html)
- ✅ All Layer 4 service domain names validated against official [BIAN Service Landscape V12.0](https://bian.org/servicelandscape-12-0-0/views/view_51891.html)
- ✅ Layer 4 components must include complete BIAN metadata (Official Name, BIAN ID, Version, Business Domain, Business Area, URL)
- ✅ Control records documented per BIAN specification for all Layer 4 service domains
- ✅ All mandatory BIAN service operations implemented (Initiate, Update, Retrieve, Control)
- ✅ Behavior qualifiers documented per BIAN spec
- ✅ Functional patterns documented per BIAN spec (Managed Object, Tracked Object, etc.)
- ✅ Full BIAN V12.0 compliance level documented
- ✅ BIAN hierarchy traceability: Service Domain → Business Domain → Business Area
- ✅ Layers documented in correct order

**META Architecture:**
- ✅ Must have all 6 layers (Channels, UX, Business Scenarios, Business, Domain, Core)
- ✅ Layer 5 must include BIAN alignment section
- ✅ Verify BIAN V12.0 is documented as the default version
- ✅ Validate BIAN service domain **names (Capabilities)** against the official [BIAN Service Landscape V12.0](https://bian.org/servicelandscape-12-0-0/views/view_51891.html)
- ✅ Ensure BIAN IDs (SD-XXX) are used for internal document tracking only
- ✅ Validate Layer 5 (Domain) components include BIAN alignment subsection with official URLs
- ✅ Layers documented in correct order

**3-Tier Architecture:**
- ✅ Must have all 3 tiers (Presentation, Application, Data)
- ✅ No direct database access from Presentation tier
- ✅ Application tier should be stateless

**Microservices Architecture:**
- ✅ Must document API Gateway and Service Mesh (or justify omission)
- ✅ Database-per-service pattern followed
- ✅ Event bus and topics documented
- ✅ Circuit breakers configured

**N-Layer Architecture:**
- ✅ Must specify which pattern (4-Layer, 5-Layer, Clean, Hexagonal)
- ✅ Dependency direction documented
- ✅ Core/domain layer is framework-free (if applicable)

**For detailed validation rules**, see: `VALIDATIONS.md` § Type-Aware Validation

### Best Practices

**DO:**
- ✅ Always prompt for architecture type when creating new ARCHITECTURE.md
- ✅ Add metadata comment to track architecture type
- ✅ Load appropriate templates based on selection
- ✅ Warn before changing architecture type of existing document
- ✅ Apply type-specific validation rules

**DON'T:**
- ❌ Assume architecture type without asking
- ❌ Mix templates from different architecture types
- ❌ Change architecture type without user confirmation
- ❌ Skip metadata comment (makes future edits harder)

### Example Workflow

**User:** "Create architecture documentation for my microservices system"

**Assistant:**
1. Detects new ARCHITECTURE.md creation
2. Presents architecture type options (1-4)
3. User selects "3" (Microservices)
4. Loads `templates/SECTION_4_MICROSERVICES.md` and `templates/SECTION_5_MICROSERVICES.md`
5. Creates ARCHITECTURE.md with:
   - Standard Sections 1-3
   - Microservices Section 4 (API Gateway, Service Mesh, Services, Event Bus)
   - Microservices Section 5 (Service catalog format)
   - Standard Sections 6-12
   - Metadata comment: `<!-- ARCHITECTURE_TYPE: MICROSERVICES -->`
6. Reports completion with architecture type confirmation

---

## Automatic Index Updates

**CRITICAL**: After ANY edit that significantly changes section line numbers (>10 lines), automatically update the Document Index.

### When to Update

**Update if:**
- ✅ Added/removed content shifting section boundaries (>10 lines)
- ✅ Modified section headers or structure
- ✅ User requests: "update the index"

**Skip if:**
- ❌ Minor edits (<10 lines)
- ❌ Only metadata changes
- ❌ Typo fixes

### Workflow Overview

**Quick Steps:**
1. **Detect**: Run `grep -n "^## [0-9]" ARCHITECTURE.md` to find section boundaries
2. **Calculate**: Parse output to determine line ranges (Section_Start to Next_Section_Start - 1)
3. **Update**: Edit Document Index (typically lines 5-21) with new ranges
4. **Timestamp**: Update "Index Last Updated" to current date
5. **Report**: Inform user which sections changed

**Example:**
```bash
# After adding content to Section 8, it grew from 906-980 to 912-996
grep -n "^## [0-9]" ARCHITECTURE.md
# Parse: Section 8 now at line 912, Section 9 at 998
# Calculate: Section 8 = 912-997, Section 9 = 998-1243
# Update index and report to user
```

### Detailed Algorithm

For complete line range calculation algorithm, step-by-step examples, verification checklist, and edge cases:
→ **METRIC_CALCULATIONS.md** § Automatic Index Updates

### Best Practices

**DO:**
- ✅ Update after significant edits
- ✅ Use grep for accuracy (don't guess)
- ✅ Update timestamp
- ✅ Report changes to user

**DON'T:**
- ❌ Update for tiny changes
- ❌ Skip timestamp update
- ❌ Change index format

---

## Metric Consistency Detection & Management

**CRITICAL**: The Executive Summary (Section 1, Key Metrics subsection around lines 31-38) contains performance metrics that serve as the **Source of Truth** for system capacity, throughput, latency, and availability targets. These metrics are often duplicated across multiple sections (especially Section 6: Data Flow, Section 7: Integration Points, and Section 10: Scalability & Performance).

**Problem**: When metrics are updated in the Executive Summary, duplicates in other sections can become stale, creating inconsistencies.

**Solution**: This skill provides automatic metric consistency detection and review-based synchronization.

### When to Trigger Metric Audit

**Automatic Trigger:**
- ✅ After editing Section 1 Executive Summary Key Metrics (typically lines 31-38)
- ✅ When user updates any metric values in the Key Metrics subsection
- ✅ After Edit tool completes on lines in Section 1 that contain metrics (TPS, latency, SLA, etc.)

**Manual Trigger:**
- User explicitly requests: "Check metric consistency", "Verify metrics", "Audit metrics", "Sync metrics"
- User updates Section 10 (Scalability & Performance) and asks to verify consistency with Exec Summary
- During quarterly documentation review or audit

**Do NOT Trigger:**
- ❌ Minor edits outside Key Metrics subsection (e.g., updating document version or date)
- ❌ Edits to narrative text that don't change numeric metric values
- ❌ Changes to other sections that don't involve performance metrics

### Metric Audit Workflow

**Step 1: Extract Executive Summary Metrics**

Read ONLY lines 30-40 (Key Metrics section) using context-efficient approach:
```bash
Read(file_path="ARCHITECTURE.md", offset=30, limit=10)
```

Parse and extract all metrics with their values using these patterns:

| Metric Type | Pattern | Example Match |
|-------------|---------|---------------|
| **Average Read TPS** | `Average\s+Read\s+TPS:\s*(\d{1,3}(?:,\d{3})*)\s*transactions/second` | "Average Read TPS: 1,500 transactions/second" |
| **Peak Read TPS** | `Peak\s+Read\s+TPS:\s*(\d{1,3}(?:,\d{3})*)\s*transactions/second` | "Peak Read TPS: 3,000 transactions/second" |
| **Average Processing TPS** | `Average\s+Processing\s+TPS:\s*(\d{1,3}(?:,\d{3})*)\s*transactions/second` | "Average Processing TPS: 450 transactions/second" |
| **Peak Processing TPS** | `Peak\s+Processing\s+TPS:\s*(\d{1,3}(?:,\d{3})*)\s*transactions/second` | "Peak Processing TPS: 1,000 transactions/second" |
| **Average Write TPS** | `Average\s+Write\s+TPS:\s*(\d{1,3}(?:,\d{3})*)\s*transactions/second` | "Average Write TPS: 300 transactions/second" |
| **Peak Write TPS** | `Peak\s+Write\s+TPS:\s*(\d{1,3}(?:,\d{3})*)\s*transactions/second` | "Peak Write TPS: 800 transactions/second" |
| **Measurement Period** | `Measurement\s+Period:\s*(.+)` | "Measurement Period: Average over last 30 days" |
| **Percentile Latency** | `p(\d{2})\s*<\s*(\d+)ms` | "p95 < 100ms", "p99 < 200ms" |
| **Availability SLA** | `(\d{2,3}\.\d+)\s*%` | "99.99%", "99.9%" |
| **Concurrent Jobs** | `(\d{1,3}(?:,\d{3})*)\+?\s*concurrent` | "10,000+ concurrent", "5000 concurrent" |
| **Jobs per Hour** | `\[(\d{1,3}(?:,\d{3})*)\s*jobs?/hour\]` | "[1,620,000 jobs/hour]" |

Build a metrics registry from parsed data:
```
METRICS_REGISTRY = [
  {name: "Read TPS", value: 1500, unit: "TPS", category: "Read", stat_type: "Average", line: 32, measurement_period: "Average over last 30 days in production"},
  {name: "Read TPS", value: 3000, unit: "TPS", category: "Read", stat_type: "Peak", line: 33, measurement_period: "Peak observed during Black Friday 2024"},
  {name: "Processing TPS", value: 450, unit: "TPS", category: "Processing", stat_type: "Average", line: 35, measurement_period: "Average over last quarter"},
  {name: "Processing TPS", value: 1000, unit: "TPS", category: "Processing", stat_type: "Peak", line: 36, measurement_period: "Peak during end-of-month batch processing"},
  {name: "Write TPS", value: 300, unit: "TPS", category: "Write", stat_type: "Average", line: 38, measurement_period: "Average over last month"},
  {name: "Write TPS", value: 800, unit: "TPS", category: "Write", stat_type: "Peak", line: 39, measurement_period: "Peak during data migration events"},
  {name: "System Availability", value: 99.99, unit: "%", line: 42},
  {name: "Latency p95", value: 100, unit: "ms", line: 43},
  {name: "Latency p99", value: 200, unit: "ms", line: 43},
  {name: "Concurrent Jobs", value: 10000, unit: "jobs", line: 44}
]
```

**Step 2: Scan Document for Metric References**

Use Grep to find all occurrences of each metric value (context-efficient, no full file load):

```bash
# For each metric in registry, search with appropriate pattern (new standardized TPS format)
Grep(pattern="Average\s+Read\s+TPS:\s*1,?500", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="Peak\s+Read\s+TPS:\s*3,?000", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="Average\s+Processing\s+TPS:\s*450", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="Peak\s+Processing\s+TPS:\s*1,?000", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="Average\s+Write\s+TPS:\s*300", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="Peak\s+Write\s+TPS:\s*800", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="99\.99%", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="p95.*100ms", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="p99.*200ms", path="ARCHITECTURE.md", output_mode="files_with_matches")
Grep(pattern="10,?000.*concurrent", path="ARCHITECTURE.md", output_mode="files_with_matches")
```

**Note**: Use `-n` flag with grep bash command if you need line numbers, or use Grep tool with `output_mode="content"`.

**Step 3: Load Context for Each Match**

For each grep result, read ±5 lines for context to determine if it's a duplicate or coincidental match:
```bash
# If match found at line 787
Read(file_path="ARCHITECTURE.md", offset=782, limit=10)
# Reads lines 782-792 for context
```

**Step 4: Classify Findings**

Categorize each finding into one of four categories:

- **✓ Exact Match**: Same value, same metric concept, already consistent with Executive Summary
- **⚠️ Mismatch**: Same metric concept but different value (ACTION REQUIRED - user must decide)
- **ℹ️ Derived Value**: Related but transformed metric (e.g., 450 TPS = 1,620,000 jobs/hour via 450 × 3600)
- **? Ambiguous**: Number matches but context is unclear (flag for manual review)

**Classification Logic**:
1. Check if line is in Section 1 (skip, that's the source of truth)
2. Read context (±5 lines) to understand what the metric represents
3. Compare metric concept (is this the same "Job Creation Capacity" or different metric?)
4. Determine relationship (exact duplicate, derived value, or unrelated)

**Step 5: Generate Consistency Report**

Present findings in structured, user-friendly format:

```markdown
═══════════════════════════════════════════════════════════
   METRIC CONSISTENCY AUDIT REPORT
═══════════════════════════════════════════════════════════
Source of Truth: Section 1 (Executive Summary) Lines 31-38
Total Metrics Audited: 10
═══════════════════════════════════════════════════════════

## ✓ Exact Matches (Consistent)

| Metric | Exec Value | Location | Context | Status |
|--------|-----------|----------|---------|--------|
| Job Latency (p95) | < 100ms | Line 787 | Section 6: Data Flow Performance | ✓ Match |
| Job Latency (p99) | < 200ms | Line 787 | Section 6: Data Flow Performance | ✓ Match |
| Job Latency (p95) | < 100ms | Line 1345 | Section 10: Performance Table | ✓ Match |
| Job Latency (p99) | < 200ms | Line 1345 | Section 10: Performance Table | ✓ Match |

## ⚠️ Mismatches Found (ACTION REQUIRED)

| Metric | Exec Value | Location | Current Value | Difference |
|--------|-----------|----------|---------------|------------|
| Peak Job Creation | 1,000 TPS | Line 1367 | 500 TPS | -500 TPS |

## ℹ️ Derived Values (Informational)

| Metric | Exec Value | Location | Derived Value | Relationship |
|--------|-----------|----------|---------------|--------------|
| Job Creation | 450 TPS | Line 32 | 1,620,000 jobs/hour | 450 × 3600 |

═══════════════════════════════════════════════════════════
SUMMARY: ✓ 4 Matches | ⚠️ 1 Mismatch | ℹ️ 1 Derived
═══════════════════════════════════════════════════════════
```

**Step 6: Await User Decision**

If mismatches found, present options to user:
```
I found 1 metric mismatch. What would you like to do?

1. [Review Details] - Show detailed context for the mismatch
2. [Update Exec Summary] - Update Section 1 to match line 1367 (500 TPS)
3. [Update Document] - Update line 1367 to match Exec Summary (1,000 TPS)
4. [Preview Changes] - Show exactly what will change before applying
5. [Manual Review] - I'll review and decide later
6. [Ignore] - Keep as-is (intentional difference)
```

**Step 7: Apply Approved Changes**

If user approves updates:
1. Show preview of all changes (exact before/after for each location)
2. Confirm user approval
3. Use Edit tool to update each location sequentially
4. Update Document Index if line numbers shift significantly (>10 lines)
5. Report completion with summary of all changes made

### Common Metric Locations

**Known Duplicate Locations** (for typical architecture documents):

| Metric | Executive Summary | Common Duplicate Locations |
|--------|------------------|---------------------------|
| Job Creation Capacity (450 TPS) | Line ~32 | Section 10 Throughput Table (~line 1367) |
| Job Execution Capacity (500 TPS) | Line ~33 | Section 10 Throughput Table (~line 1368), Section 6 Data Flow (~line 787) |
| Peak Capacity (1,000/2,000 TPS) | Line ~34 | Section 10 Throughput Table (~lines 1367-1368) |
| Initial Load (150/350 TPS) | Line ~35 | Section 10 Capacity Planning (~lines 1378-1379) |
| System Availability (99.99%) | Line ~36 | Section 2 Use Case metrics (~line 115), Section 7 Integration SLA Table (~lines 829-832) |
| Latency Targets (p95/p99) | Line ~37 | Section 6 Data Flow Performance (~line 787), Section 10 Performance Table (~line 1345) |
| Concurrent Jobs (10,000+) | Line ~38 | Less commonly duplicated (usually only in Exec Summary) |

**Note**: Line numbers are approximate and vary by document. Use grep to find exact locations.

### Metric Update Best Practices

**DO:**
- ✅ Always scan full document after Exec Summary metric changes
- ✅ Present findings before making any changes (review-only first)
- ✅ Explain context for each mismatch (which section, what table/paragraph)
- ✅ Offer batch update option for multiple occurrences of same metric
- ✅ Show preview with exact before/after text for transparency
- ✅ Update Document Index if edits shift section boundaries

**DON'T:**
- ❌ Auto-update without user approval (always review-only first)
- ❌ Change metrics in unrelated sections without confirming intent
- ❌ Ignore ambiguous matches (always flag for review)
- ❌ Forget to check derived values (e.g., jobs/hour conversions from TPS)
- ❌ Skip context loading (always read ±5 lines to understand match)

### Error Handling & Edge Cases

**1. Multiple Conflicting Values**

Scenario: Same metric has different values in multiple sections (not just Exec Summary vs. one location)

Example:
- Exec Summary (Line 32): 450 TPS
- Section 6 (Line 787): 500 TPS
- Section 10 (Line 1367): 400 TPS

**Handling:**
```
⚠️ CONFLICT DETECTED: Job Execution Capacity has 3 different values

| Location | Value | Context |
|----------|-------|---------|
| Line 32 (Exec Summary) | 450 TPS | Design Capacity |
| Line 787 (Section 6) | 500 TPS | Data Flow Performance |
| Line 1367 (Section 10) | 400 TPS | Throughput Table |

Which value is correct?
1. Use Exec Summary (450 TPS) as source of truth
2. Use most common value (if clear majority)
3. Manual review - I need to investigate further

Please clarify which value should be used across all locations.
```

**2. Derived Value Calculation Errors**

Scenario: Derived value doesn't match expected conversion

Example:
- Exec Summary: 450 TPS
- Expected derived: 1,620,000 jobs/hour (450 × 3600)
- Found in doc: 1,650,000 jobs/hour

**Handling:**
```
ℹ️ Derived Value Mismatch Detected:

Metric: Job Creation Capacity
Base Value: 450 TPS (Line 32)
Expected Derived: 1,620,000 jobs/hour (450 × 3600)
Actual Found: 1,650,000 jobs/hour (Line 32)
Difference: +30,000 jobs/hour

Possible causes:
- Rounding or overhead factor applied
- Different calculation method
- Stale value from previous metric update

Would you like to:
1. Update to calculated value (1,620,000)
2. Keep current value (1,650,000) - might be intentional overhead
3. Investigate further
```

**3. Ambiguous Metric Context**

Scenario: Number matches but unclear if it's the same metric

Example:
- Exec Summary: Peak Capacity 1,000 TPS
- Found at line 1367: "System limit 1,000 TPS (theoretical max)"
- Found at line 1372: "Target throughput 1,000 TPS (steady state)"
- Found at line 1389: "1000 TPS limit"

**Handling:**
```
? AMBIGUOUS MATCH: Value "1,000 TPS" found in multiple contexts

Exec Summary Definition (Line 34):
"Peak Capacity: 1,000 TPS job creation, 2,000 TPS job execution (system limits)"

Found Matches:
1. Line 1367: "System limit 1,000 TPS"
   Context: Throughput table - appears to be same metric ✓

2. Line 1372: "1000 TPS (theoretical max)"
   Context: Throughput calculation - likely same metric ✓

3. Line 1389: "1000 TPS limit"
   Context: Capacity planning - likely same metric ✓

All 3 appear to reference Peak Capacity. Treat as exact matches? [Yes/No/Review Each]
```

**4. No Matches Found**

Scenario: Metric exists in Exec Summary but never referenced elsewhere

Example:
- Exec Summary: "Concurrent Jobs: Support for 10,000+ concurrent scheduled jobs"
- Grep search: No matches found in rest of document

**Handling:**
```
ℹ️ INFORMATIONAL: Metric appears only in Executive Summary

Metric: Concurrent Jobs (10,000+)
Location: Line 38 (Section 1)
Document Coverage: Not referenced in other sections

This is not necessarily an error - some high-level metrics may only appear in Exec Summary.

Recommendation:
- If this metric is important to architecture, consider adding to Section 10 (Scalability & Performance)
- If it's a summary-only metric, no action needed

Would you like to:
1. Add this metric to Section 10
2. Keep as-is (Exec Summary only)
3. Ignore this finding
```

**5. Too Many Grep Results**

Scenario: Common number matches hundreds of unrelated contexts

Example: Searching for "100" also matches "100 pods", "section 100", "version 1.00"

**Handling:**
- Use context-aware grep patterns to reduce false positives:
  - Bad: `grep -n "100" ARCHITECTURE.md` (too broad)
  - Good: `grep -n "p95.*100ms" ARCHITECTURE.md` (context-aware)
  - Better: `grep -n "p95\s*<\s*100ms" ARCHITECTURE.md` (exact pattern)

- If still >20 matches after context-aware patterns:
  1. Load only first 10 matches
  2. Present summary: "Found 47 matches for '100' - showing first 10 most relevant"
  3. Ask user if they want to see all or refine search pattern

### Example: Complete Metric Update Workflow

**Scenario:** User updates Job Execution Capacity from 500 TPS to 700 TPS

**Step-by-Step Execution:**

1. **User Action**:
   ```
   User: "Update the job execution capacity to 700 TPS in the Executive Summary"
   ```

2. **Claude Applies Edit**:
   ```
   [Uses Edit tool to change line 33: "500 TPS" → "700 TPS"]
   ✓ Updated line 33: Design Capacity - Job Execution: 700 TPS

   Now checking for metric consistency across the document...
   ```

3. **Automatic Audit Triggered**:
   ```
   [Metric audit automatically triggered because Section 1 Key Metrics was edited]

   Scanning document for references to old value (500 TPS)...
   [Runs: Grep(pattern="500\s*TPS", path="ARCHITECTURE.md")]
   [Found at: Line 787, Line 1368]
   ```

4. **Load Context for Each Match**:
   ```
   [Reads context around each match]
   Read(file_path="ARCHITECTURE.md", offset=782, limit=10)  # Lines 782-792 for Line 787
   Read(file_path="ARCHITECTURE.md", offset=1363, limit=10) # Lines 1363-1373 for Line 1368
   ```

5. **Present Findings**:
   ```
   ═══════════════════════════════════════════════════════════
   METRIC CONSISTENCY AUDIT REPORT
   ═══════════════════════════════════════════════════════════
   Triggered by: Executive Summary edit (Line 33)
   Metric Changed: Job Execution Capacity (500 TPS → 700 TPS)
   ═══════════════════════════════════════════════════════════

   ## ⚠️ Outdated References Found

   | Location | Section | Current Value | Should Be | Context |
   |----------|---------|---------------|-----------|---------|
   | Line 787 | Section 6: Data Flow Patterns | 500 TPS capacity | 700 TPS | "Performance: p50 = 40ms, p95 = 100ms, p99 = 200ms \| 500 TPS capacity" |
   | Line 1368 | Section 10: Scalability & Performance | 500 | 700 | Throughput table: "Job Execution (TPS) \| 500 \| 1000 \| 2000" |

   ═══════════════════════════════════════════════════════════
   SUMMARY: Found 2 locations with outdated metric value
   ═══════════════════════════════════════════════════════════

   Would you like me to update both locations to 700 TPS?

   Options:
   1. [Yes - Update All] - Apply changes to both locations
   2. [Preview Changes] - Show me exactly what will change
   3. [Manual Selection] - Let me choose individually
   4. [No Thanks] - I'll handle this manually
   ```

6. **User Requests Preview**:
   ```
   User: "Preview changes first"
   ```

7. **Show Preview**:
   ```
   Here's a preview of the changes I'll make:

   ════════════════════════════════════════════════════════════
   CHANGE 1 of 2: Line 787 (Section 6: Data Flow Patterns)
   ════════════════════════════════════════════════════════════

   Current:
   **Performance**: p50 = 40ms, p95 = 100ms, p99 = 200ms | 500 TPS capacity

   Will become:
   **Performance**: p50 = 40ms, p95 = 100ms, p99 = 200ms | 700 TPS capacity

   ════════════════════════════════════════════════════════════
   CHANGE 2 of 2: Line 1368 (Section 10: Throughput Table)
   ════════════════════════════════════════════════════════════

   Current:
   | Job Execution (TPS) | 500 | 1000 | 2000 | Downstream domain services |

   Will become:
   | Job Execution (TPS) | 700 | 1000 | 2000 | Downstream domain services |

   ════════════════════════════════════════════════════════════

   Proceed with both changes? [Yes/No/Choose Individually]
   ```

8. **User Approves**:
   ```
   User: "Yes"
   ```

9. **Apply Changes**:
   ```
   Applying metric consistency updates...

   [Uses Edit tool on line 787]
   ✓ Updated line 787: "500 TPS capacity" → "700 TPS capacity"

   [Uses Edit tool on line 1368]
   ✓ Updated line 1368: Throughput table "500" → "700"

   Checking if Document Index needs update...
   [Runs: grep -n "^## [0-9]" ARCHITECTURE.md to check section positions]
   [Determines: No significant line shifts, index still valid]

   ✓ Document Index is still accurate (no update needed)
   ```

10. **Completion Report**:
    ```
    ═══════════════════════════════════════════════════════════
    METRIC CONSISTENCY UPDATE COMPLETE
    ═══════════════════════════════════════════════════════════

    Summary of Changes:
    ✓ Executive Summary (Line 33): 500 TPS → 700 TPS
    ✓ Section 6 Data Flow (Line 787): 500 TPS → 700 TPS
    ✓ Section 10 Throughput Table (Line 1368): 500 → 700

    Total Locations Updated: 3
    Document Index: No update needed
    All references to Job Execution Capacity are now consistent.

    ═══════════════════════════════════════════════════════════
    ```

---

## Foundational Context Anchor Protocol

**PURPOSE**: Ensure that when writing or editing any downstream section (S4–S11), the agent first loads the foundational context that governs it. This prevents downstream sections from drifting from business context, design principles, key decisions, and from each other — eliminating internal contradictions across the architecture documentation.

### Foundational Hierarchy

Architecture documentation follows a layered dependency graph where later sections depend on both the universal foundation AND earlier downstream sections:

```
Tier 0 (Source of Truth):  S1+2 (System Overview), S3 (Principles), ADRs
Tier 1:                    S4 (Architecture Layers)
Tier 2:                    S5 (Components)
Tier 3:                    S6 (Data Flow), S7 (Integration), S8 (Tech Stack)
Tier 4:                    S9 (Security), S10 (Scalability)
Tier 5:                    S11 (Operations)
```

### Full Dependency Map

| Target File | Foundation (always) | Section-Specific Parents | Why |
|-------------|-------------------|-------------------------|-----|
| **S4** `docs/03-architecture-layers.md` | S1+2, S3, ADRs | — | S4 derives directly from business scope (S1) and principles (S3). Architecture style ADRs govern the layer model. |
| **S5** `docs/components/*.md` | S1+2, S3, ADRs | **S4** (layers) | Components must map to the layer structure defined in S4. Layer boundaries determine component grouping. |
| **S6** `docs/04-data-flow-patterns.md` | S1+2, S3, ADRs | **S5** (components) | Flows reference specific components as source/destination nodes. Throughput metrics (S1) constrain flow design. |
| **S7** `docs/05-integration-points.md` | S1+2, S3, ADRs | **S5** (components) | Each integration connects to specific components. Loose coupling principle (S3) constrains protocol choices. |
| **S8** `docs/06-technology-stack.md` | S1+2, S3, ADRs | **S4** (layers), **S5** (components) | Architecture type (S4) constrains tech patterns (e.g., microservices → containers, mesh). Components (S5) specify per-component tech → S8 is the aggregate. |
| **S9** `docs/07-security-architecture.md` | S1+2, S3, ADRs | **S5** (components), **S7** (integrations), **S8** (tech stack) | Security controls span components, integration auth, and security tooling. Compliance requirements from S1. |
| **S10** `docs/08-scalability-and-performance.md` | S1+2, S3, ADRs | **S5** (components), **S8** (tech stack) | Per-component scaling strategies, infrastructure capabilities. SLO/throughput from S1 Key Metrics are source of truth. |
| **S11** `docs/09-operational-considerations.md` | S1+2, S3, ADRs | **S5** (components), **S8** (tech stack), **S10** (performance) | Deployment per component, monitoring per component, alerting thresholds from S10, infrastructure tools from S8. |

### Context Budget Per Section Edit

| Target | Files to Load | Est. Lines |
|--------|--------------|------------|
| S4 | S1+2, S3, ADRs (1-3) | ~250–450 |
| S5 | S1+2, S3, S4, ADRs | ~350–600 |
| S6 | S1+2, S3, S5/README, ADRs | ~300–550 |
| S7 | S1+2, S3, S5/README, ADRs | ~300–550 |
| S8 | S1+2, S3, S4, S5/README, ADRs | ~400–700 |
| S9 | S1+2, S3, S5/README, S7, S8, ADRs | ~500–850 |
| S10 | S1+2, S3, S5/README, S8, ADRs | ~400–700 |
| S11 | S1+2, S3, S5/README, S8, S10, ADRs | ~500–850 |

**Note on S5**: For component files, load `docs/components/README.md` (index) as context. For individual component editing, also load S4 + the component README. Do NOT load all component files — only the index.

### ADR Relevance Keyword Map

When determining which ADRs to load, match ADR titles from the `ARCHITECTURE.md` navigation table against these keywords per target section:

| Target Section | ADR Title Keywords to Match |
|---------------|---------------------------|
| S4 (Layers) | architecture, layers, style, pattern, microservice, monolith, modular |
| S5 (Components) | component, service, boundary, domain, module, separation |
| S6 (Data Flow) | data, flow, pipeline, stream, event, async, message, queue |
| S7 (Integration) | integration, API, protocol, gateway, external, third-party |
| S8 (Tech Stack) | technology, language, framework, database, infrastructure, tool |
| S9 (Security) | security, auth, encryption, compliance, access, identity, trust |
| S10 (Scalability) | scale, performance, capacity, throughput, latency, SLO, cache |
| S11 (Operations) | deploy, monitor, observe, incident, DR, recovery, operational |

### Pre-Write Anchor Loading Procedure

1. **Determine target section tier** from the Foundational Hierarchy
2. **Load Universal Foundation**: Read `docs/01-system-overview.md` + `docs/02-architecture-principles.md`
3. **Load Section-Specific Parents** from the Full Dependency Map table above
4. **Match and load relevant ADRs**: Scan `ARCHITECTURE.md` ADR navigation entries; match titles against the ADR Relevance Keyword Map for the target section; load matched ADR files (typically 1–3)
5. **Context budget**: 250–850 lines depending on section tier — well within context limits

### Source Attribution Rules

Architecture documentation is an auditable artifact. A compliance auditor, new team member, or review board must be able to trace any metric, decision, or constraint back to its origin without searching the entire documentation. Unsourced claims erode trust in the entire document.

Every claim in a downstream section that originates from another section MUST carry a traceable citation:

| Claim Type | Source | Required Citation Format |
|-----------|--------|-------------------------|
| Metrics (TPS, SLO, latency, %) | S1 Key Metrics | `(see [Key Metrics](01-system-overview.md#key-metrics))` |
| Design decisions | ADRs | `per [ADR-NNN](../adr/ADR-NNN-title.md)` |
| Principle-driven choices | S3 | `per [Principle Name](02-architecture-principles.md#anchor)` |
| Component references | S5 | `[Component Name](components/NN-name.md)` |
| Technology references | S8 | `(see [Technology Stack](06-technology-stack.md))` |
| Layer/pattern references | S4 | `(see [Architecture Layers](03-architecture-layers.md))` |
| Integration references | S7 | `[Integration Name](05-integration-points.md#anchor)` |
| Unverifiable claims | None found | `<!-- TODO: Add source reference -->` |

### Post-Write Alignment & Traceability Validation

After editing a downstream section, perform these checks:

1. **Principle traceability** — Written content does not contradict Section 3 principles
2. **Metric consistency** — Numeric values (TPS, latency, SLO, availability %) match Section 1 Key Metrics exactly
3. **ADR alignment** — Content does not contradict loaded ADR decisions
4. **Parent section alignment** — Content references valid components (S5), valid integrations (S7), valid tech (S8) as loaded from parent sections
5. **Source citation audit** — Scan the written content for:
   - Numeric values (TPS, latency, SLO, %) → must link to S1 Key Metrics or be marked as section-local
   - Technology names matching S8 → should link to tech stack or governing ADR
   - Architecture pattern references → should link to S3 or S4
   - `<!-- TODO: Add source reference -->` markers → count and report

**Output**: Silent pass if no issues found. When misalignment or missing citations are detected, display:

```
═══════════════════════════════════════════════════════════
CONTEXT ANCHOR — ALIGNMENT & TRACEABILITY REPORT
═══════════════════════════════════════════════════════════

Target: docs/07-security-architecture.md (S9)
Context Loaded: S1+2, S3, S5/README, S7, S8, ADR-003, ADR-007

Alignment Issues:
⚠ Line 42: "99.95% availability" — S1 Key Metrics specifies 99.9%
⚠ Line 78: References "Redis caching" — not listed in S8 Technology Stack

Missing Citations:
⚠ Line 23: "OAuth 2.0 + PKCE" — no link to governing ADR
⚠ Line 56: "TLS 1.3 everywhere" — no link to S3 Encrypt Everything principle

TODO Markers: 1
⚠ Line 91: <!-- TODO: Add source reference --> for "SOC 2 Type II compliance"

Recommendation: Resolve alignment issues before finalizing this section.
═══════════════════════════════════════════════════════════
```

### Change Propagation Detection

When ANY section is edited (not just S1–S3), identify downstream sections that may need review using the dependency map in reverse:

| Modified Section | Downstream Impact |
|-----------------|-------------------|
| S1+2 (System Overview) | All sections (S4–S11) |
| S3 (Principles) | All sections (S4–S11) |
| ADRs | Sections matched by ADR relevance keywords |
| S4 (Layers) | S5, S8 (and transitively S6, S7, S9, S10, S11) |
| S5 (Components) | S6, S7, S8, S9, S10, S11 |
| S7 (Integrations) | S9 |
| S8 (Tech Stack) | S9, S10, S11 |
| S10 (Scalability) | S11 |

After editing a section, generate a propagation report presenting affected downstream sections to the user. **Do NOT auto-edit downstream sections** — present the report and let the user decide which sections to review.

```
═══════════════════════════════════════════════════════════
CHANGE PROPAGATION — DOWNSTREAM IMPACT REPORT
═══════════════════════════════════════════════════════════

Modified: docs/06-technology-stack.md (S8)
Changes: Added Redis 7.2 to caching layer, removed Memcached

Potentially Affected Downstream Sections:
→ docs/07-security-architecture.md (S9) — may reference caching security controls
→ docs/08-scalability-and-performance.md (S10) — may reference caching strategy
→ docs/09-operational-considerations.md (S11) — may reference caching deployment/monitoring

Action Required: Review listed sections for consistency with S8 changes.
═══════════════════════════════════════════════════════════
```

### Integration with Existing Mechanisms

- **Metric Consistency Audit** (see above) handles numeric propagation from S1 → S6, S7, S10. The Context Anchor Protocol adds **semantic alignment** — ensuring principles, decisions, and parent section references remain consistent.
- **Design Drivers Calculation** (see below) reads S1, S2, S5, S8 to compute metrics. The Context Anchor Protocol adds the **reverse check** — ensuring S5 and S8 align BACK to S1/S2 when edited.
- **Cross-reference conventions** (RESTRUCTURING_GUIDE.md) define the *syntax* for links between docs/ files. The Context Anchor Protocol **requires** using those links when writing derived content, making the syntax convention enforceable rather than optional.

---

## Design Drivers Impact Metrics Calculation

**PURPOSE**: Automatically calculate and maintain Design Drivers impact metrics (Value Delivery, Scale, Impacts) based on data from existing architecture sections. Design Drivers provide a quantifiable assessment of the architecture's business value, customer reach, and implementation complexity.

### When to Trigger Design Drivers Calculation

**Automatic Prompt:**
- ✅ User requests "architecture review" or "audit" (prompt to calculate)
- ✅ User explicitly asks to "calculate design drivers" or "update design drivers"
- ✅ After significant architecture changes during review process

**Manual Trigger:**
- User explicitly requests: "Calculate design drivers", "Update Section 2.2.1", "Assess design impact"
- During quarterly documentation review or architecture validation
- When preparing architecture business justifications

**Do NOT Trigger:**
- ❌ Minor edits unrelated to architecture (typos, formatting, dates)
- ❌ Changes to sections not used in design drivers calculation (Sections 3, 4, 9, 11, 12)
- ❌ User explicitly declines when prompted during review

### Design Drivers Framework

The Design Drivers subsection (Section 2.2.1) measures three impact dimensions:

#### 1. Value Delivery
**Definition**: Effectiveness of change in customer experience
**Threshold**: >50% = High Impact, ≤50% = Low Impact
**Data Source**: Section 1 Executive Summary - Business Value bullets
**Extraction Pattern**: `(\d{1,3})%\s*(reduction|improvement|increase|efficiency|optimization|cost\s*savings?|faster|time\s*savings?)`

**Calculation Logic**:
1. Read Section 1 Executive Summary (lines 25-55)
2. Find all percentage metrics in Business Value subsection
3. Extract maximum percentage value
4. If ANY metric > 50% → HIGH Impact
5. If ALL metrics ≤ 50% → LOW Impact
6. Generate justification citing specific percentage and line number

**Example**:
- Input: "70% cost reduction" (Section 1, line 52)
- Output: HIGH Impact - "System delivers 70% cost reduction (Section 1, line 52)"

#### 2. Scale
**Definition**: Estimated number of customers/transactions impacted
**Threshold**: >100K = High Impact, ≤100K = Low Impact
**Data Source**: Section 2.3 Use Cases - Success Metrics
**Extraction Pattern**: `(\d{1,3}(?:,\d{3})*)\+?\s*(?:per\s*day|daily|customers?|users?|transactions?|reminders?|payments?|jobs?)`

**Calculation Logic**:
1. Read Section 2.3 Use Cases (approximate lines 100-165)
2. Find all volume metrics in Success Metrics subsections
3. Extract maximum customer/transaction count
4. If MAX count > 100,000 → HIGH Impact
5. If MAX count ≤ 100,000 → LOW Impact
6. Generate justification citing specific volume and line number

**Example**:
- Input: "500,000+ reminders per day" (Section 2.3, line 141)
- Output: HIGH Impact - "System impacts 500,000 customers/day (Section 2.3, line 141)"

#### 3. Impacts
**Definition**: Implementation complexity (configuration, development, deployment)
**Threshold**: >5 impacts = High Impact, ≤5 impacts = Low Impact
**Data Sources**: Section 5 Component Architecture + Section 8 Technology Stack
**Extraction Method**: Count subsection headers + table rows

**Calculation Logic**:
1. Read Section 5 Component Details (approximate lines 456-675)
2. Count component subsection headers matching pattern: `^###\s+\d+\.\d+`
3. Read Section 8 Technology Stack (approximate lines 912-998)
4. Count technology table rows (excluding headers and separators)
5. Total = component_count + technology_count
6. If total > 5 → HIGH Impact
7. If total ≤ 5 → LOW Impact
8. Generate justification with breakdown

**Example**:
- Input: 5 components in Section 5, 3 technologies in Section 8
- Output: HIGH Impact - "System requires 8 components/technologies (Section 5: 5, Section 8: 3)"

### 6-Phase Calculation Workflow

#### Phase 1: Detection & User Prompt

**Trigger Detection**:
When user request contains keywords:
- "architecture review", "audit", "assess architecture"
- "calculate design drivers", "update design drivers"
- "design impact", "impact metrics"

**User Prompt**:
```
═══════════════════════════════════════════════════════════
Would you like me to calculate Design Drivers impact metrics?
═══════════════════════════════════════════════════════════

Design Drivers provide quantifiable assessment across 3 dimensions:
• Value Delivery: Customer experience effectiveness (from Section 1)
• Scale: Customer/transaction volume impacted (from Section 2.3)
• Impacts: Implementation complexity (from Sections 5 & 8)

This will analyze existing architecture data and update Section 2.2.1.

[Calculate Now] [Skip] [Learn More]
```

If user selects "Learn More", explain:
```
Design Drivers help you:
✓ Quantify business value of architecture decisions
✓ Justify complexity based on scale and impact
✓ Communicate architecture priorities to stakeholders
✓ Validate alignment between architecture and business goals

The calculation is automatic - metrics are extracted from:
• Section 1: Business value percentages
• Section 2.3: Customer/transaction volumes
• Section 5: Component count
• Section 8: Technology count

Results are presented for your review before updating the document.
```

#### Phase 2: Context-Efficient Data Loading

**Sequential Loading Strategy** (minimize context usage):

```bash
# Load only required sections, NOT full document
# Total: ~400-500 lines vs 2000+ full document (75% reduction)

# Step 1: Load Document Index
Read(file_path="ARCHITECTURE.md", offset=1, limit=50)
# Parse index to get exact line ranges for Sections 1, 2, 5, 8

# Step 2: Load Section 1 Executive Summary
Read(file_path="ARCHITECTURE.md", offset=<section1_start-5>, limit=<section1_length+10>)
# Extract Business Value bullets, look for percentage metrics

# Step 3: Load Section 2.3 Use Cases
Read(file_path="ARCHITECTURE.md", offset=<section2.3_start-5>, limit=<section2.3_length+10>)
# Extract Success Metrics from all use cases, look for volume counts

# Step 4: Load Section 5 Component Details
Read(file_path="ARCHITECTURE.md", offset=<section5_start-5>, limit=<section5_length+10>)
# Count subsection headers (### X.X Component Name)

# Step 5: Load Section 8 Technology Stack
Read(file_path="ARCHITECTURE.md", offset=<section8_start-5>, limit=<section8_length+10>)
# Count technology table rows
```

**Context Buffer Guidelines**:
- Add ±5-10 lines buffer to each section for context preservation
- Load sections sequentially (one at a time), not simultaneously
- Extract metrics immediately after loading each section
- Clear temporary data before loading next section

#### Phase 3: Metric Extraction & Calculation

**Algorithm 1: Value Delivery Extraction**

```python
# Pseudo-code for Value Delivery calculation
def extract_value_delivery(section1_text, section1_start_line):
    # Find Business Value subsection
    business_value_match = re.search(r'\*\*Business Value:\*\*(.+?)(?=\n\n|\n\*\*|$)', section1_text, re.DOTALL)

    if not business_value_match:
        return {
            'impact': 'LOW',
            'justification': 'No quantifiable business value metrics found in Section 1',
            'percentage': None,
            'line': None
        }

    business_value_section = business_value_match.group(1)

    # Extract all percentage metrics
    percentage_pattern = r'(\d{1,3})%\s*(reduction|improvement|increase|efficiency|optimization|cost\s*savings?|faster|time\s*savings?)'
    matches = re.finditer(percentage_pattern, business_value_section, re.IGNORECASE)

    percentages = []
    for match in matches:
        percentage = int(match.group(1))
        context = match.group(2)
        # Calculate approximate line number within section
        line_offset = section1_text[:match.start()].count('\n')
        line_number = section1_start_line + line_offset
        percentages.append({
            'value': percentage,
            'context': context,
            'line': line_number
        })

    if not percentages:
        return {
            'impact': 'LOW',
            'justification': 'No percentage-based metrics found in Business Value',
            'percentage': None,
            'line': None
        }

    # Find maximum percentage
    max_metric = max(percentages, key=lambda x: x['value'])

    # Apply threshold
    impact = 'HIGH' if max_metric['value'] > 50 else 'LOW'

    return {
        'impact': impact,
        'justification': f"System delivers {max_metric['value']}% {max_metric['context']} (Section 1, line {max_metric['line']})",
        'percentage': max_metric['value'],
        'line': max_metric['line']
    }
```

**Algorithm 2: Scale Extraction**

```python
# Pseudo-code for Scale calculation
def extract_scale(section2_3_text, section2_3_start_line):
    # Extract all volume metrics from Success Metrics subsections
    volume_pattern = r'(\d{1,3}(?:,\d{3})*)\+?\s*(?:per\s*day|daily|customers?|users?|transactions?|reminders?|payments?|jobs?)'
    matches = re.finditer(volume_pattern, section2_3_text, re.IGNORECASE)

    volumes = []
    for match in matches:
        # Parse number (remove commas)
        volume_str = match.group(1).replace(',', '')
        volume = int(volume_str)
        context = match.group(0)
        # Calculate line number
        line_offset = section2_3_text[:match.start()].count('\n')
        line_number = section2_3_start_line + line_offset
        volumes.append({
            'value': volume,
            'context': context,
            'line': line_number
        })

    if not volumes:
        return {
            'impact': 'LOW',
            'justification': 'No volume metrics found in Use Cases success metrics',
            'volume': None,
            'line': None
        }

    # Find maximum volume
    max_volume = max(volumes, key=lambda x: x['value'])

    # Apply threshold
    impact = 'HIGH' if max_volume['value'] > 100000 else 'LOW'

    # Format volume with commas for readability
    volume_formatted = f"{max_volume['value']:,}"

    return {
        'impact': impact,
        'justification': f"System impacts {volume_formatted} customers/transactions per day (Section 2.3, line {max_volume['line']})",
        'volume': max_volume['value'],
        'line': max_volume['line']
    }
```

**Algorithm 3: Impacts Calculation**

```python
# Pseudo-code for Impacts calculation
def calculate_impacts(section5_text, section8_text):
    # Count component subsection headers in Section 5
    component_pattern = r'^###\s+\d+\.\d+\s+.+$'
    component_matches = re.findall(component_pattern, section5_text, re.MULTILINE)
    component_count = len(component_matches)

    # Count technology table rows in Section 8
    # Exclude header rows (contains | --- |) and empty rows
    tech_table_pattern = r'^\|[^|]+\|[^|]+\|.+\|$'
    separator_pattern = r'^\|\s*-+\s*\|'

    tech_rows = []
    for line in section8_text.split('\n'):
        if re.match(tech_table_pattern, line) and not re.match(separator_pattern, line):
            tech_rows.append(line)

    # First row is typically header, so subtract 1
    # Count unique tables (Languages, Frameworks, Databases, etc.)
    # Approximate: count all rows and subtract ~4-6 header rows
    technology_count = max(0, len(tech_rows) - 5)  # Conservative estimate

    # Total impacts
    total_impacts = component_count + technology_count

    # Apply threshold
    impact = 'HIGH' if total_impacts > 5 else 'LOW'

    return {
        'impact': impact,
        'justification': f"System requires {total_impacts} components/technologies (Section 5: {component_count}, Section 8: {technology_count})",
        'component_count': component_count,
        'technology_count': technology_count,
        'total': total_impacts
    }
```

#### Phase 4: Generate Calculation Report & User Review

**Report Format**:
```
═══════════════════════════════════════════════════════════
  DESIGN DRIVERS IMPACT METRICS CALCULATION
═══════════════════════════════════════════════════════════
Calculation Date: YYYY-MM-DD
Data Sources: Sections 1, 2.3, 5, 8
═══════════════════════════════════════════════════════════

## Value Delivery: [HIGH / LOW]
**Threshold**: >50% = High Impact
**Assessment**: [Percentage]% [context] exceeds threshold
**Justification**: System delivers [X]% [context] (Section 1, line [N])
**Source Data**: [Extracted percentage metric from Business Value]

## Scale: [HIGH / LOW]
**Threshold**: >100K = High Impact
**Assessment**: [Volume] [unit] exceeds threshold
**Justification**: System impacts [X] customers/day (Section 2.3, line [N])
**Source Data**: [Extracted volume metric from Success Metrics]

## Impacts: [HIGH / LOW]
**Threshold**: >5 = High Impact
**Assessment**: [Total] components/technologies exceeds threshold
**Justification**: System requires [Total] components/technologies (Section 5: [C], Section 8: [T])
**Breakdown**:
  - Components (Section 5): [Component count]
  - Technologies (Section 8): [Technology count]

═══════════════════════════════════════════════════════════
SUMMARY: [3/3 HIGH | 2/3 HIGH | 1/3 HIGH | 0/3 HIGH]
═══════════════════════════════════════════════════════════

Next Steps:
1. [Update Section 2.2.1] - Apply these metrics to Design Drivers subsection
2. [Preview Changes] - See exact updates before applying
3. [Manual Override] - Adjust values or classifications manually
4. [Export Report] - Save this report for documentation
5. [Skip Update] - Review only, don't modify ARCHITECTURE.md

Please select an option:
```

**Interactive Review Options**:

1. **Update Section 2.2.1**: Proceed with applying metrics to Design Drivers subsection
2. **Preview Changes**: Show exact before/after of Section 2.2.1
3. **Manual Override**: User can adjust thresholds, classifications, or justifications
4. **Export Report**: Save calculation report to separate file
5. **Skip Update**: Review-only mode, don't modify document

#### Phase 5: Apply Changes to ARCHITECTURE.md

**Insertion Logic**:

1. **Check if Section 2.2.1 exists**:
   ```bash
   Grep(pattern="### Design Drivers", path="ARCHITECTURE.md")
   # or
   Grep(pattern="### 2\.2\.1", path="ARCHITECTURE.md")
   ```

2. **If Section 2.2.1 DOES NOT exist** (first-time insertion):
   - Find end of Section 2.2 (Solution Overview)
   - Insert new Design Drivers subsection after Section 2.2
   - Update section numbering if needed (2.3 Use Cases remains 2.3)
   - Update Document Index (Section 2 line range expands)

3. **If Section 2.2.1 DOES exist** (update existing):
   - Read Section 2.2.1 content
   - Update only the impact assessments and justifications
   - Preserve structure and formatting
   - Update "Last Calculated" date
   - Update "Calculation Method" to "Automatic"

**Section 2.2.1 Template** (for insertion):
```markdown
### 2.2.1 Design Drivers

This architecture is driven by the following key factors:

#### Value Delivery
**Description**: Effectiveness of change in customer experience
- **Threshold**: >50% = High Impact, ≤50% = Low Impact
- **Current Assessment**: [HIGH / LOW] Impact
- **Justification**: [Extracted justification with line reference]

#### Scale
**Description**: Estimated number of customers impacted
- **Threshold**: >100K = High, ≤100K = Low
- **Current Assessment**: [HIGH / LOW] Impact
- **Justification**: [Extracted justification with line reference]

#### Impacts
**Description**: Implementation complexity across configuration, development, and applications
- **Threshold**: >5 = High, ≤5 = Low
- **Current Assessment**: [HIGH / LOW] Impact
- **Justification**: [Extracted justification with component/technology counts]

**Last Calculated**: [YYYY-MM-DD]
**Calculation Method**: Automatic
```

**Update Workflow**:

```bash
# Step 1: Find insertion point
Read(file_path="ARCHITECTURE.md", offset=82, limit=20)  # Section 2.2 Solution Overview
# Identify line number where Section 2.2 ends (before 2.3 Use Cases)

# Step 2: Insert or Update Section 2.2.1
# If new insertion:
Edit(file_path="ARCHITECTURE.md",
     old_string="[End of Section 2.2 content]\n\n### 2.3 Primary Use Cases",
     new_string="[End of Section 2.2 content]\n\n### 2.2.1 Design Drivers\n\n[Full template with calculated values]\n\n### 2.3 Primary Use Cases")

# If updating existing:
Edit(file_path="ARCHITECTURE.md",
     old_string="**Current Assessment**: [old value]\n- **Justification**: [old justification]",
     new_string="**Current Assessment**: [new value]\n- **Justification**: [new justification]")

# Step 3: Update "Last Calculated" date
Edit(file_path="ARCHITECTURE.md",
     old_string="**Last Calculated**: YYYY-MM-DD",
     new_string="**Last Calculated**: 2025-01-26")  # Use current date

# Step 4: Update Document Index (if significant line shift)
# Run grep to detect new section positions
grep -n "^## [0-9]" ARCHITECTURE.md
# Update index line ranges if Section 2 grew by >10 lines
```

#### Phase 6: Completion Report & Verification

**Completion Report Format**:
```
═══════════════════════════════════════════════════════════
  DESIGN DRIVERS UPDATE COMPLETE
═══════════════════════════════════════════════════════════

✓ Section 2.2.1 Design Drivers updated
✓ All three metrics calculated and applied
✓ Document Index updated (Section 2: Lines X-Y)
✓ Last Calculated: 2025-01-26

Summary of Metrics:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Value Delivery:  HIGH   (70% cost reduction)
Scale:           HIGH   (500,000 customers/day)
Impacts:         HIGH   (8 components/technologies)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Overall Design Impact: 3/3 HIGH

Data Sources:
• Section 1 (Executive Summary): Business value metrics
• Section 2.3 (Use Cases): Success metrics and volumes
• Section 5 (Components): Component architecture details
• Section 8 (Technology Stack): Technology inventory

Next Steps:
• Review Section 2.2.1 for accuracy
• Use these metrics in architecture reviews and stakeholder discussions
• Recalculate after significant architecture changes
• Include in quarterly architecture reviews

═══════════════════════════════════════════════════════════
```

**Verification Steps**:
1. Read Section 2.2.1 to confirm updates applied correctly
2. Verify Document Index reflects new line ranges
3. Check "Last Calculated" date is current
4. Confirm all three metrics (Value Delivery, Scale, Impacts) are present
5. Validate justifications include source line numbers

### Edge Cases & Error Handling

#### Edge Case 1: Missing Source Data

**Scenario**: No percentage metrics found in Section 1 Business Value

**Handling**:
```
⚠️ MISSING DATA: Value Delivery

No percentage-based metrics found in Section 1 Business Value.

Recommendations:
1. Add quantifiable business value metrics to Section 1
2. Provide manual override for Value Delivery assessment
3. Default to LOW impact with note: "No quantifiable metrics found"

Would you like to:
[Add Metrics to Section 1] [Manual Override] [Default to LOW]
```

**Default Behavior**:
- Value Delivery: LOW Impact with justification "No quantifiable business value metrics found in Section 1"
- Scale: LOW Impact with justification "No volume metrics found in Use Cases"
- Impacts: Calculate based on available data (Section 5 or 8, whichever exists)

#### Edge Case 2: Threshold Edge Cases (Exactly 50%, 100K, or 5)

**Scenario**: Metric value exactly equals threshold

**Handling**:
```
ℹ️ THRESHOLD EDGE CASE: Scale

Volume metric: Exactly 100,000 customers/day (Section 2.3, line 141)
Threshold rule: >100K = HIGH (not ≥)

Assessment: LOW Impact (100K is NOT > 100K)
Note: Consider if this is close enough to merit HIGH classification

Would you like to:
[Use LOW (strict threshold)] [Override to HIGH] [Review Context]
```

**Default Rule**:
- Thresholds use strict > operator (not ≥)
- 50% exactly → LOW
- 100,000 exactly → LOW
- 5 exactly → LOW
- Flag these cases for user review with note in justification

#### Edge Case 3: Conflicting Metrics (Multiple Percentages)

**Scenario**: Multiple different percentage metrics in Business Value

**Example**:
- 70% cost reduction
- 45% time savings
- 60% efficiency improvement

**Handling**:
```
ℹ️ MULTIPLE METRICS: Value Delivery

Found 3 percentage metrics in Business Value:
• 70% cost reduction (Line 52)
• 45% time savings (Line 53)
• 60% efficiency improvement (Line 54)

Using maximum value approach:
Selected: 70% cost reduction → HIGH Impact

Alternative: Could use average (58.3%) → still HIGH
Alternative: Could list all metrics in justification

Proceed with maximum value (70%)? [Yes] [Use Average] [Custom]
```

**Default Behavior**: Use maximum percentage value

#### Edge Case 4: Non-Standard Document Structure

**Scenario**: Document Index missing or section numbers non-standard

**Handling**:
1. **Fallback Detection**: Use grep to find sections by header text
   ```bash
   grep -n "^## .*Executive Summary" ARCHITECTURE.md
   grep -n "^### .*Primary Use Cases" ARCHITECTURE.md
   grep -n "^## .*Component" ARCHITECTURE.md
   grep -n "^## .*Technology Stack" ARCHITECTURE.md
   ```

2. **Prompt User**:
   ```
   ⚠️ NON-STANDARD STRUCTURE

   Document Index not found or sections not in expected locations.

   Would you like me to:
   1. Search for sections by header text (slower but works)
   2. Update document to standard structure first
   3. Manually specify section line ranges

   Recommended: Option 2 (standardize structure)
   ```

3. **Graceful Degradation**: Calculate what's possible, note what's missing

#### Edge Case 5: Section 2.2.1 Already Exists with Manual Overrides

**Scenario**: User previously set manual override values in Section 2.2.1

**Detection**:
Check if "Calculation Method: Manual Override" exists in Section 2.2.1

**Handling**:
```
⚠️ MANUAL OVERRIDE DETECTED

Section 2.2.1 was previously updated with manual overrides.

Current Values (Manual):
• Value Delivery: HIGH (manually set)
• Scale: LOW (manually set)
• Impacts: HIGH (manually set)

New Calculated Values (Automatic):
• Value Delivery: HIGH (70% cost reduction)
• Scale: HIGH (500,000 customers/day)  ← DIFFERS
• Impacts: HIGH (8 components/technologies)

Differences found:
⚠️ Scale changed from LOW → HIGH (new data: 500,000/day)

Would you like to:
[Accept New Values] [Keep Manual Overrides] [Review Changes] [Merge (keep manual where unchanged)]
```

**Default Behavior**: Present comparison, require user approval before overwriting manual overrides

### Manual Override Workflow

**User can manually override any metric classification:**

```
Manual Override: Value Delivery
────────────────────────────────────────────────────────────
Calculated: HIGH (70% cost reduction)

Override Options:
1. [Keep Calculated] - Use automatic calculation (HIGH)
2. [Change to LOW] - Override to LOW impact
3. [Custom Percentage] - Specify different percentage
4. [Custom Justification] - Keep HIGH but change justification

If overriding, provide reason (for documentation):
[Text input: "Business value is delayed until Year 2"]

Method will be marked as: "Manual Override (2025-01-26)"
```

**When Manual Override Applied**:
- Update "Calculation Method" to "Manual Override"
- Add note in justification: "(Manual override: [reason])"
- Subsequent automatic calculations will prompt before overwriting

### Best Practices for Design Drivers

**DO:**
- ✅ Calculate after major architecture changes (new components, technologies)
- ✅ Recalculate quarterly during architecture reviews
- ✅ Use metrics in stakeholder reviews and justifications
- ✅ Document assumptions in justifications (which metric was used, why)
- ✅ Update source sections (1, 2.3, 5, 8) before calculating for accuracy
- ✅ Review calculation report carefully before applying

**DON'T:**
- ❌ Auto-calculate on every minor documentation update
- ❌ Override thresholds without documentation
- ❌ Ignore edge cases (flag for review, don't silently default)
- ❌ Skip user review (always present findings before updating)
- ❌ Forget to update "Last Calculated" date
- ❌ Calculate with stale data (ensure source sections are current)

### Integration with Existing Workflows

**Metric Consistency Workflow Integration**:
- Design Drivers calculation runs AFTER metric consistency check
- If Section 1 metrics are updated, first sync duplicates, then calculate drivers
- Ensures Design Drivers use most current metric values

**Document Index Integration**:
- Design Drivers insertion may shift Section 2.3 line numbers
- Automatically trigger Document Index update if Section 2 grows >10 lines
- Update index before final completion report

**Architecture Review Integration**:
- When user requests "review", "audit", or "validate" for ARCHITECTURE.md, follow the **Two-Phase Review & Audit Workflow**
- Workflow is documented in: **REVIEW_AUDIT_WORKFLOW.md**
- Phase 1 (Form Validation) must pass before Phase 2 (Content Improvement) runs
- Workflow provides prioritized feedback: HIGH → MEDIUM → LOW criticality

**Two-Phase Workflow Overview**:
  ```
  Phase 1: Form Validation (Blocking)
  ☐ Section structure compliance (12 required sections)
  ☐ Architecture type validation (META/3-TIER/MICROSERVICES/N-LAYER)
  ☐ Architecture Principles enforcement (9 required principles)
  ☐ Markdown formatting validation
  ☐ Metric consistency check (Section 1 vs rest of document)

  If Phase 1 passes → Proceed to Phase 2
  If Phase 1 fails → User must fix ❌ issues before Phase 2 runs

  Phase 2: Content Improvement (Prioritized)
  ☐ HIGH: Missing implementation details, dishonest trade-offs, incomplete components
  ☐ MEDIUM: Weak justifications, outdated references, missing cross-references
  ☐ LOW: Formatting suggestions, clarity improvements, redundancy reduction

  For complete workflow details, see: REVIEW_AUDIT_WORKFLOW.md
  ```

### Example: Complete Design Drivers Calculation

**Scenario**: User requests architecture review for Task Scheduling System

**Step-by-Step Execution**:

1. **User Request**:
   ```
   User: "Review the architecture documentation"
   ```

2. **Detection & Prompt**:
   ```
   [Architecture review trigger detected]

   As part of the architecture review, would you like me to calculate Design Drivers impact metrics?

   Design Drivers assess:
   • Value Delivery: Business value effectiveness
   • Scale: Customer/transaction volume
   • Impacts: Implementation complexity

   [Calculate Now] [Skip] [Learn More]
   ```

3. **User Approves**:
   ```
   User: "Calculate now"
   ```

4. **Context-Efficient Data Loading**:
   ```
   Loading architecture data for Design Drivers calculation...

   ✓ Document Index loaded (lines 1-21)
   ✓ Section 1 Executive Summary loaded (lines 25-54)
   ✓ Section 2.3 Use Cases loaded (lines 99-162)
   ✓ Section 5 Component Details loaded (lines 456-675)
   ✓ Section 8 Technology Stack loaded (lines 912-998)

   Total lines loaded: 436 / 2,000 (78% reduction)
   ```

5. **Metric Extraction**:
   ```
   Extracting metrics from architecture sections...

   Value Delivery:
   ✓ Found: 70% cost reduction (Section 1, line 52)
   ✓ Found: 60% time savings (Section 1, line 53)
   ✓ Maximum: 70% → HIGH Impact (exceeds 50% threshold)

   Scale:
   ✓ Found: 500,000+ reminders per day (Section 2.3, line 141)
   ✓ Found: 10,000+ concurrent jobs (Section 2.3, line 38)
   ✓ Maximum: 500,000 → HIGH Impact (exceeds 100K threshold)

   Impacts:
   ✓ Components (Section 5): 5 subsections found
   ✓ Technologies (Section 8): 3 table rows found
   ✓ Total: 8 → HIGH Impact (exceeds 5 threshold)
   ```

6. **Present Findings**:
   ```
   ═══════════════════════════════════════════════════════════
     DESIGN DRIVERS IMPACT METRICS CALCULATION
   ═══════════════════════════════════════════════════════════
   Calculation Date: 2025-01-26
   Data Sources: Sections 1, 2.3, 5, 8
   ═══════════════════════════════════════════════════════════

   ## Value Delivery: HIGH
   **Threshold**: >50% = High Impact
   **Assessment**: 70% exceeds threshold
   **Justification**: System delivers 70% cost reduction (Section 1, line 52)

   ## Scale: HIGH
   **Threshold**: >100K = High Impact
   **Assessment**: 500,000 exceeds threshold
   **Justification**: System impacts 500,000 customers/day (Section 2.3, line 141)

   ## Impacts: HIGH
   **Threshold**: >5 = High Impact
   **Assessment**: 8 exceeds threshold
   **Justification**: System requires 8 components/technologies (Section 5: 5, Section 8: 3)

   ═══════════════════════════════════════════════════════════
   SUMMARY: 3/3 HIGH - Strong business justification for architecture complexity
   ═══════════════════════════════════════════════════════════

   [Update Section 2.2.1] [Preview Changes] [Manual Override] [Skip]
   ```

7. **User Selects Update**:
   ```
   User: "Update Section 2.2.1"
   ```

8. **Apply Changes**:
   ```
   Updating Section 2.2.1 Design Drivers...

   [Checking if section exists]
   ✓ Section 2.2.1 not found - will insert new section

   [Finding insertion point]
   ✓ Section 2.2 ends at line 97
   ✓ Section 2.3 starts at line 99
   ✓ Insertion point: line 98

   [Inserting Design Drivers subsection]
   ✓ Inserted 22 lines at line 98
   ✓ Section 2.3 renumbered (now starts at line 121)

   [Updating Document Index]
   ✓ Section 2 range updated: Lines 56-154 → Lines 56-176
   ✓ Sections 2.3-12 ranges shifted by +22 lines
   ✓ Index Last Updated: 2025-01-26
   ```

9. **Completion Report**:
   ```
   ═══════════════════════════════════════════════════════════
     DESIGN DRIVERS UPDATE COMPLETE
   ═══════════════════════════════════════════════════════════

   ✓ Section 2.2.1 Design Drivers inserted at line 98
   ✓ All three metrics calculated and applied
   ✓ Document Index updated (Section 2: Lines 56-176)
   ✓ Last Calculated: 2025-01-26

   Summary of Metrics:
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Value Delivery:  HIGH   (70% cost reduction)
   Scale:           HIGH   (500,000 customers/day)
   Impacts:         HIGH   (8 components/technologies)
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   Overall Design Impact: 3/3 HIGH

   This indicates strong business justification for:
   • Architecture complexity (8 components/technologies)
   • Investment in scalability (500K customers/day)
   • Development resources (70% cost reduction value)

   ═══════════════════════════════════════════════════════════
   ```

### Performance & Context Optimization

**Context Usage Comparison**:

| Approach | Lines Loaded | Percentage |
|----------|-------------|------------|
| Full Document Load | 2,000+ | 100% |
| Design Drivers (Context-Efficient) | ~400-500 | 20-25% |
| **Savings** | **~1,500-1,600** | **75-80%** |

**Sequential Loading Benefits**:
- Load one section at a time (not all simultaneously)
- Extract metrics immediately after loading
- Clear section data before loading next
- Minimize context window pressure
- Faster execution with large documents

**Optimization Strategies**:
- Use Document Index for exact line ranges (no guessing)
- Load only ±5-10 line buffers (minimal context)
- Use Grep for initial detection (no loading required)
- Cache Document Index in memory (reuse across phases)
- Batch similar operations (all regex extractions together)

---

### Best Practices

✅ **DO**:
- Load specific sections using `Read` with offset/limit
- Use `Grep` to search for keywords without loading full file
- Work incrementally on one section at a time
- Read the table of contents or first 50 lines to orient yourself

❌ **DON'T**:
- Read the entire ARCHITECTURE.md file unless creating it from scratch
- Load multiple sections simultaneously unless they're small
- Use full document reads for simple section updates
- Assume section locations without checking first

### Example Workflows

**Task 1**: Update the Technology Stack section

1. Read lines 1-50 to confirm document structure
2. Use Grep to find "## Section 8: Technology Stack"
3. Read ONLY Section 8 (estimated lines 1001-1150)
4. Make edits using Edit tool
5. Read ONLY the updated section to verify

**Task 2**: Update Executive Summary metrics and ensure consistency

1. User edits Section 1 Key Metrics (lines 31-38)
2. Skill detects metric change automatically
3. Scan document using Grep for all metric values
4. Load context for each match (Read with ±5 line buffer)
5. Generate consistency report with findings
6. Present findings to user for review
7. If user approves, batch update all locations
8. Update Document Index if needed (>10 lines shifted)
9. Report completion summary

These approaches keep context usage minimal while maintaining full editing capability.

## Documentation Structure

This project uses **ARCHITECTURE_DOCUMENTATION_GUIDE.md** as the authoritative guide for creating and maintaining architecture documentation. This guide ensures consistency across all architecture documents.

## When to Use the Guide

### 1. Creating New Architecture Documents
When starting a new system architecture document (ARCHITECTURE.md):
- Follow the 12-section structure defined in ARCHITECTURE_DOCUMENTATION_GUIDE.md
- Use the templates provided for each section
- Maintain detailed coverage for: Layers, Components, Integration Points, Tech Stack, Security, and Performance
- Keep Executive Summary, System Overview, and Principles concise
- Reference ADR_GUIDE.md for architectural decision documentation

### 2. Documenting Existing Projects
When documenting an existing system:
- Use ARCHITECTURE_DOCUMENTATION_GUIDE.md as the structure template
- Focus on capturing current state accurately (not ideal state)
- Prioritize Sections 4-10 (core architecture sections)
- Document actual technologies, versions, and configurations in use
- Create ADRs for past decisions if context is available

### 3. Maintaining Architecture Documentation
When updating existing architecture docs:
- Follow the same section structure from ARCHITECTURE_DOCUMENTATION_GUIDE.md
- Keep core sections (Layers, Components, Security, Performance) detailed and current
- Update version numbers and technology stack as system evolves
- Add new ADRs for architectural changes
- Review quarterly for accuracy

## Key Principles

1. **Use Templates**: Follow section templates from ARCHITECTURE_DOCUMENTATION_GUIDE.md
2. **Stay Concise**: Keep Sections 1-3, 6, 12 brief; detail in Sections 4-5, 7-11
3. **Separate ADRs**: Use ADR_GUIDE.md for decision records, link from main doc
4. **Maintain Accuracy**: Document actual state, include version numbers
5. **Context Efficiency**: Balance detail with readability for Claude Code context

## Architecture Principles Enforcement (Section 3)

**CRITICAL**: Section 3 must include **9 required principles** in exact order, each with Description, Implementation, and Trade-offs.

### Required Principles

1. Separation of Concerns
2. High Availability
3. Scalability First
4. Security by Design
5. Observability
6. Resilience
7. Simplicity
8. Cloud-Native
9. Open Standards
10. Decouple Through Events (OPTIONAL - apply where async provides clear decoupling benefits)

### Quick Validation

✅ All 9 core principles present in order
✅ Each has Description + Implementation + Trade-offs
✅ Implementation is system-specific (not generic placeholders)
✅ Trade-offs honestly assess costs
✅ Decouple Through Events (#10) only where async benefits temporal decoupling

### Detailed Rules

For complete principle definitions, required structure, validation checklist, common mistakes, and updating workflows:
→ **VALIDATIONS.md** § Architecture Principles Enforcement
→ **ARCHITECTURE_DOCUMENTATION_GUIDE.md** § Section 3: Architecture Principles

## Required Section Names (Strict Enforcement)

**CRITICAL**: All ARCHITECTURE.md documents MUST use these **12 exact section names** in this **exact order**.

### Standard Section Names

1. Executive Summary
2. System Overview
3. Architecture Principles
4. Architecture Layers
5. Component Details
6. Data Flow Patterns (OPTIONAL - omit for simple systems, renumber 7→6, 8→7, etc.)
7. Integration Points
8. Technology Stack
9. Security Architecture
10. Scalability & Performance
11. Operational Considerations
12. Architecture Decision Records (ADRs)

### Format Requirement

Use exact format: `## [NUMBER]. [SECTION NAME]`

**Examples**:
- ✅ `## 1. Executive Summary`
- ✅ `## 12. Architecture Decision Records (ADRs)`
- ❌ `## Executive Summary` (missing number)
- ❌ `## 12. ADR References` (wrong name)

### Verification Command

```bash
grep -n "^## [0-9]" ARCHITECTURE.md
```

### Detailed Rules

For complete section name list, common mistakes, optional sections, renumbering workflow, and validation checklist:
→ **VALIDATIONS.md** § Section Name Enforcement
→ **ARCHITECTURE_DOCUMENTATION_GUIDE.md** § Document Structure Overview

---

## Workflow 7: Informational Query (Answer Questions About Architecture)

### When to Use

This workflow is activated when users ask questions about the architecture documented in ARCHITECTURE.md:
- Understanding how the system works
- Learning about specific architectural decisions
- Querying technology choices, patterns, or principles
- Understanding data flows, integrations, or components

### Automatic Invocation Conditions

This workflow is **automatically invoked** when:
1. User asks an architectural question (authentication, scaling, components, tech stack, etc.)
2. **AND** an ARCHITECTURE.md file exists in the current project

If ARCHITECTURE.md does not exist, this workflow is NOT invoked, allowing general architectural guidance.

**Detection Logic**:
```python
# Check if ARCHITECTURE.md exists before answering architectural questions
architecture_file_exists = file_exists("ARCHITECTURE.md")

if architecture_file_exists:
    # Invoke Workflow 7: Use ARCHITECTURE.md as source of truth
    load_architecture_and_answer()
else:
    # Don't invoke skill - provide general guidance
    pass
```

### Prerequisites

- ARCHITECTURE.md file exists in the project
- Document has valid structure (12 sections)
- Document Index is present (lines 5-21 typically)

### Process

#### Step 1: Detect Query Intent

Classify the user's question to determine which section(s) to reference:

- **Authentication/Security** → Section 9 (Security Considerations)
- **Data flow/pipelines** → Section 6 (Data Flow)
- **Components/services** → Section 5 (Component Details)
- **Technology stack** → Section 8 (Technology Stack)
- **Integrations** → Section 7 (Integration Points)
- **Scaling/performance** → Section 10 (Scalability & Performance)
- **Deployment** → Section 4 (Architecture Layers/Deployment)
- **Patterns/principles** → Section 3 (Architecture Principles)
- **Problem/solution** → Section 2 (System Overview)
- **Operations** → Section 11 (Operational Considerations)
- **Decisions/trade-offs** → Section 12 (Architecture Decision Records)
- **General overview** → Section 1 (Executive Summary)

**See QUERY_SECTION_MAPPING.md for detailed query-to-section mapping guide.**

#### Step 2: Discover Target File via Navigation Index

Read `ARCHITECTURE.md` navigation table to find which `docs/` file contains the target section:

```python
# Read ARCHITECTURE.md navigation index (it's short — ~130 lines)
nav_content = Read(file_path="ARCHITECTURE.md")

# Parse Documentation table to find the file for the target section
# Section routing:
# - Authentication/Security   → docs/07-security-architecture.md
# - Data flow/pipelines       → docs/04-data-flow-patterns.md
# - Components/services       → docs/components/README.md (then individual component files)
# - Technology stack          → docs/06-technology-stack.md
# - Integrations              → docs/05-integration-points.md
# - Scaling/performance       → docs/08-scalability-and-performance.md
# - Deployment/Layers         → docs/03-architecture-layers.md
# - Patterns/principles       → docs/02-architecture-principles.md
# - Problem/solution          → docs/01-system-overview.md
# - Operations                → docs/09-operational-considerations.md
# - Decisions/ADRs            → adr/ directory
# - General overview          → docs/01-system-overview.md
```

#### Step 3: Load Relevant File(s) in Full

Individual `docs/` files are 50–400 lines — load the entire file(s) needed:

```python
# Single-section query
section_content = Read(file_path="docs/07-security-architecture.md")

# Multi-section query: load each relevant file in full
section_a = Read(file_path="docs/01-system-overview.md")
section_b = Read(file_path="docs/08-scalability-and-performance.md")

# Component-specific query
component = Read(file_path="docs/components/01-api-gateway.md")
```

**Context budget** (multi-file structure is more efficient):

| Query Type | Files Loaded | Approx Lines |
|------------|-------------|--------------|
| Single section | 1 docs/ file | ~50–400 |
| Multi-section | 2–4 docs/ files | ~200–800 |
| Component detail | 1 component file | ~40–200 |
| Full architecture | All docs/ files | ~2,000–3,500 |

#### Step 4: Extract Relevant Information

- Search for keywords from user query within the loaded section
- Identify specific subsections that address the question
- Calculate line numbers for citations

**Line Number Calculation**:
```python
# Calculate absolute line numbers for citations
subsection_start = section_range["start"] + relative_offset
subsection_end = subsection_start + content_length

# Example: Found answer in Section 9.2 Authentication
# Section 9 starts at line 1301
# Subsection 9.2 is at relative offset 42 lines into the section
# Absolute line: 1301 + 42 = 1343
```

#### Step 5: Respond with Citations

**Standard Format**: Answer first, then footer citation

**Template**:
```
[Answer content based on ARCHITECTURE.md]

**Source**: ARCHITECTURE.md Section N (Section Name), lines X-Y
```

**Single-Section Answer Example**:
```
User: "How do we handle authentication?"

Response:
The system uses OAuth 2.0 for authentication with JWT tokens for API access.
Multi-factor authentication (MFA) is required for administrative users, and
session tokens expire after 24 hours of inactivity.

**Source**: ARCHITECTURE.md Section 9 (Security Considerations → Authentication &
Authorization), lines 1090-1114
```

**Multi-Section Answer Example**:
```
User: "What's the backend tech stack and how is it deployed?"

Response:
The backend is built with Java 17 and Spring Boot 3.2, using PostgreSQL 15 for the
primary database and Redis 7.0 for caching. The system is deployed on AWS using
containerized services via ECS with auto-scaling enabled for high availability.

**Sources**:
- ARCHITECTURE.md Section 8 (Technology Stack → Backend), lines 901-925
- ARCHITECTURE.md Section 8 (Technology Stack → Data Tier), lines 960-970
- ARCHITECTURE.md Section 4 (Architecture Layers → Cloud Infrastructure), lines 350-375
```

#### Step 6: Handle Missing Information

If the question cannot be answered from ARCHITECTURE.md:

**Response Format**:
1. Provide general architectural guidance from knowledge base
2. Clearly tag that it's NOT from the documented architecture
3. Recommend documenting it in the appropriate section

**Example**:
```
User: "What's our disaster recovery RTO?"

Response:
Based on the 99.99% availability SLA documented in Section 10, a typical RTO would be
4 hours for Tier 1 systems. Common industry practice for this availability tier includes:
- RTO: 4 hours
- RPO: 1 hour
- Quarterly DR testing

⚠️ **Recommendation**: This information is not currently documented in ARCHITECTURE.md.
Consider adding disaster recovery procedures to Section 11.4 (Operational Considerations →
Disaster Recovery) including:
- Recovery Time Objective (RTO)
- Recovery Point Objective (RPO)
- Backup strategies
- Failover procedures
- DR testing schedule

Would you like me to help document these DR procedures?

**Source**: General architectural guidance (not documented in ARCHITECTURE.md)
```

**Missing Information Template**:
```
[General architectural answer based on industry best practices]

⚠️ **Recommendation**: This information is not documented in ARCHITECTURE.md.
Consider adding to Section N.M ([Section Name → Subsection Name]).

Would you like me to help document this?

**Source**: General architectural guidance (not documented in ARCHITECTURE.md)
```

### Context Efficiency Guidelines

**Buffer Sizes**:
- Standard queries: ±20 lines
- Narrow queries (specific config): ±10 lines
- Cross-section queries: ±20 lines per section

**Loading Strategy**:
- Load Document Index once, cache in memory
- Load only relevant sections, not full document
- Use sequential loading (one section at a time) for multi-section queries
- Minimize total lines loaded (aim for <300 lines per query)

**Performance Comparison**:

| Approach | Lines Loaded | Context Usage |
|----------|-------------|---------------|
| Full Document Load | 2,000+ | 100% |
| Workflow 7 (Single Section) | ~100-150 | 5-7.5% |
| Workflow 7 (Multi-Section) | ~200-300 | 10-15% |
| **Savings** | **~1,700-1,800** | **85-90%** |

### Best Practices

**DO:**
- ✅ Always check if ARCHITECTURE.md exists before invoking this workflow
- ✅ Use Document Index for exact section ranges
- ✅ Load only relevant sections with minimal buffers
- ✅ Include line number citations in all responses
- ✅ Explicitly state when information is not documented
- ✅ Offer to help update documentation when gaps are found
- ✅ Use footer citations (answer first, source last)

**DON'T:**
- ❌ Load full document for simple queries
- ❌ Provide answers without citations when ARCHITECTURE.md exists
- ❌ Guess or assume content without reading the document
- ❌ Mix documented architecture with general guidance (clearly separate)
- ❌ Skip buffer context (always load ±10-20 lines)

### Example Workflows

#### Example 1: Question Fully Answered (Single Section)

**User**: "What databases do we use?"

**Process**:
1. Detect query intent → Section 8 (Technology Stack)
2. Load Document Index → Section 8: Lines 912-998
3. Load Section 8 with buffer → Read(offset=891, limit=126)
4. Extract database information → PostgreSQL 15, Redis 7.0, Elasticsearch 8.x
5. Calculate line numbers → Lines 960-970

**Response**:
```
The system uses three database technologies:

**Relational Database**: PostgreSQL 15 for primary transactional data
**Caching**: Redis 7.0 for session management and query caching
**Search**: Elasticsearch 8.x for full-text search and analytics

**Source**: ARCHITECTURE.md Section 8 (Technology Stack → Data Tier), lines 960-970
```

#### Example 2: Information Missing (With Recommendation)

**User**: "What's our backup retention policy?"

**Process**:
1. Detect query intent → Section 11 (Operational Considerations)
2. Load Document Index → Section 11: Lines 1751-1950
3. Load Section 11 with buffer → Read(offset=1730, limit=239)
4. Search for backup/retention information → Not found
5. Provide general guidance + recommendation

**Response**:
```
Based on industry best practices for systems with 99.99% availability, typical backup
retention policies include:
- Daily backups: Retained for 30 days
- Weekly backups: Retained for 90 days
- Monthly backups: Retained for 1 year
- Annual backups: Retained for 7 years (compliance)

⚠️ **Recommendation**: This information is not currently documented in ARCHITECTURE.md.
Consider adding backup retention policies to Section 11.3 (Operational Considerations →
Backup & Recovery) including:
- Backup frequency (full vs. incremental)
- Retention periods by backup type
- Storage location and redundancy
- Recovery testing schedule

Would you like me to help document the backup retention policy?

**Source**: General architectural guidance (not documented in ARCHITECTURE.md)
```

#### Example 3: Multi-Section Query

**User**: "How does user authentication integrate with our external systems?"

**Process**:
1. Detect query intent → Section 9 (Security) + Section 7 (Integrations)
2. Load Document Index
3. Load Section 9 → Read(offset=1280, limit=269)
4. Load Section 7 → Read(offset=980, limit=189)
5. Synthesize answer from both sections
6. Calculate line numbers for both citations

**Response**:
```
User authentication uses OAuth 2.0 with JWT tokens, requiring MFA for administrative
users. External system integrations leverage this authentication framework through
API Gateway integration.

For external systems, authentication tokens are validated at the API Gateway layer
before requests are forwarded to downstream services. Integration with Salesforce
uses OAuth 2.0 client credentials flow, while the payment gateway integration uses
mutual TLS certificates for secure communication.

**Sources**:
- ARCHITECTURE.md Section 9 (Security Considerations → Authentication), lines 1090-1114
- ARCHITECTURE.md Section 7 (Integration Points → External Systems), lines 1025-1048
```

### Query-to-Section Mapping Reference

For detailed query-to-section mapping, including:
- Common question patterns for each section
- Example queries and expected section targets
- Multi-section query scenarios
- Edge cases and ambiguous queries

**See: QUERY_SECTION_MAPPING.md**

### Example Conversations

#### Example 1: Informational Query - Single Section

**User**: "What technologies do we use for the backend?"

**Skill Response**:

*Step 1: Detect query intent → Section 8 (Technology Stack)*
*Step 2: Load Document Index (lines 1-50)*
*Step 3: Load Section 8 with buffer (lines 891-1017)*
*Step 4: Extract backend technologies*

According to ARCHITECTURE.md Section 8 (Technology Stack):

**Backend Stack**:
- Language: Java 17 (LTS)
- Framework: Spring Boot 3.2
- Build Tool: Maven 3.9
- Testing: JUnit 5, Mockito, Spring Test
- Code Quality: SonarQube

**Data Tier**:
- Relational Database: PostgreSQL 15
- Caching: Redis 7.0
- Search: Elasticsearch 8.x

**Source**: ARCHITECTURE.md Section 8 (Technology Stack → Backend & Data Tier), lines 901-970

---

#### Example 2: Informational Query - Information Missing

**User**: "What's our disaster recovery plan?"

**Skill Response**:

*Step 1: Detect query intent → Section 11 (Operational Considerations)*
*Step 2: Load Document Index*
*Step 3: Load Section 11 (lines 1730-1969)*
*Step 4: Search for disaster recovery information → Not found*
*Step 5: Provide general guidance + recommendation*

Based on the 99.99% availability SLA documented in Section 10, typical disaster recovery procedures for this tier include:

- **RTO (Recovery Time Objective)**: 4 hours
- **RPO (Recovery Point Objective)**: 1 hour
- **Backup frequency**: Continuous replication + daily snapshots
- **Failover**: Automated multi-region failover
- **Testing**: Quarterly DR drills

⚠️ **Recommendation**: This information is not currently documented in ARCHITECTURE.md.

Consider adding disaster recovery procedures to Section 11.4 (Operational Considerations → Disaster Recovery) including:
- Recovery Time Objective (RTO)
- Recovery Point Objective (RPO)
- Backup strategies and retention
- Failover procedures
- DR testing schedule

Would you like me to help document these DR procedures?

**Source**: General architectural guidance (not documented in ARCHITECTURE.md)

---

#### Example 3: Informational Query - Multi-Section

**User**: "How does user authentication integrate with our external systems?"

**Skill Response**:

*Step 1: Detect query intent → Section 9 (Security) + Section 7 (Integrations)*
*Step 2: Load Document Index*
*Step 3: Load Section 9 (lines 1280-1569)*
*Step 4: Load Section 7 (lines 980-1169)*
*Step 5: Synthesize answer from both sections*

User authentication uses OAuth 2.0 with JWT tokens for API access. Multi-factor authentication (MFA) is required for administrative users, and session tokens expire after 24 hours of inactivity.

For external system integrations:

**API Gateway Integration**: Authentication tokens are validated at the API Gateway layer before requests are forwarded to downstream services. The gateway enforces token expiration and refresh policies.

**Salesforce Integration**: Uses OAuth 2.0 client credentials flow for service-to-service authentication. Tokens are cached for 30 minutes to reduce authentication overhead.

**Payment Gateway Integration**: Uses mutual TLS certificates for secure communication. Certificate rotation is automated and occurs every 90 days.

All external integrations log authentication events to the central security monitoring system for audit and threat detection.

**Sources**:
- ARCHITECTURE.md Section 9 (Security Architecture → Authentication & Authorization), lines 1090-1114
- ARCHITECTURE.md Section 7 (Integration Points → External Systems), lines 1025-1075

---

#### Example 4: Standard Workflow - Creating Architecture Documentation

**User**: "Create architecture documentation for my microservices system"

**Skill Response**:

*Detects new ARCHITECTURE.md creation → Invokes Architecture Type Selection Workflow*

I'll help you create architecture documentation for your microservices system.

📐 **Architecture Type Selection**

Before creating your ARCHITECTURE.md, please select the architecture type that best describes your system:

**1. META Architecture** (6-Layer Enterprise)
   - Best for: Large enterprise systems, financial services, complex integrations
   - Layers: Channels → UX → Business Scenarios → Integration → Domain → Core
   - Complexity: Very High
   - Team Size: Large (10+)

**2. 3-Tier Architecture** (Classic Web Application)
   - Best for: Web apps, REST APIs, standard CRUD systems
   - Tiers: Presentation → Application/Business Logic → Data
   - Complexity: Low
   - Team Size: Small-Medium (2-8)

**3. Microservices Architecture** (Cloud-Native Distributed)
   - Best for: Cloud-native systems, independently deployable services
   - Components: API Gateway → Services → Data Stores → Event Bus
   - Complexity: High
   - Team Size: Large (10+)

**4. N-Layer Architecture** (Customizable Patterns)
   - Best for: DDD, Clean Architecture, Hexagonal Architecture
   - Patterns: 4-Layer DDD, 5-Layer Extended, Clean Architecture, Hexagonal
   - Complexity: Medium-High
   - Team Size: Medium (4-10)

**Which architecture type best describes your system? (1-4)**

*User selects: "3" (Microservices)*

Perfect! I'll create your ARCHITECTURE.md using the Microservices Architecture template.

*Loads templates/SECTION_4_MICROSERVICES.md and templates/SECTION_5_MICROSERVICES.md*
*Creates ARCHITECTURE.md with:*
- *Standard Sections 1-3*
- *Microservices Section 4 (API Gateway, Service Mesh, Services, Event Bus)*
- *Microservices Section 5 (Service catalog format)*
- *Standard Sections 6-12*
- *Metadata comment: `<!-- ARCHITECTURE_TYPE: MICROSERVICES -->`*

✓ Created ARCHITECTURE.md with Microservices Architecture structure

The document includes:
- Document Index (lines 5-21) for context-efficient navigation
- Section 4: Microservices architecture layers (API Gateway, Service Mesh, Services, Event Bus)
- Section 5: Service catalog template (ready for your services)
- Architecture type metadata for future validation

Next steps:
1. Fill in Section 1 (Executive Summary) with your system overview
2. Document your microservices in Section 5
3. Add your technology stack to Section 8
4. Define integration points in Section 7

Would you like help filling in any specific section?

---

#### Example 5: Metric Consistency Workflow

**User**: "Update the job execution capacity to 700 TPS in the Executive Summary"

**Skill Response**:

*Uses Edit tool to change line 33: "500 TPS" → "700 TPS"*

✓ Updated line 33: Design Capacity - Job Execution: 700 TPS

*Automatic audit triggered because Section 1 Key Metrics was edited*

Now checking for metric consistency across the document...

*Scans document for references to old value (500 TPS)*
*Found at: Line 787, Line 1368*
*Loads context around each match*

═══════════════════════════════════════════════════════════
   METRIC CONSISTENCY AUDIT REPORT
═══════════════════════════════════════════════════════════
Triggered by: Executive Summary edit (Line 33)
Metric Changed: Job Execution Capacity (500 TPS → 700 TPS)
═══════════════════════════════════════════════════════════

## ⚠️ Outdated References Found

| Location | Section | Current Value | Should Be | Context |
|----------|---------|---------------|-----------|---------|
| Line 787 | Section 6: Data Flow Patterns | 500 TPS capacity | 700 TPS | "Performance: p50 = 40ms, p95 = 100ms, p99 = 200ms \| 500 TPS capacity" |
| Line 1368 | Section 10: Scalability & Performance | 500 | 700 | Throughput table: "Job Execution (TPS) \| 500 \| 1000 \| 2000" |

═══════════════════════════════════════════════════════════
SUMMARY: Found 2 locations with outdated metric value
═══════════════════════════════════════════════════════════

Would you like me to update both locations to 700 TPS?

Options:
1. [Yes - Update All] - Apply changes to both locations
2. [Preview Changes] - Show me exactly what will change
3. [Manual Selection] - Let me choose individually
4. [No Thanks] - I'll handle this manually

*User selects: "Preview changes first"*

Here's a preview of the changes I'll make:

════════════════════════════════════════════════════════════
CHANGE 1 of 2: Line 787 (Section 6: Data Flow Patterns)
════════════════════════════════════════════════════════════

Current:
**Performance**: p50 = 40ms, p95 = 100ms, p99 = 200ms | 500 TPS capacity

Will become:
**Performance**: p50 = 40ms, p95 = 100ms, p99 = 200ms | 700 TPS capacity

════════════════════════════════════════════════════════════
CHANGE 2 of 2: Line 1368 (Section 10: Throughput Table)
════════════════════════════════════════════════════════════

Current:
| Job Execution (TPS) | 500 | 1000 | 2000 | Downstream domain services |

Will become:
| Job Execution (TPS) | 700 | 1000 | 2000 | Downstream domain services |

════════════════════════════════════════════════════════════

Proceed with both changes? [Yes/No/Choose Individually]

*User approves: "Yes"*

Applying metric consistency updates...

✓ Updated line 787: "500 TPS capacity" → "700 TPS capacity"
✓ Updated line 1368: Throughput table "500" → "700"
✓ Document Index is still accurate (no update needed)

═══════════════════════════════════════════════════════════
   METRIC CONSISTENCY UPDATE COMPLETE
═══════════════════════════════════════════════════════════

Summary of Changes:
✓ Executive Summary (Line 33): 500 TPS → 700 TPS
✓ Section 6 Data Flow (Line 787): 500 TPS → 700 TPS
✓ Section 10 Throughput Table (Line 1368): 500 → 700

Total Locations Updated: 3
Document Index: No update needed
All references to Job Execution Capacity are now consistent.

═══════════════════════════════════════════════════════════

---

## Workflow 8: Diagram Generation (Generate Architecture Diagrams)

**⚡ Auto-triggers**: This workflow automatically activates when diagram-related keywords are detected (see Automatic Workflow Detection section above).

### When to Use

This workflow is activated when users request diagram generation from ARCHITECTURE.md:
- "Generate architecture diagrams"
- "Create Mermaid diagrams from architecture"
- "Generate diagrams for my architecture"
- "Add diagrams to ARCHITECTURE.md"
- User references: "visual diagrams", "data flow diagrams", "component diagrams"

### Activation Triggers

**Automatic Invocation:**
- User asks to "generate diagrams", "create diagrams", "add diagrams", "make diagrams"
- User specifies: "Mermaid diagrams", "architecture diagrams", "visual diagrams"
- User references sections: "generate diagrams for Section 4"

**Manual Invocation:**
- User explicitly: "Run diagram generation workflow"
- User references: `/workflow diagrams`

### Prerequisites

- **`docs/` directory exists** with numbered section files
- **`ARCHITECTURE.md`** navigation index exists at project root
- **MERMAID_DIAGRAMS_GUIDE.md** available for templates (optional but recommended)

### Step-by-Step Process

#### Step 1: Detect Request & Present Diagram Type Options

When diagram generation is requested, present the following:

```
📊 **Architecture Diagram Generation**

I'll generate Mermaid architecture diagrams for your architecture documentation.

**Mandatory Diagrams** (always generated — not optional):
- ✅ High-Level Architecture Diagram → `docs/03-architecture-layers.md`
- ✅ Data Flow Diagrams → `docs/04-data-flow-patterns.md`

**Additional Diagrams** (opt-in — select any or none):
A. Infrastructure & Deployment → `docs/09-operational-considerations.md`
B. High Availability & Failover → `docs/08-scalability-and-performance.md`
C. Performance & Scaling → `docs/08-scalability-and-performance.md`
D. Integration Diagrams → `docs/05-integration-points.md`
E. Security Architecture Diagrams → `docs/07-security-architecture.md`

Would you like any additional diagrams? Reply with letters (e.g., "A, C") or "None".
```

**Mandatory Diagram Details**:
- **High-Level Architecture**: `graph TB` flowchart — all tiers/layers, color-coded, component relationships
- **Data Flow Diagrams**: `sequenceDiagram` — participant interactions, message flows, pipeline descriptions

**On-Request Diagram Details**:
- **Infrastructure / Deployment**: `graph TB` with subgraphs — AKS, pods, containers, load balancers
- **HA / Failover**: `graph TB` or `graph LR` — replica sets, failover paths, health checks
- **Performance / Scaling**: `graph LR` — autoscaling groups, cache layers, throughput flows
- **Integration**: `sequenceDiagram` or `graph LR` — API endpoints, event flows, webhooks
- **Security**: `graph TB` — auth flows, encryption boundaries, identity providers

---

#### Step 2: External Diagram Detection & Intake

**Check for external diagrams**:
- Has the user provided or referenced an external file path containing diagrams?
- Has the user pasted mermaid code blocks directly in their message?

**If NO external diagrams detected** → Skip to Step 4.

**If external diagrams detected**:

1. Scan the external file for all ` ```mermaid ` code blocks
2. For each diagram found, extract: heading/title, mermaid type (`graph TB`, `sequenceDiagram`, etc.), key node labels
3. **Classify each diagram** using keyword analysis of mermaid content:

   | Keywords Found | Diagram Category | Canonical Target |
   |----------------|-----------------|-----------------|
   | Subgraph + layer/tier/presentation/application/data | Architecture Layer | `docs/03-architecture-layers.md` |
   | `sequenceDiagram` OR data/message/pipeline/flow patterns | Data Flow | `docs/04-data-flow-patterns.md` |
   | AKS/VM/cluster/pod/container/LB/namespace | Infrastructure | `docs/09-operational-considerations.md` |
   | HA/failover/replica/primary/secondary/standby | High Availability | `docs/08-scalability-and-performance.md` |
   | autoscale/performance/throughput/scaling/cache | Performance | `docs/08-scalability-and-performance.md` |
   | API/endpoint/webhook/integration/event/queue | Integration | `docs/05-integration-points.md` |
   | auth/token/encryption/TLS/identity/RBAC/security | Security | `docs/07-security-architecture.md` |
   | (none of the above match) | **UNCLASSIFIABLE** | Ask user |

4. Present intake inventory to user:

```
📥 **External Diagram Intake**

Found [N] diagrams in [filename]:

| # | Title | Type | Detected Category | Proposed Target |
|---|-------|------|-------------------|----------------|
| 1 | [title] | graph TB | Architecture Layer | docs/03-architecture-layers.md |
| 2 | [title] | sequenceDiagram | Data Flow | docs/04-data-flow-patterns.md |
| 3 | [title] | graph TB | UNCLASSIFIABLE | ❓ Please classify |

For unclassifiable diagrams, choose: A) Architecture Layer  B) Data Flow  C) Infrastructure
D) High Availability  E) Performance  F) Integration  G) Security  H) Discard
```

---

#### Step 3: External Diagram Reconciliation

**Architecture docs are the single source of truth. A diagram is valid ONLY if its flow or component is documented.**

**Reconciliation logic**:
1. Load relevant architecture doc sections incrementally (03, 04, 05, 07, 08, 09) — read headings and key entity names only (±30 lines per section)
2. For each external diagram, extract key entities: node names, subgraph labels, participant names, component identifiers
3. Match each diagram's entities against documented components and flows in architecture docs
4. Score and classify each diagram:

   | Score | Classification | Action |
   |-------|---------------|--------|
   | All entities match documented components | **MATCH** | RELOCATE to canonical location |
   | 50%+ entities match, some undocumented | **PARTIAL** | RELOCATE with WARNING listing undocumented entities |
   | < 50% entities match documented components | **NO MATCH** | **DISCARD** — notify user |

5. Present reconciliation report and wait for confirmation:

```
📋 **Reconciliation Report**

| # | Title | Category | Action | Notes |
|---|-------|----------|--------|-------|
| 1 | [title] | Architecture Layer | ✅ RELOCATE | Full match |
| 2 | [title] | Data Flow | ⚠️ RELOCATE | Warning: "ServiceX" not documented |
| 3 | [title] | Infrastructure | ❌ DISCARD | No matching components found |

Summary: [N] to relocate, [N] discarded (undocumented flows)

⚠️ Discarded diagrams cannot be relocated until their flows are documented in the architecture docs.
To retain a discarded diagram, document its flow first, then re-run this workflow.

Proceed with reconciliation? [Yes/No]
```

**STRICT ENFORCEMENT**: No override for discarded diagrams. "Insert anyway" is not an option.

---

#### Step 4: Canonical Location Enforcement (Mandatory — No Override)

**All diagrams — whether newly generated or relocated from external files — MUST go to their canonical location. This is not configurable.**

**Mandatory diagrams** (always generated):

| Diagram Type | Canonical Location | Section |
|-------------|-------------------|---------|
| High-Level Architecture | `docs/03-architecture-layers.md` | Section 4 |
| Data Flow Diagrams | `docs/04-data-flow-patterns.md` | Section 6 |

**On-request diagrams** (generated only when user opts in):

| Diagram Type | Canonical Location | Section |
|-------------|-------------------|---------|
| Integration Diagrams | `docs/05-integration-points.md` | Section 7 |
| Security Diagrams | `docs/07-security-architecture.md` | Section 9 |
| Infrastructure / Deployment | `docs/09-operational-considerations.md` | Section 11 |
| HA / Failover | `docs/08-scalability-and-performance.md` | Section 10 |
| Performance / Scaling | `docs/08-scalability-and-performance.md` | Section 10 |

**Enforcement behavior**: If user requests non-canonical placement (e.g., "put it in ARCHITECTURE.md inline" or "add it to a different section"), DENY the request and explain:

```
⛔ **Non-Canonical Placement Denied**

Diagrams must be placed in their canonical docs/ file. This is a regulatory-grade
enforcement rule and cannot be overridden.

The canonical location for [Diagram Type] is: [canonical file]

All placements follow the canonical location table above.
```

---

#### Step 5: Confirmation (with Reconciliation Summary)

```
**Step 5: Confirmation**

I'll proceed with the following actions:

**Diagrams to Generate** ([N] total):
- ✅ High-Level Architecture → docs/03-architecture-layers.md [REQUIRED]
- ✅ Data Flow Diagrams → docs/04-data-flow-patterns.md [REQUIRED]
- [optional diagrams selected by user]

**External Diagrams to Relocate** ([N] total):
- ✅ [title] → docs/03-architecture-layers.md (full match)
- ⚠️ [title] → docs/04-data-flow-patterns.md (partial match — warning noted)

**Discarded External Diagrams** ([N] total):
- ❌ [title] — No matching documented flow found

**Placement Rule**: All diagrams follow canonical location enforcement (not configurable).

Proceed? [Yes/No]
```

---

#### Step 6: Load Document & Identify Architecture Type

**Process**:
1. Read lines 1-50 of ARCHITECTURE.md (Document Index)
2. Parse Document Index to extract section line ranges
3. Read Section 1 (Executive Summary) to identify system type
4. Determine architecture pattern (3-Tier, Microservices, META, N-Layer)
5. Read Section 4 (Architecture Layers) to understand component structure

**Architecture Type Detection**:
- **3-Tier**: Frontend → Backend → Database layers
- **Microservices**: Multiple independent services with API Gateway
- **META (6-Layer)**: Channels → UX → Business Scenarios → Service Orchestration → Atomic Services → Data
- **N-Layer**: Custom layered architecture (N > 3)

---

#### Step 7: Load Required Sections Incrementally

**Context-Efficient Loading**:
- Load only sections required for the selected diagram types
- Use ±10 line buffer around each section
- Minimize total lines loaded

**Sections by Diagram Type**:
- **Mandatory (High-Level + Data Flow)**: Sections 3, 4 (~300-500 lines, 80-90% reduction vs. full document)
- **+ Infrastructure**: add Section 9 (~150-250 lines additional)
- **+ HA / Performance**: add Section 8 (~200-350 lines additional)
- **+ Integration**: add Section 5 (~150-250 lines additional)
- **+ Security**: add Section 7 (~150-250 lines additional)
- **Full set (all 7 types)**: Sections 3, 4, 5, 7, 8, 9 (~800-1200 lines, 40-60% reduction)

---

#### Step 8: Generate Diagrams Using MERMAID_DIAGRAMS_GUIDE Templates

**Process**:
1. Load MERMAID_DIAGRAMS_GUIDE.md for reference templates
2. For each diagram to generate:
   - Identify appropriate Mermaid diagram type (graph TB, sequenceDiagram, etc.)
   - Extract key components, data flows, or infrastructure elements from loaded sections
   - Apply color scheme and styling from templates
   - Generate complete Mermaid code block
3. For Sequence Diagrams (Default Set only):
   - Parse `docs/04-data-flow-patterns.md` (Section 6) to identify every H3 subsection (`### [Flow Name] Flow`)
   - For **each** H3 flow subsection, generate one `sequenceDiagram`:
     - Extract participants and step-by-step interactions from that flow's description
     - Map each step as `ComponentA->>ComponentB: Action` entries
     - Title the diagram `#### Diagram: [Flow Name] Sequence`
   - Total sequence diagrams = number of H3 flow subsections found in Section 6
4. Format all diagrams with proper markdown code block syntax

**Mermaid Diagram Types**:
- **High-Level Architecture**: `graph TB` (top-to-bottom flowchart)
- **Data Flow**: `sequenceDiagram` (participant interactions)
- **Infrastructure**: `graph TB` with subgraphs (nested component groups)
- **HA/Performance**: `graph TB` or `graph LR` (left-to-right)
- **Integration**: `sequenceDiagram` or `graph LR`
- **Security**: `graph TB` with trust boundary annotations

**Color Scheme** (from MERMAID_DIAGRAMS_GUIDE):
```
classDef presentation fill:#4A90E2,stroke:#2E5C8A,stroke-width:3px,color:#fff
classDef application fill:#F5A623,stroke:#B8791A,stroke-width:3px,color:#fff
classDef data fill:#7ED321,stroke:#5A9B18,stroke-width:3px,color:#fff
classDef cache fill:#BD10E0,stroke:#8A0CA3,stroke-width:3px,color:#fff
classDef azure fill:#0078D4,stroke:#005A9E,stroke-width:2px,color:#fff
```

---

#### Step 9: Insert Diagrams into Canonical Docs Files

**Process**:
1. For each diagram (generated or relocated):
   - Identify the canonical target file from the location table
   - Read target file to find appropriate insertion point:
     - For `docs/03-architecture-layers.md`: **ALWAYS append at the end of the document**
     - For `docs/04-data-flow-patterns.md`: insert each sequence diagram **immediately after its corresponding H3 flow subsection** (`### [Flow Name] Flow`), in document order
     - For all other files: insert after the section header, before the next subsection
   - Use Edit tool to insert diagram with proper heading and description

2. Maintain proper markdown structure:
   ```markdown
   #### Diagram: High-Level System Architecture

   ```mermaid
   graph TB
       %% Diagram content here
   ```

   **Description**: This diagram shows...
   ```

3. For relocated external diagrams: prepend a note indicating the diagram was relocated and from which file.

---

#### Step 10: Flow-Diagram Completeness Audit

**This step always runs and cannot be skipped.**

**Audit scope**: Check all canonical diagram types. Missing REQUIRED diagrams produce a compliance warning. Missing OPTIONAL diagrams are informational gaps.

**Audit logic**:

1. **Extract all documented flows** from each canonical file by scanning headings and content:

   | Canonical File | Looks For | Diagram Type | Tag |
   |---------------|-----------|--------------|-----|
   | `docs/03-architecture-layers.md` | Layer headings, component relationships | High-Level Architecture | `[REQUIRED]` |
   | `docs/04-data-flow-patterns.md` | Flow names, sequence descriptions, pipeline descriptions | Data Flow | `[REQUIRED]` |
   | `docs/05-integration-points.md` | Integration endpoints, API interactions, event flows | Integration | `[OPTIONAL]` |
   | `docs/07-security-architecture.md` | Auth flows, encryption flows | Security | `[OPTIONAL]` |
   | `docs/08-scalability-and-performance.md` | Scaling flows, HA/failover flows | HA / Performance | `[OPTIONAL]` |
   | `docs/09-operational-considerations.md` | Deployment flows, CI/CD pipelines | Infrastructure | `[OPTIONAL]` |

2. **Match flows to existing mermaid blocks**: search each canonical file for ` ```mermaid ` blocks within ±30 lines of the flow heading, with entity name matching

3. **Generate completeness report**:

```
📊 **Flow-Diagram Completeness Audit**

| Section File | Flows Found | With Diagram | Missing | Coverage |
|-------------|-------------|--------------|---------|----------|
| 03-architecture-layers.md | 3 | 3 | 0 | 100% ✅ [REQUIRED] |
| 04-data-flow-patterns.md | 4 | 3 | 1 | 75% ⚠️ [REQUIRED] |
| 05-integration-points.md | 2 | 0 | 2 | 0% ℹ️ [OPTIONAL] |
| 07-security-architecture.md | 1 | 1 | 0 | 100% ✅ [OPTIONAL] |
| 08-scalability-and-performance.md | 2 | 2 | 0 | 100% ✅ [OPTIONAL] |
| 09-operational-considerations.md | 1 | 0 | 1 | 0% ℹ️ [OPTIONAL] |

**Missing Diagrams**:
1. [Flow Name] in docs/04-data-flow-patterns.md — suggested type: sequenceDiagram [REQUIRED]
2. [Integration Name] in docs/05-integration-points.md — suggested type: graph LR [OPTIONAL]
3. [Deployment Pipeline] in docs/09-operational-considerations.md — suggested type: graph TB [OPTIONAL]

Generate missing diagrams? [Yes/All | Yes/Select | No]
```

4. **If user selects Yes/All**: Generate all missing diagrams and insert at canonical locations, then re-run audit to confirm 100% coverage on REQUIRED types
5. **If user selects Yes/Select**: Present numbered list, user picks which to generate
6. **If user selects No**: Log audit result, proceed to verification

**Compliance rule**: If any `[REQUIRED]` diagram is missing after the audit step, verification includes a compliance warning.

---

#### Step 11: Verification & User Notification

**Verification Checklist**:
- ✅ / ❌ All diagrams in canonical locations
- ✅ / ❌ No diagrams outside canonical locations
- ✅ / ❌ Mermaid syntax valid (no unclosed code blocks)
- ✅ / ❌ Proper headings and descriptions added
- ✅ / ❌ Document structure maintained
- ✅ / ❌ / N/A External reconciliation complete (N/A if no external file)
- ✅ / ❌ Flow-diagram completeness audit complete

**Notification**:
```
✅ **Diagram Generation Complete**

**Actions Performed**:
- Generated: [N] new diagrams
- Relocated: [N] external diagrams (from [source file])
- Discarded: [N] external diagrams (undocumented flows — see reconciliation report)

**Diagram Inventory**:
| Diagram | Type | Location | Status |
|---------|------|----------|--------|
| High-Level Architecture | graph TB | docs/03-architecture-layers.md | ✅ GENERATED |
| [Data Flow Name] | sequenceDiagram | docs/04-data-flow-patterns.md | ✅ GENERATED |
| [External Diagram] | graph TB | docs/09-operational-considerations.md | ✅ RELOCATED |

**Completeness Coverage**:
- REQUIRED diagrams: [N]/[N] (100% ✅) — or — ⚠️ COMPLIANCE WARNING: [N] REQUIRED diagrams missing
- OPTIONAL diagrams: [N]/[N] ([%])

**Placement**: All diagrams follow canonical location enforcement.

**Rendering**:
- GitHub/GitLab: Diagrams will render automatically
- VS Code: Install "Markdown Preview Mermaid Support" extension
- Online: Use https://mermaid.live/ to preview

**Next Steps**:
- Review diagrams in your markdown viewer
- Verify all components are correctly represented
- Request updates if any changes needed
```

### Error Handling

**Common Issues**:

| Issue | Solution |
|-------|----------|
| ARCHITECTURE.md not found | Prompt user to create ARCHITECTURE.md first (Workflow 1) |
| Document Index missing | Generate index first or use default line ranges with warning |
| Section not found | Skip that diagram type or prompt user to add section |
| Invalid Mermaid syntax | Validate syntax using mermaid-cli or fallback to simpler diagram |
| External file not found | Prompt user for correct file path |
| External file has no mermaid blocks | Notify user, skip reconciliation, proceed to generation |
| Diagram category unclassifiable | Present to user, ask for manual classification (A–H options) |
| Flow documented but diagram generation fails | Log failure, continue with remaining diagrams, report in audit |
| User requests non-canonical placement | DENY — explain canonical rule and display location table |
| Completeness audit finds 0 flows in any section | Warn that architecture docs section may be incomplete |

### Integration with Other Workflows

**Workflow Dependencies**:
- **Workflow 1 (Create ARCHITECTURE.md)**: Must run first if document doesn't exist
- **Workflow 7 (Informational Query)**: Can answer questions about generated diagrams

**Complementary Actions**:
- Recommend adding diagram descriptions to ADRs (Workflow 2) if design decisions are visualized

---

## Workflow 9: Migrate Existing ARCHITECTURE.md to docs/ Structure

**⚡ Auto-triggers**: This workflow activates when migration/restructuring keywords are detected.

### Automatic Workflow Detection

Add this trigger pattern to the `## 🎯 AUTOMATIC WORKFLOW DETECTION` section:

#### Workflow 9: Migrate Existing ARCHITECTURE.md to docs/ Structure

**Triggers:**
- Keywords: "migrate", "restructure", "split", "reorganize", "convert" + "architecture", "ARCHITECTURE.md"
- Size complaints: "too large", "too long", "hard to navigate", "split into files"
- Explicit: "migrate my architecture to the new structure", "convert to docs/ layout"

**Action when detected:**
1. Confirm: "I'll help you migrate ARCHITECTURE.md to the multi-file docs/ structure."
2. Jump directly to Workflow 9, Step 1

### When to Use

- Existing project has a monolithic `ARCHITECTURE.md` (typically 2,000–5,000+ lines)
- User wants to split into the multi-file `docs/` structure
- User mentions the file is "too large", "hard to navigate", or wants it "split into files"

### Step-by-Step Process

#### Step 1: Analyze Current Document

```bash
# Count total lines
wc -l ARCHITECTURE.md

# Find all major sections
grep -n "^## [0-9]" ARCHITECTURE.md
```

Report to user:
- Total lines in ARCHITECTURE.md
- Sections found (list with line numbers)
- Estimated destination files with line count estimates

#### Step 2: Propose Target Structure

Show the user the exact target layout based on what was found in Step 1:

```
I'll split ARCHITECTURE.md into these files:

docs/
├── 01-system-overview.md           (~NNN lines — Sections 1+2)
├── 02-architecture-principles.md   (~NNN lines — Section 3)
├── 03-architecture-layers.md       (~NNN lines — Section 4)
├── 04-data-flow-patterns.md        (~NNN lines — Section 6)
├── 05-integration-points.md        (~NNN lines — Section 7)
├── 06-technology-stack.md          (~NNN lines — Section 8)
├── 07-security-architecture.md     (~NNN lines — Section 9)
├── 08-scalability-and-performance.md (~NNN lines — Section 10)
├── 09-operational-considerations.md (~NNN lines — Section 11)
├── 10-references.md                (~NNN lines — References)
└── components/
    ├── README.md                   (component index)
    └── NN-<component>.md           (one per Section 5 subsection)

ARCHITECTURE.md → rewritten as navigation index (~130 lines)

Proceed? [Yes/No]
```

**Wait for user approval before making any changes.**

#### Step 3: Extract Files (Safe Order)

**CRITICAL**: Do NOT delete content from `ARCHITECTURE.md` until all destination files are verified.

1. Create `docs/` and `docs/components/` directories
2. **Extract Section 5 sub-components first** (each subsection → `docs/components/NN-name.md`):
   - Add breadcrumb at top: `[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > <Component Name>`
   - Adjust ADR link depth from `adr/` to `../../adr/`
   - Convert any `(Section X.Y)` references to relative Markdown links
3. Create `docs/components/README.md` as component index (links to all component files)
4. Extract remaining sections in order → `docs/NN-name.md` files:
   - Update `(Section X.Y)` references to relative Markdown links
   - Adjust ADR links from `adr/` to `../adr/`

#### Step 4: Rewrite ARCHITECTURE.md as Navigation Index

Use the navigation index template from **RESTRUCTURING_GUIDE.md** to replace the full content of `ARCHITECTURE.md` with the navigation table + ADR table.

#### Step 5: Update External References

- `adr/README.md` — update any "Section 12 of ARCHITECTURE.md" references to point to `ARCHITECTURE.md` directly
- `compliance-docs/COMPLIANCE_MANIFEST.md` — check for stale section/line references

#### Step 6: Verify

Run the verification checklist from **RESTRUCTURING_GUIDE.md**.

---

## Optional Enhancements

- Data Flow Patterns (for complex flows)
- **Architecture Diagrams**: Create visual representations using Mermaid (recommended)
  - Section 4 templates include Mermaid examples for each architecture type
  - See MERMAID_DIAGRAMS_GUIDE.md for complete instructions
  - Supported diagram types: Layer diagrams, service topology, data flow, sequence diagrams
  - Benefits: Interactive, version-control friendly, renders in GitHub/GitLab
  - Alternative formats: C4 Model, PlantUML also supported
- Cost breakdown tables
- Disaster recovery procedures
- Runbooks (common operations and incident response procedures)

## Reference Documents

This skill includes the following asset files:

- **ARCHITECTURE_DOCUMENTATION_GUIDE.md**: Primary template and structure guide (located in skill assets)
- **ADR_GUIDE.md**: Architectural Decision Record format and guidelines (located in skill assets)
- **RESTRUCTURING_GUIDE.md**: Multi-file docs/ structure reference — directory layout, naming conventions, cross-reference conventions, and verification checklist

These reference documents are bundled with this skill and will be available when the skill is active.

**Note**: ARCHITECTURE_DOCUMENTATION_GUIDE.md is optimized for Claude Code context management. Load it when creating or significantly updating architecture docs.