/*RTPS: */
// Cube     : Plan1
// Trigger  : Jobs page (run after OIC has placed sf_opportunities.csv in the EPM inbox)
// Purpose  : read the Salesforce SOQL extract (CSV) from the inbox, GROUP BY Entity/Account/Period
//            in Groovy (sum Amount), and load the totals with DataGridBuilder.
// CSV layout assumed (header first): Entity,Account,Period,Amount
//            e.g. E100,Sales_Revenue,Jan,1200.50   (one row per Opportunity; many rows per key)
// POV assumed: Scenario=Plan, Version=Working, Years=FY26, Currency=USD
// Placeholders: file name, cube, POV, member names - map them to your application.

final String FILE = "sf_opportunities.csv"
final String CUBE = "Plan1"
final List<String> PERIODS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

// key "Entity|Account" -> [period -> total]
Map<String, Map<String, BigDecimal>> grouped = [:].withDefault { [:].withDefault { 0G } }

int read = 0, skipped = 0
csvIterator(FILE).withCloseable { CloseableIterator<String[]> reader ->
    boolean header = true
    reader.each { String[] v ->
        if(header) { header = false; return }
        String period = v.length > 2 ? v[2]?.trim() : null
        if(v.length < 4 || !v[0]?.trim() || !v[1]?.trim() || !PERIODS.contains(period) || !v[3]?.trim()?.isBigDecimal()) {
            skipped++; println "Skipping row: ${Arrays.toString(v)}"; return
        }
        grouped["${v[0].trim()}|${v[1].trim()}"][period] += v[3].trim().toBigDecimal()
        read++
    }
}
if(!grouped) { println "No valid rows in $FILE (skipped=$skipped)"; return }

Cube cube = operation.application.getCube(CUBE)
DataGridBuilder b = cube.getDataGridBuilder("MM/DD/YYYY", ExecuteAsUser.SYSTEM_USER)
b.addPov('Plan', 'Working', 'FY26', 'USD')
b.addColumn(*PERIODS)
grouped.each { String key, Map<String, BigDecimal> byPeriod ->
    String[] k = key.split('\\|')
    b.addRow([k[0], k[1]], PERIODS.collect { String p -> byPeriod.containsKey(p) ? byPeriod[p].toString() : '#missing' })
}

DataGridBuilder.Status st = new DataGridBuilder.Status()
b.build(st).withCloseable { DataGrid g ->
    println "read=$read skipped=$skipped groups=${grouped.size()} accepted=$st.numAcceptedCells rejected=$st.numRejectedCells"
    if(st.numRejectedCells) println "Rejected sample: $st.cellsRejected"
    cube.saveGrid(g)
}
return
