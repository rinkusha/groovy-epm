# 02 — Patterns cookbook

Each pattern points to the full example in `examples/`. Member/cube names are placeholders.

## P1. "What did the user change?" (the core of every form rule)
```groovy
Set<String> edited = []
operation.grid.dataCellIterator({ DataCell c -> c.edited }, MemberNameType.ESSBASE_NAME).each { DataCell c ->
    edited << c.periodName << c.getMemberName("Product")
}
if(!edited) { println "nothing edited"; return }
```
Variants: `dataCellIterator('Salary','Bonus')` (by member), `dataCellIterator({ DataCell c -> c.missing })`, `{ c -> c.locked }`, `{ c -> !c.readOnly }`.
→ `examples/02`, `04`, `13`, `14`

## P2. Focused calc script from the POV + edited members
```groovy
String calc = """
FIX(${cscParams(operation.grid.pov, edited)})
  "Calc Member"("Target" = "A" * "B";)
ENDFIX"""
return calc.toString()           // executed because it is the last String
```
If the calc already fixes on a dimension that is in the POV, exclude it:
`operation.grid.pov.findAll { DataGrid.HeaderCell h -> h.dimName != "Plan Element" }`.
→ `examples/02`, `19`

## P3. Run a calc now and keep going (`executeCalcScript` + `@RETURN`)
→ `examples/14`

## P4. Validate and veto
```groovy
def mbl = messageBundleLoader(["en": messageBundle(["k.neg":"{0} cannot be negative"])])
cell.addValidationError(0xFF0000, "message")        // marks the cell, blocks the save
throwVetoException(mbl, "k.neg", memberName)        // stops the operation with a localized message
validateRtp(rtps.X, /regex/, mbl, "key", rtps.X.enteredValue)
validateRtp(rtps.X, { (1..10).contains(it.enteredValue as int) }, mbl, "key", rtps.X)
```
→ `examples/03`, `06`, `19`

## P5. Read a slice (DataGridDefinitionBuilder → loadGrid)
→ `examples/03`, `08`, `12`, `16`. Remember `withCloseable`, `false` to keep `#MISSING`, `crossDimCell` to hop to a sibling member, `setSuppressMissingBlocks(true)` to shrink big retrieves.

## P6. Write cells (DataGridBuilder → saveGrid)
→ `examples/09`, `16`, `17`. `ExecuteAsUser.SYSTEM_USER` to bypass the runner's security; inspect `Status` for rejected cells; `'#missing'` to clear.

## P7. Move data between cubes
| Situation | Use |
|---|---|
| Form has a Smart Push defined | `operation.grid.getSmartPush("Name").execute(overrides[, exclusions])` → `examples/04` |
| No form; Data Map exists | `operation.application.getDataMap("Name").createSmartPush().execute(overrides)` → `examples/05` |
| Same cube, different intersection | `cube.createCopyDataRequest()` … `copyData()` ⚠ or a calc script `DATACOPY` |

Override map values are **one string** per dimension: `'"Mbr1", "Mbr2"'`.

## P8. Member CRUD
```groovy
parent.hasChild(name)
parent.addDynamicChild(name)                                    // dynamic-children-enabled parent
dim.saveMember(parent.newChildAsMap(name), DynamicChildStrategy.ALWAYS_DYNAMIC)
dim.saveMember(["Member": m, "Parent": p] as Map<String,Object>)          // move
dim.saveMember(["Member": m, "New Name": n] as Map<String,Object>)        // rename
member.delete()                                                 // + descendants!
dim.getEvaluatedMembers('ILvl0Descendants("Total Entity")', cube)         // member functions
```
→ `examples/06`, `07`. Follow with a Cube Refresh job when Essbase must see the change (`examples/13`).

## P9. Call another rule / ruleset / refresh
```groovy
Job j = executeJob(operation.application.jobFactory.job("Rule Name", "Rules", ["rtp": cscParams(...)]))
```
→ `examples/13`

## P10. REST with polling
```groovy
HttpResponse<String> r = operation.application.getConnection("DM").post().body(json([...])).asString()
if(!(200..299).contains(r.status)) throwVetoException("Error: $r.statusText")
ReadContext ctx = JsonPath.parse(r.body)
for(long d = 50; status == IN_PROGRESS; d = Math.min(1000, d*2)) { sleep(d); status = poll(...) }
```
Internal targets: Data Management (`/aif/rest/V1/jobs`), Planning jobs (`/HyperionPlanning/rest/v3/applications/{app}/jobs` → status 0 ok / -1 running / >0 failed), Migration (`/interop/rest`), EDMCS (`/epm/rest/v1`), external APIs (OIC, Workday, S3 presigned URLs, Google Places).
→ `examples/10`, `11`

## P11. Files in the inbox/outbox
```groovy
csvWriter('out.csv').withCloseable { CsvWriter o -> o.writeNext 'a','b' }
csvIterator('in.csv').withCloseable { it.each { String[] row -> … } }
createFilePrintWriter('log.txt').withCloseable { it.println 'x' }
workbook().withCloseable { Workbook b -> b.createSheet().with { addRow('a','b') }; b.save('x.xlsx') }
```
→ `examples/12`, `17`, `18`, `22`. Download with EPM Automate `downloadfile`.

## P12. EPM Automate from inside the pod
→ `examples/15`. Service Administrator only. `execute('cmd','arg1',…)` → `status`, `output`, `itemsList`.

## P13. Text and Smart List cells
- Text account: store an id → `getTextCellId("some text", true)` then `"Account" = $id;` in the calc.
- Smart List: `getEntryId("ListName", "EntryName")` for calc scripts; `cell.dataAsSmartListMemberName` / `cell.formattedValue` to read; write the **label** via DataGridBuilder.

## P14. Logging / profiling
```groovy
long t0 = currentTimeMillis()
… 
println "took ${currentTimeMillis() - t0} ms"
printf("%-20s %,12.2f%n", name, value)
```

## P15. Guarding the return value
End every non-calc script with `return`. A stray trailing string literal, a `println` is fine (void), but `"done"` as the last line **will be sent to Essbase** and fail validation.

## P16. CSV with a variable/limited column list, imported slowly and watchably
When the CSV's column set changes run to run (e.g. one period, a quarter, or a
custom subset instead of a fixed Jan-Dec list), read the header row first and
build the `DataGridBuilder` column list from it instead of hard-coding `PERIODS`:
```groovy
List<String[]> lines = []
csvIterator(FILE).withCloseable { it.each { String[] v -> lines << v } }
List<String> periods = lines[0][2..-1]*.trim()   // header after Entity,Account
b.addColumn(*periods)
dataRows.eachWithIndex { String[] v, int idx ->
    // ... addRow ...
    println "Row ${idx + 1}/$total: ..."
    sleep(PAUSE_MS)                              // deliberate throttle, small files only
}
```
→ `examples/22`. Use a small `sleep(ms)` between rows (not `Thread.sleep`) only for
small, human-watched files; never throttle a bulk/level-0 load this way.

## P17. Multi-dimension file import: three ways to map columns, pick by robustness/reuse
| Mechanism | Where the dimension mapping lives | Groovy's job |
|---|---|---|
| **Data Management Import Format + Data Load Rule** | Configured once in Data Management (Data Exchange > Data Load Mapping): file column -> dimension, plus value mappings. Reusable across runs/files. | Upload the file (EPM Automate `uploadfile`), then only **trigger + poll** the data load rule — internal REST `/aif/rest/V1/jobs` (`examples/10`) or `getEpmAutomate().execute('rundatarule', ...)` / `importdata` (`examples/15`). No `DataGrid`/`DataGridBuilder` code at all. → `examples/23` |
| **Data Map (`DataMap`)** | Configured once in Planning (Data Exchange > Data Maps): source cube/dims -> target cube/dims. | `operation.application.getDataMap("Name").execute(clearFirst)` or `.createSmartPush().execute(overrides)` — moves data **cube to cube**, not from an external file. → `examples/05`, P7. |
| **`DataGridBuilder` over a parsed CSV** | Hand-written in the Groovy rule itself (column index -> `addRow`/`addColumn` position). | Parse every row yourself with `csvIterator`, build the grid, `cube.saveGrid(grid)`. No reuse outside this script; fine for small ad-hoc driver files. → `examples/17`, `22`. |

Rule of thumb: if the file's column layout is stable and reusable, map it once in Data
Management and keep Groovy to trigger/poll (`P10`, `examples/23`). Reach for
`DataGridBuilder` CSV parsing (`P16`) only for small, one-off files where setting up an
Import Format isn't worth it. `DataMap` is a different problem — cube-to-cube, not file-to-cube.

## P18. Import a Planning cell-format file directly into a cube
For a file that already has one data cell per line, use `Cube.createDataImportRequest()`
instead of parsing rows into a `DataGridBuilder`:
```groovy
cube.createDataImportRequest()
    .setDateFormat("dd/MM/yyyy")
    .setDataLoadOption(ADD_TO_EXISTING_VALUE)
    .setErrorFileName("Data_Load.log")
    .importDataFromFile("Data_File.txt", CELL_FORMAT, ',')
```
Upload `examples/24_import_data_file_request_sample.txt` to the inbox as `Data_File.txt`
before running `examples/24_import_data_file_request.groovy` from the Jobs page. The sample
has no header: each line contains members in the target cube's required dimension order,
then the data value. The sample order is Scenario, Version, Year, Period, Entity, Currency,
Account, Value; replace the placeholder members and adapt the order to the target cube.
The `Close Date` account in the sample must be a date account. Rejected rows are written to
the configured outbox error file. These `DataImportRequest` methods and enum constants are
marked ⚠ in `docs/01-api-reference.md`; confirm their live signatures/constants in Oracle's
Javadoc before deploying.

## P19. Submit an EDMCS node import into a view
EDMCS is a separate product and runtime from Calculation Manager Groovy. From an EPM Groovy
rule, use a named Connection to call the EDMCS REST API; upload the import file to EDMCS
before submitting the request. `examples/27` demonstrates request submission only: a
successful HTTP response does not mean the asynchronous import has completed. Inspect its
response and the EDMCS request history, and confirm the endpoint and body fields against the
live EDMCS REST API reference before deploying.
