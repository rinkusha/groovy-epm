/*RTPS: {GitPath} {GitRef}*/
// Cube     : Plan1
// Trigger  : Jobs page
// Purpose  : data load rule that pulls a CSV from a GIT repository, validates/transforms it and loads it with DataGridBuilder.
//            The Groovy sandbox has no git client / java.io.File / java.net, so the repository is read through its
//            HTTPS API using a named Connection "GitRepo" (Connections page; URL such as
//            https://api.github.com/repos/<owner>/<repo>/contents, token stored in the connection, never in code).
// RTPs     : GitPath = file path in the repo (default data/drivers.csv), GitRef = branch/tag (default main)
// CSV layout: Entity,Account,Jan,...,Dec (header row first) - see 22_git_data_load/data/drivers.csv
// Settings : mirrored in 22_git_data_load/config/*.properties (edit the constants below to match)
// Dims     : Scenario/Version/Years/Currency in POV; Entity, Account in rows; Period in columns

final String CUBE = "Plan1"
final String GIT_CONNECTION = "GitRepo"
final List<String> POV = ['Plan', 'Working', 'FY26', 'USD']
final List<String> PERIODS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
final BigDecimal MAX_ABS = 1000000000G
final Set<String> VALID_ENTITIES = ['East', 'West', 'North', 'South'] as Set   // placeholder: validate against your app

String path = rtps.GitPath?.toString()?.trim() ?: "data/drivers.csv"
String ref  = rtps.GitRef?.toString()?.trim() ?: "main"

// ---- 1. fetch from git (raw media type returns file body) ------------------------------------
HttpResponse<String> resp
try {
    resp = operation.application.getConnection(GIT_CONNECTION)
            .get("/${path}")
            .queryParam("ref", ref)
            .header("Accept", "application/vnd.github.raw")
            .asString()
} catch(Exception e) {
    throwVetoException("Cannot reach git repository: ${e.message}")
}
if(!(200..299).contains(resp.status))
    throwVetoException("Git fetch of $path@$ref failed: $resp.status $resp.statusText")
println "Fetched $path@$ref (${resp.body.length()} chars)"

// ---- 2. parse, validate, transform -----------------------------------------------------------
List<String> errors = []
List<List> rows = []
List<String> lines = resp.body.readLines().findAll { it.trim() }
if(lines.size() < 2) throwVetoException("No data rows in $path")

lines.drop(1).eachWithIndex { String line, int idx ->
    int lineNo = idx + 2
    String[] v = line.split(',', -1)
    if(v.length < 2 + PERIODS.size()) { errors << "line $lineNo: expected ${2 + PERIODS.size()} columns, got ${v.length}"; return }
    String entity = v[0].trim(), account = v[1].trim()
    if(!VALID_ENTITIES.contains(entity)) { errors << "line $lineNo: unknown entity '$entity'"; return }
    if(!account) { errors << "line $lineNo: empty account"; return }
    List vals = []
    for(int i = 0; i < PERIODS.size(); i++) {
        String raw = v[2 + i].trim().replace('"', '')
        if(!raw) { vals << '#missing'; continue }
        try {
            BigDecimal n = new BigDecimal(raw)
            if(n.abs() > MAX_ABS) throw new NumberFormatException("out of range")
            vals << n.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()   // transform: 2 decimals
        } catch(NumberFormatException ignored) {
            errors << "line $lineNo ${PERIODS[i]}: bad number '$raw'"
            return
        }
    }
    if(vals.size() == PERIODS.size()) rows << [[entity, account], vals]
}
errors.take(20).each { println "REJECTED $it" }
if(errors) println "${errors.size()} validation problem(s); valid rows will still load"
if(!rows) throwVetoException("Nothing valid to load from $path")

// ---- 3. load ---------------------------------------------------------------------------------
Cube cube = operation.application.getCube(CUBE)
DataGridBuilder b = cube.getDataGridBuilder("MM/DD/YYYY", ExecuteAsUser.SYSTEM_USER)
b.addPov(*POV)
b.addColumn(*PERIODS)
rows.each { List r -> b.addRow(r[0], r[1]) }

DataGridBuilder.Status st = new DataGridBuilder.Status()
b.build(st).withCloseable { DataGrid g ->
    println "rows=${rows.size()} accepted=$st.numAcceptedCells rejected=$st.numRejectedCells"
    if(st.numRejectedCells) println "Rejected sample: $st.cellsRejected"
    cube.saveGrid(g)
}
return
