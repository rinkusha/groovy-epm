/*RTPS: {Scenario} {Version} {Department}*/
// Cube     : SourceCube (placeholder) -> TargetCube (reporting cube)
// Trigger  : Jobs page
// Purpose  : run a Data Map (PLAN_TYPE_MAP) with a dynamic Department filter supplied through
//            overrideMembersMap, then poll until finished. Extends example 25.
// Assumes  : Connection "Rest_Api_Call"; Data Map "DataPush Source - Target"; Department dimension
//            named "Department". All names are placeholders.
//
// TEST CASES (run from the Jobs page, read the Jobs console):
//  T1 Department = "Sales"            -> only Sales is pushed; console "Department filter: Sales"
//  T2 Department = "Sales,Marketing"  -> both pushed (comma list expands to a member list)
//  T3 Department left blank           -> falls back to ILVL0Descendants(TotalDepartment)
//  T4 Department = "NoSuchDept"       -> job fails; rule vetoes with "Data Map push failed"
//  T5 Scenario/Version prompts blank  -> rule vetoes before calling REST

String app = "AppName"
String scenario   = rtps.Scenario.toString()
String version    = rtps.Version.toString()
String department = rtps.Department.toString().trim()

if(!scenario || !version) throwVetoException("Scenario and Version are required")

String deptFilter = department ?
    department.split(',')*.trim().findAll { it }.join(',') :
    'ILVL0Descendants(TotalDepartment)'
println "Department filter: $deptFilter"

HttpResponse<String> response = operation.application
    .getConnection("Rest_Api_Call")
    .post("/rest/v3/applications/${app}/jobs")
    .header("Content-Type", "application/json")
    .body(json([
        "jobType"   : "PLAN_TYPE_MAP",
        "jobName"   : "DataPush Source - Target",
        "parameters": [
            "overrideMembersMap": [
                "Scenario"   : scenario,
                "Version"    : version,
                "Department" : deptFilter]]]))
    .asString()

if(!awaitCompletion(response, "Rest_Api_Call", app, "Department data push is now "))
    throwVetoException("Data Map push failed — see the Jobs console for details")
return

// ---- helpers ---------------------------------------------------------------------------------
/** Poll a Planning job: status -1 = running, 0 = success, >0 = failed. Back-off 50ms -> 1000ms. */
boolean awaitCompletion(HttpResponse<String> response, String connectionName, String app, String label) {
    final int IN_PROGRESS = -1
    if(!(200..299).contains(response.status))
        throwVetoException("Error occurred: $response.statusText")

    ReadContext ctx = JsonPath.parse(response.body)
    int status = ctx.read('$.status')
    String jobId = ctx.read('$.jobId') as String
    for(long delay = 50; status == IN_PROGRESS; delay = Math.min(1000, delay * 2)) {
        sleep(delay)
        HttpResponse<String> ping = operation.application.getConnection(connectionName)
            .get("/rest/v3/applications/${app}/jobs/${jobId}").asString()
        status = JsonPath.parse(ping.body).read('$.status')
    }
    println("$label ${status == 0 ? 'complete' : 'failed'}.")
    return status == 0
}
