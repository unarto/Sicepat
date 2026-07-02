# HevSocks5Tunnel Bridge Configuration

Dokumentasi ini menjelaskan konfigurasi `PKGNAME` dan `CLSNAME` yang digunakan sebagai jembatan penghubung JNI (Java Native Interface) untuk modul `hev-socks5-tunnel` di dalam aplikasi ini.

## JNI Bridge Configuration

Ketika mengompilasi library `libhevsocks5tunnel.so`, compiler menggunakan parameter `APP_CFLAGS` untuk memetakan nama paket (package name) dan nama kelas (class name) agar sesuai dengan entry point di kode Kotlin/Java Android.

Berikut adalah nilai parameter yang didefinisikan:

| Parameter | Nilai C/C++ | Nilai Kotlin (Android) | Keterangan |
| :--- | :--- | :--- | :--- |
| **PKGNAME** | `com/example` | `com.example` | Lokasi package jembatan JNI (menggunakan separator `/` pada C) |
| **CLSNAME** | `hevsocks5tunnel` | `hevsocks5tunnel` | Nama kelas Kotlin yang mengekspos fungsi `external` |

### Pemetaan Kelas Kotlin Lengkap:
* **Package**: `package com.example`
* **Class**: `class hevsocks5tunnel`
* **Method Signatures**:
  * `@JvmStatic external fun TProxyStartService(configPath: String, fd: Int)`
  * `@JvmStatic external fun TProxyStopService()`
  * `@JvmStatic external fun TProxyGetStats(): LongArray`

---

## Implementasi Kompilasi (compile-hevtun.sh)

Konfigurasi di atas dilewatkan saat proses kompilasi NDK melalui argumen `APP_CFLAGS` di skrip `compile-hevtun.sh`:

```bash
"$NDK_HOME/ndk-build" \
    NDK_PROJECT_PATH=. \
    APP_BUILD_SCRIPT=jni/Android.mk \
    "APP_ABI=armeabi-v7a arm64-v8a x86 x86_64" \
    APP_PLATFORM=android-24 \
    NDK_LIBS_OUT="$TMPDIR/libs" \
    NDK_OUT="$TMPDIR/obj" \
    "APP_CFLAGS=-O3 -DPKGNAME=com/example -DCLSNAME=hevsocks5tunnel" \
    "APP_LDFLAGS=-Wl,--build-id=none -Wl,--hash-style=gnu"
```

## Legacy Compatibility (Backup)
Untuk mendukung integrasi lama, aplikasi juga mendeteksi kelas backward-compatibility berikut:
* **Package**: `com.example.service`
* **Class**: `HevSocks5Tunnel`
