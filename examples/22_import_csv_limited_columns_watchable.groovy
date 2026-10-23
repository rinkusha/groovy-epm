/*RTPS: */
// Cube     : Plan1
// Trigger  : Jobs page (after an EPM Automate `uploadfile drivers_small.csv`)
// Purpose  : import a CSV that does NOT always have the full Jan-Dec column list -
//            the column set is whatever is in the header row (e.g. only Jan,Feb,Mar
//            for a quarterly load, or just Jan for a single-month correction) - and
//            run it deliberately slowly, one row at a time with a progress line and
//            a short pause, so it can be watched live in the Jobs console.
// CSV layout: Entity,Account,<period1>,<period2>,...   header row first, column count varies.
// For large/level-0 loads use Data Management or `importdata` instead; this pattern is
// for small, human-watched driver files only - the sleep() makes it slower on purpose.

final String FILE        = "drivers_small.csv"
final String CUBE        = "Plan1"
final int    PAUSE_MS    = 500          // slow it down so progress is visible; set 0 to disable

List<String[]> lines = []
csvIterator(FILE).withCloseable { CloseableIterator<String[]> reader ->
    reader.each { String[] v -> lines << v }
}
if(lines.isEmpty()) { println "No data in $FILE"; return }

String[] header = lines[0]
if(header.length < 3) { throwVetoException("Header must be Entity,Account,<at least one period>: ${Arrays.toString(header)}") }
List<String> periods = header[2..-1].collect { String p -> p.trim() }
List<String[]> dataRows = lines.size() > 1 ? lines[1..-1] : []
println "Header columns for this run: Entity,Account,${periods.join(',')} (${periods.size()} period column(s))"
if(dataRows.isEmpty()) { println "No data rows in $FILE"; return }

Cube cube = operation.application.getCube(CUBE)
DataGridBuilder b = cube.getDataGridBuilder("MM/DD/YYYY", ExecuteAsUser.SYSTEM_USER)
b.addPov('Plan', 'Working', 'FY26', 'USD')     // Scenario, Version, Years, Currency
b.addColumn(*periods)                          // only the columns actually present this run

int accepted = 0
int total = dataRows.size()
dataRows.eachWithIndex { String[] v, int idx ->
    if(v.length < 2 + periods.size()) {
        println "Row ${idx + 1}/$total: skipped, too few columns: ${Arrays.toString(v)}"
        return
    }
    String entity = v[0].trim()
    String account = v[1].trim()
    List vals = (0..<periods.size()).collect { int i ->
        String raw = v[2 + i]
        raw?.trim() ? raw.trim() : '#missing'
    }
    b.addRow([entity, account], vals)
    accepted++
    println "Row ${idx + 1}/$total: $entity / $account -> ${periods.withIndex(0).collect { p, i -> "$p=${vals[i]}" }.join(', ')}"
    if(PAUSE_MS > 0 && idx < total - 1) sleep(PAUSE_MS)
}
if(accepted == 0) { println "No usable rows in $FILE"; return }

DataGridBuilder.Status st = new DataGridBuilder.Status()
b.build(st).withCloseable { DataGrid g ->
    println "rows=$accepted accepted=$st.numAcceptedCells rejected=$st.numRejectedCells"
    if(st.numRejectedCells) println "Rejected sample: $st.cellsRejected"
    cube.saveGrid(g)
}
return
