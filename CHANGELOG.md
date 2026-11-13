# Changelog

All notable changes to **DebloatYourTV** are documented in this file.

## [1.0.0] - 2026-11-13

### Brand & Core Identity
- **Complete Rebranding**: Transitioned launcher to `com.debloatyourtv.launcher` with 0 legacy identifiers.
- **Strict Zero-Internet Launcher**: The launcher process holds zero network permissions (`android.permission.INTERNET` omitted from `AndroidManifest.xml`). All network features are decoupled into standalone add-on providers.
- **Micro-Footprint**: Optimized R8 minification producing an ultra-fast ~550 KB release APK with zero bloat.

### Navigation & UX
- **Centered 2-Tab Navigation**: Introduced clean frosted-glass top navigation tabs (`Home • Settings`) adhering to `/unslop` design guidelines.
- **Quiet Chrome**: Removed the top-right Control Center button to present an unencumbered, serene living room experience.
- **Apple TV-Style Single Dock**: Curved bottom dock featuring favorite streaming apps with fluid spring-physics glass highlight and ambient glow.
- **Smooth App Reordering ("Lift & Carry")**:
  - Elevated $1.08\times$ scale with subtle glowing card highlight during reordering.
  - Background neighbor tiles smoothly slide into position with 200ms `DefaultItemAnimator`.
  - Floating HUD indicator (`Moving [App] · ◄ ► slide · OK drop`) for intuitive TV remote feedback.

### Settings Hub & TV Controls
- **Dedicated 3×3 Settings Dashboard**:
  - **Personalize Home Name**: TV-friendly on-screen dialog to enter your household name, rendered in dynamic time-of-day greetings (*Good morning, Good afternoon, Good evening, Good night*).
  - **Live Cricket Pill**: Single toggle switch to enable/disable match scores and streaming countdowns under the clock.
  - **Video Inputs Switcher**: Direct switcher for HDMI 1, HDMI 2 (eARC), HDMI 3, and AV inputs.
  - **Network & Wi-Fi**: Live network status indicator and direct shortcut to TV Wi-Fi settings.
  - **Remotes & Accessories**: Direct Bluetooth pairing shortcut for soundbars, headphones, and game controllers.
  - **Reorder Apps**: One-click trigger into dock reordering mode.
  - **Launcher Appearance**: Customize columns, glass transparency, and atmospheric wallpapers.
  - **System Settings & Power**: Seamless access to Android TV native preferences and system power menu.

### Performance & TV Compatibility
- Direct app launching without PIN code prompts or watch timers.
- Verified on Xiaomi Mi TV 4K / Android TV 11+ with zero dropped frames.
