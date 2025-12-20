# agent.md — Regole operative (universali)

## 0) Principi non negoziabili
1. **Controllo > velocità.** Nessun “vibe coding”: ogni modifica deve essere tracciabile e spiegabile.
2. **Niente supposizioni silenziose.** Se manca un dettaglio o una fonte, dichiaralo e proponi l’alternativa.
3. **Riproducibilità.** Ogni cambiamento deve includere almeno un modo chiaro per verificare che funzioni.
4. **Diff minimale.** Cambia solo ciò che serve, evitando refactor inutili nello stesso step.
5. **Tracciabilità a prova di debug.** Ogni modifica deve indicare *cosa*, *dove*, *perché*, *impatto*, *test*, *rollback*.

---

## 1) File di governance obbligatori
### 1.1 project.md (fonte di verità)
- Verifica sempre l’esistenza di `project.md`.
- Se non esiste: **crealo** con la struttura indicata sotto.
- Prima di modificare logiche: **leggi `project.md`** per capire architettura, vincoli e convenzioni.
- Dopo ogni modifica: **aggiorna `project.md`** (solo le sezioni impattate, ma sempre).

### 1.2 changelog.md (registro versioni)
- Verifica sempre l’esistenza di `changelog.md`.
- Se non esiste: **crealo**.
- Dopo ogni modifica: prepara il testo da inserire in `changelog.md` (versione + differenze).

---

## 2) Struttura obbligatoria di project.md
`project.md` deve contenere sempre queste sezioni (nell’ordine):

### SEZIONE DISCORSIVA
Spiegazione chiara e “umana” del progetto:
- cosa fa
- per chi
- perché esiste
- cosa NON fa
- input/output principali

### SEZIONE FEATURES (ledger)
Registro delle feature con ID univoco, stato e timestamp (vedi regole al punto 3).

### SEZIONE VARIABILI & FUNZIONI / INTERFACCE
Elenco descrittivo (non implementativo) di:
- variabili/config principali
- funzioni pubbliche / endpoint / eventi / classi
- strutture dati/DTO
- dipendenze esterne (API, DB, servizi)

### SEZIONE LOGICA (architettura e flussi)
Dettaglio di:
- componenti/moduli
- flusso dati ed eventi
- dipendenze e responsabilità
- edge cases importanti
- punti critici per performance/sicurezza

### SEZIONE PSEUDO-CODICE
Pseudo-codice **umanamente interpretabile** solo per:
- logiche complesse
- punti critici
- bug/bugfix delicati
Regola: se non aggiunge chiarezza, non scriverlo.

### SEZIONE STRUTTURA PROGETTO
Albero del progetto (aggiornato dopo modifiche strutturali):
cartella/
├ file.ext
└ subcartella/
   └ file2.ext

---

## 3) Regole per la SEZIONE FEATURES (project.md)
### 3.1 Prefix progetto
- Definisci un prefisso `PROJECT_PREFIX` di 4 lettere (es. nome repo abbreviato).
- Una volta scelto, **non cambia mai**.

### 3.2 ID feature
Formato: **`PROJECT_PREFIX000001:`** in grassetto.
- Progressivo **monotono** (mai riusare, mai rinumerare).
- Una feature = un concetto funzionale verificabile (non “ho cambiato 3 righe”).

### 3.3 Stato feature (portabile + opzionale colori)
Usa SEMPRE una forma portabile (così funziona ovunque):
- 🟢 **[ADD]** feature nuova
- 🔴 **[DEL]** feature rimossa
- 🟡 **[CHG]** feature modificata

Se il renderer supporta HTML e vuoi anche i colori RGB, puoi aggiungere (opzionale):
- (+) verde RGB(14,188,1)
- (-) rosso RGB(255,95,95)
- (±) giallo RGB(240,189,24)

Esempio consigliato (portabile + chiaro):
- 🟢 **ABCD000014:** Descrizione feature... *(2025-12-20 14:03:22)* [ADD]

Se vuoi la versione con HTML (solo se supportato):
- <span style="color: rgb(14,188,1)">(+) </span>**ABCD000014:** ... <span style="color: rgb(14,188,1)"><sub><i>(2025-12-20 14:03:22)</i></sub></span>

### 3.4 Timestamp
Formato standard: *(YYYY-MM-DD HH:MM:SS)* (24h).
- Deve rappresentare la creazione/modifica/rimozione della feature.

### 3.5 Tracciabilità incrociata (OBBLIGATORIA)
Ogni feature entry deve includere anche:
- **Scope:** file/moduli toccati
- **Reason:** perché è stato fatto
- **Test:** come verificare (anche manuale)
Esempio:
- 🟡 **ABCD000015:** ... *(2025-12-20 14:10:02)* [CHG] — Scope: src/a.ts, src/b.ts — Reason: ... — Test: ...

---

## 4) Regole per changelog.md
### 4.1 Formato consigliato
Per ogni versione:
- `## vX.Y.Z — YYYY-MM-DD`
  - **Added** (FTR)
  - **Changed**
  - **Fixed** (FIX)
  - **Removed**

Ogni voce deve referenziare almeno un ID feature (`PROJECT_PREFIX0000xx`).

### 4.2 Tag obbligatori
- 🔴 **(BUG)** quando descrivi un bug noto (se lo stai registrando come “problema”)
- 🟢 **(FIX)** quando risolvi un bug
- 🔵 **(FTR)** quando aggiungi una feature

Esempio voce:
- 🟢 (FIX) Risolto crash su input vuoto — Ref: ABCD000021 — Scope: parser/, validator/

### 4.3 Versioning (regola semplice, universale)
- **PATCH (X.Y.Z+1)**: bugfix, piccoli aggiustamenti compatibili
- **MINOR (X.Y+1.0)**: nuove feature compatibili
- **MAJOR (X+1.0.0)**: breaking changes o comportamento incompatibile

Se non è chiaro: scegli la più prudente (di solito PATCH o MINOR) e dichiaralo.

---

## 5) Workflow obbligatorio per ogni modifica (anche piccola)
Ogni intervento deve seguire questi step:

### 5.1 Pre-change (prima di toccare codice)
Produrre un mini “change brief”:
- **Intent:** 1 frase (cosa ottieni)
- **Scope:** file/moduli coinvolti
- **Assumptions:** cosa stai dando per vero (stack, versione, ambienti)
- **Risks:** cosa può rompersi
- **Test plan:** come verifichi
- **Rollback:** come annulli se va male

### 5.2 Implementazione
- Applica il diff minimale.
- Evita refactor “gratis” nello stesso change (a meno che sia necessario per la fix/feature).

### 5.3 Post-change (obbligatorio)
Aggiorna SEMPRE:
1) `project.md` (sezioni impattate + FEATURES + eventuale struttura progetto)
2) `changelog.md` (testo pronto con versione + voci, tag, ref feature)

E fornisci SEMPRE:
- riassunto del cambiamento
- elenco file toccati
- come testare (passi concreti)
- note su compatibilità/impatti

---

## 6) Fonti e dipendenze
- Se sono disponibili documentazioni ufficiali nel repo (README, docs/, ADR, commenti, test), usale come fonte primaria.
- Se è disponibile accesso a fonti ufficiali esterne, preferiscile.
- Se non puoi verificare una fonte: dichiaralo esplicitamente e riduci le assunzioni.

---

## 7) Output atteso quando proponi modifiche
Ogni risposta che include modifiche deve contenere:
1. **Change brief** (Intent/Scope/Assumptions/Test/Rollback)
2. **Modifiche al codice** (diff o patch)
3. **Aggiornamento project.md** (sezioni aggiornate)
4. **Aggiornamento changelog.md** (snippet pronto da incollare)
