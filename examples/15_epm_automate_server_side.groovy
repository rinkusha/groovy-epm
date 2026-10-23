/*RTPS: */
// Cube     : any
// Trigger  : Jobs page / scheduled — Service Administrator only (getEpmAutomate is privileged)
// Purpose  : run EPM Automate commands INSIDE the pod (no client install): export data, list files,
//            sub vars, snapshot copy for DR, email. Every command returns EpmAutomateStatus:
//            getStatus() 0 = ok, getOutput() text, getItemsList() for list commands.
// Source   : Oracle "Running Commands without Installing EPM Automate" + clone example

EpmAutomate automate = getEpmAutomate()

// --- helper: run and fail fast ------------------------------------------------------------
def run = { String... args ->
    EpmAutomateStatus st = automate.execute(*args)
    println "epmautomate ${args.join(' ')} -> status ${st.status}\n${st.output}"
    if(st.status != 0) throwVetoException("EPM Automate command failed: ${args[0]} — ${st.output}")
    return st
}

// 1. Export level-0 data with a saved Export Data job, then list the outbox
run('exportdata', 'ExportPlan1Lvl0', 'plan1_lvl0.zip')
EpmAutomateStatus files = run('listfiles')
files.itemsList.each { println "  outbox/inbox file: $it" }

// 2. Read / set substitution variables
EpmAutomateStatus sv = run('getsubstvar', 'ALL')
sv.itemsList.each { println "  subvar: $it" }
// run('setsubstvars', 'ALL', 'CurrMonth=Jun')

// 3. Email a status (requires sendmail support in the pod)
// run('sendmail', 'finance.systems@example.com', 'Nightly export done', 'BODY=Export completed OK')

// 4. DR pattern (cross-pod): encrypt once, then login/recreate/copysnapshotfrominstance/importsnapshot/logout
// run('encrypt', 'password', 'encryptionKey', 'targetPassword.epw')
// run('login', 'username', 'targetPassword.epw', 'https://target-pod.epm.us-phoenix-1.ocs.oraclecloud.com')
// run('recreate', '-f')
// run('copysnapshotfrominstance', 'Artifact Snapshot', 'sourceuser', 'sourcePassword.epw', 'https://source-pod...')
// run('importsnapshot', 'Artifact Snapshot')
// run('logout')

return
