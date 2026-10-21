# Changes

<!--
Как выпустить версию:
1. Поднять appVersion в gradle.properties.
2. Добавить сюда раздел «## X.Y.Z — ГГГГ-ММ-ДД» со списком изменений (он станет текстом релиза).
3. Закоммитить и запушить тег:  git tag vX.Y.Z && git push origin vX.Y.Z
Сборка на GitHub проверит, что версия в теге, gradle.properties и здесь совпадает.
-->

## 2.2.0 — not released yet

- Photo thumbnails in the editor and in the entry view are easy to browse when a day has many of them:
  the mouse wheel scrolls the strip sideways, and arrow buttons at its edges page through it.
- Tags are shown in their colors, as in Diarium: entry tags as colored labels, the Tags section with colored
  filters.
- New tags can be created in the Tags section ("New tag"), and right-clicking a tag changes its name or color.
- Tags can be attached to an entry in the editor: the tag button in the editor header lists all tags and has
  "New tag…". The entry's tags are shown under the photos; clicking one removes it.
- Entries have an editable heading: a title line above the text in the editor.
- Diary templates (created in Diarium on the phone) can be inserted from the editor's "⋮" menu → "Template…".
  As on the phone, in a new untouched entry the template replaces the text and sets the heading; otherwise it is
  appended at the end. The template's tags are added to the entry.
- Settings → Entries: a template that new entries start with.
- The theme can follow the system: Settings → Appearance → "System" switches between light and dark together
  with the desktop (via the XDG desktop portal, e.g. KDE Plasma, GNOME). The theme button moved from the side panel
  to Settings → Appearance.
- The "Quit" button moved from Settings to the bottom of the side panel.
- Settings → Lock: "Lock immediately when the window is closed" — a window closed to the tray or the taskbar
  opens already locked, without waiting for the idle timeout. On by default.

## 2.1.0 — 2026-10-19

- The calendar (or the list of entries in other sections) can be hidden like a navigation drawer, so the
  entry takes the whole window: the button at the left of the top bar or `Ctrl+\`. The choice is remembered.

## 2.0.0 — 2026-10-19

- New name: **Linary** (was "Diarium for Linux"). The command, packages and menu entry are now `linary`.
  Settings and backups move from `diarium-linux` to `linary` automatically on first start.
- The sync key is no longer included in the app. Linary asks for it on first start; it can be changed
  or removed in Settings → Sync.
- The calendar opens on today instead of the day of the latest entry.
- "Today" and the calendar follow the date change at midnight: before, an app left running (for example in
  the tray) kept treating the previous day as today.
- The window has a minimum size (720 × 480). In KDE, dragging the window off a screen edge after
  `Super+Arrow` used to shrink it to a tiny, practically invisible window.

## 1.0.5 — 2026-10-17

- Clearer tray icon: a simplified notebook with a star on a teal square instead of the full app icon,
  which was hard to make out at tray size.

## 1.0.4 — 2026-10-17

- The tray icon now uses the native StatusNotifierItem protocol (D-Bus), like other KDE apps: the panel draws
  the app icon itself, sharp at any scale and with transparency, and shows its own "Open / Quit" menu.
  Desktops without it (and without the AppIndicator extension on GNOME) fall back to the old icon.
- `Ctrl+L` locks the app again regardless of what has focus (it stopped working in 1.0.2), and shortcuts
  such as `Ctrl+N` work right after closing the settings.

## 1.0.3 — 2026-10-17

- New tray icon drawn for small sizes: it is no longer cropped and has no black background.
- The window opens with a single left click on the tray icon.

## 1.0.2 — 2026-10-17

- Tray icon: the app can show its icon in the system tray (on by default, can be turned off in settings).
  Clicking the icon opens the window; its menu has "Open" and "Quit".
- Closing the window (the close button, `Alt+F4`) no longer quits the app: the window hides to the tray, or is
  minimized when the tray icon is off. This can be turned off in settings. Quit with `Ctrl+Q`, the tray menu or
  the "Quit" button in settings.
- Only one copy of the app runs at a time: starting it again brings the open window to the front, restoring it
  if it was minimized or hidden to the tray.
- `Ctrl+F4` no longer minimizes the window (replaced by the behavior above).
- Settings open as a full page instead of a small dialog, with sections on the left: Lock, Lock screen,
  Appearance (light or dark theme), Window and About. `Esc` or the back arrow closes them.
- Viewing an entry: the date header stays in place, only the entry content scrolls.
- Editor: after "Insert time" (`Ctrl+T`) at the end of the entry, the text scrolls down to the inserted time.
- The text in the editor and the viewer now uses the full width of the window instead of stopping at a fixed width.

## 1.0.1 — 2026-10-17

- Settings: a gear button at the bottom of the side panel opens the settings window.
- Lock screen: optional lock after 1–60 minutes of inactivity and at start, unlocked with a 4–8 digit PIN;
  `Ctrl+L` locks at once. Five wrong PINs pause further attempts for 30 seconds. The PIN is stored only
  as a salted PBKDF2 hash.
- Lock screen style: solid, or a blurred view of the app with an adjustable blur level.
- The attachments strip of an entry (in the viewer and the editor) can be collapsed; the choice is remembered.
  By default the strip collapses and the full-screen photo closes when the app locks (can be turned off in settings).
- `Ctrl+F4` minimizes the window instead of closing the app.
- The settings window shows the installed version and a link to the project page.

## 1.0.0 — 2026-10-17

First public release.

- Calendar with photo thumbnails, feed, search, tags, "On this day" and an attachments gallery.
- Reading and editing entries right in the Dropbox sync folder (Diarium sync format v4), so changes reach the phone.
- Rich text editor: bold, italic, underline, strikethrough, lists, colors, links; rating, "Insert time", undo/redo.
- Several entries per day, new entries, deleting with confirmation.
- Photos: add from files, paste a screenshot, drag and drop; reorder, make a day cover, download.
- Changes from the phone are picked up automatically; conflicts are detected before saving.
- Backups before every change, atomic writes with verification.
- English and Russian UI, light and dark theme, keyboard shortcuts in Latin and Cyrillic layouts.
