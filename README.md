# WebView App (Android)

Ye ek **native Android app** hai jo sirf ek WebView me tumhari website kholta hai —
matlab website hi "app" ban jaati hai (WebView wrapper).

Isme already ye sab kaam kar raha hai:

- 📷 **Camera Access** — website ka `<input type="file" capture>` ya `getUserMedia()` camera khol sakta hai
- ⬇️ **File Download** — website se koi bhi file (PDF, image, etc.) download hoke seedha "Downloads" folder me jaati hai
- 📤 **Share** — website ka `navigator.share()` Android ke native share sheet (WhatsApp, Gmail, etc.) se judha hua hai

## Already configure kiya hua hai

- **App name:** `navi`
- **Website URL:** `https://sanaxd.blogspot.com/`
- **App icon:** tumhari di hui icon-512.png se sab sizes (48/72/96/144/192 px) generate karke `mipmap-*` folders me daal di gayi hai
- **Splash Screen:** app open hote hi tumhare diye design jaisa splash screen dikhega — white background, center me green logo, PCI DSS / ISO 27001 badges, aur "100% SECURE MOBILE PAYMENTS" text — ~1.2 second baad website (WebView) khul jayegi

Agar URL ya naam badalna ho to yahan se: `app/src/main/res/values/strings.xml`
```xml
<string name="app_name">navi</string>
<string name="website_url">https://sanaxd.blogspot.com/</string>
```

## Package name badalna (optional, professional look ke liye)

`app/build.gradle` me `namespace` aur `applicationId`, aur `AndroidManifest.xml` me
`${applicationId}.fileprovider` — sab me `com.example.webviewapp` ko apne naam se replace kar sakte ho
(jaise `com.yourcompany.yourapp`). Android Studio me right-click karke "Rename Package" se ye easily ho jata hai.

## Project ko build karna (APK banane ke liye)

1. [Android Studio](https://developer.android.com/studio) install karo
2. **Open** karo is poore `webview-app` folder ko (File → Open)
3. Gradle sync hone do (pehli baar thoda time lagega, internet chahiye)
4. Upar menu se **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. APK yahan milegi: `app/build/outputs/apk/debug/app-debug.apk`
6. Phone me install karke test karo

Play Store par publish karne ke liye "release" build banani hogi aur sign karni hogi —
Android Studio me **Build → Generate Signed Bundle / APK** se ho jata hai.

## GitHub par push karna

```bash
cd webview-app
git init
git add .
git commit -m "Android WebView app with camera, download, share"
git branch -M main
git remote add origin https://github.com/<aapka-username>/<repo-name>.git
git push -u origin main
```

## GitHub par push karte hi APK khud ban jayegi (GitHub Actions)

Is project me `.github/workflows/build.yml` already daala hua hai. Iska matlab:

1. Upar wale push karte hi GitHub apne aap APK build kar dega — Android Studio ki zarurat nahi
2. APK download karne ke 2 tarike:
   - **Actions tab se:** GitHub repo → **Actions** tab → sabse upar wala run kholo → neeche **Artifacts** section me `app-debug-apk` milega, download kar lo (zip me APK hoga)
   - **Releases se:** repo ke right side **Releases** section me `build-1`, `build-2`... naam se release milega, wahan seedha `app-debug.apk` file milegi, download karke phone me install kar lo
3. Manually bhi chala sakte ho: **Actions** tab → left side **Build APK** workflow → **Run workflow** button

⚠️ Pehli baar push karne ke baad Actions tab me build 3-5 minute lega — wahan live progress dikhega.

⚠️ Ye APK "debug" build hai (testing/personal use ke liye theek hai). Play Store par daalne ke liye "release" build sign karni padegi — bata dena to woh workflow bhi bana dunga.


## Files ka structure

```
webview-app/
├── app/
│   ├── build.gradle                 # app-level dependencies
│   └── src/main/
│       ├── AndroidManifest.xml      # permissions (camera, internet, storage)
│       ├── java/.../MainActivity.kt   # WebView + camera + download + share logic
│       ├── java/.../SplashActivity.kt # splash screen (logo dikhake MainActivity kholta hai)
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/strings.xml     # <- yahan URL aur app name
│           ├── values/themes.xml      # normal theme + splash theme
│           ├── values/colors.xml      # splash background color
│           ├── drawable/splash_background.xml  # splash layout (color + centered logo)
│           ├── drawable/splash_logo.png
│           ├── xml/file_paths.xml     # camera photo ke liye FileProvider config
│           └── mipmap-*/              # app icon (sab densities, tumhari icon-512.png se)
├── build.gradle                     # project-level
├── settings.gradle
└── gradle.properties
```

## Permissions jo already set hain

- `CAMERA` — photo kheenchne ke liye
- `INTERNET` — website load karne ke liye
- `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` — purane Android (9 aur neeche) par download ke liye

App pehli baar khulte hi camera permission khud maang legi. User allow karega to sab kaam karega,
deny karega to sirf camera wala feature kaam nahi karega, baaki sab (download, share) chalega.

## Kuch aur cheez add karni ho?

- **Push Notifications** chahiye to Firebase Cloud Messaging (FCM) add karna hoga
- **Splash Screen** chahiye to `SplashScreen` API ya custom launch activity add ho sakti hai
- **Location Access** chahiye to `ACCESS_FINE_LOCATION` permission + `onGeolocationPermissionsShowPrompt` add karna hoga

Bata dena, isi project me add kar dunga.
