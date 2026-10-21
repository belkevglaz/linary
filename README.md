<p align="center">
  <img src="packaging/icons/linary-128.png" width="96" alt="">
</p>

<h1 align="center">Linary</h1>

<p align="center">
  A journal app for Linux that works with a Diarium sync folder.<br>
  Reads and edits your diary right in the Dropbox sync folder, so changes reach your phone.
</p>

<p align="center">
  <a href="https://github.com/belkevglaz/linary/releases/latest"><img src="https://img.shields.io/github/v/release/belkevglaz/linary" alt="Release"></a>
  <a href="https://github.com/belkevglaz/linary/actions/workflows/ci.yml"><img src="https://github.com/belkevglaz/linary/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
</p>

<p align="center"><a href="README.ru.md">Русская версия</a> · <a href="CHANGES.md">Changes</a></p>

## Disclaimer

Linary is an independent project. It is not affiliated with, endorsed by or supported by the developer
of Diarium; "Diarium" is mentioned only to say which sync folder Linary works with.

Linary is provided **as is**, without warranty of any kind (see the [license](LICENSE)). You use it at your
own risk: the author is not responsible for lost or damaged diary entries or any other damage. Keep
backups of your diary (see [Safety](#safety)).

## Features

- **Calendar** with photo thumbnails, days with several entries, today marker.
- **Feed, Search, Tags, "On this day"** and an **Attachments** gallery. Tags are shown in their colors;
  new tags can be created and existing ones renamed or recolored.
- **Viewer**: formatted text, clickable links, photos, tags, rating, location; full-screen photo viewer.
- **Editor** close to the phone app:
  - rich text — bold, italic, underline, strikethrough, lists, text and highlight colors, links;
  - rating, tags (the tag button in the editor header, with "New tag…"), "Insert time" (`Ctrl+T`), undo/redo;
  - an entry heading above the text, and diary templates: "⋮" → "Template…" inserts one; Settings → Entries sets
    the template a new entry starts with;
  - several entries per day, new entries, deleting with confirmation.
- **Photos**: add from files, paste a screenshot with `Ctrl+V` or drag and drop. Images are resized
  and saved as JPEG the same way the phone does it. Reorder photos, make one the day cover (the
  photo shown in the calendar) and download them to a file. The attachments strip can be collapsed.
- **Sync with the phone** through the Dropbox folder. Changes coming from the phone are picked up
  automatically.
- Keyboard shortcuts work with both Latin and Cyrillic keyboard layouts.
- **Lock screen**: optionally locks the app after a chosen idle time and at start; unlocks with a PIN.
- **Tray icon**; closing the window keeps the app running in the tray. Only one copy runs at a time.
- English and Russian UI, light and dark theme (or following the system theme), remembered window size and position.

## Requirements

- Linux x86_64 (developed on Manjaro KDE; any desktop should work).
- Diarium on another device with **Dropbox sync** turned on, and the Dropbox desktop client syncing
  the `Apps/Diarium` folder to this computer.
- **End-to-end encryption (E2EE) must be off** in Diarium: E2EE sync is not supported yet.
- **The sync key** — see below.
- Optional: `kdialog` (KDE) or `zenity` (GNOME, XFCE, ...) for native file dialogs.

A `.diary` backup file can also be opened, read-only.

## Sync key

The files in a Diarium sync folder are encrypted. **The key is not included in Linary**, and this project
does not provide it or explain how to obtain it: the developer of Diarium asked for that, and Linary
respects it. Please do not ask for the key in Issues — such requests will be closed.

Without the key Linary cannot open the sync folder. If you already have the key, enter it on the
screen Linary shows on first start, or later in **Settings → Sync**. It is stored only in Linary's
settings on your computer (`~/.java/.userPrefs/linary`) and is never shown again.

Without a key you can still open a `.diary` backup exported from Diarium, if the backup is not
encrypted. Backups are read-only, so changes cannot be made or synced to the phone that way.

## Installation

Every [release](https://github.com/belkevglaz/linary/releases/latest) has ready-made packages for
x86_64. Each one includes its own Java runtime, so nothing else needs to be installed.
The commands below use version `2.0.0` — replace it with the version you download.

### Arch Linux / Manjaro

```bash
curl -LO https://github.com/belkevglaz/linary/releases/download/v2.0.0/linary-2.0.0-1-x86_64.pkg.tar.zst
sudo pacman -U linary-2.0.0-1-x86_64.pkg.tar.zst
```

The app goes to `/opt/linary`, with a `linary` command and an entry in the application menu.
To update, install a newer package the same way. To remove: `sudo pacman -R linary`.

### Debian / Ubuntu / Linux Mint

```bash
curl -LO https://github.com/belkevglaz/linary/releases/download/v2.0.0/linary_2.0.0_amd64.deb
sudo apt install ./linary_2.0.0_amd64.deb
```

To remove: `sudo apt remove linary`.

### Fedora / openSUSE

```bash
curl -LO https://github.com/belkevglaz/linary/releases/download/v2.0.0/linary-2.0.0-1.x86_64.rpm
sudo dnf install ./linary-2.0.0-1.x86_64.rpm        # openSUSE: sudo zypper install ./linary-2.0.0-1.x86_64.rpm
```

To remove: `sudo dnf remove linary`.

### AppImage (any distribution)

```bash
curl -LO https://github.com/belkevglaz/linary/releases/download/v2.0.0/linary-2.0.0-x86_64.AppImage
chmod +x linary-2.0.0-x86_64.AppImage
./linary-2.0.0-x86_64.AppImage
```

AppImage needs FUSE 2: `sudo pacman -S fuse2` on Arch/Manjaro, `sudo apt install libfuse2` on Ubuntu 22.04
(`libfuse2t64` on 24.04 and newer). It does not add itself to the application menu; tools such as
AppImageLauncher or Gear Lever can do that. To remove, delete the file.

### Archive (any distribution)

```bash
curl -LO https://github.com/belkevglaz/linary/releases/download/v2.0.0/linary-2.0.0-x86_64.tar.gz
mkdir -p ~/.local/opt
tar -xzf linary-2.0.0-x86_64.tar.gz -C ~/.local/opt
~/.local/opt/linary/bin/linary
```

No menu entry is created. To remove, delete `~/.local/opt/linary`.

### Checking the download

Each release also has `SHA256SUMS`. Put it next to the downloaded file and run:

```bash
sha256sum -c --ignore-missing SHA256SUMS
```

### Building from source

Building needs JDK 17+.

```bash
git clone https://github.com/belkevglaz/linary.git
cd linary

./gradlew run                                       # run without installing
./gradlew run --args="$HOME/Dropbox/Apps/Diarium"   # ... and open a specific folder or .diary file

./packaging/install.sh                              # install for the current user: ~/.local/opt, menu entry
./packaging/uninstall.sh                            # remove it (settings and backups are kept)

cd packaging/arch && makepkg -si                    # Arch / Manjaro: build and install a pacman package
```

Settings are kept in `~/.java/.userPrefs/linary` and backups of the diary in
`~/.local/share/linary/backups`; uninstalling the app does not remove them.

## Usage

On first start the app looks for `~/Dropbox/Apps/Diarium` (or the localized `Приложения/Diarium`).
Otherwise, click the folder button and pick the `v4_database` file inside your sync folder (or a
`.diary` backup). The chosen source is remembered.

| Where | Shortcut | Action |
|---|---|---|
| Main window | `Ctrl+N` | New entry |
| | `Ctrl+R` | Reload |
| | `Ctrl+F` | Search |
| | `Ctrl+\` | Hide / show the calendar or list |
| | `PgUp` / `PgDn` | Previous / next entry |
| Anywhere | `Ctrl+L` | Lock now (when the lock is turned on) |
| | `Ctrl+Q` | Quit |
| Editor | `Ctrl+S` | Save |
| | `Ctrl+T` | Insert time (`— 14:30 —`, or with the date for another day) |
| | `Ctrl+Z` / `Ctrl+Shift+Z`, `Ctrl+Y` | Undo / redo |
| | `Ctrl+B` / `Ctrl+I` / `Ctrl+U` | Bold / italic / underline |
| | `Ctrl+V` | Paste text, or attach an image from the clipboard |
| | `Esc` | Close (asks if there are unsaved changes) |
| Photo viewer | `←` / `→`, `Esc` | Previous / next, close |

Long-press or right-click a day in the calendar to choose one of its entries or start a new one.
Hover over any icon button for two seconds to see what it does.

By default the app shows an icon in the system tray, and closing the window (the close button or `Alt+F4`)
hides it to the tray instead of quitting; click the icon to bring the window back. Without the tray icon the
window is minimized instead. To quit, press `Ctrl+Q`, choose **Quit** in the tray menu or click the quit button at the bottom of the side panel. Both
behaviors can be changed in **Settings → Window**. The icon uses the StatusNotifierItem protocol (D-Bus), so
it looks native in KDE Plasma, XFCE, Cinnamon, LXQt and others; GNOME needs the "AppIndicator and
KStatusNotifierItem Support" extension.

Starting the app while it is already running brings the open window to the front instead of starting a second copy.

## How it works

Diarium's Dropbox sync (format **v4**) keeps everything in one folder:

| File | Contents |
|---|---|
| `v4_database` | the whole diary: an SQLite database, gzip-compressed and AES-256-CBC encrypted |
| `v4_Media/<GUID>` | each attachment, AES-encrypted |
| `v4_mediaindex` | gzip-compressed JSON list of the attachment GUIDs |

Without E2EE, these files are encrypted with a key that is not part of this project: Linary asks you
for it once and keeps it in its settings (**Settings → Sync**).

The phone compares the modification time of `v4_database` in Dropbox with the time of its last sync,
then downloads the database, uploads its own, or merges the two entry by entry. Linary only has
to write a correct `v4_database`. The Dropbox desktop client uploads it, and the phone picks it up on
its next sync.

## Safety

Writing someone else's sync format is risky, so every change goes through the same steps:

1. Read and decrypt the **current** `v4_database` from disk, not the copy shown on screen.
2. Check that the entry being changed is still the same as when you opened it. If it was changed on
   another device, you get a conflict message and can save your version over it or cancel.
3. Apply the change, encrypt, and check again that Dropbox did not replace the file in the meantime.
4. Keep a **backup** of the previous `v4_database` in `~/.local/share/linary/backups`
   (the last 20, still encrypted).
5. Replace the file **atomically** via a temporary `.~v4_database.tmp`, which Dropbox ignores.
6. Read the file back and verify the change. If verification fails, the backup is restored.

Photos are written in the phone's order: the files to `v4_Media`, then `v4_mediaindex`, then the
database. Removed photos are only unlinked in the database; the phone deletes unused files from
Dropbox during its own sync.

One gap remains: a window of a few milliseconds between steps 3 and 5. If Dropbox writes the phone's
new version exactly then, that version is overwritten and the phone's latest changes can be lost.
This is unlikely in practice, but avoid saving here at the very moment the phone is syncing.

**Privacy.** The app makes no network connections; Dropbox is accessed only through local files.
Decrypted data needed while the app is running (the database while it is read, attachments opened
in an external player) is kept in `$XDG_RUNTIME_DIR/linary` (in memory, readable only by you),
or in `/tmp/linary-<user>` if that is not available. It is removed on exit, and leftovers from
a crash are removed on the next start.

## Lock screen

In **Settings → Lock** (the gear button at the bottom of the side panel) you can turn on **Lock when idle**,
choose the idle time (1–60 minutes) and set a 4–8 digit PIN. With the lock on, the app starts locked and
locks again when you do nothing for that long; `Ctrl+L` locks it at once. Closing the window to the tray or the
taskbar locks it immediately too (this can be turned off). After five wrong PINs the
next try is possible only in 30 seconds.
In **Settings → Lock screen** the lock screen can be **solid** or **blurred**: the app stays visible behind it, blurred as
strongly as you choose. By default the attachments strip is collapsed and a full-screen photo is closed
when the app locks, so photos are not visible behind the blur; this can be turned off in Settings.

The lock hides the diary from other people at your computer. It is not encryption: the diary files on
disk stay as they are. The PIN itself is stored only as a salted PBKDF2 hash.

### Forgot the PIN

Quit the app (`Ctrl+Q` works on the lock screen too, or use the tray menu) and remove the lock settings from `~/.java/.userPrefs/linary/prefs.xml`:

```bash
sed -i '/lock\./d' ~/.java/.userPrefs/linary/prefs.xml
```

The app then starts unlocked, with the lock turned off. Your other settings and the diary are not affected.

## Limitations

- E2EE sync is not supported.
- Only tested with Dropbox. Other cloud providers that sync the same folder to disk may work but
  are untested.
- Editing covers the heading, text, rating, photos and tags; diary templates can be inserted. Values of tracking
  tags and location are shown but not editable, and templates themselves are created in Diarium on the phone.
- On Wayland the window manager may ignore the saved window position (the size is still restored).

## License

[MIT](LICENSE)
