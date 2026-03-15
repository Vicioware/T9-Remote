# T9 Remote
📺 A T9-style keyboard app for Android TV that lets you type using the number buttons on your TV remote control — just like old cell phones.

<img src="./banner.png" width="800">

## Features

- **T9 input** using number keys (0–9) on any TV remote
- **English and Spanish** with instant switching
- **Accented characters** (á, é, í, ó, ú, ñ) via long press in Spanish mode
- **Customizable key mapping** — assign any remote button to toggle case, delete, or change language
- **6 visual themes** for the character bar
- **[Advanced button mapping](#advanced-button-mapping)** via adb command — detect even special remote buttons (Netflix, Prime Video, Assistant, etc.)

## Screenshots

![](./screenshots/main_menu.png)
![](./screenshots/mapper_menu.png)
![](./screenshots/theme_menu.png)
![](./screenshots/character_bar.png)

## How to Use

### Typing

| Button | Characters |
|:---:|:---|
| **1** | . , ! ? - ' 1 (+ ¿ ¡ in Spanish) |
| **2** | a b c 2 |
| **3** | d e f 3 |
| **4** | g h i 4 |
| **5** | j k l 5 |
| **6** | m n o 6 (+ ñ in Spanish) |
| **7** | p q r s 7 |
| **8** | t u v 8 |
| **9** | w x y z 9 |
| **0** | space 0 |

Press a number key repeatedly to cycle through characters. The character auto-commits after 1.3 seconds, or press a different key to commit immediately.

### Accented Characters (Spanish)

In Spanish mode, **long press** a number key to add an accent to the current vowel:

- Long press **2** → á
- Long press **3** → é
- Long press **4** → í
- Long press **6** → ó
- Long press **8** → ú

### Default Special Keys

| Action | Default buttons |
|:---|:---|
| **Toggle case** (abc ↔ ABC) | Channel Up |
| **Delete** | Channel Down |
| **Change language** (EN ↔ ES) | Menu, Red button |

These can be customized in the **Key Mapping** screen.

## Key Mapping

Open **Configure button mapping** from the setup screen.

- Tap **"Add button"** under any action
- Press the remote button you want to assign
- To remove a mapping, select the button chip and press OK

### Reserved Keys (cannot be remapped)

Numbers (0–9), D-pad (Up/Down/Left/Right/Center), Enter, and Back are reserved for navigation and typing.

## Advanced Button Mapping

By default, the app can only detect standard remote buttons. To capture **all** buttons (Netflix, Prime Video, Assistant, etc.), you need to enable the **T9 Remote Mapper** accessibility service.

On Android 13+, sideloaded apps are blocked from enabling accessibility services unless you first allow restricted settings via ADB. Follow this guide:

### Step 1 — Enable Developer Options

1. Go to **Settings → System → About**
2. Press OK **7 times** on "Build number" or "Android OS build number"

### Step 2 — Enable Wireless Debugging

1. Go to **Settings → System → Developer options**
2. Enable **Wireless debugging**
3. Select **"Pair device with pairing code"** — note the code shown

### Step 3 — Pair from your Phone

1. On your phone, install **[Remote ADB Over WiFi](https://play.google.com/store/apps/details?id=app.remote.adb)** from the Play Store
2. Open the app — your TV should appear under "Available Wi-Fi Devices"
3. Tap **Pair** and enter the pairing code from your TV

### Step 4 — Run the ADB Command

Once you've done that, in the app on your phone, type in the text field this command and send it:

```
adb shell appops set com.example.t9remote ACCESS_RESTRICTED_SETTINGS allow
```

_This may take up to 30 seconds to take effect_

### Step 5 — Enable the Accessibility Service

1. Go to **Settings → Accessibility** on your TV
2. Find **T9 Remote Mapper** and enable its switch
3. Go back to **Key Mapping** — you can now capture any remote button

_You only need to do this once_
