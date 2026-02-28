# Command Action

Use a Command Action to send commands from the radial.

## Fields

- **Title**
- **Note**
- **Command** (multi-line)
- **Multi-command delay (ticks)**
- **Cycle commands (one per use)**
- **Icon**

## Command Box Rules

- One line = one command.
- Leading `/` is optional (EZ Actions strips it before sending).
- Empty lines are ignored.

## Delay Behavior

`Multi-command delay (ticks)` controls spacing between command lines when not cycling.

- `0`: send lines immediately.
- `>0`: queue line-by-line with that delay.

## Cycle Commands

If enabled, each radial use sends exactly one line and rotates to the next.

Example:

```text
/time set day
/time set night
```

Use #1 -> day  
Use #2 -> night  
Use #3 -> day

## Practical Use Cases

- Quick utility commands (`/home`, `/spawn`, `/warp mine`)
- Roleplay toggles (`/hat`, `/nick`)
- Admin workflows split across lines

## Notes

- This is client-side dispatch: server permissions still apply.
- If a new command sequence starts, previous queued sequence is replaced.

???+ info "Deep dive: sequencing model"
    - Non-cycling multi-line commands use a client tick sequencer.
    - Cycling mode stores an internal cursor in the action instance.
    - Cycling and immediate modes cancel in-flight sequences before dispatch.