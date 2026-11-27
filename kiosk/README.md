# Square Roots kiosk

Android cabinet app that sells from a lane grid, takes Paystack, dispenses, and installs a remote APK. It follows the 3.1.80 machine flow (catalog, cart, pay, vend, MQTT or polled app update). It is a new project. The decompiled tree is not a buildable source drop.

The Paystack secret never goes on the device. The cabinet asks this server to open a checkout, shows the QR, and vends only after the server verifies the charge.

## Run the server

The cabinet talks to the Phoenix API in `../vader`, not a server inside this folder.

```bash
cd vader
mix phx.server
```

The emulator reaches the host at `http://10.0.2.2:8080`. A board on the LAN should use the computer's LAN address. Set that address in machine setup.

## Scan the QR from a phone

Point the phone camera at the QR on the emulator or cabinet screen. The phone has to open the address inside that code. The default `PUBLIC_BASE_URL` is `http://127.0.0.1:8080`, which only this computer can reach.

Put the phone and the computer on the same Wi-Fi. Get this machine's LAN address:

```bash
ipconfig getifaddr en0
```

In `vader/.env`, set `PUBLIC_BASE_URL` to that address and restart the server:

```bash
PUBLIC_BASE_URL=http://192.168.1.42:8080
```

```bash
cd vader
mix phx.server
```

The server listens on every interface. If the phone cannot open the page, allow incoming connections for Erlang in the macOS firewall.

Start a sale and choose **Scan QR code**.

- With no `PAYSTACK_SECRET_KEY`, the code opens `http://<lan-ip>:8080/pay/demo?reference=...`. Confirm on the phone. The kiosk sees that and continues.
- With Paystack test keys, the code opens Paystack's checkout. The phone needs internet for that page. `PUBLIC_BASE_URL` is the address Paystack sends the phone back to after payment.

Before you scan, open `http://<lan-ip>:8080` in the phone's browser. If that page loads, the QR will too.

## Build the cabinet app

```bash
cd kiosk
./gradlew :app:assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk`. On first launch, set the machine number and server. Hold the green header to open operator settings. The first PIN is `258046`.

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
