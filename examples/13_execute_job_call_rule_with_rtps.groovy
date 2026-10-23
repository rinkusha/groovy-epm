/*RTPS: */
// Cube     : OEP_FS
// Trigger  : After Save
// Purpose  : orchestrate — call ANOTHER rule (which has an RTP "editedMembers") with values computed here,
//            then a cube refresh. executeJob is synchronous and creates child jobs under this one.
// Job types: "Rules", "Ruleset", "Template", "Cube Refresh", "Clear Cube", "Scenario Rollup"
// Source   : Oracle tutorial "Calculating Modified Data Using Groovy" (Execute Job section)

if(!operation.hasGrid()) return

Set<String> editedMembers = []
operation.grid.dataCellIterator({ DataCell cell -> cell.edited }, MemberNameType.ESSBASE_NAME).each { DataCell cell ->
    editedMembers << cell.periodName << cell.getMemberName("Product")
}
if(!editedMembers) { println "No edited cells found!"; return }

JobFactory jf = operation.application.jobFactory

// POV members except the dimension the target rule already fixes on ("Plan Element")
List<DataGrid.HeaderCell> povMemberCells = operation.grid.pov.findAll { DataGrid.HeaderCell h -> h.dimName != "Plan Element" }

// 1. Run the RTP-driven rule. The target rule reads: FIX($rtps.editedMembers) ...
Job calcJob = executeJob(jf.job("Groovy Calculate Product Revenue RTPs", "Rules",
                                ["editedMembers": cscParams(povMemberCells, editedMembers)]))
println "Calc job finished with status: ${calcJob.status}"

// 2. Optional: cube refresh after metadata changes (no RTPs)
// Job refresh = executeJob(jf.job("Refresh Database", "Cube Refresh", [:]))

return
