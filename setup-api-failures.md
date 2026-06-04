# Project Setup API — Unexpected Failures

## 1. `complete_environment_configuration` — HTTP 409 persists after rename

**Expected behaviour:** After renaming the draft EnvConfig to a unique name, calling
`complete_environment_configuration` should promote the draft and return success.

**Actual behaviour:** Always returns HTTP 409 regardless of the name in the draft:

```
{ "error": "air-backend rejected the request (HTTP 409): An environment configuration
  named 'gitcards' already exists for 'cf9b34b0-71c7-af73-6433-beafbb64ba6d'" }
```

The error message always references the *original* repository-derived name `'gitcards'`,
not the name currently stored in the draft. This suggests the backend derives/enforces the
final name from `repositoryName` (or similar), ignoring the `name` field that was
successfully updated via `update_environment_configuration`.

**Calls made (in order):**

```
# Attempt 1 — after first rename to "gitcards Android App (Setup)"
POST $SETUP_API_URL/tools/complete_environment_configuration
{}
→ HTTP 409: "An environment configuration named 'gitcards' already exists …"

# Attempt 2 — after second rename to "gitcards-dev-91ae9b57"
POST $SETUP_API_URL/tools/update_environment_configuration
{ "update": { "name": "gitcards-dev-91ae9b57" } }
→ HTTP 200, draft name in response = "gitcards-dev-91ae9b57"  ✓

POST $SETUP_API_URL/tools/complete_environment_configuration
{}
→ HTTP 409: "An environment configuration named 'gitcards' already exists …"
```

**Reasoning that the calls were correct:** The schema marks `name` as a settable field
(not in the `immutable` list: id, serviceHost, organization, repositoryName, isDraft).
The update endpoint acknowledged both new names. The conflict check appears to use
`repositoryName` rather than `name`, which is not documented and makes the `name` field
effectively useless for resolving this conflict.

---

## 2. `update_environment_configuration` — array fields are fully replaced, not merged

**Expected behaviour (from docs):** "Variables are upserted by key — existing variables
not included in the request are kept."

**Actual behaviour:** Array fields other than `variables` (specifically `allowedDomains`
and presumably `predefinedDomainLists`) are **fully replaced** by the value in the
request. Omitting them from a partial update silently clears them.

**Example:**

```
# State before: allowedDomains = ["jitpack.io"]

POST $SETUP_API_URL/tools/update_environment_configuration
{ "update": { "name": "gitcards Android App (Setup)" } }

# State after (from response): allowedDomains = []   ← silently cleared
```

**Reasoning that this is unexpected:** The upsert-by-key semantics documented for
`variables` reasonably implies that other collection fields behave similarly. There is no
warning in the schema guidance or `mcpGuidance` that array fields are replace-on-write.
The loss is silent — no error, no diff in the response highlighting the deletion.
