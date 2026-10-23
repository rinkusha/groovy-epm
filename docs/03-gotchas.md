# 03 — Gotchas, error messages, and how to avoid them

| Symptom | Cause | Fix |
|---|---|---|
| Rule "succeeds" but nothing calculates | Script did not end with the calc-script `String` (e.g. a `println` after it) | Make the GString the last expression or `return calcScript.toString()` |
| Essbase syntax error on a rule that only reads data | A trailing String literal / method returning String got executed as a calc script | End the script with `return` |
| `No grid is available for this operation` (or similar) | Rule run from Jobs page / EPM Automate but uses `operation.grid` | Guard with `if(!operation.hasGrid()) return`, or pass members as RTPs |
| `cell.data` is `0` for cells that look empty | `#MISSING` reads as 0 | Test `cell.missing` first |
| FIX includes alias names and fails | Member names taken with default name type on an aliased form | `dataCellIterator(pred, MemberNameType.ESSBASE_NAME)` or `pov*.essbaseMbrName` |
| `IllegalScriptArgumentException: null items` from `cscParams` | A `null` slipped into the list (e.g. `getMemberName` of a dim not on the grid) | Filter nulls; check the dimension really is on the form |
| DataGridBuilder saves 0 cells, `numRejectedCells` > 0 | Read-only intersection (security, locked period, dynamic calc, approvals) | Check `status.cellsRejected`; use `ExecuteAsUser.SYSTEM_USER` if it is a security scope issue |
| Smart Push pushes nothing | Override map value not a quoted list, or Smart Push not attached to the form | `["Dim": '"A", "B"']` ; check `grid.hasSmartPush(name)` |
| Validation of the rule fails on RTPs | RTPs need sample values at validation time; the rule is not executed | Enter valid members; mark RTPs *Hide Prompt* / *Use Members on Form* on the form |
| `addDynamicChild` throws | Parent not *Enable for Dynamic Children* or no free placeholders | Enable on the parent, increase *Number of Possible Dynamic Children*, refresh cube |
| New member not visible to calc | Essbase not refreshed | `executeJob(jf.job("Refresh", "Cube Refresh", [:]))` after metadata changes |
| REST call returns 401/403 | Named Connection creds / URL wrong, or connection type mismatch | Fix under **Tools → Connections**; for internal APIs use the "Other Web Service Provider" type with the pod URL |
| `JsonPath` read throws `PathNotFoundException` | Different response shape (DM vs Planning vs OIC) | `println(response.body)` first; guard with `ctx.read('$.jobId', String)` inside try/catch |
| Long REST job times out the rule | Rule exceeded the Groovy execution limit while polling | Back-off polling, or fire-and-forget and check status in a scheduled second rule |
| `sleep` not allowed / sandbox error | Used `Thread.sleep` | Use Groovy's `sleep(ms)` |
| File not found in outbox | File name has a path or illegal characters | Simple names only: letters, digits, `_ - .`, spaces |
| `getEpmAutomate` fails | Caller is not a Service Administrator, or command not server-side supported | Run as admin; check the supported-commands list (docs/04) |
| Script works in one app, fails in another | Member/cube/dimension names differ | Every name in this repo is a placeholder — map them |
| Groovy compile error on a `//` comment that wraps | Pasted from a PDF/HTML tutorial where a comment line wrapped | Re-add `//` to the wrapped line |
| `Calc script exceeded …` | Focused calc still too wide | Narrow with edited members; split by entity; run via `executeJob` in parallel rules |

## Sandbox limits to remember
- No `java.io.File`, `java.net.*`, threads, reflection, `System.*`, `@Grab`, `Grape`, `ClassLoader`.
- JSON: use `json(map)` to build and `JsonPath` (`ReadContext`) to read. `groovy.json.JsonSlurper` is available in most pods but JsonPath is what Oracle documents.
- Available Java libs: standard `java.util`, `java.text`, `java.time`, `java.math`, `java.util.zip` (via the provided stream helpers).
- Execution time and memory are capped per job; iterate once, collect, then act.

## Form-rule lifecycle details
- **Before Save** runs before data hits Essbase: `cell.edited` and the *new* values are available; `addValidationError`/`throwVetoException` cancel the save.
- **After Save** runs after Essbase has the data; `cell.edited` still tells you what changed on that save.
- **After Load** cell formatting (`bgColor`) is visual only and lost on refresh.
- **Right-click menu** rules get the clicked member(s) through RTPs that the menu maps (e.g. `Employee` = row member). Set *Use Members on Form* so the user is not prompted.
