<div align="center">

# ⚡ CodeFlow

### *Egyszerű. Sötét. Gyors.*

Egy letisztult, natív **JavaFX** szövegszerkesztő — mély éjkék és elektromos lila tónusokban.

<br/>

![Java](https://img.shields.io/badge/Java-21-2b1a5e?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-6a3fc9?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-4b2fae?style=for-the-badge&logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-Educational-0d0b1e?style=for-the-badge)

<br/>

```
┌─────────────────────────────────────────┐
│  ⚡ CodeFlow                    ─ □ ✕   │
├─────────────────────────────────────────┤
│  File   Edit   Help                      │
├──────────┬──────────────────────────────┤
│ 📄 a.txt │                              │
│ 📄 b.txt │   > Írj valamit...           │
│ 📄 c.txt │                              │
│          │                              │
└──────────┴──────────────────────────────┘
```

</div>

<br/>

## 🌌 Miről szól?

**CodeFlow** egy minimalista, natív desktop szövegszerkesztő, amit sebességre és tiszta megjelenésre terveztünk. Nincs felesleges dísz — csak egy mappa nézet, egy szerkesztőmező, és egy design, amiben jólesik dolgozni.

<br/>

## ✨ Funkciók

<table>
<tr>
<td width="50%" valign="top">

### 📂 Fájlkezelés
- Fájl megnyitása egy kattintással
- Teljes mappa böngészése
- Mentés / Mentés másként
- Fájl törlése a listából

</td>
<td width="50%" valign="top">

### 🎨 Felület
- Egyedi mélykék–lila téma
- Letisztult, zavartalan szerkesztőmező
- Reszponzív elrendezés (`BorderPane`)
- Natív ablak, natív sebesség

</td>
</tr>
</table>

<br/>

## 🎨 Színpaletta

<div align="center">

| | Szín | Hex | Szerep |
|:---:|:---|:---|:---|
| 🟣 | **Void Blue** | `#0d0b1e` | Fő háttér |
| 🟪 | **Deep Indigo** | `#150f2b` | Panelek, szerkesztő |
| 🔮 | **Twilight Purple** | `#2a1f52` | Másodlagos felület |
| 💜 | **Electric Violet** | `#6a3fc9` | Kijelölés, kiemelés |
| 🤍 | **Moonlight Lilac** | `#e6dcff` | Szöveg |

</div>

<br/>

## 🚀 Gyors indítás

```bash
git clone <repo-url> CodeFlow
cd CodeFlow
mvn clean javafx:run
```

**Előfeltétel:** Java 21 (JDK) + Maven

<br/>

## 🧭 Projektstruktúra

```
CodeFlow/
│
├── 📦 src/main/java/com/example/myapp/
│   ├── HelloApplication.java      ⚡ belépési pont
│   ├── HelloController.java       🧠 UI logika
│   └── Launcher.java              🚀 indító
│
├── 🎨 src/main/resources/com/example/myapp/
│   ├── hello-view.fxml            🧩 felület felépítése
│   └── dark-theme.css             🌌 sötét téma
│
└── ⚙️ pom.xml                      📋 build konfiguráció
```

<br/>

## 🛠️ Tech stack

<div align="center">

`Java 21` · `JavaFX 21.0.6` · `FXML` · `Maven` · `BootstrapFX`

</div>

<br/>

## 🧩 Testreszabás

A design a `dark-theme.css`-ben él. Két szinten nyúlhatsz hozzá:

```css
/* Minden elemtípusra hat */
.text-area { -fx-background-color: #150f2b; }

/* Csak egy konkrét, fx:id-vel jelölt elemre */
#editText { -fx-text-fill: #e6dcff; }
```

<br/>

<div align="center">

---

*Készült ☕ és 💜 sok-sok CSS változóval*

</div>
