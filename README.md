# TKupon
![Version](https://img.shields.io/badge/Version-1.0.0-007ec6)
![Paper API](https://img.shields.io/badge/Paper_API-1.21+-97ca00)
![Folia](https://img.shields.io/badge/Folia-Supported-97ca00)
![Java](https://img.shields.io/badge/Java-21-e87a00)

> An advanced, highly optimized coupon and playtime reward system designed for high-performance Minecraft servers.

---

## 🇬🇧 English

### About the Project
**TKupon** is a custom-built voucher and reward management plugin developed exclusively for modern Minecraft architectures. It is designed to handle high player volumes without compromising server performance (TPS). The core mechanic allows players to claim specific coupon codes to receive immediate items, while simultaneously starting a playtime-based objective to earn delayed rewards (such as VIP ranks) after remaining active on the server for a specified duration.

### Key Features
* **Folia Compatibility & Asynchronous Execution:** Built with `FoliaLib`, all database queries, playtime calculations, and data serialization processes are executed asynchronously across multiple threads. This guarantees zero lag and zero TPS drops on the main server thread.
* **Dual Database Architecture:** Natively supports both **SQLite** (for local, out-of-the-box usage) and **MySQL/MariaDB** (for large-scale network synchronization) with dynamic query formatting.
* **Smart Playtime Tracking:** Implements a passive timestamp system instead of heavy active runnables. It features a "Restart-Safe" mechanism that immediately saves all active player sessions to the database if the server stops or crashes, preventing any data loss.
* **Anti-Abuse & Inventory Protection:** Features strict UUID and IP address logging to prevent players from claiming the same code twice. It also includes monthly usage limits and an inventory-full protection system to ensure valuable items are not dropped on the ground.
* **In-Game GUI Management:** Administrators can create and edit the contents of a coupon directly in-game using a 54-slot chest interface. Items, including custom names and enchantments, are automatically serialized into Base64 formats.

### Commands & Permissions
* `/kupon kullan <code>` — Claims a specific coupon.
* `/kupon süre` — Checks the remaining playtime required for the delayed reward.
* `/kupon düzenle <code>` — Opens the GUI to edit a coupon's items. *(Requires: `tkupon.admin`)*
* `/kupon reload` — Reloads configurations and messages without restarting the server. *(Requires: `tkupon.admin`)*

---

## 🇹🇷 Türkçe

### Proje Hakkında
**TKupon**, modern Minecraft mimarileri için özel olarak geliştirilmiş, yüksek performanslı bir kupon ve ödül yönetim eklentisidir. Sunucu performansından (TPS) ödün vermeden yüksek oyuncu trafiğini idare edebilecek şekilde tasarlanmıştır. Temel işleyişinde, oyuncuların özel kupon kodlarını kullanarak anında başlangıç eşyaları almasını sağlar. Aynı zamanda arka planda bir aktiflik süresi görevi başlatarak, oyuncunun sunucuda belirli bir süre aktif kalması sonucunda gecikmeli ödüller (VIP üyelik vb.) kazanmasını organize eder.

### Temel Özellikler
* **Folia Uyumluluğu ve Asenkron İşlemler:** `FoliaLib` altyapısı ile inşa edilmiştir. Tüm veritabanı sorguları, süre hesaplamaları ve veri şifreleme işlemleri çoklu çekirdeklere dağıtılarak asenkron (arka planda) yürütülür. Ana sunucu döngüsünde (Main Thread) sıfır gecikme (lag) garantisi sunar.
* **Çift Veritabanı Mimarisi:** İhtiyaca göre dinamik sorgu yapılandırması ile hem yerel kullanım için **SQLite** hem de geniş ağ senkronizasyonları için **MySQL/MariaDB** altyapılarını tam destekler.
* **Akıllı Süre Takibi:** Sunucuyu yoran sürekli döngüler (Task/Runnable) yerine pasif zaman damgaları kullanır. Ani çökmelere veya planlı bakımlara karşı içerideki oyuncuların aktif sürelerini anında veritabanına kaydeden "Yeniden Başlatma Koruması"na (Restart-Safe) sahiptir.
* **İstismar Koruması (Anti-Abuse):** Aynı kuponun birden fazla kullanılmasını engellemek için sıkı UUID ve IP adresi denetimleri yapar. Aylık kullanım limitleri ve oyuncunun ödüllerinin yere düşüp kaybolmasını engelleyen "Dolu Envanter" koruması içerir.
* **Oyun İçi Menü (GUI) Yönetimi:** Yöneticiler, kupon içeriklerini 54 slotluk oyun içi bir sandık menüsü üzerinden düzenleyebilirler. Büyüler ve özel isimler dahil tüm eşya verileri otomatik olarak Base64 formatında şifrelenerek kaydedilir.

### Komutlar ve Yetkiler
* `/kupon kullan <kod>` — Belirtilen kupon kodunu kullanır.
* `/kupon süre` — Gecikmeli ödülün (VIP) teslim edilmesi için gereken kalan aktif süreyi gösterir.
* `/kupon düzenle <kod>` — Bir kuponun içerdiği eşyaları düzenlemek için arayüzü açar. *(Gereksinim: `tkupon.admin`)*
* `/kupon reload` — Sunucuyu yeniden başlatmadan mesaj ve ayar dosyalarını yeniler. *(Gereksinim: `tkupon.admin`)*
