# Order Management System

JavaFX tabanlı, **çok iş parçacıklı (multithreaded)** bir sipariş yönetim masaüstü uygulaması. Müşteriler eşzamanlı olarak sipariş oluştururken, ürün kataloğu **okuma/yazma kilitleri (ReentrantReadWriteLock)** ile tutarlı tutulur; siparişler **öncelik** sırasına göre işlenir.

> Kocaeli Üniversitesi YazLab projesi kapsamında geliştirilmiştir.

## 🧰 Teknolojiler
- **Java 23**
- **JavaFX 23** (Controls + FXML) — masaüstü arayüz
- **MySQL** (`mysql-connector-java 8.0.33`) — kalıcı veri
- **java.util.concurrent** — `ReentrantReadWriteLock`, eşzamanlılık yönetimi
- **JUnit 5** — testler
- **Maven** — bağımlılık ve derleme yönetimi

## ✨ Özellikler
- 👥 **Müşteri & Admin panelleri** — ayrı yetki ve görünümler.
- 🧵 **Eşzamanlı sipariş işleme** — birden çok müşteri aynı anda işlem yapabilir.
- 🔒 **Okuma/Yazma kilidi** — katalog okuması paralel; stok güncellemesi (yazma) sıralı ve güvenli (`CatalogRWLock`, `LocksRegistry`).
- 🥇 **Öncelikli kuyruk** — siparişler önceliğe göre sıralanıp işlenir (`PriorityPanel`, `OrderRequest`).
- 📦 **Stok yönetimi** — gerçek zamanlı stok takibi (`StockPanel`).
- 📝 **İşlem günlüğü** — tüm işlemler `LogPanel` üzerinden izlenir (`LogRepository`).
- ⏳ **Meşgul göstergesi** — uzun işlemlerde UI bloklanmadan `BusyOverlay`.

## 🏛️ Mimari
Katmanlı (layered) mimari:

```
src/main/java/org/example/
├── Main.java                # Giriş noktası
├── Model/                   # Veri modelleri (Customer, Product, Order, Admin)
├── Repository/              # Veri erişim katmanı (DB bağlantısı + CRUD)
├── Service/                 # İş mantığı + eşzamanlılık (RW lock, öncelik)
├── ui/                      # JavaFX arayüz panelleri
└── util/                    # Yardımcılar (DatabaseSeeder)
```

## 🚀 Kurulum & Çalıştırma
**Gereksinimler:** JDK 23, Maven, çalışan bir MySQL sunucusu.

1. MySQL'de veritabanını oluşturun ve `Repository/DatabaseConnection.java` içindeki bağlantı bilgilerini kendi ortamınıza göre güncelleyin.
2. Bağımlılıkları indirip uygulamayı çalıştırın:
   ```bash
   mvn clean javafx:run
   ```
   veya
   ```bash
   mvn clean compile exec:java -Dexec.mainClass=org.example.Main
   ```
3. İlk çalıştırmada örnek veriler `DatabaseSeeder` ile yüklenebilir.

## 📂 Proje Yapısı
| Katman | Sorumluluk |
|---|---|
| `Model` | Alan nesneleri (entity) |
| `Repository` | Veritabanı erişimi |
| `Service` | İş kuralları, kilit yönetimi, öncelik |
| `ui` | JavaFX bileşenleri ve paneller |

## 👤 Geliştirici
**Kadirhan Şahin** — [github.com/kdrhanshn-Dev](https://github.com/kdrhanshn-Dev)
