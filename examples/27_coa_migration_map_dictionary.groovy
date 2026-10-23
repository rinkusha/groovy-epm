/*RTPS: */
// Cube     : Plan1
// Trigger  : Jobs page (after uploading coa_mapping.csv and old_coa_balances.csv)
// Purpose  : migrate data from an OLD chart of accounts to a NEW one. A mapping CSV is loaded into a
//            Map (dictionary) keyed by concatenated source dimensions ("Account|Entity|CostCenter");
//            lookups fall back from most specific to least specific key ('*' = any).
//            Unmapped rows are written to an exceptions CSV in the outbox, mapped rows are saved with DataGridBuilder.
// Mapping CSV  (header): OldAccount,OldEntity,OldCostCenter,NewAccount,NewEntity,NewDepartment
// Balances CSV (header): OldAccount,OldEntity,OldCostCenter,Jan,Feb,...,Dec
// Assumptions: POV Plan/Working/FY26/USD; target dims Entity, Account, Department are the row dims
//              (Department sits in the POV via the grid row [Entity, Account, Department] below). All names are placeholders.

final String MAP_FILE  = "coa_mapping.csv"
final String DATA_FILE = "old_coa_balances.csv"
final String EXC_FILE  = "coa_unmapped.csv"
final String CUBE      = "Plan1"
final String SEP       = "|"
final List<String> PERIODS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

// 1. Load mapping dictionary: concatenated source key -> [newAccount, newEntity, newDept]
Map<String, List<String>> coaMap = [:]
csvIterator(MAP_FILE).withCloseable { CloseableIterator<String[]> r ->
    boolean header = true
    r.each { String[] v ->
        if(header) { header = false; return }
        if(v.length < 6) { println "Skipping bad mapping row: ${Arrays.toString(v)}"; return }
        String key = [v[0], v[1], v[2]].collect { it.trim() }.join(SEP)
        if(coaMap.containsKey(key)) println "Duplicate mapping key $key - last one wins"
        coaMap[key] = [v[3].trim(), v[4].trim(), v[5].trim()]
    }
}
if(!coaMap) throwVetoException("No mappings found in $MAP_FILE")
println "Loaded ${coaMap.size()} mappings"

// 2. Most specific key first, then wildcards
Closure<List<String>> lookup = { String acct, String ent, String cc ->
    [[acct, ent, cc], [acct, ent, '*'], [acct, '*', cc], [acct, '*', '*']]
        .collect { it.join(SEP) }
        .findResult { String k -> coaMap[k] }
}

Cube cube = operation.application.getCube(CUBE)
DataGridBuilder b = cube.getDataGridBuilder("MM/DD/YYYY", ExecuteAsUser.SYSTEM_USER)
b.addPov('Plan', 'Working', 'FY26', 'USD')
b.addColumn(*PERIODS)

// 3. Translate rows; the same new target can be hit by several old rows, so sum them first
Map<String, List<BigDecimal>> target = [:]
int read = 0, unmapped = 0
csvWriter(EXC_FILE).withCloseable { out ->
    out.writeNext 'OldAccount', 'OldEntity', 'OldCostCenter', 'Reason'
    csvIterator(DATA_FILE).withCloseable { CloseableIterator<String[]> r ->
        boolean header = true
        r.each { String[] v ->
            if(header) { header = false; return }
            if(v.length < 3 + PERIODS.size()) { println "Skipping short row: ${Arrays.toString(v)}"; return }
            read++
            List<String> m = lookup(v[0].trim(), v[1].trim(), v[2].trim())
            if(m == null) { unmapped++; out.writeNext v[0], v[1], v[2], 'No mapping'; return }
            String tKey = m.join(SEP)
            List<BigDecimal> sums = target.get(tKey, PERIODS.collect { 0G })
            PERIODS.eachWithIndex { String p, int i ->
                String s = v[3 + i]?.trim()
                if(s) sums[i] = sums[i] + new BigDecimal(s)
            }
        }
    }
}
println "read=$read unmapped=$unmapped targetRows=${target.size()}"
if(!target) { println "Nothing to load"; return }

// 4. Build the grid: row members are [Entity, Account, Department]
target.each { String key, List<BigDecimal> vals ->
    List<String> m = key.split("\\|") as List     // [newAccount, newEntity, newDept]
    b.addRow([m[1], m[0], m[2]], vals.collect { it.toPlainString() })
}

DataGridBuilder.Status st = new DataGridBuilder.Status()
b.build(st).withCloseable { DataGrid g ->
    println "accepted=$st.numAcceptedCells rejected=$st.numRejectedCells"
    if(st.numRejectedCells) println "Rejected sample: $st.cellsRejected"
    cube.saveGrid(g)
}
if(unmapped) println "$unmapped rows unmapped - see $EXC_FILE in the outbox"
return
