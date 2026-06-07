## 📖 Tentang Aplikasi

**Quick Invoice** adalah aplikasi Android yang membantu pengguna — terutama pelaku UMKM dan freelancer — untuk membuat invoice secara cepat, rapi, dan profesional menggunakan bantuan kecerdasan buatan **Google Gemini**. Dibuat oleh **[Bicara Digital](https://bagoesbicaradigital.net)** untuk mempermudah pengelolaan tagihan tanpa kerumitan teknis.

---

## ✨ Fitur Utama

- 🤖 **AI-Powered** — Didukung Google Gemini AI untuk pengisian invoice yang lebih cerdas
- 📄 **Generate Invoice Cepat** — Buat invoice dalam hitungan detik
- 📱 **Native Android** — Dibangun dengan Android Studio untuk performa optimal
- 🎨 **Tampilan Profesional** — Hasil invoice bersih dan siap kirim ke klien

---

## 🚀 Cara Menjalankan Secara Lokal

### Prasyarat

- [Android Studio](https://developer.android.com/studio) sudah terinstal
- API Key dari [Google AI Studio](https://aistudio.google.com/app/apikey)

### Langkah-langkah

1. **Buka Android Studio**, lalu pilih **Open** dan arahkan ke direktori proyek ini.

2. **Biarkan Android Studio menyelesaikan import** — jika ada ketidakcocokan dependensi, izinkan Android Studio memperbaikinya secara otomatis.

3. **Buat file `.env`** di direktori root proyek, lalu tambahkan baris berikut:

   ```env
   GEMINI_API_KEY=your_api_key_here
   ```

   > Lihat file `.env.example` untuk contoh formatnya.

4. **Edit file `build.gradle.kts`** pada modul `app`, lalu hapus baris berikut:

   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```

5. **Jalankan aplikasi** pada emulator atau perangkat fisik Android.

---

## 🔑 Mendapatkan Gemini API Key

1. Kunjungi [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Login dengan akun Google kamu
3. Klik **Create API Key**
4. Salin key tersebut ke file `.env` seperti langkah di atas

---

## 🗂️ Struktur Proyek

```
quick-invoice/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/        # Source code Kotlin/Java
│   │       └── res/         # Resource (layout, drawable, dll)
│   └── build.gradle.kts     # Konfigurasi build modul app
├── .env.example             # Contoh konfigurasi environment
├── .env                     # ⚠️ Buat sendiri, jangan di-commit!
└── build.gradle.kts         # Konfigurasi build level project
```

---

## ⚠️ Catatan Penting

> **Jangan pernah meng-commit file `.env` ke repository!**
> Pastikan file `.env` sudah masuk ke `.gitignore` agar API Key kamu tetap aman.

---

## 👨‍💻 Dibuat Oleh

<div align="center">

**Bicara Digital**

Solusi digital untuk bisnis yang lebih maju.

🌐 [bagoesbicaradigital.net](https://bagoesbicaradigital.net) · 📧 bagoes.bicaradigital@gmail.com

</div>

---

## 📄 Lisensi

Proyek ini dibuat untuk keperluan internal dan portofolio **Bicara Digital**. Seluruh hak cipta dilindungi.
