/*RTPS: */
// Cube     : Plan2 (source)  ->  reporting cube via Smart Push "Employee Data" attached to the form
// Form     : Employee form with Salary / Reporting Manager accounts
// Trigger  : After Save
// Purpose  : push only the employees whose data changed. Smart Push is auto-scoped to the grid POV;
//            the override map narrows it further to the edited employees.
// Source   : Oracle tutorial "Moving Modified Data Using Groovy and Smart Push"

if(!operation.hasGrid()) return
if(!operation.grid.hasSmartPush("Employee Data")) { println "Form has no Smart Push 'Employee Data'"; return }

Set<String> employees = []
operation.grid.dataCellIterator('Salary', 'Reporting Manager').each { DataCell cell ->
    if(cell.edited) employees << cell.getMemberName("Employee")
}

if(!employees) { println "Nothing edited; nothing pushed"; return }

// Override values are ONE string containing a quoted, comma-separated member list
String employeesStr = '"' + employees.join('", "') + '"'

// execute(overrideMembers)                        -> push only these employees
// execute(overrideMembers, exclusionMembers)      -> ...minus the excluded ones
operation.grid.getSmartPush("Employee Data").execute(["Employee": employeesStr],
                                                     ["Employee": "No Employee"])
println "Pushed ${employees.size()} employee(s): $employeesStr"
return
