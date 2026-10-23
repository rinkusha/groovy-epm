/*RTPS: {ApplicationName} {DimensionName} {ViewName} {FileName} {ImportMode}*/
// Cube     : N/A (EDMCS is the REST target; this runs as an EPM Groovy rule)
// Trigger  : Jobs page
// Purpose  : submit a node import into an EDMCS view through a named REST Connection.
// Assumptions: The import file is already uploaded to EDMCS; the named Connection
//              "EDMCS" uses this environment's base URL through /epm/rest/v1 and
//              credentials authorized to import into the target application/dimension.
//              Confirm the endpoint and request fields against the live EDMCS REST API
//              reference before deployment; this request is asynchronous and only submits it.

String importPath = "/applications/${rtps.ApplicationName.enteredValue}" +
        "/dimensions/${rtps.DimensionName.enteredValue}/imports"

HttpResponse<String> response = operation.application.getConnection("EDMCS")
        .post(importPath)
        .body(json([
            "fileName"  : rtps.FileName.enteredValue,
            "viewName"  : rtps.ViewName.enteredValue,
            "importMode": rtps.ImportMode.enteredValue]))
        .asString()

if(!(200..299).contains(response.status))
    throwVetoException("EDMCS node import submission failed: $response.status $response.statusText")

println "EDMCS node import submitted. Inspect the response and EDMCS request history for completion: ${response.body}"
return
