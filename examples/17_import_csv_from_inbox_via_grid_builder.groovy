/*RTPS: */
// Cube     : Plan1
// Trigger  : Jobs page (after an EPM Automate `uploadfile drivers.csv`)
// Purpose  : read a CSV from the inbox/outbox with csvIterator and write it with DataGridBuilder.
//            Good for small driver files; for large level-0 loads use Data Management / `importdata`.
// CSV layout assumed: Entity,Account,Jan,Feb,...,Dec   (header row first)

final String FILE  = "drivers.csv"
final String CUBE  = "Plan1"
final List<String> PERIODS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

Cube cube = operation.application.getCube(CUBE)
DataGridBuilder b = cube.getDataGridBuilder("MM/DD/YYYY", ExecuteAsUser.SYSTEM_USER)
b.addPov('Plan', 'Working', 'FY26', 'USD')          // Scenario, Version, Years, Currency
b.addColumn(*PERIODS)                               // spread list into varargs

int rows = 0
csvIterator(FILE).withCloseable { CloseableIterator<String[]> reader ->
    boolean header = true
    reader.each { String[] v ->
        if(header) { header = false; return }       // skip header line
        if(v.length < 2 + PERIODS.size()) { println "Skipping short row: ${Arrays.toString(v)}"; return }
        List vals = (2..<(2 + PERIODS.size())).collect { int i -> v[i]?.trim() ? v[i].trim() : '#missing' }
        b.addRow([v[0].trim(), v[1].trim()], vals)   // [Entity, Account], [12 values]
        rows++
    }
}
if(rows == 0) { println "No data rows in $FILE"; return }

DataGridBuilder.Status st = new DataGridBuilder.Status()
b.build(st).withCloseable { DataGrid g ->
    println "rows=$rows accepted=$st.numAcceptedCells rejected=$st.numRejectedCells"
    if(st.numRejectedCells) println "Rejected sample: $st.cellsRejected"
    cube.saveGrid(g)
}
return
