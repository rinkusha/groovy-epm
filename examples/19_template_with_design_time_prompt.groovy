/*RTPS: {BonusMultiplier} */
// Object   : Calculation Manager TEMPLATE "GT_Calculate Employee Bonuses" (cube Plan2), not a rule.
//            Design-Time Prompt: BonusMultiplier, Type Integer, Prompt it + Mandatory, text "Enter a value between 0 and 8".
//            Wizard step "Enter a Bonus Multiplier" with that DTP. DTPs arrive in rtps exactly like RTPs.
// Trigger  : attach the generated rule to the ManageEmployees form, After Save.
// Purpose  : edited-employee bonus = salary * multiplier%.
// Source   : Oracle tutorial "Creating Groovy Templates"

def bonusMultiplier = rtps.BonusMultiplier
def mbUs = messageBundle(["validation.invalidbonusmultiplier": "Bonus multiplier must be between 0 and 8: {0}"])
def mbl  = messageBundleLoader(["en": mbUs])
validateRtp(rtps.BonusMultiplier, { (0..8).contains(it.enteredValue as int) }, mbl, "validation.invalidbonusmultiplier", rtps.BonusMultiplier)

if(!operation.hasGrid()) return

Set<String> employees = []
operation.grid.dataCellIterator("Salary").each { DataCell cell ->
    if(cell.edited) employees << cell.getMemberName("Employee")
}
if(!employees) { println "No employee's salary has been updated"; return }

// Simple join idiom (alternative to cscParams) using Essbase names of the POV
List<String> povMemberNames = operation.grid.pov*.essbaseMbrName
String calcScript = """
FIX("${povMemberNames.join('", "')}", "${employees.join('", "')}")
    "Bonus" = "Salary" * (${bonusMultiplier}/100);
ENDFIX;"""

println("The following calc script was executed by $operation.user.fullName:\n$calcScript")
return calcScript.toString()
