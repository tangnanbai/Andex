# Andex 0.92

- The right work-record panel now wraps the chat in a mirrored
  `ModalNavigationDrawer`, using the same 322 dp sheet, 30 dp corners, colors and
  drawer motion as the left conversation panel. It closes by swipe, scrim tap,
  close button or Android Back. The edge gesture leaves chat clicks available.
- Model, balance and role controls use the same capsule component. The role
  control follows the balance in the top row and uses the model menu's popup
  transitions, item spacing and selection indicator.
- Role avatars are selected through the system image picker. There is no avatar
  text field. Images are sampled, resized and embedded in the saved role.
- General-settings summaries show language only.
- Background-progress settings reuse the app's settings scaffold, card groups,
  permission row and switch, and follow both light and dark modes.
- Accessibility and ADB controls are directly shown after Agent Mode
  authorization and before virtual-display controls. Pairing/connection fields
  stay on this page.
- Status/navigation bar icon colors follow the selected app theme.

Android validation includes a build, 10 focused unit tests and emulator UI
checks for both panels, role menu, image selection/save, settings summaries,
background progress, dark mode and Agent Mode ordering. Screenshots are retained
under `D:/vb/andex-092-check`. iOS shared/native source changes require a macOS
build for runtime validation.
