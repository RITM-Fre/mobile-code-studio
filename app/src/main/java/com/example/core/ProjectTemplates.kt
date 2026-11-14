package com.example.core

enum class ProjectTemplateId(val id: String) {
    EMPTY("empty"),
    BASIC_HTML("basic_html"),
    WEB_BUNDLE("web_bundle"),
    RESPONSIVE("responsive"),
    CUSTOM("custom");

    companion object {
        fun fromId(id: String): ProjectTemplateId =
            entries.firstOrNull { it.id == id } ?: WEB_BUNDLE
    }
}

enum class CustomStarterFile(val relativePath: String, val label: String) {
    INDEX_HTML("index.html", "index.html"),
    STYLE_CSS("style.css", "style.css"),
    SCRIPT_JS("script.js", "script.js"),
    DATA_JSON("data.json", "data.json"),
    LOGO_SVG("assets/logo.svg", "assets/logo.svg"),
    README_MD("README.md", "README.md")
}

object ProjectTemplates {

    fun generateStarterFiles(
        templateId: ProjectTemplateId,
        projectName: String,
        description: String,
        customSelections: Set<CustomStarterFile> = setOf(
            CustomStarterFile.INDEX_HTML,
            CustomStarterFile.STYLE_CSS,
            CustomStarterFile.SCRIPT_JS
        )
    ): Map<String, String> {
        val safeTitle = projectName.ifBlank { "Mobile Web App" }
        val safeDesc = description.ifBlank { "Built in Mobile Code Studio" }

        return when (templateId) {
            ProjectTemplateId.EMPTY -> emptyMap()

            ProjectTemplateId.BASIC_HTML -> mapOf(
                "index.html" to """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>$safeTitle</title>
  <style>
    body {
      margin: 0;
      min-height: 100vh;
      display: grid;
      place-items: center;
      font-family: system-ui, -apple-system, sans-serif;
      background: linear-gradient(135deg, #0b132b, #1c2541);
      color: #f8fafc;
      padding: 24px;
    }
    main {
      max-width: 520px;
      padding: 28px;
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.16);
      backdrop-filter: blur(12px);
    }
    h1 { margin-top: 0; color: #38bdf8; }
  </style>
</head>
<body>
  <main>
    <h1>$safeTitle</h1>
    <p>$safeDesc</p>
    <p>Edit <code>index.html</code> and tap Run to preview changes live.</p>
  </main>
</body>
</html>
""".trimIndent()
            )

            ProjectTemplateId.WEB_BUNDLE -> mapOf(
                "index.html" to """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>$safeTitle</title>
  <link rel="stylesheet" href="style.css" />
</head>
<body>
  <div class="glass-shell">
    <span class="badge">Local Server Live</span>
    <h1>$safeTitle</h1>
    <p class="subtitle">$safeDesc</p>
    <div class="counter-box">
      <button id="decrementBtn" type="button">-</button>
      <span id="counterValue">0</span>
      <button id="incrementBtn" type="button">+</button>
    </div>
    <p id="statusText" class="status">Ready to build something remarkable.</p>
  </div>
  <script src="script.js"></script>
</body>
</html>
""".trimIndent(),
                "style.css" to """
:root {
  --bg: #07111f;
  --glass: rgba(255, 255, 255, 0.09);
  --border: rgba(255, 255, 255, 0.18);
  --accent: #38bdf8;
  --text: #f1f5f9;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  font-family: system-ui, -apple-system, sans-serif;
  background: radial-gradient(circle at top left, #0f294a, var(--bg));
  color: var(--text);
}

.glass-shell {
  width: 100%;
  max-width: 440px;
  padding: 28px;
  border-radius: 24px;
  background: var(--glass);
  border: 1px solid var(--border);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.35);
  text-align: center;
}

.badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #082f49;
  background: var(--accent);
}

h1 {
  margin: 16px 0 8px;
  font-size: 26px;
}

.subtitle {
  margin: 0 0 24px;
  opacity: 0.8;
  line-height: 1.5;
}

.counter-box {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  padding: 10px 18px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--border);
}

.counter-box button {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 12px;
  font-size: 20px;
  font-weight: 700;
  background: var(--accent);
  color: #082f49;
  cursor: pointer;
}

#counterValue {
  min-width: 36px;
  font-size: 22px;
  font-weight: 700;
}

.status {
  margin-top: 20px;
  font-size: 14px;
  color: #93c5fd;
}
""".trimIndent(),
                "script.js" to """
const valueEl = document.getElementById("counterValue");
const statusEl = document.getElementById("statusText");
const incBtn = document.getElementById("incrementBtn");
const decBtn = document.getElementById("decrementBtn");

let count = 0;

function updateUI() {
  valueEl.textContent = String(count);
  statusEl.textContent = count === 0
    ? "Ready to build something remarkable."
    : "Interactive JS state updated: " + count;
}

incBtn.addEventListener("click", () => {
  count += 1;
  updateUI();
});

decBtn.addEventListener("click", () => {
  count -= 1;
  updateUI();
});
""".trimIndent()
            )

            ProjectTemplateId.RESPONSIVE -> mapOf(
                "index.html" to """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>$safeTitle</title>
  <link rel="stylesheet" href="css/layout.css" />
</head>
<body>
  <header class="topbar">
    <div class="brand">$safeTitle</div>
    <button id="menuToggle" class="menu-btn" aria-label="Toggle Menu">Menu</button>
    <nav id="navLinks" class="nav-links">
      <a href="#features">Features</a>
      <a href="#grid">Showcase</a>
      <a href="#contact">Contact</a>
    </nav>
  </header>

  <main class="container">
    <section class="hero">
      <h1>Responsive Mobile-First Grid</h1>
      <p>$safeDesc</p>
      <a class="cta" href="#grid">Explore Components</a>
    </section>

    <section id="grid" class="cards">
      <article class="card">
        <h3>Fluid Layout</h3>
        <p>Automatically adapts from compact phones to wide tablets.</p>
      </article>
      <article class="card">
        <h3>CSS Variables</h3>
        <p>Customize palette, spacing, and radii in one place.</p>
      </article>
      <article class="card">
        <h3>Local HTTP Assets</h3>
        <p>Stylesheets and scripts load cleanly from nested folders.</p>
      </article>
    </section>
  </main>

  <script src="js/app.js"></script>
</body>
</html>
""".trimIndent(),
                "css/layout.css" to """
:root {
  --surface: #0b1120;
  --card: rgba(30, 41, 59, 0.72);
  --border: rgba(148, 163, 184, 0.22);
  --primary: #22d3ee;
  --text: #f8fafc;
}

* { box-sizing: border-box; }

body {
  margin: 0;
  font-family: system-ui, -apple-system, sans-serif;
  background: linear-gradient(180deg, #090f1d, #0f172a);
  color: var(--text);
}

.topbar {
  position: sticky;
  top: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(11, 17, 32, 0.85);
  border-bottom: 1px solid var(--border);
}

.brand {
  font-weight: 700;
  font-size: 18px;
  color: var(--primary);
}

.menu-btn {
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text);
}

.nav-links {
  display: none;
  width: 100%;
  flex-direction: column;
  gap: 10px;
  padding-top: 12px;
}

.nav-links.open {
  display: flex;
}

.nav-links a {
  color: var(--text);
  text-decoration: none;
  opacity: 0.88;
}

.container {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.hero {
  padding: 28px;
  border-radius: 22px;
  background: var(--card);
  border: 1px solid var(--border);
  margin-bottom: 20px;
}

.cta {
  display: inline-block;
  margin-top: 12px;
  padding: 10px 18px;
  border-radius: 12px;
  background: var(--primary);
  color: #083344;
  font-weight: 700;
  text-decoration: none;
}

.cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.card {
  padding: 20px;
  border-radius: 18px;
  background: var(--card);
  border: 1px solid var(--border);
}

@media (min-width: 640px) {
  .menu-btn { display: none; }
  .nav-links {
    display: flex;
    width: auto;
    flex-direction: row;
    padding-top: 0;
    gap: 18px;
  }
  .cards {
    grid-template-columns: repeat(3, 1fr);
  }
}
""".trimIndent(),
                "js/app.js" to """
const toggle = document.getElementById("menuToggle");
const nav = document.getElementById("navLinks");

if (toggle && nav) {
  toggle.addEventListener("click", () => {
    nav.classList.toggle("open");
  });
}
""".trimIndent()
            )

            ProjectTemplateId.CUSTOM -> {
                val files = linkedMapOf<String, String>()
                val hasCss = CustomStarterFile.STYLE_CSS in customSelections
                val hasJs = CustomStarterFile.SCRIPT_JS in customSelections
                if (CustomStarterFile.INDEX_HTML in customSelections) {
                    val cssLink = if (hasCss) "\n  <link rel=\"stylesheet\" href=\"style.css\" />" else ""
                    val jsTag = if (hasJs) "\n  <script src=\"script.js\"></script>" else ""
                    files["index.html"] = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>$safeTitle</title>$cssLink
</head>
<body>
  <main class="panel">
    <h1>$safeTitle</h1>
    <p>$safeDesc</p>
  </main>$jsTag
</body>
</html>
""".trimIndent()
                }
                if (hasCss) {
                    files["style.css"] = """
body {
  margin: 0;
  padding: 24px;
  font-family: system-ui, sans-serif;
  background: #0f172a;
  color: #f8fafc;
}
.panel {
  max-width: 560px;
  margin: 40px auto;
  padding: 24px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
}
""".trimIndent()
                }
                if (hasJs) {
                    files["script.js"] = """
document.addEventListener("DOMContentLoaded", () => {
  console.log("$safeTitle loaded");
});
""".trimIndent()
                }
                if (CustomStarterFile.DATA_JSON in customSelections) {
                    files["data.json"] = """
{
  "project": "$safeTitle",
  "version": "1.0.0",
  "offline": true
}
""".trimIndent()
                }
                if (CustomStarterFile.LOGO_SVG in customSelections) {
                    files["assets/logo.svg"] = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120" width="120" height="120">
  <rect width="120" height="120" rx="24" fill="#0B132B"/>
  <path d="M44 40 L24 60 L44 80" stroke="#38BDF8" stroke-width="8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M76 40 L96 60 L76 80" stroke="#A78BFA" stroke-width="8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
""".trimIndent()
                }
                if (CustomStarterFile.README_MD in customSelections) {
                    files["README.md"] = """
# $safeTitle

$safeDesc
""".trimIndent()
                }
                files
            }
        }
    }
}
