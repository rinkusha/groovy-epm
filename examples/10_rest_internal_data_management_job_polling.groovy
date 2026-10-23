/*RTPS: */
// Cube     : OEP_FS
// Trigger  : Right-click menu or Jobs page
// Purpose  : call the INTERNAL Data Management REST API through a named Connection ("DM",
//            URL .../aif/rest/V1/jobs with service-admin creds), then poll until the job finishes.
//            Same polling skeleton works for Planning jobs (/HyperionPlanning/rest/v3/applications/{app}/jobs),
//            OIC, EDMCS, Workday, etc. — only the status field/values change.
// Source   : Oracle tutorial "Calling an Internal REST API Using Groovy"

HttpResponse<String> jsonResponse = operation.application.getConnection("DM")
        .post()
        .body(json([
            "jobType"    : "DATARULE",
            "jobName"    : "ProductVolumes",
            "startPeriod": "Jan-17",
            "endPeriod"  : "Dec-17",
            "importMode" : "REPLACE",
            "exportMode" : "STORE_DATA",
            "fileName"   : "inbox/ProductVolumes.csv"]))
        .asString()

boolean ok = awaitCompletion(jsonResponse, "DM", "Push Product Volumes to Planning")
if(!ok) throwVetoException("Data load failed — see Data Management process details")
return

// ---- helpers ---------------------------------------------------------------------------------
/** Poll a DM job: status -1 = running, 0 = success, >0 = failed. Back-off 50ms -> 1000ms. */
boolean awaitCompletion(HttpResponse<String> response, String connectionName, String label) {
    final int IN_PROGRESS = -1
    if(!(200..299).contains(response.status))
        throwVetoException("Error occurred: $response.statusText")

    ReadContext ctx = JsonPath.parse(response.body)
    int status = ctx.read('$.status')
    String jobId = ctx.read('$.jobId') as String
    for(long delay = 50; status == IN_PROGRESS; delay = Math.min(1000, delay * 2)) {
        sleep(delay)
        status = getJobStatus(connectionName, jobId)
    }
    println("$label ${status == 0 ? 'successful' : 'failed'}.")
    return status == 0
}

int getJobStatus(String connectionName, String jobId) {
    HttpResponse<String> ping = operation.application.getConnection(connectionName).get("/" + jobId).asString()
    return JsonPath.parse(ping.body).read('$.status')
}
