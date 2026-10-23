# 22 – Data load rule from a git repository

Rule: `../22_git_data_load_rule.groovy` (Jobs page, RTPs `GitPath`, `GitRef`).

Flow: fetch CSV from git over HTTPS (named Connection) → validate (column count, entity list, numeric/range) → transform (2-decimal, blank → `#missing`) → load with `DataGridBuilder`. Errors: bad rows are logged and skipped; unreachable git, HTTP failure or no valid rows → `throwVetoException`.

## Setup
1. Connections page: create `GitRepo` with URL `https://api.github.com/repos/<owner>/<repo>/contents` and a read-only token (see `config/git.properties`).
2. Push `data/drivers.csv` (sample) to your repo.
3. Adjust constants at the top of the rule (mirrored in `config/import.properties`); EPM Groovy cannot read repo config files at runtime.
4. Paste the rule into Calculation Manager → Validate → Deploy → run from Jobs with `GitPath=data/drivers.csv`, `GitRef=main`; read the Jobs console.

## Alternative
The sandbox has no git client. `scripts/fetch_and_upload.sh` clones/pulls on a workstation and uploads with EPM Automate; then use `examples/17_*`.
