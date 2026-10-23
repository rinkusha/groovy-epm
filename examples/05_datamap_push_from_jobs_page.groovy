/*RTPS: {Scenario} {Version} {Entity}*/
// Cube     : Plan1 (source) -> reporting ASO cube, via Data Map "Plan to Reporting" (Data Exchange > Data Maps)
// Trigger  : Jobs page / EPM Automate / scheduled — no form grid needed
// Purpose  : run a Data Map as a Smart Push with run-time-prompt scoping, after a calc.
// Verify   : exact DataMap/SmartPush.execute(...) overloads in the javadoc (see docs/04-links.md);
//            the override-map form is shown in Oracle tutorials, the clear-data boolean is marked ⚠ in docs/01.

Cube cube = operation.application.getCube("Plan1")

// 1. Aggregate first so the push carries rolled-up numbers
cube.executeCalcScript("""
FIX(${fixValues(rtps.Scenario, rtps.Version, rtps.Entity)})
  AGG("Account", "Product");
ENDFIX""")

// 2. Push the same slice. Override values are quoted member lists as a single string.
DataMap dm = operation.application.getDataMap("Plan to Reporting")
dm.createSmartPush().execute([
        "Scenario": '"' + rtps.Scenario.member.name + '"',
        "Version" : '"' + rtps.Version.member.name  + '"',
        "Entity"  : '"' + rtps.Entity.member.name   + '"'
])

println "Data map 'Plan to Reporting' pushed for ${rtps.Scenario.member.name}/${rtps.Version.member.name}/${rtps.Entity.member.name}"
return
