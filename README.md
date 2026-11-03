# FINANCIIEA — Your hassle-free money diary 💰

A beautiful Material 3 Android money-diary app by **AVIOIVA**.
Dark navy + teal design, 7 languages, PIN lock, backups, PDF/CSV reports — everything works **offline, no login needed**.

**You do NOT need to know coding.** Follow the steps below and GitHub's robot will build the app for you.

---

## 📱 What's inside this folder

```
FINANCIIEA/
├── .github/workflows/android-build.yml   ← The ROBOT that builds your APK on GitHub
├── app/                                   ← The app itself (screens, languages, icons)
├── gradle/  +  gradlew  +  gradlew.bat    ← Build helper files (do not edit)
├── build.gradle.kts  /  settings.gradle.kts  /  gradle.properties  ← Build recipes (do not edit)
└── README.md                              ← This guide
```

---

## 🚀 Step-by-step: build your APK with GitHub (free, no coding)

### Step 1 — Create a GitHub account (skip if you have one)
Go to **https://github.com** → **Sign up** → verify your email.

### Step 2 — Create a new repository
1. Click the **+** in the top-right corner → **New repository**.
2. Repository name: `FINANCIIEA`
3. Choose **Private** (only you can see it) or Public — your choice.
4. Do **NOT** tick "Add a README file" (you already have one).
5. Click **Create repository**.

### Step 3 — Upload the files
1. On the new empty repository page, click the link **"uploading an existing file"**.
2. Open the unzipped `FINANCIIEA` folder on your computer.
3. Select **EVERYTHING inside it** — including the hidden `.github` folder — and **drag & drop** it into the GitHub upload area.
   - ⚠️ On Windows: `.github` looks like a normal folder — include it!
   - ⚠️ On Mac: press `Cmd + Shift + .` to show hidden folders.
   - The easiest way: press `Ctrl+A` (select all) inside the folder, then drag everything.
4. Wait until all files finish uploading (hundreds of files — be patient).
5. Click **Commit changes**.

### Step 4 — The robot builds your app automatically
1. Click the **Actions** tab at the top of your repository.
2. If asked, click **"I understand my workflows, go ahead and enable them"**.
3. You will see a workflow called **"Build FINANCIIEA APK"** running with a yellow dot 🟡.
4. Wait **5–10 minutes**. When it turns into a green ✅, the build succeeded.

### Step 5 — Download your app
1. Click the finished (green) run in the **Actions** tab.
2. Scroll down to the **Artifacts** section.
3. Download **FINANCIIEA-debug-apk** (it's a ZIP containing the APK).
4. Unzip it → you get `app-debug.apk`.

### Step 6 — Install FINANCIIEA on your phone
1. Send the `app-debug.apk` to your phone (WhatsApp to yourself, USB, Google Drive, etc.).
2. Tap the file → Android asks *"Install unknown apps?"* → **Allow** for that app.
3. Tap **Install** → open **FINANCIIEA** and enjoy! 🎉

> Every time you change something in the repository later (or just re-run the workflow), GitHub builds a fresh APK the same way.

---

## ✨ App features (all included and working)

| Feature | Where |
|---|---|
| Splash screen with your logo | Opens automatically |
| Home dashboard: big Current Balance (Cash + Bank), today's income & expenses, recent transactions | Home |
| Narrow title bar: ☰ menu + FINANCIIEA + italic tagline | Top of Home |
| Sidebar with 4 items: Add Transaction / History & Reports / Settings / About | ☰ icon |
| Add transaction: Expense/Payment/Credit toggle, "From Whom" for credit, amount + currency (BDT default), 8 category icons, date & time picker, notes, camera/gallery receipt, Save | Sidebar → Add Transaction |
| History: Day / Week / Month / Custom filters, tap any entry to edit or delete | History tab |
| Reports: category pie chart + 6-month bar chart, Export PDF and Export CSV/Excel | Reports tab |
| 7 languages (English, বাংলা, हिन्दी, العربية, 中文, Français, Русский) — applies instantly | Settings → Language |
| App Lock PIN — asks PIN every time the app opens | Settings → App Lock |
| Daily reminder with time picker | Settings → Daily Reminder |
| Backup & Restore: Connect Google Drive, Backup Now, Restore, Save to Phone | Settings → Backup & Restore |
| Theme colour: 10 presets + fully custom colour picker — applied instantly | Settings → Theme Colour |
| About page with AVIOIVA info, version, copyright and contact | Sidebar → About |

---

## 🔧 Useful things you can change later (no coding needed)

**Change the app version number:** open `app/build.gradle.kts`, find `versionName = "1.0.0"` and change it, then upload the changed file. GitHub rebuilds automatically.

**Change any text in the app:** all English text lives in
`app/src/main/res/values/strings.xml` (and each language in `values-bn`, `values-hi`, etc.).

---

## 🆘 Troubleshooting

| Problem | Fix |
|---|---|
| Actions tab shows a red ❌ | Click the failed run → click the build step → read the error. The most common cause: a missing file from the upload (especially the `.github` folder). Re-upload and click **Re-run all jobs**. |
| Phone says "app not installed" | You may be installing an old APK over a new one, or a corrupted download. Uninstall the old FINANCIIEA first, then install again. |
| "Upload blocked" on GitHub | Files must be smaller than 100 MB — all files here are tiny, so this shouldn't happen. |
| Google Drive button | When the picker opens, navigate to your **Google Drive** section and choose/create a folder there — backups will then be saved to Drive. |

---

## 📄 Ownership

© 2026 AVIOIVA. All rights reserved.
App: FINANCIIEA • Developed by AVIOIVA
Email: hex.leo.jara@gmail.com • Website: http://moviiea.blogspot.com

Made with purpose by AVIOIVA.
