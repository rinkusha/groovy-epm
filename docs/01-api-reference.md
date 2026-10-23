# 01 — Condensed API reference (`oracle.epm.api.*`)

Source: *Oracle Fusion Cloud EPM Groovy Scripting Reference* (javadoc E96254-40, Jan 2026) and Oracle's Groovy tutorials. Sections marked **[javadoc]** were transcribed from the javadoc; sections marked **[usage]** are compiled from Oracle tutorials/examples and list the methods those examples actually call. Anything marked ⚠ should be confirmed against the live javadoc (`docs/04-links.md`) before relying on an exact signature.

Packages: `oracle.epm.api`, `oracle.epm.api.grid`, `oracle.epm.api.model`, `oracle.epm.api.model.csv`, `oracle.epm.api.model.excel`, `oracle.epm.api.model.rest`, `oracle.epm.api.script`.

**All classes:** AllocationParameters (+ enums AllocationMethod, BasisTimeSpanOption, DataLoadOption, NegativeBasisOption, RoundingMethod, SpreadSkipOption, TargetTimeSpanOption, ZeroAmountOption, ZeroBasisOption), Application, ApplicationType, AttributeDimension, AttributeMember, BaseRequest, CloseableIterator, Connection, CopyDataRequest, CsvWriter, Cube, CurrencyMode, CustomCalcParameters (+DataLoadOption), DataExporter, DataExportLevel, DataFileFormat, DataGrid (+ Cell, DataCell, HeaderCell, Row), DataGridBuilder (+Status), DataGridDefinition (+GridSegment), DataGridDefinitionBuilder, DataImportAsoDataValueOption, DataImportAsoDuplicateAggregationMethod, DataImportDataLoadOption, DataImporter, DataImportRequest, DataMap, DataType, DataValue, DataValueProvider, DateUnit, DayCountBasis, Dimension, DynamicChildStrategy, EpmScript, EssbaseCompatible, Excel, ExecuteAsUser, FiscalCalendar, FiscalYearStart, FlexibleDataGridDefinitionBuilder (+GridSegment), GridIterator, GroovyDynamicLocalizedException, GroovyMessageBundle, GroovyResourceBundleLoader, HttpMethod, HttpRequest, HttpRequestWithBody, HttpResponse, InterestCalcMethod, Job, JobDefinition, JobFactory, JobStatus, Member, MemberNameType, MlModel, MlPredictionStatus, Operation, PaymentFrequency, PaymentType, PeriodDimension, RequestBodyEntity, RowData, RtpValue, Rule, ScenarioRollup, SmartList (+Entry), SmartPush, StrategicModel, StrategicModelMissingValueOption, StrategicModelPreserveValueOption, SubstitutionVariable, Tuple, User, UserVariable, VetoOperationException, Workbook, YearBasis, YearDimension.

---

## `EpmScript` (oracle.epm.api.script) — base class, all methods unqualified **[javadoc]**

> "By default, parameters are case insensitive and may not be null or empty unless otherwise specified."

### Calc-script helpers
| Method | Returns | Notes |
|---|---|---|
| `cscParams(Object... items)` / `cscParams(Collection items)` | `String` | Flattens arrays/iterables/maps(values), **quotes** each item, comma-separates. Accepts `HeaderCell`, `RtpValue`, `Member`, `String`. Plug into `FIX(...)`. Throws `IllegalScriptArgumentException` on null items. |
| `fixValues(EssbaseCompatible... objects)` | `String` | Like `cscParams` but typed: `Member`, `Dimension`, `RtpValue`, … → `FIX(${fixValues(rtps.Scenario, rtps.Year, employee)})`. |
| `mdxParams(Object... items)` | `String` | Same idea for **MDX** delimited identifiers (ASO allocations / custom calcs). |
| `stripQuotes(String text)` | `String` | Removes double quotes. |
| `getEntryId(String smartListName, String entryName)` | `long` | Smart List entry id (entry name or label key e.g. `LABEL_ACTIVE`). |
| `getTextCellId(String textVal, boolean createIfNotFound)` | `int` | Id to assign a text value in a calc script: `"Employee Address" = $address;`. |

### Validation / messages
| Method | Notes |
|---|---|
| `messageBundle(Map<String,String> resourceMap)` → `GroovyMessageBundle` | In-memory bundle; `{0}` placeholders. |
| `messageBundleLoader(Map<String,GroovyMessageBundle> byLocale)` → `GroovyResourceBundleLoader` | e.g. `["en": mbUs]`. |
| `validateRtp(RtpValue rtp, Closure/Predicate<RtpValue> p, loader, String key, Object... params)` | Closure receives the `RtpValue` as `it`; veto if false. |
| `validateRtp(RtpValue rtp, String regex, loader, String key, Object... params)` | Regex form. |
| `throwVetoException(loader, String key, Object... params)` | Localized veto shown to user. |
| `throwVetoException(String message)` | Plain veto. |
| `throwVetoException(HspLocalizedException / HspDynamicLocalizedException cause)` | Re-throw. |

### Logging / timing
`print(Object)`, `println()`, `println(Object)`, `printf(String, Object)`, `printf(String, Object[])` → Jobs console. `currentTimeMillis()`, `nanoTime()`.

### Files (inbox/outbox only; simple names: letters, digits, `_ - .` and spaces; no paths)
| Method | Notes |
|---|---|
| `csvWriter(String file)` / `(file, boolean quoteAll)` / `(file, char sep)` / `(file, String sep)` / `(file, char sep, boolean quoteAll)` / `(file, String sep, boolean quoteAll)` / `(file, char sep, boolean quoteAll, boolean bom)` → `CsvWriter` | `out.writeNext 'a','b','c'`; overwrites; use `withCloseable`. |
| `csvIterator(String file)` / `(file, char sep)` / `(file, String sep)` → `CloseableIterator<String[]>` | Reads a CSV from the outbox row by row. |
| `createFileWriter(String file)` / `(file, String charset, boolean bom)` → `Writer` | Appears in Outbox when closed. |
| `createFilePrintWriter(String file)` / `(file, charset, bom)` → `PrintWriter` | |
| `createZipFileWriter(String zip, String innerFile)` → `Writer` | |
| `zipInputStream(String file)` / `zipOutputStream(String file)` | Java `ZipInputStream`/`ZipOutputStream` on outbox files. |
| `createEpmCellCsvWriter(Writer, Collection<Dimension> rowDims, Collection<Member> colMembers[, char delim])` | "Cell" format: one value per line. |
| `createEpmColumnCsvWriter(Writer, Collection<Dimension> rowDims, Collection<Member> colMembers[, char delim])` | "Column" format: column members across. |
| `createRowData(Tuple rowMembers, Collection/Set<Member> colMembers, Map<String,String> colToValue)` / `createRowData(Tuple, Map)` → `RowData` | Row objects for data export/import. |
| `workbook()` → `Workbook` | New Excel workbook: `createSheet([name])`, `sheet.addRow(...)`, `getSheet(i)`, `save('x.xlsx')`, `write(OutputStream)`. Close it. |

### Jobs / REST / misc
| Method | Notes |
|---|---|
| `executeJob(JobDefinition jobDef)` → `Job` | **Synchronous**; child job of current rule. Types: Rules, Ruleset, Template, Cube Refresh, Clear Cube, Scenario Rollup. |
| `connection(String url, String user, String password)` → `Connection` | Ad-hoc HTTP connection (prefer named Connections). |
| `json(Map<String,?> input)` → `String` | Map → JSON string for request bodies. |
| `getEpmAutomate()` → `EpmAutomate` | Admin only. `automate.execute('cmd', 'arg1', …)` → `EpmAutomateStatus` with `getStatus()` (0 = ok), `getOutput()`, `getItemsList()`. |
| `getRtps()` → `Map<String,RtpValue>` | Same as the implicit `rtps`. |
| `getCurrentRule()` / `getRule()` → `Rule` | |

---

## `Operation` **[usage]**
| Member | Type | Notes |
|---|---|---|
| `operation.application` | `Application` | |
| `operation.cube` | `Cube` | Cube the rule is deployed against. |
| `operation.grid` | `DataGrid` | Only when launched from a form/menu. |
| `operation.hasGrid()` | `boolean` | Guard before using `grid`. |
| `operation.user` | `User` | `user.fullName`, `user.userName` ⚠(property names), used in logs. |

## `Application` **[usage]**
| Method | Returns |
|---|---|
| `getCube(String name)` | `Cube` |
| `getCubes()` ⚠ | `List<Cube>` |
| `getDimension(String name)` | `Dimension` |
| `getDimensions()` / `getDimensions(Cube)` ⚠ | `List<Dimension>` |
| `getConnection(String name)` | `Connection` (named connection from Tools → Connections) |
| `getDataMap(String name)` | `DataMap` |
| `getJobFactory()` (`application.jobFactory`) | `JobFactory` |
| `getSubstitutionVariable(String name)` / `setSubstitutionVariable(String name, String value)` ⚠ | `SubstitutionVariable` (`.value`) |
| `getSmartList(String name)` ⚠ | `SmartList` → `.entries`, `getEntry(name)` |
| `getUserVariable(String name)` ⚠ | `UserVariable` |
| `getCurrencyMode()`, `getType()` ⚠ | `CurrencyMode`, `ApplicationType` |

## `Cube` **[usage]**
| Method | Returns / notes |
|---|---|
| `getName()` | |
| `dataGridDefinitionBuilder()` | `DataGridDefinitionBuilder` — **read** grids |
| `flexibleDataGridDefinitionBuilder()` ⚠ | `FlexibleDataGridDefinitionBuilder` — asymmetric row/column segments |
| `loadGrid(DataGridDefinition def, boolean suppressMissing)` | `DataGrid` (close it; `false` keeps `#MISSING` cells) |
| `getDataGridBuilder(String dateFormat)` / `getDataGridBuilder(String dateFormat, ExecuteAsUser user)` | `DataGridBuilder` — **write** grids. `ExecuteAsUser.SYSTEM_USER` bypasses current user's security; default `CURRENT_USER`. |
| `saveGrid(DataGrid grid)` | Persists cell values. |
| `executeCalcScript(String calc)` | Runs Essbase calc immediately (no need to return it). Throws `CalcScriptException` with `atReturnMessage` from `@RETURN`. |
| `createDataExporter()` | `DataExporter` (level-0 export, Planning format, iterator or outbox file) ⚠ exact flow |
| `createDataImportRequest()` | `DataImportRequest` ⚠ |
| `createCopyDataRequest()` | `CopyDataRequest` → `copyData()` ⚠ |
| `clearPartialData(String region, boolean …)` | ASO slice clear ⚠ |
| `executeAsoAllocation(AllocationParameters p)` / `executeAsoCustomCalculation(CustomCalcParameters p)` | ASO only |
| `getSubstitutionVariable(name)` / `setSubstitutionVariable(name, value)` ⚠ | cube-scoped sub vars |
| `getDimensions()` ⚠ | |

## `DataGridDefinitionBuilder` (read) **[usage]**
```groovy
DataGridDefinitionBuilder b = cube.dataGridDefinitionBuilder()
b.setPov(['Years','Scenario','Currency','Period','Version','Entity'],
         [['FY16'],['Current'],['Local'],['BegBalance'],['BU Version_1'],['No Entity']])
b.addColumn(['Account'], [['Min Salary','Max Salary','Min Bonus','Max Bonus']])
b.addRow(['Grades'],     [['ILvl0Descendants("Grades")']])     // member functions allowed
b.setSuppressMissingBlocks(true)       // optional ⚠
DataGridDefinition def = b.build()
cube.loadGrid(def, false).withCloseable { DataGrid g -> … }
```
- `setPov(List<String> dims, List<List<String>> members)` (also `addPov` with the same shape).
- `addColumn(List<String> dims, List<List<String>> members)` / `addRow(...)` — call repeatedly to add segments.
- `build()` → `DataGridDefinition`.

## `DataGridBuilder` (write) **[usage]**
```groovy
DataGridBuilder b = cube.getDataGridBuilder("MM/DD/YYYY")           // date format for date cells
b.addPov('FY16','Current','USD','BegBalance','BU Version_1','No Entity')  // plain members
b.addColumn('Min Salary','Max Salary','Min Bonus','Max Bonus')       // one call per column dim
b.addRow(['Grade 1'], [30000, 50000, '#missing', '#missing'])        // [row headers], [values]
DataGridBuilder.Status st = new DataGridBuilder.Status()
b.build(st).withCloseable { DataGrid g ->
    println "accepted=$st.numAcceptedCells rejected=$st.numRejectedCells first100=$st.cellsRejected"
    cube.saveGrid(g)
}
```
Read-only cells are rejected. Values: numbers, `'#missing'`, text, Smart List labels, dates in the given format.

## `DataGrid` **[usage]**
| Member | Notes |
|---|---|
| `dataCellIterator()` | all data cells → `GridIterator` |
| `dataCellIterator(String... memberNames)` | cells whose member set contains those members (e.g. `'Salary','Bonus'`) |
| `dataCellIterator(Closure/Predicate<DataCell>)` | e.g. `{ DataCell c -> c.edited }` |
| `dataCellIterator(Predicate, MemberNameType)` | control name form (`ESSBASE_NAME`, `MEMBER_NAME`, `ALIAS` ⚠) |
| `pov` | `List<HeaderCell>` — `pov*.mbrName`, `pov*.essbaseMbrName`, `pov*.dimName` |
| `rows` / `columns` ⚠ | header cells |
| `empty` | `boolean` |
| `hasSmartPush(String name)` / `getSmartPush(String name)` | Smart Pushes attached to the form |
| `close()` | Use `withCloseable` on loaded/built grids |

## `DataGrid.DataCell` **[usage]**
| Property / method | Notes |
|---|---|
| `data` | `double`; **0 for both `#MISSING` and 0** — check `missing` |
| `missing`, `edited`, `locked`, `readOnly`, `valid` | booleans |
| `formattedValue` | display string (text, Smart List label, date) |
| `dataAsSmartListMemberName` | Smart List entry name |
| `bgColor` (int RGB e.g. `0xFF8C00`), `setBgColor(int)` | After-load formatting |
| `addValidationError(int color, String message)` | Before-save validation marker |
| `crossDimCell(String... members)` | Sibling cell on the same row/col with substituted members |
| `getMemberName(String dim)` / `getMemberName(String dim, MemberNameType)` | |
| `memberNames` | all members of the cell |
| `periodName`, `yearName` ⚠, `scenarioName` ⚠ … | convenience getters for standard dims |

## `DataGrid.HeaderCell`: `mbrName`, `essbaseMbrName`, `dimName`, `member` ⚠

## `Dimension` **[usage]**
| Method | Notes |
|---|---|
| `getName()` | |
| `getMember(String name)` | `Member` (throws if absent — ⚠ use `hasMember(name)`) |
| `getEvaluatedMembers(String memberSelection, Cube cube)` | e.g. `"ILvl0Descendants(Product)"` → `List<Member>` |
| `saveMember(Map<String,Object> props)` / `saveMember(Map, DynamicChildStrategy)` | Create/update. Keys as in the metadata import file: `"Member"`, `"Parent"`, `"New Name"`, `"Alias: Default"`, `"Data Storage"`, `"Plan Type (Plan1)"` … |
| `getMembers()` ⚠ | all members |

## `Member` **[usage]**
| Method | Notes |
|---|---|
| `name`, `dimension` | |
| `hasChild(String name)` | |
| `newChildAsMap(String name)` | `Map` seeded from parent — pass to `dimension.saveMember(map, DynamicChildStrategy.ALWAYS_DYNAMIC)` |
| `addDynamicChild(Object name)` | Parent must be *Enable for Dynamic Children* |
| `delete()` | Deletes member **and descendants** |
| `toMap()` ⚠ | properties as a map |
| `children`, `parent`, `descendants` ⚠ | navigation |

`DynamicChildStrategy`: `ALWAYS_DYNAMIC`, `ALWAYS_STATIC` ⚠, `DYNAMIC_IF_AVAILABLE` ⚠ (check enum constants in javadoc).

## `RtpValue` **[usage]**
`enteredValue` (String), `member` (Member), `members` (List<Member>), and GString rendering `"$rtps.X"` → Essbase-ready literal. `rtps.X.enteredValue as int` for numbers.

## `SmartPush` / `DataMap` **[usage]**
```groovy
// form-attached Smart Push, scoped to the grid automatically
operation.grid.getSmartPush("Employee Data").execute(["Employee": '"E1", "E2"'])            // override members
operation.grid.getSmartPush("Employee Data").execute(["Employee": '"E1"'], ["Employee": "No Employee"])  // + exclusions
// data map by name (no form needed)
operation.application.getDataMap("Plan to Reporting").execute(true)                         // ⚠ boolean = clear target first
operation.application.getDataMap("Plan to Reporting").createSmartPush().execute(["Entity": '"East"'], true) // ⚠
```
Override map values are **quoted, comma-separated member lists** in one string.

## `JobFactory` / `JobDefinition` / `Job` **[usage]**
```groovy
JobFactory jf = operation.application.jobFactory
JobDefinition jd = jf.job("Rule Name", "Rules", ["rtpName": cscParams(povCells, edited)])  // type: Rules|Ruleset|Template|Cube Refresh|Clear Cube|Scenario Rollup
Job job = executeJob(jd)          // synchronous
println job.status                // JobStatus ⚠ ; job.awaitCompletion() exists for async cases
```

## REST — `Connection`, `HttpRequest`, `HttpRequestWithBody`, `HttpResponse` **[usage]**
```groovy
Connection c = operation.application.getConnection("DM")        // or connection(url,user,pwd)
HttpResponse<String> r = c.post("/optional/path")                // get(path) / put / delete ⚠ names
        .header("Content-Type","application/json")               // ⚠
        .queryParam("input", value)
        .body(json(["jobType":"DATARULE","jobName":"ProductVolumes"]))
        .asString()                                              // or asByteArray() ⚠
r.status  // int ; r.statusText ; r.body ; r.headers ⚠
ReadContext ctx = JsonPath.parse(r.body); ctx.read('$.jobId')    // JsonPath (jayway) is available
```
Internal EPM REST endpoints: `/HyperionPlanning/rest/v3/applications/{app}/jobs` (Planning jobs), `/aif/rest/V1/jobs` (Data Management). The named Connection holds base URL + credentials.

## Excel functions — `Excel` (static) **[javadoc index]**
Financial/date helpers usable in Groovy: `ACCRINT`, `ACCRINTM`, `AMORDEGRC`, `AMORLINC`, `COUPDAYS`, `COUPNCD`, `COUPNUM`, `COUPPCD`, `DATEDIF(Date,Date,DateUnit)`, `EDATE(Date,n)`, `EFFECT(rate,n)`, `EOMONTH(Date,n)`, … (PMT/NPV/IRR-style functions ⚠ check index). Enums: `DateUnit`, `DayCountBasis`, `InterestCalcMethod`, `PaymentFrequency`, `PaymentType`, `YearBasis`.

## ASO — `AllocationParameters` **[javadoc example]**
Properties: `pov` (MDX crossjoin), `amount`, `range`, `target`, `basis`, `zeroAmountOption`, `targetTimeSpanOption`, `basisTimeSpanOption`, `zeroBasisOption`, `negativeBasisOption`, `allocationMethod` (`SHARE`), `roundDigits`, `roundMethod`; run with `cube.executeAsoAllocation(p)`. Use `mdxParams(rtps.Month)` inside the MDX.

## Other classes (one-liners)
- `User`: `fullName`, `userName` ⚠, `email` ⚠.
- `SubstitutionVariable`: `name`, `value`.
- `SmartList`/`SmartList.Entry`: entries, ids, labels.
- `PeriodDimension`, `YearDimension`, `FiscalCalendar`, `FiscalYearStart`: calendar-aware helpers (e.g. period ordering) ⚠.
- `StrategicModel`, `ScenarioRollup`: Strategic Modeling integration (`addBusinessCaseToExecute(String)`).
- `MlModel`, `MlPredictionStatus`: ML models imported into EPM (IPM Bring Your Own ML).
- `VetoOperationException`: what `throwVetoException` throws.
- `CloseableIterator`: `withCloseable { }`.
