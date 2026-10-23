# AGENTS.md

Guidance for any AI coding agent (GitHub Copilot coding agent, Claude Code, Cursor, etc.) working in this repository.

**What this repo is:** a reference library for Oracle Fusion Cloud EPM server-side Groovy business rules. There is nothing to build or run locally — EPM Groovy only executes inside Calculation Manager in an EPM Cloud environment. "Tests" are: copy the rule into a rule in Calculation Manager, Validate, Deploy, run it, and read the Jobs console.

**Read first:** `.github/copilot-instructions.md` (runtime model + house style), then `docs/01-api-reference.md`.

**Hard constraints**
- No `import oracle.epm…`, no `@Grab`, no `System.*`, no `java.io.File`, no `java.net`, no threads. The EPM sandbox rejects them.
- The script's final `String` expression is executed as an Essbase calc script. End with `return` unless that is intended.
- Never hard-code credentials or environment URLs; HTTP goes through named Connections (`operation.application.getConnection("Name")`).
- Treat every cube/dimension/member/form name in this repo as a placeholder.

**When adding an example**
- File name: `examples/NN_short_description.groovy` (two-digit prefix, snake_case).
- Header: `/*RTPS: …*/` line, then a comment block with Cube / Form / Trigger / assumptions.
- Add one row to the table in `README.md` and, if it introduces a new pattern, a section in `docs/02-patterns-cookbook.md`.
- Only use API calls that appear in `docs/01-api-reference.md` or in Oracle's javadoc (`docs/04-links.md`). If you are not sure a method exists, say so in a comment rather than inventing it.

**Groovy level:** EPM Cloud runs Groovy 2.4.x semantics (no `var`, no Java records, no switch expressions). Static typing is optional but closures over `DataCell` should declare the parameter type.
