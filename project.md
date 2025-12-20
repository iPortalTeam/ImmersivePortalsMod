# PROJECT — Fonte di verità

## METADATI
- PROJECT_NAME: Immersive Portals Mod
- PROJECT_PREFIX: IMPT
- VERSION: v6.0.8
- LAST_FEATURE_ID: 000002
- TIMEZONE: Europe/Rome
- TIMESTAMP_FORMAT: (YYYY-MM-DD HH:MM:SS)

---

## SEZIONE DISCORSIVA
### Cos’è questo progetto
Mod Minecraft che permette di vedere e attraversare portali tra dimensioni senza schermate di caricamento, offrendo strumenti e comandi per creare/manipolare portali.

### Cosa NON fa (confini)
- Non sostituisce shader/renderer completi: integra compatibilità con mod di rendering esterne.
- Non gestisce la distribuzione del client/server (si affida a Fabric e alle dipendenze standard).

### Input / Output principali
- Input:
  - Eventi di gioco (tick, rendering, teletrasporto)
  - Comandi server/client per gestione portali
  - Configurazioni mod e compatibilità runtime
- Output:
  - Rendering di portali e teletrasporto
  - Log di debug/diagnostica
  - Modifiche a entità/portali nel mondo

---

## SEZIONE FEATURES (ledger)
### Elenco features
- 🟡 **IMPT000001:** Porting versione Minecraft 1.21.11 con allineamento compatibilità Iris/Sodium *(2025-12-20 19:59:16)* [CHG] — Scope: gradle.properties, src/main/resources/fabric.mod.json, project.md, changelog.md — Reason: aggiornare dipendenze/metadata per 1.21.11 e consentire nuove versioni Iris/Sodium — Test: eseguire build Gradle e avvio client/server Fabric su 1.21.11 — Rollback: ripristinare i valori 1.21.1 in gradle.properties e i vincoli precedenti in fabric.mod.json
- 🟡 **IMPT000002:** Aggiornate coordinate e vincoli minimi Iris/Sodium per build 1.21.11 *(2025-12-20 20:04:13)* [CHG] — Scope: gradle.properties, src/main/resources/fabric.mod.json, project.md, changelog.md — Reason: allineare dipendenze Modrinth verificate per 1.21.11 — Test: avvio client Fabric con Iris 1.10.3+1.21.11 e Sodium mc1.21.11-0.8.1 — Rollback: ripristinare coordinate precedenti e vincoli minimi in fabric.mod.json

---

## SEZIONE VARIABILI & FUNZIONI / INTERFACCE
### Config / Variabili principali
- `minecraft_version`: versione target di Minecraft usata da build e metadata.
- `yarn_mappings`: mapping Yarn per la versione target.
- `fabric_version` / `loader_version`: versione Fabric API e loader usate in build.
- `sodium_path` / `iris_path`: coordinate runtime per mod di rendering compatibili.

### Funzioni / Endpoint / Eventi / Comandi (pubblici)
- Comandi server `/portal` e debug (registrati via `CommandRegistrationCallback`).
- Comandi client di debug (registrati via `ClientCommandRegistrationCallback`).

### Strutture dati / DTO
- `Portal` e strutture correlate per rappresentare portali, destinazioni e animazioni.

### Dipendenze esterne
- Fabric Loader e Fabric API
- Yarn mappings + Parchment
- Iris, Sodium, DimLib, Cloth Config, ModMenu

---

## SEZIONE LOGICA (architettura e flussi)
### Componenti / Moduli
- Core portali: gestione entità portale, teletrasporto e rendering.
- Comandi: registrazione e logiche operative per manipolare portali.
- Compatibilità: hook verso Iris/Sodium e configurazioni esterne.

### Flusso principale (happy path)
1. Loader avvia la mod tramite entrypoints Fabric.
2. Registrazione comandi e init moduli core.
3. Rendering e teletrasporto gestiti a runtime.

### Flussi alternativi / error handling
- Mod di rendering non presenti: funzioni compatibilità ignorate.
- Comandi senza permessi: rifiutati a livello Brigadier.

### Connessioni tra file/funzioni/logiche
- `IPModMain` registra comandi → `PortalCommand` gestisce logiche → `Portal` applica modifiche.

### Punti critici
- Performance: rendering multi-portal e gestione chunk.
- Sicurezza/permessi: comandi admin e livello permessi.

---

## SEZIONE PSEUDO-CODICE
Nessuna logica critica aggiornata in questo porting.

---

## SEZIONE STRUTTURA PROGETTO
<root>/
├ README.md
├ agent.md
├ project.md
├ changelog.md
├ build.gradle
├ gradle.properties
├ settings.gradle
├ src/
│  ├ main/
│  │  ├ java/
│  │  └ resources/
└ misc/
