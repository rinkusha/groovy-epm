# salew — Salesforce → OIC → EPM inbox → grouped load

Flow:

1. **OIC scheduled integration** runs a SOQL query on Salesforce (Salesforce Adapter → *Query*/*Run SOQL*).
2. OIC maps the result to CSV (`Entity,Account,Period,Amount`) with a stage-file write.
3. OIC uploads `sf_opportunities.csv` to the EPM **inbox** (EPM Cloud adapter → *Upload File*, or the EPM REST file upload API).
4. OIC triggers the Groovy rule (Planning Jobs REST API, `jobType=RULES`, `jobName=LoadSalesforceCsv`).
5. `01_load_salesforce_csv_grouped.groovy` groups rows by Entity/Account/Period (sum) and loads with `DataGridBuilder`.

## SOQL (run in OIC)

```sql
SELECT Owner.Region__c Entity, Product2.Family Account, CALENDAR_MONTH(CloseDate) Month, SUM(Amount) Amount
FROM Opportunity
WHERE IsWon = true AND CALENDAR_YEAR(CloseDate) = 2026
GROUP BY Owner.Region__c, Product2.Family, CALENDAR_MONTH(CloseDate)
```

Field names are placeholders (verify against your Salesforce org). Grouping in SOQL is optional — the Groovy
rule groups again, so raw row-level queries also work. Convert month number → `Jan..Dec` and
region/family → EPM member names in the OIC mapper.

## Notes

- Names (cube, POV, members, file, rule) are placeholders.
- Credentials live in OIC connections and the EPM Connections page — never in the rule.
- For very large files, use Data Management / `importdata` instead of a grid load (see `examples/17`, `examples/24`).
