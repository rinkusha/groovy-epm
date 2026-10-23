/*RTPS: {Scenario} {Version} {Year} {Entity}*/
// Cube     : OEP_WFP (Workforce) — read a headcount-gap driver from a labor-model cube, then
//            fill unused "To Be Hired" placeholder employees in Workforce with the hires needed.
// Trigger  : After Save on the labor-model driver form, or Jobs page with RTPs
// Purpose  : write-back pattern combining READ (definition builder) + WRITE (grid builder as SYSTEM_USER).
// Placeholders you must map to your app:
//   LABOR_CUBE, GAP_ACCOUNT, TBH_PARENT (e.g. "OWP_New Employees" / your TBH pool), Workforce
//   properties (OWP_Start Date, OWP_Job / OWP_Grade / OWP_Pay Type ...), and the Smart List labels.
// Out of scope here: benefit components — let Workforce's own OWP rules derive them after the write.

final String LABOR_CUBE   = "LabPlan"
final String WF_CUBE      = "OEP_WFP"
final String GAP_ACCOUNT  = "LM_Headcount Gap"        // positive = hires needed
final String TBH_PARENT   = "OWP_New Employees"       // parent holding TBH placeholder members
final String QUARTER_PERIOD = "Q2"                    // hiring wave period; derive from rtps if needed

String scen = rtps.Scenario.member.name, ver = rtps.Version.member.name
String year = rtps.Year.member.name,     ent = rtps.Entity.member.name

// ---- 1. READ the gap per Job from the labor model ------------------------------------------
Cube labor = operation.application.getCube(LABOR_CUBE)
DataGridDefinitionBuilder rb = labor.dataGridDefinitionBuilder()
rb.setPov(['Scenario','Version','Years','Entity','Currency'], [[scen],[ver],[year],[ent],['No Currency']])
rb.addColumn(['Period'],  [[QUARTER_PERIOD]])
rb.addRow(['Job', 'Account'], [['ILvl0Descendants("Total Jobs")'], [GAP_ACCOUNT]])

Map<String, Integer> hiresByJob = [:]
labor.loadGrid(rb.build(), true).withCloseable { DataGrid g ->
    g.dataCellIterator(GAP_ACCOUNT).each { DataCell c ->
        int n = c.missing ? 0 : Math.ceil(c.data) as int
        if(n > 0) hiresByJob[c.getMemberName('Job')] = n
    }
}
if(!hiresByJob) { println "No headcount gap for $ent $year $QUARTER_PERIOD — nothing to create"; return }
println "Hires needed: $hiresByJob"

// ---- 2. Find unused TBH placeholders (no FTE for this scenario/version/year/entity) ----------
Cube wf = operation.application.getCube(WF_CUBE)
Dimension empDim = operation.application.getDimension("Employee")
List<Member> tbhPool = empDim.getEvaluatedMembers("ILvl0Descendants(\"$TBH_PARENT\")", wf)

DataGridDefinitionBuilder ub = wf.dataGridDefinitionBuilder()
ub.setPov(['Scenario','Version','Years','Entity','Job','Currency'], [[scen],[ver],[year],[ent],['OWP_Total Jobs'],['No Currency']])
ub.addColumn(['Period'],  [['BegBalance']])
ub.addRow(['Employee','Account'], [[ "ILvl0Descendants(\"$TBH_PARENT\")" ], ['OWP_FTE']])

List<String> free = []
wf.loadGrid(ub.build(), false).withCloseable { DataGrid g ->
    g.dataCellIterator('OWP_FTE').each { DataCell c -> if(c.missing) free << c.getMemberName('Employee') }
}
int needed = hiresByJob.values().sum() as int
if(free.size() < needed) throwVetoException("Need $needed TBH placeholders under $TBH_PARENT but only ${free.size()} are free.")

// ---- 3. WRITE one placeholder per hire (Workforce "Add Requisition"-style properties) --------
DataGridBuilder wb = wf.getDataGridBuilder("MM/DD/YYYY", ExecuteAsUser.SYSTEM_USER)
wb.addPov(scen, ver, year, ent, 'No Currency', 'BegBalance')
wb.addColumn('OWP_FTE', 'OWP_Start Date', 'OWP_Pay Type', 'OWP_Employee Type')   // << map to your accounts
String startDate = "04/01/${year.replace('FY','20')}"                              // first day of the hiring quarter
int i = 0
hiresByJob.each { String job, int n ->
    n.times {
        // row headers are the row dimensions in the order you want: Employee, Job
        wb.addRow([free[i++], job], [1, startDate, 'Exempt', 'Regular'])         // Smart List labels as text
    }
}

DataGridBuilder.Status st = new DataGridBuilder.Status()
wb.build(st).withCloseable { DataGrid g ->
    println "accepted=$st.numAcceptedCells rejected=$st.numRejectedCells ${st.cellsRejected ?: ''}"
    if(st.numRejectedCells) throwVetoException("Some new-hire cells were rejected; check security/form settings.")
    wf.saveGrid(g)
}

// ---- 4. Let Workforce compute comp/benefits for the new rows ---------------------------------
JobFactory jf = operation.application.jobFactory
executeJob(jf.job("OWP_Process Loaded Data", "Rules",
    ["Scenario": '"' + scen + '"', "Version": '"' + ver + '"', "Years": '"' + year + '"', "Entity": '"' + ent + '"']))  // RTP names per your rule
println "Created $needed TBH rows for $ent and processed loaded data."
return
