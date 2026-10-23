/*RTPS: {Scenario} {Version} {StartYear}*/
// Cube     : SourceCube (placeholder) -> TargetCube (reporting cube)
// Trigger  : Jobs page
// Purpose  : run a Data Map (job type PLAN_TYPE_MAP) through the Planning REST API with
//            overrideMembersMap, then poll until it finishes and report the result.
// Assumes  : a Connection "Rest_Api_Call" whose base URL is the EPM host, and a Data Map named
//            "DataPush Source - Target". Application, dimension and member names are placeholders.

String app = "AppName"
String strScenario  = rtps.Scenario.toString()
String strVersion   = rtps.Version.toString()
String strStartYear = rtps.StartYear.toString()

HttpResponse<String> jsonResponse = operation.application
    .getConnection("Rest_Api_Call")
    .post("/rest/v3/applications/${app}/jobs")
    .header("Content-Type", "application/json")
    .body(json([
        "jobType"   : "PLAN_TYPE_MAP",
        "jobName"   : "DataPush Source - Target",
        "parameters": [
            "overrideMembersMap": [
                "Account"  : 'ILVL0Descendants(GrossMargin)',
                "Scenario" : strScenario,
                "Version"  : strVersion,
                "Years"    : strStartYear,
                "Currency" : 'USD_Reporting',
                "Entity"   : 'ILVL0Descendants(TotalEntity)',
                "Period"   : 'ILVL0Descendants(YearTotal)']]]))
    .asString()

boolean ok = awaitCompletion(jsonResponse, "Rest_Api_Call", app, "Elimination data push is now ")
if(!ok) throwVetoException("Data Map push failed — see the Jobs console for details")
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
