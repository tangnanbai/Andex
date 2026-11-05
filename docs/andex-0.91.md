# Andex 0.91

Fix releases advance by 0.01: 0.91, 0.92, 0.93. Run
`python scripts/bump-fix-version.py` for the next release. Both Android and iOS
build numbers also increase; do not override the version with an old Gradle property.

- Phone-control JSON reports use a full-width, 240 dp tall scrollable code area
  with copy and clear buttons.
- Background progress has its own settings entry and a visible return button.
- Role definitions retain the existing shared system instructions. Each role has
  a name, prompt and emoji or resized image avatar. New chats use the default
  role; the current role can be selected above the conversation.
- Every new agent turn includes the current role in its system instructions.
  Changing a role during a running turn applies to the next turn.
- The left conversation drawer holds the light/dark toggle. Dark backgrounds are
  black; surfaces keep contrast for readability.
- Swipe inward from the right conversation edge to open work records. Recorded
  inputs, replies, status, reasoning summaries and tool calls are searchable.
  Each item expands to show its stored contents, arguments and result.
  This reads the existing persisted conversation and live execution records;
  it does not invent unrecorded steps or hidden model reasoning.
- Work records render lazily; parsing and filtering run off the UI thread.

Android is compiled and checked on the emulator. iOS source changes use the same
role model, prompt composition, theme and work-record panel; a macOS/Xcode build
is required to validate iOS.
