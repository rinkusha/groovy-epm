# 00 — Quickstart: how an EPM Groovy rule actually runs

## The mental model

```
┌──────────────────────────┐    runs     ┌────────────────────────────────┐
│ Data form / Jobs page /  │ ─────────▶  │  Your script (extends          │
│ right-click menu / REST  │             │  oracle.epm.api.script.EpmScript)│
└──────────────────────────┘             │                                │
                                         │  operation  ─▶ Operation       │
                                         │     .application ─▶ Application│
                                         │     .cube        ─▶ Cube       │
                                         │     .grid        ─▶ DataGrid   │
                                         │     .user        ─▶ User       │
                                         │  rtps ─▶ Map<String,RtpValue>  │
                                         │                                │
                                         │  last String expr ─▶ executed  │
                                         │  as an Essbase calc script     │
                                         └────────────────────────────────┘
```

- **`EpmScript`** is the base class. All its methods (`println`, `cscParams`, `fixValues`, `throwVetoException`, `validateRtp`, `messageBundle`, `csvWriter`, `executeJob`, `json`, `workbook`, `getEpmAutomate`, …) are called unqualified. Full list in `01-api-reference.md`.
- **`operation`** is the current job. Everything in the object model hangs off it. If a script references `operation.grid` and there is no grid, EPM stops the script with an error in the job console — check `operation.hasGrid()` first when the rule can run without a form.
- **`rtps`** holds the run-time prompts declared on line 1: `/*RTPS: {Scenario} {Year} {EmployeeName}*/`. Access as `rtps.Scenario`. Each is an `RtpValue`:
  - `rtps.X.enteredValue` → the raw string the user typed/picked
  - `rtps.X.member` → the `Member` object (member prompts)
  - `rtps.X.members` → `List<Member>` (multi-member prompts)
  - `"$rtps.X"` inside a GString → calc-script-ready value (quoted member names, numeric literal for numbers). This is why `FIX($rtps.Scenario)` works.
- **Return value:** Groovy returns the last evaluated expression. If it is a `String`, EPM executes it as an Essbase calc script. Two consequences:
  1. To run a calc script, make the `"""…"""` GString the last expression (or `return calcScript.toString()`).
  2. To *not* run anything, finish with a bare `return`.

## Script contexts (where the same syntax is used)

| Context | How it is wired | `operation.grid` available? |
|---|---|---|
| Business rule run from Jobs / EPM Automate / REST | Calculation Manager rule, Script Type = Groovy | No |
| Form **After Load** | Form → Business Rules tab, "Run After Load" | Yes — use to format cells, set defaults |
| Form **Before Save** | "Run Before Save" | Yes — validate, `addValidationError`, `throwVetoException` vetoes the save |
| Form **After Save** | "Run After Save" | Yes — `cell.edited` tells you what changed; focused calc / Smart Push |
| Right-click **Action Menu** | Menu item → Business Rule, with member context passed as RTPs | Yes (the form the menu was opened on) |
| **Template** with Design-Time Prompts | Calculation Manager template; DTPs become `rtps` | Depends on where the generated rule is attached |
| **Groovy expression** in a rule (one-liner) | Same engine; "expression" vs "script" is just length | Same |

Groovy also exists in **Data Management/Data Integration** (Jython there, not this API) and in **EDMCS expressions** — different engines; this repo is only about the Calculation Manager Groovy API.

## Minimal rule skeletons

**1. Format a form after load**
```groovy
/*RTPS: */
// Cube: any   Trigger: After Load
operation.grid.dataCellIterator().each { DataCell cell ->
    if(cell.missing)          cell.bgColor = 0x00BFFF   // #MISSING → light blue
    else if(cell.data < 0)    cell.bgColor = 0xFF8C00   // negatives → orange
}
```

**2. Calculate only what was edited (After Save)**
```groovy
/*RTPS: */
// Cube: OEP_FS   Trigger: After Save
Set<String> edited = []
operation.grid.dataCellIterator({ DataCell c -> c.edited }, MemberNameType.ESSBASE_NAME).each { DataCell c ->
    edited << c.periodName << c.getMemberName("Product")
}
if(!edited) { println "No edited cells"; return }

String calc = """
FIX(${cscParams(operation.grid.pov, edited)})
  "OFS_Calculated"("OFS_Product Revenue" = "OFS_Volume" * "OFS_Avg Selling Price";)
ENDFIX"""
println calc
return calc          // last String → executed as calc script
```

**3. Veto a save (Before Save)**
```groovy
/*RTPS: */
def mb  = messageBundle(["val.neg":"Salary cannot be negative for {0}"])
def mbl = messageBundleLoader(["en": mb])
operation.grid.dataCellIterator('Salary').each { DataCell c ->
    if(c.edited && c.data < 0)
        throwVetoException(mbl, "val.neg", c.getMemberName("Employee"))
}
return
```

## Deploying a rule (what Copilot should tell the user)

1. Navigator → **Rules** (Calculation Manager) → select the application → Rules → **New**.
2. Name it, pick the **cube**, Designer = **Edit Script**, Script Type = **Groovy Script**.
3. Paste the script, **Validate** (RTPs need sample values; the rule is not executed during validation), **Save**, **Deploy**.
4. Attach to a form: form editor → **Business Rules** tab → add the rule → set *Run After Load / Run Before Save / Run After Save / Use Members on Form / Hide Prompt* as needed.
5. Run and read **Jobs** → the job → *Details* for `println` output and the generated calc script.

## Debugging

- `println` everything you compute before returning it — the Jobs console is your only log.
- Benchmark with `currentTimeMillis()` around iterators.
- Wrap `cube.executeCalcScript(calc)` in `try/catch(CalcScriptException e)` and read `e.atReturnMessage` to get values back from `@RETURN`.
- Errors shown to users must go through `throwVetoException`; uncaught exceptions appear as a generic failure.
