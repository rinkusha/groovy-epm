/*RTPS: {Scenario} {Year}*/
// Cube     : Plan1
// Trigger  : Jobs page (then `downloadfile` / Planning `importdata` from the outbox)
// Purpose  : write a CSV with a LIMITED set of columns in the cube's "list" (one-value-per-line) import format:
//            one column per dimension that varies + a Data column; the dimensions held constant are NOT repeated
//            per line - they are supplied in the file header POV columns listed in HEADER_DIMS below.
//            Layout:  Entity,Account,Period,Scenario,Version,Years,Currency,Data   (header row first)
// Assumptions: Entity/Account in rows, Period in columns on the read grid; Version/Currency fixed (placeholders).
//            The header dimension order MUST match the cube's data import (Planning "importdata") column order -
//            check it with an export of your own cube and adjust HEADER_DIMS.

final String CUBE = "Plan1"
final List<String> HEADER_DIMS = ['Entity', 'Account', 'Period', 'Scenario', 'Version', 'Years', 'Currency']
final String DATA_COL = 'Data'
final String VERSION = 'Working', CURRENCY = 'USD'
final List<String> PERIODS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

String scenario = rtps.Scenario.member.name
String year = rtps.Year.member.name

Cube cube = operation.application.getCube(CUBE)
DataGridDefinitionBuilder b = cube.dataGridDefinitionBuilder()
b.setPov(['Scenario','Years','Version','Currency'], [[scenario],[year],[VERSION],[CURRENCY]])
b.addColumn(['Period'], [PERIODS])
b.addRow(['Entity','Account'], [['ILvl0Descendants("Total Entity")'], ['Units','Price']])
b.setSuppressMissingBlocks(true)

String fileName = "list_${scenario}_${year}.csv".replaceAll(/[^A-Za-z0-9_\-. ]/, '_')
int lines = 0

csvWriter(fileName).withCloseable { CsvWriter out ->
    out.writeNext((HEADER_DIMS + [DATA_COL]) as String[])
    cube.loadGrid(b.build(), true).withCloseable { DataGrid grid ->
        grid.dataCellIterator(PERIODS[0]).each { DataCell first ->      // one cell per row, walk across periods
            String entity = first.getMemberName('Entity')
            String account = first.getMemberName('Account')
            PERIODS.each { String p ->
                DataCell c = (p == PERIODS[0]) ? first : first.crossDimCell(p)
                if(c == null || c.missing) return                      // list format: skip #MISSING
                out.writeNext([entity, account, p, scenario, VERSION, year, CURRENCY, c.data.toString()] as String[])
                lines++
            }
        }
    }
}
if(lines == 0) { println "No data found - $fileName is header-only"; return }
println "Wrote $lines list-format lines to outbox file $fileName"
return
