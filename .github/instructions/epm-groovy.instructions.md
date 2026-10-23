---
applyTo: "**/*.groovy"
---

# Rules for every `.groovy` file in this repo

These are Oracle EPM Cloud server-side business rules (Calculation Manager, Script Type = Groovy Script).

1. Line 1 is the RTP header: `/*RTPS: {Prompt1} {Prompt2}*/` (or `/*RTPS: */` when none). Nothing may precede it.
2. Lines 2–6: a comment block stating **Cube**, **Form** (if form-bound), **Trigger** (After Load / Before Save / After Save / Right-click menu / Jobs page), and the dimension/member assumptions.
3. No `import` lines for `oracle.epm.api.*`. No `@Grab`, no `System.*`, no `new File`, no `Thread`.
4. If the rule is form-bound, guard: `if(!operation.hasGrid()) return`.
5. Read with `DataGridDefinitionBuilder` → `cube.loadGrid(def, false)`; write with `DataGridBuilder` → `cube.saveGrid(grid)`; close grids with `withCloseable`.
6. Build calc-script member lists with `cscParams(...)` / `fixValues(...)`. Return the calc script `String` as the **last statement only when it must execute**; otherwise end with `return`.
7. User-facing errors: `throwVetoException(...)` (optionally with `messageBundle` + `messageBundleLoader`). Log with `println`.
8. HTTP only through `operation.application.getConnection("Name")` or `connection(url,user,pwd)`; parse JSON with `JsonPath.parse(response.body).read('$.path')`; build JSON with `json([...])`.
9. Poll long-running REST jobs with exponential back-off starting at 50 ms, capped at 1000 ms, and stop on a terminal status.
10. Keep member/cube/form names as clearly-marked placeholders; never hard-code an environment URL or password — use the Connections page.
