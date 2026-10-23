/*RTPS: {Driver} {DriverValue}*/
// Cube     : Plan1
// Trigger  : Jobs page / menu
// Purpose  : (a) the one-line "expression" style: return a calc that sets a global driver from RTPs;
//            (b) read/set substitution variables (⚠ verify get/setSubstitutionVariable names in the javadoc).
// Source   : Oracle javadoc "Example expression to update a global driver by returning a calc script"

// (b) substitution variables — application-level and cube-level
SubstitutionVariable currYear = operation.application.getSubstitutionVariable("CurrYear")
println "CurrYear = ${currYear.value}"
// operation.application.setSubstitutionVariable("CurrMonth", "Jun")
// operation.cube.setSubstitutionVariable("CubeOnlyVar", "X")

// (a) The driver: `$rtps.Driver` renders as a quoted member, `$rtps.DriverValue` as the typed literal.
"""FIX("No Scenario", "No Version", "No Entity", "No Year", "BegBalance", "Local")
   $rtps.Driver = $rtps.DriverValue;
ENDFIX;"""
