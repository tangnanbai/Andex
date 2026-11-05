# Andex 0.93

- Both conversation sidebars now use one shared directional drawer. Horizontal
  drags can start anywhere in the chat, follow the finger, and settle with the
  same spring animation. Vertical scrolling and taps do not claim a drawer.
  Only one side can be active; scrim taps, reverse swipes and Android Back close
  it. The work-trace header has no close button.
- Native Pi sessions automatically compact at 80% of the resolved model context
  window, including custom windows. The reserve is calculated for Pi's strict
  comparison; recent content retention scales down for small windows. Pi keeps
  its existing compaction boundaries, persisted session and failure recovery.
- Long-term memory is enabled by default in the agent core on Android and iOS.
  Its bundled settings extension is named 长效记忆. The agent has a `memory`
  read/search/add/replace/remove tool and receives a frozen snapshot per session.
  It is instructed to save durable preferences, project facts and reusable lessons.
- User profile and learned notes live in `~/.andex/memory/USER.md` and
  `MEMORY.md`, bounded to 4,000 and 6,000 characters. Writes are locked and atomic,
  deduplicated, and refuse over-capacity edits. Known credential patterns are
  rejected. This design follows Hermes' separated curated files and frozen
  session snapshot; it is an Andex implementation, not a bundled Hermes runtime.
- Before compaction, the selected model extracts up to three new entries per
  category from the text about to be discarded. This adds a model request in
  addition to compaction. Extraction failure reports a warning and retains saved
  memory without blocking compaction. This uses the current provider and API.
- The extension settings toggle stops injection and reads/writes without erasing
  files. The trash action requires two taps within ten seconds. Existing session
  snapshots remain frozen; a new session loads the cleared/updated files.
  App updates preserve files; clearing app data or uninstalling removes them.

Validation: Kotlin conversation/trace tests, bridge mock API tests for the exact
80% boundary and pre-compaction memory flush, persistence/disable/snapshot tests,
concurrent store edits and extension settings actions. Emulator UI checks cover
global drags in both directions, partial drag frames, closing, scrim taps and
ordinary role-button taps. The emulator's Alpine PRoot still reports a ptrace
permission error; model integration uses a local mock provider rather than that
runtime. iOS source is updated; Windows cannot run the native iOS build.

Reference: NousResearch/hermes-agent, `tools/memory_tool.py` (read-only reference).
