/*RTPS: {EmployeeName} {EmployeePhone} {EmployeeEmail} {Employee} {Scenario} {Year} {Period} {Entity} {Version}*/
// Cube     : Plan2
// Trigger  : Right-click Action Menu on an Employee row (the clicked member arrives as rtps.Employee)
// Purpose  : validate text RTPs with regex/closure, create the new employee as a dynamic child
//            of the clicked parent, then return a calc script that stores phone/email as text cells.
// Needs    : parent member "Enable for Dynamic Children" + spare dynamic-child slots.
// Source   : Oracle tutorial "Adding Dimension Members with Groovy"

def mbUs = messageBundle([
    "validation.invalidemail"     : "Email address is invalid: {0}",
    "validation.invalidphone"     : "Phone number is invalid: {0}",
    "validation.memberexists"     : "The member you have specified already exists and cannot be created: {0}.",
    "validation.invalidnamelength": "Employee name must be 5 to 40 characters: {0}"])
def mbl = messageBundleLoader(["en": mbUs])

Member parentEmployee = rtps.Employee.member

// ---- validation (each call vetoes the operation with a localized message on failure) --------
if(parentEmployee.hasChild(rtps.EmployeeName.enteredValue))
    throwVetoException(mbl, "validation.memberexists", rtps.EmployeeName)

validateRtp(rtps.EmployeeName,  { (5..40).contains(it.enteredValue.size()) }, mbl, "validation.invalidnamelength", rtps.EmployeeName)
validateRtp(rtps.EmployeeEmail, /^.+@.+/,                                     mbl, "validation.invalidemail",      rtps.EmployeeEmail.enteredValue)
validateRtp(rtps.EmployeePhone, /^(?:\+?1[- ]?)?\(?([0-9]{3})\)?[- ]?([0-9]{3})[- ]?([0-9]{4})$/,
                                                                              mbl, "validation.invalidphone",      rtps.EmployeePhone)

// ---- create the member -----------------------------------------------------------------------
Member employee = parentEmployee.addDynamicChild(rtps.EmployeeName.enteredValue)
println "Created ${employee.name} under ${parentEmployee.name}"

// Alternative when you need full control over properties (alias, storage, plan types...):
//   Map props = parentEmployee.newChildAsMap(rtps.EmployeeName.enteredValue)
//   props["Alias: Default"] = "..."
//   Member employee = operation.application.getDimension("Employee").saveMember(props, DynamicChildStrategy.ALWAYS_DYNAMIC)

// ---- store properties as text cells; last String expression is executed -----------------------
"""SET CREATENONMISSINGBLK ON;
FIX(${fixValues(rtps.Scenario, rtps.Year, rtps.Period, rtps.Entity, rtps.Version, employee)}, "USD")
    "Employee Phone" = $rtps.EmployeePhone;
    "Employee Email" = $rtps.EmployeeEmail;
ENDFIX;"""
