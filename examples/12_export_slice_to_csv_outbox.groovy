/*RTPS: {Scenario} {Year}*/
// Cube     : Plan1
// Trigger  : Jobs page / EPM Automate (then `downloadfile` the result from the outbox)
// Purpose  : export a slice to a CSV in the Outbox (Inbox/Outbox Explorer) using a read grid + csvWriter.
//            For full level-0 extracts prefer Cube.createDataExporter() (see docs/01, ⚠ check signature)
//            or an EPM Automate `exportdata` job; this pattern is for shaped, filtered extracts.

Cube cube = operation.application.getCube("Plan1")
List<String> periods = (1..12).collect { "Period" + it }   // adjust to Jan..Dec in your app

DataGridDefinitionBuilder b = cube.dataGridDefinitionBuilder()
b.setPov(['Scenario','Years','Version','Currency'],
         [[rtps.Scenario.member.name],[rtps.Year.member.name],['Final'],['USD']])
b.addColumn(['Period'],  [periods])
b.addRow(['Entity','Account'], [['ILvl0Descendants("Total Entity")'], ['ILvl0Descendants("Net Income")']])
b.setSuppressMissingBlocks(true)

String fileName = "export_${rtps.Scenario.member.name}_${rtps.Year.member.name}.csv".replaceAll(/[^A-Za-z0-9_\-. ]/, '_')
int rows = 0

csvWriter(fileName).withCloseable { CsvWriter out ->
    out.writeNext((['Entity','Account'] + periods) as String[])
    cube.loadGrid(b.build(), true).withCloseable { DataGrid grid ->
        grid.dataCellIterator(periods[0]).each { DataCell first ->          // one cell per row -> walk across with crossDimCell
            List<String> line = [first.getMemberName('Entity'), first.getMemberName('Account')]
            periods.each { String p ->
                DataCell c = (p == periods[0]) ? first : first.crossDimCell(p)
                line << ((c == null || c.missing) ? '' : c.formattedValue)
            }
            out.writeNext(line as String[])
            rows++
        }
    }
}
println "Wrote $rows rows to outbox file $fileName"
return
