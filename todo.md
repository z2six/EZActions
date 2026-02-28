EZActions API parity todo (implement all)

1. Expose bundle flags in API:
- Add `hideFromMainRadial` and `bundleKeybindEnabled` to `ApiMenuItem`.
- Preserve/read/write these flags through API CRUD and JSON APIs.

2. Expose action details in read API:
- Provide action type and payload details for key/command actions.
- Include key mapping name, toggle, delivery mode, command text, and delay ticks.

3. Implement icon round-trip in API:
- Include icon kind/id in API model.
- Ensure API import/export reads and writes icon data instead of null placeholders.

4. Preserve rich text title/note through API JSON:
- Keep support for component JSON in API import/export.
- Do not flatten component-backed title/note into plain strings.

5. Add runtime radial controls to API:
- Open radial at root.
- Open radial at a specific bundle id.

6. Make InputOps/EditorOps real and reachable from `EzActionsApi`:
- Add accessors on `EzActionsApi`.
- Implement both in internal API implementation.

7. Wire API events end-to-end:
- Add event accessor on `EzActionsApi`.
- Fire menu/import events from all mutating paths.

8. Unify API JSON with live menu format:
- API import/export should use the same shape as `menu.json`/clipboard import-export.
- Avoid alternate wrapper-only schema for API paths.

9. Fix ImportExport contract/validation:
- `importInto` must support both object and array inputs as documented.
- Implement meaningful structural/schema validation beyond object/array shape.

10. Fix docs/package parity:
- Update docs to actual package (`org.z2six.ezactions.api`) and current surface.
- Remove stale/nonexistent API examples and claims.
