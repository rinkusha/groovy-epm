/*RTPS: */
// Cube     : Plan1
// Trigger  : Jobs page or right-click menu
// Purpose  : WRITE pattern — set default driver values cell-by-cell without a calc script.
//            DataGridBuilder takes plain member lists (no dimension-name lists, unlike the definition builder).
// Notes    : read-only cells (security / locked / dynamic calc) are REJECTED, see Status.
//            getDataGridBuilder(fmt, ExecuteAsUser.SYSTEM_USER) writes regardless of the running user's security.
// Source   : Oracle tutorial "Working with Data Grids and Iterators" (Setting default data values)

Cube cube = operation.application.getCube("Plan1")

DataGridBuilder builder = cube.getDataGridBuilder("MM/DD/YYYY")   // date format for any date cells
builder.addPov('FY16', 'Current', 'USD', 'BegBalance', 'BU Version_1', 'No Entity')
builder.addColumn('Min Salary', 'Max Salary', 'Min Bonus', 'Max Bonus')
builder.addRow(['Grade 1'], [30000, 50000, '#missing', '#missing'])
builder.addRow(['Grade 2'], [40000, 60000, '#missing', 5000])
builder.addRow(['Grade 3'], [70000, 90000, '#missing', 10000])

DataGridBuilder.Status status = new DataGridBuilder.Status()
builder.build(status).withCloseable { DataGrid grid ->
    println("Cells accepted : $status.numAcceptedCells")
    println("Cells rejected : $status.numRejectedCells")
    println("First rejected : $status.cellsRejected")
    cube.saveGrid(grid)
}
return
