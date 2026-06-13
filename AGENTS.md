# Project Instructions

- Do not add fallback behavior, alternate input shapes, compatibility paths, silent defaults, or best-effort parsing unless the user explicitly asks for it.
- Preserve strict contracts. If a payload, model, or API shape is documented, fail loudly when it is violated.
- If a fallback seems useful, propose it first and wait for approval before implementing it.
- Prefer fixing the real source of invalid data over accepting invalid variants downstream.
- Do not add or modify tests unless the user explicitly asks for tests.
