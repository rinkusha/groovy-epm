---
mode: agent
description: Scaffold a new Oracle EPM Cloud Groovy business rule from this repo's patterns
---

Create a new Oracle EPM Cloud Groovy business rule.

Ask me (if not already given) for:
1. Cube name and whether the rule is form-bound (After Load / Before Save / After Save / right-click menu) or run from Jobs.
2. The run-time prompts (name + type: Member, Members, String, Integer, Date, Smart List).
3. What should happen: validate / format cells / focused calc on edited cells / Smart Push / member CRUD / REST call / export file / call another rule.

Then:
- Pick the closest file in `examples/` and use its structure.
- Follow `.github/instructions/epm-groovy.instructions.md` exactly (RTPS header, assumptions comment, no imports, `cscParams`, early-exit on no edits, `withCloseable`, veto for errors).
- Only use methods listed in `docs/01-api-reference.md`; if you need something not listed, say so and link the Oracle javadoc from `docs/04-links.md` instead of guessing.
- Output the rule as a single `.groovy` file under `examples/` (or the path I give), followed by a short "How to deploy" note: Calculation Manager → Rules → New → Edit Script → Script Type Groovy → Validate → Deploy → attach to form (Business Rules tab) with the right trigger.
