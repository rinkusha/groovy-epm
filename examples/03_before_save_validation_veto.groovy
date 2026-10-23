/*RTPS: */
// Cube     : Plan2 (form cube) ; driver data read from Plan1
// Form     : Employee grid with Grade / Salary / Bonus accounts on columns, Employee on rows
// Trigger  : Before Save
// Purpose  : load allowed salary/bonus ranges per Grade from a driver cube, then flag out-of-range
//            cells with addValidationError (red) and veto if the grid has no Grade cells at all.
// Source   : Oracle javadoc "Example multi line script to demonstrate advanced data validations"

if(!operation.hasGrid()) return

class GradeData {
    DataGrid.DataCell minSalary, maxSalary, minBonus, maxBonus
    String toString() { "min/max salary ${minSalary?.formattedValue}/${maxSalary?.formattedValue}, min/max bonus ${minBonus?.formattedValue}/${maxBonus?.formattedValue}" }
}

def mbUs = messageBundle(["validation.missingmember.grade": "No Grades found on the grid."])
def mbl  = messageBundleLoader(["en": mbUs])

// ---- 1. Read driver ranges from the driver cube -------------------------------------------
Cube driverCube = operation.application.getCube("Plan1")
DataGridDefinitionBuilder builder = driverCube.dataGridDefinitionBuilder()
builder.setPov(['Years','Scenario','Currency','Period','Version','Entity'],
               [['FY16'],['Current'],['Local'],['BegBalance'],['BU Version_1'],['No Entity']])
builder.addColumn(['Account'], [['Min Salary','Max Salary','Min Bonus','Max Bonus']])
builder.addRow(['Grades'],     [['ILvl0Descendants("Grades")']])

Map<String, GradeData> gradeDataMap = [:]
driverCube.loadGrid(builder.build(), false).withCloseable { DataGrid dataGrid ->
    dataGrid.dataCellIterator('Min Salary').each { DataCell cell ->
        GradeData gd = new GradeData()
        gd.minSalary = cell
        gd.maxSalary = cell.crossDimCell('Max Salary')
        gd.minBonus  = cell.crossDimCell('Min Bonus')
        gd.maxBonus  = cell.crossDimCell('Max Bonus')
        gradeDataMap[cell.getMemberName('Grades')] = gd
    }
}

// ---- 2. Validate the grid being saved -----------------------------------------------------
DataGrid grid = operation.grid
GridIterator itr = grid.dataCellIterator('Grade')
if(!grid.empty && !itr.hasNext())
    throwVetoException(mbl, "validation.missingmember.grade")

itr.each { DataCell cell ->
    GradeData gd = gradeDataMap[cell.dataAsSmartListMemberName]     // Grade is a Smart List cell
    if(gd == null) { println "No driver data for grade ${cell.dataAsSmartListMemberName}"; return }

    DataCell salary = cell.crossDimCell('Salary')
    if(salary && (salary.data < gd.minSalary.data || salary.data > gd.maxSalary.data))
        salary.addValidationError(0xFF0000, "Salary is outside of the allowed range.")

    DataCell bonus = cell.crossDimCell('Bonus')
    if(bonus && (bonus.data < gd.minBonus.data || bonus.data > gd.maxBonus.data))
        bonus.addValidationError(0xFF0000, "Bonus is outside of the allowed range.")
}

return
