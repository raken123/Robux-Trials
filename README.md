# Islamiska Förbundet – Android-app

Islamiska Förbundets officiella app: spåra dina böner, lyssna på Quran och donera.

| Verktyg | Vad det gör |
|---|---|
| **Salah** | Bönetider för din plats. Vid bönetid ringer telefonen och larmet stängs bara av när du skannar koden på din bönematta – bönen loggas då automatiskt. Larmet är **på för Fajr** från start, övriga böner slås på med 🔔. Statistik för senaste 7 dagarna och svit. |
| **Tasbih** | Räknare med vibration, mål 33/99/100/∞ och vanliga dhikr. |
| **Quran** | Alla 114 suror med arabisk text och svensk översättning (Knut Bernström), ljud av Alafasy eller Al-Husary. Lästa suror sparas för offline. |
| **Qibla** | Kompass mot Kaba med avstånd till Mecka. |
| **Donera** | Välj ändamål och belopp, betala med Swish. |
| **Islamiska videor** | Kategorier som öppnas i YouTube; fler kategorier med Premium. |
| **Premium** | Prenumeration via Google Play – inga reklamer, fler videor, 98 % går till zakat. |
| **Pengarkalkyl** | Kostnad för Hajj/Umrah och hur mycket du behöver spara per månad. |

## Bönematta
Under **Bön → Bönematta** väljer du antingen *Skapa QR-kod* (skriv ut den och fäst på mattan) eller *Registrera* för att skanna en QR-/streckkod som redan sitter på mattan. När larmet ringer öppnas kameran och endast den koden stänger av ljudet. Larmet slutar av sig självt efter 15 minuter, och det finns ett diskret nödstopp som inte loggar bönen.

## Bygga
APK och AAB byggs automatiskt av GitHub Actions (`.github/workflows/android.yml`) vid varje push. Filerna laddas upp som artefakt och som en GitHub-release.

Lokalt (kräver Android SDK):
```
./gradlew assembleRelease bundleRelease
```

### Signering för Google Play
Utan nyckel signeras release-bygget med debug-nyckeln (fungerar att installera, men inte att ladda upp till Play). Skapa en uppladdningsnyckel och lägg in den som secrets i repot:
```
keytool -genkeypair -v -keystore release.jks -alias if -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # -> IF_KEYSTORE_BASE64
```
Secrets: `IF_KEYSTORE_BASE64`, `IF_KEYSTORE_PASSWORD`, `IF_KEY_ALIAS`, `IF_KEY_PASSWORD`.

## Innan publicering
Fyll i `app/src/main/java/se/islamiskaforbundet/app/Config.kt`:
- `SWISH_NUMBER` – förbundets Swish-nummer (tomt = knappen öppnar webbplatsen)
- `BANKGIRO` – visas på donationssidan om ifyllt
- `YOUTUBE_CHANNEL` – förbundets kanal
- `PREMIUM_PRODUCT_ID` – skapa prenumerationen `premium_monthly` i Play Console

Paketnamn: `se.islamiskaforbundet.app`. Ikonfil för Play Store: `store/icon-512.png`.
