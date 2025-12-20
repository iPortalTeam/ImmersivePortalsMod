# PROJECT — Fonte di verità

## METADATI
- PROJECT_NAME: <nome-progetto>
- PROJECT_PREFIX: <IMPT>   <!-- 4 lettere, deterministico: vedi regola sotto -->
- VERSION: v0.1.0
- LAST_FEATURE_ID: 000000  <!-- incrementare quando aggiungi una feature -->
- TIMEZONE: Europe/Rome
- TIMESTAMP_FORMAT: (aaaa/mm/gg hh.mm.ss)

---

## SEZIONE DISCORSIVA
### Cos’è questo progetto
<Spiegazione in linguaggio semplice: cosa fa, per chi, perché esiste.>

### Cosa NON fa (confini)
- <non fa X>
- <non fa Y>

### Input / Output principali
- Input:
  - <es: eventi, richieste, file, comandi, API call>
- Output:
  - <es: UI, file generati, risposte API, log, modifiche DB>

---

## SEZIONE FEATURES (ledger)
> Regole:
> - Ogni feature: **XXXX000001:** (ID monotono, mai riusato)
> - Stato: 🟢[ADD] 🔴[DEL] 🟡[CHG]
> - Timestamp: *(aaaa/mm/gg hh.mm.ss)* in corsivo
> - Ogni voce include: Scope, Reason, Test, Rollback

### Elenco features
- 🟢 **XXXX000001:** <titolo/descrizione feature> *(2025/12/20 14.03.22)*  
  - Scope: <file/moduli/eventi>
  - Reason: <perché è stata fatta>
  - Test: <passi riproducibili per verificare>
  - Rollback: <come annullare se rompe>

- 🟡 **XXXX000002:** <descrizione modifica> *(2025/12/20 14.10.02)*  
  - Scope: <...>
  - Reason: <...>
  - Test: <...>
  - Rollback: <...>

---

## SEZIONE VARIABILI & FUNZIONI / INTERFACCE
> Elenco descrittivo (non implementativo). Qui spieghi “cosa sono” e “a cosa servono”.

### Config / Variabili principali
- `<NOME_VARIABILE>`: <scopo, formato, valori attesi>
- `<NOME_VARIABILE>`: <scopo, formato, valori attesi>

### Funzioni / Endpoint / Eventi / Comandi (pubblici)
- `<nome>`:
  - Scopo: <...>
  - Input: <...>
  - Output: <...>
  - Side effects: <...>
  - Errori/Edge cases: <...>

### Strutture dati / DTO
- `<NomeStruttura>`:
  - Campi: <...>
  - Vincoli: <...>

### Dipendenze esterne
- <framework/librerie/servizi>
- Versioni: <se note>
- Note integrazione: <...>

---

## SEZIONE LOGICA (architettura e flussi)
### Componenti / Moduli
- <modulo A>: responsabilità
- <modulo B>: responsabilità

### Flusso principale (happy path)
1. <step 1>
2. <step 2>
3. <step 3>

### Flussi alternativi / error handling
- Caso A: <...>
- Caso B: <...>

### Connessioni tra file/funzioni/logiche
- `<fileA>` → chiama `<funzioneB>` → emette `<eventoC>` → aggiorna `<statoD>`

### Punti critici
- Performance: <...>
- Concorrenza/race: <...>
- Sicurezza/permessi: <...>

---

## SEZIONE PSEUDO-CODICE
> Solo per logiche complesse/critiche o per bug/bugfix importanti.

### <NomeFunzioneCritica>
- Se <condizione>, allora <azione>
- Altrimenti <azione>
- Se fallisce <X>, allora <fallback>
- Garantisce: <invariante>

---

## SEZIONE STRUTTURA PROGETTO
> Aggiorna quando cambia struttura o file rilevanti.

<root>/
├ README.md
├ agent.md
├ project.md
├ changelog.md
├ src/
│  ├ ...
└ tests/
   └ ...
