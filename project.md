# PROJECT — Fonte di verità

## METADATI
- PROJECT_NAME: Immersive Portals Mod
- PROJECT_PREFIX: IMPT
- VERSION: v6.0.12
- LAST_FEATURE_ID: 000007
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
- 🟡 **IMPT000003:** Aggiornato Fabric Loom e Gradle per compatibilità con Javadoc intermediari *(2025-12-20 21:01:32)* [CHG] — Scope: build.gradle, gradle.properties, gradle/wrapper/gradle-wrapper.properties, project.md, changelog.md — Reason: risolvere il blocco Loom su Javadoc non in namespace intermediary e requisiti Gradle del plugin (snapshot 9.2.1) — Test: ./gradlew build — Rollback: ripristinare le versioni precedenti di fabric-loom e gradle wrapper
- 🟡 **IMPT000004:** Allineati hook server/render per API 1.21.11 (listener livelli, preset grafici, occlusion culling Sodium) *(2025-12-21 11:40:51)* [CHG] — Scope: src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java, src/main/java/qouteall/imm_ptl/core/compat/mixin/sodium/MixinSodiumOcclusionCuller.java, src/main/java/qouteall/q_misc_util/mixin/MixinMinecraftServer_Misc.java, src/main/java/qouteall/imm_ptl/peripheral/mixin/common/dim_stack/MixinMinecraftServer_DimStack_CVB.java, src/main/java/qouteall/imm_ptl/peripheral/alternate_dimension/NormalSkylandGenerator.java, project.md, changelog.md — Reason: aggiornare i riferimenti a classi/metodi rinominati in 1.21.11 — Test: ./gradlew build — Rollback: ripristinare gli import e le signature precedenti nei file indicati
- 🟡 **IMPT000005:** Aggiornati tipi di teletrasporto/proiettili, input GUI e lettura tag opzionali per compatibilità 1.21.11 *(2025-12-21 12:01:26)* [CHG] — Scope: src/main/java/qouteall/imm_ptl/core/portal/Portal.java, src/main/java/qouteall/q_misc_util/Helper.java, src/main/java/qouteall/imm_ptl/peripheral/dim_stack/DimEntryWidget.java, src/main/java/qouteall/imm_ptl/peripheral/dim_stack/DimListWidget.java, src/main/java/qouteall/imm_ptl/peripheral/dim_stack/DimStackEntry.java, src/main/java/qouteall/imm_ptl/core/portal/EndPortalEntity.java, src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinAbstractArrow.java, src/main/java/qouteall/imm_ptl/core/mixin/common/collision/MixinThrownEnderPearl.java, src/main/java/qouteall/imm_ptl/core/platform_specific/mixin/common/MixinServerPlayerEntity_MA.java, src/main/java/qouteall/imm_ptl/core/mixin/common/position_sync/MixinServerGamePacketListenerImpl.java, src/main/java/qouteall/q_misc_util/MiscHelper.java, src/main/java/qouteall/q_misc_util/MiscNetworking.java — Reason: allineare API rinominate e nuovi Optional/ValueInput nei salvataggi — Test: ./gradlew build — Rollback: ripristinare le firme/usi precedenti nei file indicati
- 🟡 **IMPT000006:** Adeguata la gestione ticket chunk ImmPtl a TicketStorage e TicketType di 1.21.11 *(2025-12-21 11:15:41)* [CHG] — Scope: src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/MixinDistanceManager.java, src/main/java/qouteall/imm_ptl/core/ducks/IEDistanceManager.java, src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/IEDistanceManager.java, src/main/java/qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTickets.java, src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java, src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/IEChunkTaskPriorityQueueSorter.java, project.md, changelog.md — Reason: aggiornare l’accesso ai ticket e rimuovere le API obsolete (Ticket<?>/ChunkTaskPriorityQueueSorter) — Test: ./gradlew build (fallisce con errori render/shader ancora da portare) — Rollback: ripristinare l’uso di SortedArraySet<Ticket<?>> e le vecchie accessor su DistanceManager
- 🟡 **IMPT000007:** Allineati accesso NBT, ResourceKey e cleanup mixin cloud/clipping per API 1.21.11 *(2025-12-21 13:05:00)* [CHG] — Scope: src/main/java/qouteall/q_misc_util/my_util/Mesh2D.java, src/main/java/qouteall/q_misc_util/my_util/IntBox.java, src/main/java/qouteall/q_misc_util/dimension/DimIntIdMap.java, src/main/java/qouteall/q_misc_util/Helper.java, src/main/java/qouteall/q_misc_util/my_util/DQuaternion.java, src/main/java/qouteall/imm_ptl/core/ClientWorldLoader.java, src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java, src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinRenderSystem_Clipping.java, src/main/java/qouteall/imm_ptl/core/mixin/client/render/optimization/MixinLevelRenderer_Clouds.java, src/main/java/qouteall/imm_ptl/core/render/context_management/CloudContext.java, src/main/java/qouteall/imm_ptl/core/IPMcHelper.java, src/main/java/qouteall/imm_ptl/core/mixin/client/MixinGlDebug.java, src/main/java/qouteall/imm_ptl/core/mixin/client/sync/MixinReceivingLevelScreen.java, src/main/resources/imm_ptl.accesswidener — Reason: adeguare API opzionali NBT, identifier di ResourceKey e isolare mixin dipendenti dal vecchio render pipeline — Test: ./gradlew build (fallisce con errori shader/render pipeline ancora da portare) — Rollback: ripristinare access widener e mixin originali, reintrodurre getInt/getDouble non-optional

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
