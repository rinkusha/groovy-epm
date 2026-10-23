/*RTPS: */
// Cube     : any
// Trigger  : Jobs page
// Purpose  : produce an .xlsx (optionally zipped) in the Outbox with the built-in Workbook API.
// Source   : Oracle javadoc EpmScript.workbook() / zipOutputStream()

workbook().withCloseable { Workbook book ->
    book.createSheet("Summary").with { sheet ->
        addRow('', 'Jan', 'Feb', 'Mar')
        addRow('Plan',   '10', '',    '30')
        addRow('Budget', '',   '200', '100')
        addRow('Actual', '',   '110', '120')
    }
    book.createSheet('Detail').with { sheet ->
        addRow('', 'Apr', 'May', 'Jun')
        addRow('Variance', '1', '2', '3')
    }
    book.getSheet(0).addRow('Working', '15', '20', '300')
    book.save('Report.xlsx')                       // lands in the Outbox
}

// Zipped variant:
// zipOutputStream('Report.zip').withCloseable { ZipOutputStream zos ->
//     workbook().withCloseable { Workbook book ->
//         book.createSheet().with { addRow('a','b') }
//         book.write(zos)
//     }
// }
println "Report.xlsx written to outbox"
return
