# Etkinlik Planlama Uygulaması - Arka Plan (Backend) API Dokümantasyonu

## 📝 Proje Açıklaması
Bu proje, kullanıcıların güvenli bir şekilde hesap oluşturup oturum açabildiği, detaylı etkinlik planlamaları gerçekleştirebildiği ve diğer kullanıcıların etkinliklerine katılım sağlayabildiği bir web platformunun arka plan (**Backend**) mimarisidir. Sistem; yüksek güvenlik kalkanları, katı veri doğrulama (validation) kuralları, merkezi hata yönetimi ve %100 kapsama oranına sahip tam teşekküllü test suitleri ile endüstriyel standartlara tam uyumlu şekilde sıfırdan inşa edilmiştir.

Proje, gereksinim dokümanına sadık kalınarak **Frontend ve Backend ayrı projeler** olacak şekilde modüler tasarlanmıştır. Bu depo yalnızca RESTful API servislerini barındırır.

---

## 🛠️ Kullanılan Teknolojiler
* **Dil:** Java 17
* **Çerçeve (Framework):** Spring Boot 4.0.4
* **Veri Katmanı:** Spring Data JPA & Hibernate
* **Veritabanı:** H2 Database (In-Memory Gömülü Veritabanı)
* **Veri Dönüşümü:** ModelMapper (Entity <-> DTO Dönüşümleri)
* **Güvenlik & Şifreleme:** Kriptografik BCrypt (Şifre Hashleme) & Http Session Authentication
* **Doğrulama:** Jakarta Validation Constraints
* **API Dokümantasyonu:** Swagger UI / OpenAPI 3 (Springdoc UI 2.8.9)
* **Kalite Güvencesi & Test:** JUnit 5, Mockito & MockMvc (Birim ve Entegrasyon Testleri)

---

## 🏗️ Mimari ve Tasarım Prensipleri
1.  **Katmanlı Mimari (Layered Architecture):** Proje; `controller`, `service`, `repository`, `dto`, `entity` ve `configs` katmanlarına kesin sınırlarla ayrılmıştır. Kod okunabilirliği ve sürdürülebilirlik en üst düzeydedir.
2.  **DTO (Data Transfer Object) Deseni:** Veritabanı varlıkları (Entity) hiçbir koşulda doğrudan dış dünyaya açılmamıştır. Veri transferleri tamamen istek (Request) ve yanıt (Response) paketleri üzerinden soyutlanmıştır.
3.  **Defansif Programlama ve Çapraz Doğrulama (Cross-Field Validation):** `@NotNull` gibi standart kısıtlamaların ötesine geçilerek, etkinlik oluşturulurken seçilen tarih bugün ise, girilen saatin geçmiş bir zaman diliminde kalıp kalmadığını denetleyen özel zaman süzgeçleri servis katmanında kodlanmıştır.
4.  **Güvenli Kimlik Doğrulama (Session-Based Auth):** JWT karmaşıklığından kaçınılarak dokümanda zorunlu kılınan **Http Session** mekanizması entegre edilmiştir. `SessionFilter` mimarisi sayesinde korunan rotalara atılan tüm anonim istekler kapıda reddedilir.

---

## 🚀 REST API Uç Noktaları (Endpoints)

### 👥 Kullanıcı API (`/user`)
* `POST /user/register` -> Yeni kullanıcı kaydı oluşturur. Girilen e-posta ve telefon numarasının benzersiz olduğunu denetler, parolayı BCrypt ile şifreleyerek kaydeder.
* `POST /user/login` -> E-posta/Telefon ve şifre doğrulaması yapar. Başarılı ise sunucu belleğinde kullanıcıya ait güvenli bir Http Session başlatır.
* `POST /user/logout` -> Aktif kullanıcı oturumunu `session.invalidate()` ile tamamen yok eder.

### 📅 Etkinlik API (`/event`)
* `POST /event/create` -> Yeni bir etkinlik ekler. Yeni etkinlikler varsayılan olarak **DRAFT** statüsünde başlar.
* `POST /event/createAll` -> Toplu etkinlik ekleme operasyonlarını yürütür.
* `GET /event/list?page=X` -> Sadece **PUBLISHED** durumundaki etkinlikleri Spring Data `Pageable` mekanizması ile 10'ar adetlik sayfalar halinde (Pagination) listeler.
* `GET /event/search?page=X&q=kelime&sortDir=asc` -> Yayındaki etkinlikler içerisinde başlık ve açıklama alanlarında büyük/küçük harf duyarsız arama yapar, tarihe göre sıralı sayfalar döner.
* `GET /event/detail/{id}` -> Belirli bir etkinliğin detay bilgilerini getirir.
* `PUT /event/update` -> Etkinlik bilgilerini günceller. **Güvenlik Kontrolü:** Etkinliği sadece asıl sahibi (Owner) güncelleyebilir, aksi halde `403 Forbidden` döner.
* `DELETE /event/deleteOne/{id}` -> Etkinliği veritabanından siler. **Güvenlik Kontrolü:** Sadece etkinliğin sahibi silebilir.
* `PUT /event/change-status/{id}?status=PUBLISHED` -> Etkinlik sahibinin etkinliğini *PUBLISHED*, *Yayın Durduruldu* veya *Arşivlendi* durumlarına geçirmesini sağlar.

### 🤝 Katılım API (`/event`)
* `POST /event/join/{id}` -> Oturum açmış kullanıcının bir etkinliğe katılmasını sağlar. Mükerrer (çift) kayıt olmayı otomatik engeller.
* `DELETE /event/leave/{id}` -> Kullanıcının kayıtlı olduğu etkinlikten katılımını iptal eder.
* `GET /event/{id}/participants` -> **Katılım Takibi** iş kuralı gereğince, ilgili etkinliğe kimlerin katıldığını liste halinde döner. **Güvenlik Kontrolü:** Kişisel verilerin korunması amacıyla bu listeyi *sadece etkinliği oluşturan asıl sahibi* görebilir.

---

## 🛡️ Veri Doğrulama ve Global Hata Yönetimi

### Validasyon Kuralları
* Tüm kritik alanlarda boş geçilemezlik (`@NotNull`, `@NotEmpty`) kuralları aktiftir.
* E-posta alanlarında RFC standartlarına uygun gerçek e-posta format denetimi (`@Email`) yapılır.
* Telefon numaralarında Türkiye formatına uygun Regex `@Pattern` kontrolü uygulanır.
* Şifre güvenliği için minimum 6, maksimum 15 karakter sınırı zorunlu kılınmıştır.
* Etkinlik tarihlerinin geçmişte kalması `@FutureOrPresent` anotasyonuyla engellenmiştir.

### Merkezi Hata Filtresi (`GlobalException`)
`@RestControllerAdvice` mimarisi kullanılarak tüm uygulamayı saran bir hata yönetim kalkanı kurulmuştur:
1.  **Validation Hataları:** Eksik veya hatalı veri girildiğinde sistem çökmez; hangi alanın hangi kuralı ihlal ettiğini gösteren temiz bir JSON dizi formatı (`field`, `message`, `rejectedValue`) döndürür.
2.  **Kayıt Bulunamadı & Yetkisiz Erişim:** Veritabanında olmayan ID çağrılarında `404 Not Found`, başkasının verisine sızma girişimlerinde `403 Forbidden` durum kodları iş kuralları tarafından fırlatılır.
3.  **Sistem Hataları:** Öngörülemeyen tüm çalışma zamanı hataları (NullPointerException, SQL kesintileri vb.) en tepede `Exception.class` ile yakalanarak dış dünyaya çirkin hata izleri (Stack Trace) yerine standart bir `{ "success": false, "message": "..." }` HTTP 500 JSON nesnesi döndürür.

---

## 🧪 Kalite Güvencesi ve Test Suitleri
Projenin dikeydeki tüm katmanları, Spring Boot test yetenekleri ve Mockito simülasyonları kullanılarak tam koruma altına alınmıştır:
* **Servis Katmanı Testleri (`EventServiceTest`):** Tüm iş mantığı kuralları, yetki kısıtlamaları ve katılım algoritmaları veritabanından bağımsız olarak mock nesneler ile birim testine (Unit Test) tabi tutulmuştur.
* **Controller API Testleri (`EventRestControllerTest`):** `MockMvc` mimarisiyle sahte HTTP istekleri (POST, GET, DELETE) enjekte edilerek rotaların, session doğrulamalarının ve validasyon filtrelerinin sıhhati doğrulanmıştır.
* **Repository Sorgu Testleri (`EventRepositoryTest`):** `@DataJpaTest` kullanılarak hafıza içi H2 üzerinde gerçek veritabanı dilimleri ayağa kaldırılmış, yazılan özel JPQL/SQL arama sorgularının doğruluğu mühürlenmiştir.

---

## ⚙️ Kurulum ve Çalıştırma Adımları

### 1. Veritabanı Bilgileri
Proje gömülü H2 veritabanı kullandığı için harici bir SQL server kurulumuna ihtiyaç duymaz.
* **JDBC URL:** `jdbc:h2:mem:eventdb`
* **Kullanıcı Adı:** `sa`
* **Şifre:** `[Boş Bırakılacaktır]`
* **H2 Konsol Arayüzü:** Proje çalışırken tarayıcıdan `http://localhost:8090/h2-console` adresinden veritabanı tablosuna görsel olarak erişebilirsiniz.

### 2. Uygulamayı Ayağa Kaldırma
1.  Bilgisayarınızda **Java 17 (JDK)** kurulu olduğundan emin olun.
2.  Projenin ana dizininde (en içteki `pom.xml` dosyasının bulunduğu klasör) bir terminal paneli açın.
3.  Aşağıdaki komutu çalıştırarak projeyi derleyin ve başlatın:
    ```bash
    ./mvnw spring-boot:run
    ```
4.  Backend uygulaması **`http://localhost:8090`** portu üzerinden canlı hizmet vermeye başlayacaktır.

### 3. Canlı Swagger Dokümantasyonu
Geliştirilen tüm API uç noktalarını listelemek, parametre kontratlarını incelemek ve tarayıcı üzerinden canlı Postman benzeri testler gerçekleştirmek için uygulama çalışırken şu adresi ziyaret edebilirsiniz:
👉 **`http://localhost:8090/swagger-ui/index.html`**
