/*RTPS: {Employee} {EmployeeAddress} {Scenario} {Year} {Period} {Entity} {Version}*/
// Cube     : Plan2
// Trigger  : Right-click menu on an Employee
// Purpose  : call an EXTERNAL REST API (named Connection "Places" = Google Places find-place URL
//            with the API key baked into the connection URL), parse JSON with JsonPath, and
//            write the result into a TEXT account using getTextCellId().
// Source   : Oracle tutorial "Calling an External REST API Using Groovy"

Member employee = rtps.Employee.member

def address = rtps.EmployeeAddress                 // fallback: whatever the user typed (RtpValue renders as a literal)
HttpResponse<String> jsonResponse = operation.application.getConnection("Places")
        .get()
        .queryParam("input", rtps.EmployeeAddress.enteredValue)
        .asString()
println(jsonResponse.body)

ReadContext ctx = JsonPath.parse(jsonResponse.body)
if(ctx.read('$.status') == "OK")
    address = getTextCellId(ctx.read('$.candidates[0].formatted_address') as String, true)   // true = create id if new

// Text accounts store an integer id; the calc script assigns that id.
"""SET CREATENONMISSINGBLK ON;
FIX(${fixValues(rtps.Scenario, rtps.Year, rtps.Period, rtps.Entity, rtps.Version, employee)}, "USD")
    "Employee Address" = $address;
ENDFIX;"""
