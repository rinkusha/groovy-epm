/*RTPS: */
// Cube     : OEP_FS
// Trigger  : After Save
// Purpose  : run a calc script INLINE with cube.executeCalcScript() and get a value back from
//            Essbase via @RETURN — the value arrives as CalcScriptException.atReturnMessage.
//            Use this when Groovy must continue after the calc (e.g. push, log, second calc).
// Source   : Oracle tutorial "Calculating Modified Data Using Groovy" (inline calc section)

if(!operation.hasGrid()) return

Set<String> editedMembers = []
operation.grid.dataCellIterator({ DataCell cell -> cell.edited }, MemberNameType.ESSBASE_NAME).each { DataCell cell ->
    editedMembers << cell.periodName << cell.getMemberName("Product")
}
if(!editedMembers) { println "No edited cells found!"; return }

String fixMembers = cscParams(operation.grid.pov, editedMembers)
String calcScript = """
VAR numCellsCalculated = 0;
FIX($fixMembers)
  "OFS_Calculated" (
      "OFS_Product Revenue" = "OFS_Volume" * "OFS_Avg Selling Price";
      numCellsCalculated = numCellsCalculated + 1;
  )
ENDFIX;
FIX($fixMembers)
  "OFS_Calculated" (
      @RETURN(@HspNumToString(numCellsCalculated), ERROR);
  )
ENDFIX;
"""

Cube cube = operation.application.getCube("OEP_FS")
try {
    cube.executeCalcScript(calcScript)
} catch(CalcScriptException e) {
    // @RETURN(..., ERROR) deliberately raises; the message carries our value
    println("Calc script executed by $operation.user.fullName:\n$calcScript")
    println("Total number of cells calculated: $e.atReturnMessage")
}

// ...Groovy continues here: push, second calc, logging, etc.
return
