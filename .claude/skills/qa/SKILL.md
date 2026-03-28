---
name: qa
description: Run the QA workflow. Launches code-review, build-test, and doc-check teams in parallel. If issues are found, delegates fixes to the implementer team. Aggregates results and reports.
disable-model-invocation: true
argument-hint: "[focus]"
---

# QA Workflow

You are the QA lead. Execute the following PDCA cycle.

## Phase 1: Plan

Assess the current state:
- Run `git status` and `git diff` to identify changes
- Check recent commit log

Briefly report the scope and impact of changes to the user, and announce that you are launching 3 teams in parallel.

## Phase 2: Do

Launch the following 3 agents **in parallel** (a single message with 3 Agent tool calls):

1. **code-reviewer** — Code quality review
   - Prompt: provide a summary of current changes (git diff overview) and request review

2. **build-tester** — Build and test verification
   - Prompt: request build and test execution

3. **doc-reviewer** — Documentation consistency check
   - Prompt: request consistency check between CLAUDE.md / blpc-overview and the current codebase

Include the current branch name and change summary in each agent's prompt.

## Phase 3: Check

After collecting results from all teams:

1. Summarize each team's results
2. Flag any CRITICAL/FAIL findings immediately
3. Produce an integrated quality report

## Phase 3.5: Fix — Only if issues were found

If Check detected CRITICAL or FAIL:

1. List all items requiring fixes
2. Launch the **implementer** agent with specific fix instructions:
   - What to fix (file, line, content)
   - Why it needs fixing (cite review findings)
   - Run `spotlessApply` and `build` after changes
3. Verify implementer results
4. Re-launch **build-tester** if needed to confirm build success

Skip this phase if no fixes are required.

## Phase 4: Act

### Documentation cleanup
Based on results, if needed:
- Propose specific CLAUDE.md corrections (reflecting doc-reviewer findings)
- Save any insights worth remembering to memory

### Final report
Report to the user in the following format:

```
## QA Report

### Overview
- Branch: <branch name>
- Changes: <change summary>

### Team Results
| Team | Verdict | Critical | Warning | Note |
|------|---------|----------|---------|------|
| Code Review | PASS/WARN/FAIL | 0 | 0 | ... |
| Build & Test | PASS/WARN/FAIL | 0 | 0 | ... |
| Documentation | PASS/WARN/FAIL | 0 | 0 | ... |
| Implementation | PASS/SKIP/FAIL | 0 | 0 | ... |

### Critical Issues
- (list if any)

### Warnings
- (list if any)

### Suggestions
- (list if any)

### Overall Verdict: PASS / PASS_WITH_WARNINGS / FAIL
```

If any CRITICAL exists, Overall Verdict must be FAIL.
