# groovy-epm

A GitHub-ready knowledge base that teaches **GitHub Copilot (and any other coding agent) how to write Oracle Fusion Cloud EPM Groovy business rules** — Planning / EPBCS / Workforce / Financials / FreeForm / Strategic Modeling / EPCM.

Built from Oracle's *Groovy Scripting Reference* javadoc (E96254-40, Jan 2026), Oracle's example page, and twelve official tutorials. Every example is a complete rule you can paste into Calculation Manager.

## How Copilot uses this repo

| File | Picked up by |
|---|---|
| `.github/copilot-instructions.md` | Copilot Chat / code completion in VS Code, Visual Studio, JetBrains and github.com — automatically, for every prompt in this repo |
| `.github/instructions/epm-groovy.instructions.md` | Copilot, for any `*.groovy` file (path-scoped rules) |
| `.github/prompts/new-epm-rule.prompt.md` | Copilot Chat: type `/new-epm-rule` (VS Code prompt files) |
| `AGENTS.md` | Copilot coding agent, Claude Code, Cursor and others |
| `docs/` + `examples/` | Everything the agent reads when it needs the API |
| `snippets/epm-groovy.code-snippets` | VS Code snippets (`epmrule`, `epmedited`, `epmread`, `epmwrite`, `epmrest`, …) |

**Setup:** clone (or copy these folders into your own rules repo), open in VS Code with Copilot enabled, make sure *Use instruction files* is on in Copilot settings, then ask e.g.
> "Write an After Save rule for form `ProductRevenue` on cube `OEP_FS` that recalculates only the edited products and then runs Smart Push `Rev to Reporting`."

Copilot will reuse `examples/02` + `examples/04` and keep the house style.

## Contents

```
.github/
  copilot-instructions.md        <- runtime model + house style + don'ts
  instructions/epm-groovy.instructions.md
  prompts/new-epm-rule.prompt.md
AGENTS.md
docs/
  00-quickstart.md               <- operation / rtps / return-value rule / deploy steps
  01-api-reference.md            <- condensed javadoc (EpmScript verbatim, other classes by usage)
  02-patterns-cookbook.md        <- 15 reusable patterns
  03-gotchas.md                  <- error table + sandbox limits
  04-links.md                    <- every Oracle URL used
examples/                        <- complete rules, one pattern each
snippets/epm-groovy.code-snippets
.vscode/extensions.json          <- recommends a Groovy language extension
```

## Examples

| # | File | Pattern | Trigger |
|---|---|---|---|
| 01 | `after_load_cell_formatting` | colour cells, `#MISSING` vs 0 | After Load |
| 02 | `after_save_focused_calc_edited_cells` | edited members → `cscParams` → calc script | After Save |
| 03 | `before_save_validation_veto` | read driver cube, `crossDimCell`, `addValidationError`, veto | Before Save |
| 04 | `smart_push_edited_rows` | form Smart Push with override/exclusion maps | After Save |
| 05 | `datamap_push_from_jobs_page` | calc then `DataMap.createSmartPush()` with RTPs | Jobs |
| 06 | `add_member_dynamic_child_with_rtp_validation` | `validateRtp`, `messageBundle`, `addDynamicChild`, text cells | Menu |
| 07 | `move_rename_delete_member` | `Dimension.saveMember(Map)` | Menu |
| 08 | `read_grid_with_definition_builder` | `DataGridDefinitionBuilder` + `loadGrid` | Jobs |
| 09 | `write_grid_with_grid_builder` | `DataGridBuilder` + `saveGrid` + `Status` | Jobs |
| 10 | `rest_internal_data_management_job_polling` | named Connection, `json()`, `JsonPath`, back-off polling | Menu/Jobs |
| 11 | `rest_external_api_text_cell` | external REST, `getTextCellId` | Menu |
| 12 | `export_slice_to_csv_outbox` | `csvWriter` + read grid | Jobs |
| 13 | `execute_job_call_rule_with_rtps` | `JobFactory`, `executeJob` | After Save |
| 14 | `execute_calc_script_and_read_return` | `executeCalcScript`, `@RETURN`, `CalcScriptException` | After Save |
| 15 | `epm_automate_server_side` | `getEpmAutomate().execute(...)` | Jobs |
| 16 | `workforce_headcount_gap_to_new_hires` | read labor-model gap → fill Workforce TBH rows (read+write+job) | After Save/Jobs |
| 17 | `import_csv_from_inbox_via_grid_builder` | `csvIterator` → `DataGridBuilder` | Jobs |
| 18 | `excel_workbook_to_outbox` | `workbook()`, zip | Jobs |
| 19 | `template_with_design_time_prompt` | Calculation Manager template + DTP | After Save |
| 20 | `aso_allocation_mdx_params` | `AllocationParameters`, `mdxParams` | Jobs |
| 21 | `substitution_variables_and_rtp_driver` | sub vars, one-line expression style | Jobs |
| 22 | `import_csv_limited_columns_watchable` | header-driven column list (varying/limited columns), per-row progress + `sleep` throttle | Jobs |
| 23 | `data_management_import_with_dimension_mapping` | file import via Data Management Import Format (multi-dim column mapping configured in UI), REST trigger + poll, no grid built in Groovy | Jobs |

## Accuracy notes

- `docs/01-api-reference.md` marks with ⚠ any signature that was compiled from usage rather than read from the javadoc. Check those against the live javadoc before relying on an exact overload.
- All cube, form, dimension, member and rule names are **placeholders** from Oracle's sample app (Plan1, Plan2, OEP_FS, OEP_WFP, Employee, Product…). Map them to your application.
- Nothing here runs locally; EPM Groovy executes only inside Calculation Manager. "Testing" = Validate → Deploy → run → read the Jobs console.

## Contributing

Add a rule as `examples/NN_snake_case.groovy` with the standard header (RTPS line, then Cube/Form/Trigger/Purpose comment), add a row above, and add a pattern to `docs/02` if it is new. Keep `AGENTS.md` and `.github/copilot-instructions.md` in sync.
