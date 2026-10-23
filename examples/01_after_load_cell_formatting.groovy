/*RTPS: */
// Cube     : any (form-bound)
// Form     : EmployeeDriverForm (or any form)
// Trigger  : After Load
// Purpose  : colour cells by value. Shows the #MISSING vs 0 distinction (cell.data is 0 for both).
// Source   : Oracle tutorial "Working with Data Grids and Iterators in Groovy"

if(!operation.hasGrid()) return

operation.grid.dataCellIterator().each { DataCell cell ->
    if(cell.missing) {
        cell.bgColor = 0x00BFFF            // #MISSING -> light blue
    } else if(cell.data <= 50000) {
        cell.bgColor = 0xFF8C00            // <= 50k   -> dark orange
    } else {
        cell.bgColor = 0x00FA9A            // > 50k    -> spring green
    }
}

// Variant: only one account, and only flag negatives
operation.grid.dataCellIterator('Bonus').each { DataCell cell ->
    if(!cell.missing && cell.data < 0) cell.bgColor = 0xFF0000
}

return   // nothing to execute as a calc script
