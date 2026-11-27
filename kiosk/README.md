# Square Roots kiosk

Android cabinet app that sells from a lane grid, takes Paystack, dispenses, and installs a remote APK. It follows the 3.1.80 machine flow (catalog, cart, pay, vend, MQTT or polled app update). It is a new project. The decompiled tree is not a buildable source drop.

The Paystack secret never goes on the device. The cabinet asks this server to open a checkout, shows the QR, and vends only after the server verifies the charge.

## Run the server

The cabinet talks to the Phoenix API in `../vader`, not a server inside this folder.

```bash
cd vader
mix phx.server
```

The emulator reaches the host at `http://10.0.2.2:8080`. A board on the LAN should use the computer's LAN address. Set that address in machine setup, and set `PUBLIC_BASE_URL` to the same address so the QR opens on a phone.

## Build the cabinet app

```bash
cd kiosk
./gradlew :app:assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk`. On first launch the machine number is already `19265359` and the install grid is 6 rows by 6 coils. Set the server, then hold the green header to open operator settings. The first PIN is `258046`.

Vend is simulated until you turn **Vend mode: serial port** and set a device such as `/dev/ttyS1`. The dispense frame is the Vendele command `03` (slot transfer) from the 3.1.80 driver: `57 58 00 66` + id + `03` + length + lane + checksum.

## Paystack

Amount is in minor units (kobo for NGN). The server calls `transaction/initialize`, the cabinet polls `transaction/verify`, and a failed lane is refunded with Paystack's refund API for the failed amount only.

## Remote updates

The cabinet polls `vader` at `/api/app/v2/appLastedVersion`. A release is installed only when `version_code` is higher, `url` and `sha256` are set, and `required_version` is empty or equal to the version on the machine. The desk Release page writes that record.

```bash
shasum -a 256 app-release.apk
```

Put the apk where `url` can download it. The same payload can be pushed over MQTT to `machines/<machine number>`:

```json
{"method":"app_upgrade","number":"42","version":"3.2.1","version_code":321,"url":"https://example.com/kiosk.apk","sha256":"<sha256>","required_version":"3.2.0"}
```

`refresh_goods` reloads the catalog. `reboot` runs only when this app is the device owner.

Silent install needs device owner, set before any account is added on the board:

```bash
adb shell dpm set-device-owner com.squarerootsfoods.kiosk/.admin.KioskDeviceAdmin
```

Otherwise Android shows the normal install confirmation. The package id is `com.squarerootsfoods.kiosk`, so it does not replace the signed 3.1.80 app or an earlier `en.openvem.kiosk` install. Uninstall those, then pick this one as the home app.
