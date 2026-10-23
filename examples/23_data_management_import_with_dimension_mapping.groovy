/*RTPS: {StartPeriod} {EndPeriod}*/
// Cube     : OEP_FS (Data Management target cube)
// Trigger  : Jobs page / EPM Automate `uploadfile` + run this rule, or scheduled
// Purpose  : import a MULTI-DIMENSION data file directly through Data Management instead of
//            parsing the CSV and building a DataGridBuilder grid in Groovy. The column->dimension
//            mapping (and any value mapping) lives entirely in the Data Management Import Format
//            + Data Load Rule ("ProductVolumes") that is configured once in the UI
//            (Data Exchange > Data Load Mapping). Groovy only uploads/triggers/polls the job.
// Setup assumed (configure once in Data Management, not in Groovy):
//   - Import Format maps file columns -> Entity/Account/Product/... dimensions for OEP_FS.
//   - Data Load Rule "ProductVolumes" uses that Import Format and target period range.
//   - A Connection named "DM" points at this environment's own
//     .../aif/rest/V1/jobs endpoint with service-admin credentials (Tools > Connections).
// Compare with: examples/05 (DataMap — cube-to-cube push, mapping also configured in the UI)
//               examples/17, 22 (DataGridBuilder — ad-hoc CSV, mapping is hand-written in Groovy)

final int IN_PROGRESS = -1

HttpResponse<String> jsonResponse = operation.application.getConnection("DM")
        .post()
        .body(json([
            "jobType"    : "DATARULE",
            "jobName"    : "ProductVolumes",
            "startPeriod": rtps.StartPeriod.enteredValue,
            "endPeriod"  : rtps.EndPeriod.enteredValue,
            "importMode" : "REPLACE",      // Import Format mapping decides which dims get which columns
            "exportMode" : "STORE_DATA",   // load into the Essbase cube (not just the DM staging table)
            "fileName"   : "inbox/ProductVolumes.csv"]))   // uploaded beforehand via EPM Automate `uploadfile`
        .asString()

if(!(200..299).contains(jsonResponse.status))
    throwVetoException("Error starting Data Management job: $jsonResponse.statusText")

ReadContext ctx = JsonPath.parse(jsonResponse.body)
int status = ctx.read('$.status')
String jobId = ctx.read('$.jobId') as String
for(long delay = 50; status == IN_PROGRESS; delay = Math.min(1000, delay * 2)) {
    sleep(delay)
    HttpResponse<String> ping = operation.application.getConnection("DM").get("/" + jobId).asString()
    status = JsonPath.parse(ping.body).read('$.status')
}

println "Data Management import 'ProductVolumes' (${rtps.StartPeriod.enteredValue}-${rtps.EndPeriod.enteredValue}) ${status == 0 ? 'succeeded' : 'FAILED, status=' + status}"
if(status != 0) throwVetoException("Data load failed — see Data Management process details for job $jobId")
return
