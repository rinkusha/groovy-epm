# Copilot instructions — Oracle EPM Cloud Groovy business rules

This repository is a knowledge base for writing **server-side Groovy business rules for Oracle Fusion Cloud EPM** (Planning / EPBCS / FreeForm / Workforce / Financials / Projects / Capital / Strategic Modeling / EPCM). Every `.groovy` file here is an EPM rule that runs **inside** Calculation Manager, not a standalone Groovy program.

When asked to write, fix, or explain an EPM Groovy rule, follow these rules.

## 1. Runtime model (never forget this)

- Scripts are compiled against `oracle.epm.api.script.EpmScript`. Every public method of `EpmScript` (`println`, `cscParams`, `fixValues`, `throwVetoException`, `validateRtp`, `messageBundle`, `csvWriter`, `executeJob`, `json`, `getEpmAutomate`, …) is callable **unqualified**.
- Entry points are the implicit variables:
  - `operation` → `oracle.epm.api.model.Operation` (the running job: `operation.application`, `operation.cube`, `operation.grid`, `operation.user`, `operation.hasGrid()`).
  - `rtps` → `Map<String, RtpValue>` of run-time prompts declared in the **first line** comment `/*RTPS: {Name1} {Name2}*/`.
- The classes in `oracle.epm.api.*` (`Cube`, `DataGrid`, `DataCell`, `Dimension`, `Member`, `DataGridBuilder`, `DataGridDefinitionBuilder`, `HttpResponse`, `JobFactory`, `MemberNameType`, `DynamicChildStrategy`, …) are **already imported** — do not add `import oracle.epm...` lines.
- `operation.grid` only exists when the rule runs from a data form (after load / before save / after save / right-click menu). If a rule may run from the Jobs page, guard with `if(!operation.hasGrid()) return`.
- **Return value rule:** if the **last expression** of the script is a `String`, EPM treats it as an **Essbase calc script and executes it** against the cube. To avoid accidental execution, end non-calc scripts with `return` (or a non-String value). To deliberately run a calc script, build it in a `"""…"""` GString and return it.
- Output goes to the **Jobs console** via `println`. There is no stdout.

## 2. House style for generated code

- First line is always the RTP declaration, even when empty: `/*RTPS: */`.
- Type the closure parameter in iterators: `operation.grid.dataCellIterator().each { DataCell cell -> … }` (not bare `it`) in anything longer than one line.
- Build FIX members with `cscParams(...)` or `fixValues(...)` — never hand-concatenate quotes, except the `"${list.join('", "')}"` idiom for a simple list of names.
- Use `MemberNameType.ESSBASE_NAME` when member names will be injected into a calc script and the app uses aliases/shared names.
- Early-exit when nothing was edited: collect edited members into a `Set`, `if(!set) { println "No edited cells"; return }`.
- Veto user actions with `throwVetoException(...)`; for user-facing messages use `messageBundle` + `messageBundleLoader` so text is localizable.
- Use `withCloseable { }` on anything that implements close: grids built by `DataGridBuilder`, `csvWriter`, `createFilePrintWriter`, `workbook()`, `csvIterator`, zip streams.
- Comment the **form/cube/dimension assumptions** at the top of every rule (cube name, which dims are POV/row/column). Member and cube names differ per application — treat all names in this repo as placeholders to be replaced.
- Prefer `DataGridDefinitionBuilder` + `cube.loadGrid()` to read data, `DataGridBuilder` + `cube.saveGrid()` to write cell-level data, calc scripts for bulk math, Smart Push / `DataMap` to move data between cubes, `executeJob` to call other rules.

## 3. Things Copilot must NOT do

- Do not generate `import` statements for `oracle.epm.*`, `groovy.json`, or Java classes already available; do not use `@Grab`.
- Do not use `System.out`, `Thread`, file paths outside the inbox/outbox, `new File(...)`, reflection, or network calls via `java.net` — the sandbox forbids them. Use `operation.application.getConnection("Name")` (connections are defined in the app's **Connections** page) or `connection(url, user, pwd)` for HTTP.
- Do not call `Thread.sleep`; `sleep(ms)` from Groovy's DefaultGroovyMethods is allowed in polling loops.
- Do not invent API methods. If unsure, say "verify in the API reference" and link `docs/01-api-reference.md` or the Oracle javadoc URL in `docs/04-links.md`.
- Do not write Essbase calc scripts that `FIX` on every dimension by hand when `operation.grid.pov` is available — use `cscParams(operation.grid.pov, editedMembers)`.
- Do not assume the `data` of a `#MISSING` cell is distinguishable from `0` — check `cell.missing`.

## 4. Where to look in this repo

| Need | File |
|---|---|
| Mental model, lifecycle, how a rule is wired to a form | `docs/00-quickstart.md` |
| Class/method reference (condensed javadoc) | `docs/01-api-reference.md` |
| Copy-paste patterns (edited-cell calc, Smart Push, REST + polling, member CRUD, CSV export…) | `docs/02-patterns-cookbook.md` and `examples/*.groovy` |
| Pitfalls and error messages | `docs/03-gotchas.md` |
| Official Oracle URLs | `docs/04-links.md` |

When the user asks for a new rule, start from the closest file in `examples/` and adapt it, keeping its structure (RTPS line → assumptions comment → validation → capture edited members → act → return).
