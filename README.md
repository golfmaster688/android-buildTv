# Mediabox — Android TV IPTV player

แอป Android TV สำหรับเล่น IPTV/HLS สร้างจาก scaffold ขั้นต่ำในรีโปนี้ แล้วพัฒนาเป็นแอปที่ใช้งานได้จริง

## ฟีเจอร์

- โหลด playlist จาก **asset ที่ bundle มา** (`app/src/main/assets/channels.m3u`) หรือจาก **URL ที่ผู้ใช้กรอก** (บันทึกใน SharedPreferences)
- Parser M3U/M3U8 รองรับ `tvg-name`, `tvg-logo`, `group-title`, CRLF, comment, ชื่อที่มี comma
- รายการช่องแบบ D-pad focus ได้ (Android TV remote)
- เล่นวิดีโอด้วย **Media3 / ExoPlayer** พร้อม `media3-exoplayer-hls` → รองรับ HLS (`.m3u8`)
- ล็อกอินเทอร์เน็ตผ่าน `HttpURLConnection` (ไม่เพิ่ม dependency HTTP)

## โครงสร้าง

```
app/src/main/java/com/mediabox/tv/
├── MainActivity.java         รายการช่อง + dialog ตั้งค่า playlist
├── PlayerActivity.java       Media3 ExoPlayer + PlayerView
├── ChannelAdapter.java       RecyclerView adapter (TV focus)
└── data/
    ├── Channel.java              model
    ├── M3UParser.java            parser (pure Java → unit-testable)
    └── PlaylistRepository.java   โหลด asset/HTTP บน background thread
app/src/test/java/.../M3UParserTest.java   13 unit tests
```

## CI

`.github/workflows/android-build.yml` — build บน `ubuntu-latest`:

| trigger | สิ่งที่ทำ |
|---|---|
| push `main` / manual | unit test → `assembleDebug` + `assembleRelease` → upload 2 artifacts |
| release published | ดาวน์โหลด artifact แล้วแนบ `mediabox-release.apk` เข้า release |

## Build ในเครื่อง

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

APK อยู่ที่ `app/build/outputs/apk/{debug,release}/`

## ข้อควรระวัง

- release build เซ็นด้วย **debug key** (`app/build.gradle.kts`) เพื่อให้ได้ `app-release.apk` ที่ติดตั้งได้โดยไม่ต้องมี keystore — **ห้ามใช้ขึ้น Play Store** ให้เปลี่ยนเป็น signing config ของจริงผ่าน GitHub Secrets
- `usesCleartextTraffic="true"` เปิดไว้เพราะ IPTV จำนวนมากเสิร์ฟผ่าน `http://` — ถ้าใช้แต่ `https://` ควรปิด
- ยังไม่มีการทดสอบบนอุปกรณ์/emulator ใน CI (มีแค่ unit test ของ parser)
- ทั้งหมดนี้ gen ด้วย deepseek harness ไม่รอบผมหากนำไปใช้

## วิธีติดตั้ง APK (จากหน้า Releases)

ดาวน์โหลด `mediabox-release.apk` จากแท็บ **Releases** (ตัวล่าสุด) แล้ว:

```bash
adb install mediabox-release.apk
# ถ้าขึ้น INSTALL_FAILED_UPDATE_INCOMPATIBLE (ลายเซ็นไม่ตรงกับตัวเก่า):
adb uninstall com.mediabox.tv && adb install mediabox-release.apk
```

> APK ที่ build บน GitHub runner ถูกเซ็นด้วย debug key ที่ **สร้างใหม่ทุกเครื่อง/ทุก runner**
> ลายเซ็นจึงไม่ตรงกับ APK ที่ build ในเครื่องคุณ → ต้อง `adb uninstall` ก่อนติดตั้งทับเสมอ

## วิธีใส่ playlist ของคุณเอง

ในแอปมี 2 ปุ่มมุมขวาบน:

| ปุ่ม | ทำอะไร |
|---|---|
| **+ เพิ่มจาก URL** | กรอก playlist URL (http/https) แล้วกด "บันทึกและโหลด" — ใน dialog มีปุ่มย่อย "เปิดไฟล์ .m3u ในเครื่อง" และ "ใช้ตัวอย่างในแอป" |
| **+ เปิดไฟล์ .m3u** | เปิด file picker ของ Android เลือกไฟล์ playlist ในเครื่อง (เก็บสิทธิ์ถาวร อ่านซ้ำได้หลังปิดแอป) |

ทั้งสองทางจำค่าไว้ใน SharedPreferences — เปิดแอปครั้งต่อไปโหลดแหล่งเดิมอัตโนมัติ

