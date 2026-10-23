/*RTPS: */
// Cube     : OEP_FS (Financials)
// Form     : Product Revenue form — Product on rows, Period on columns, everything else in POV
// Trigger  : After Save
// Purpose  : build a calc script that FIXes only on the Products/Periods the user edited,
//            instead of recalculating the whole form. The returned String is executed by EPM.
// Source   : Oracle tutorial "Calculating Modified Data Using Groovy"

if(!operation.hasGrid()) return

// 1. Capture edited members (Essbase names so aliases don't break the FIX)
Set<String> editedMembers = []
operation.grid.dataCellIterator({ DataCell cell -> cell.edited }, MemberNameType.ESSBASE_NAME).each { DataCell cell ->
    editedMembers << cell.periodName << cell.getMemberName("Product")
}

// 2. Early exit
if(editedMembers.isEmpty()) {
    println("No edited cells found!")
    return
}

// 3. Generate the focused calc script. cscParams() quotes + comma-separates POV header cells and strings.
String calcScript = """
FIX(${cscParams(operation.grid.pov, editedMembers)})
  "OFS_Calculated"
  (
     "OFS_Product Revenue" = "OFS_Volume" * "OFS_Avg Selling Price";
  )
ENDFIX;"""

println("The following calc script was executed by $operation.user.fullName:\n$calcScript")

// 4. Last expression is a String -> executed as an Essbase calc script
return calcScript.toString()
