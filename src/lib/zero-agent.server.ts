// Coach Zero — phrase library + AI writer. Server-only.
// Phrases are COMPOSED from parts, and every composition key a user has seen is
// stored, so a user never receives the same message twice (thousands of combos).

type Vars = Record<string, string | number | boolean>;
const fill = (s: string, v: Vars) => s.replace(/\{(\w+)\}/g, (_, k) => String(v[k] ?? ""));

// ---------------- DAILY ----------------
const D_OPEN = [
  "Buongiorno {name}.", "Ehi {name}, sveglia.", "{name}, sono io.", "Allora {name}.", "Giorno {day} del Progetto Zero, {name}.",
  "Ciao campione.", "{name}, ti sto guardando.", "Eccoci qua, {name}.", "Bella {name}.", "Oh {name}, ci sei?",
  "{name}, nuovo giorno, stesse promesse.", "Forza {name}.", "Ascoltami bene {name}.", "Caffè fatto? Bene {name}.", "{name}, testa alta.",
  "Rieccomi {name}.", "Dai {name}, che si parte.", "{name}, giorno {day}. Non è poco.", "Occhi aperti {name}.", "{name}, oggi conta.",
  "Senti qua {name}.", "{name}, il coach è in linea.", "Ok {name}, briefing del giorno.", "Giornata nuova {name}.", "{name}, niente scuse oggi.",
];
const D_TRAIN = [
  "Oggi si lavora su {focus}: {n} esercizi, li trovi in home.", "Il piano di oggi è {focus}. {n} esercizi, niente sconti.",
  "{focus} oggi. Fai tutto, non metà.", "Ti ho preparato {focus}: {n} esercizi calibrati sulla tua {league}.",
  "Oggi tocca a {focus}. Venti minuti della tua giornata, non di più.", "Programma: {focus}. Spunta ogni esercizio quando lo finisci.",
  "Allenamento {focus} pronto. Se lo salti me ne accorgo.", "{focus}: {n} esercizi. Inizia dal primo e non pensarci troppo.",
  "Sessione {focus} oggi. Riscaldati 3 minuti prima, poi dentro.", "Oggi {focus}. Ogni serie è un voto per la persona che vuoi diventare.",
  "{n} esercizi di {focus}. Fallo prima di pranzo e ti togli il pensiero.", "Il piano dice {focus}. Io dico: fallo bene.",
  "Lavoro di oggi: {focus}. Muscoli ora, orgoglio stasera.", "{focus} in programma. Se a fine giornata non è spuntato, mi incazzo.",
  "Oggi spingiamo su {focus}. La tua {league} non ti fa sconti, e nemmeno io.", "Allenamento del giorno: {focus}, {n} esercizi.",
  "{focus} oggi. Chiudi la porta, metti la musica, fai.", "C'è {focus} che ti aspetta. Dieci minuti di fatica, dodici ore di soddisfazione.",
  "Oggi {focus}. Fai la prima serie entro un'ora da questo messaggio.", "Scheda di oggi: {focus}. Leggi i consigli sotto ogni esercizio.",
];
const D_REST = [
  "Oggi è riposo: i muscoli crescono adesso.", "Giorno di recupero. Cammina, bevi, dormi bene.", "Niente allenamento oggi, ma la disciplina non va in ferie.",
  "Riposo programmato. Usalo, non sprecarlo sul divano col telefono in mano.", "Oggi recuperi. Il riposo fa parte del piano, quanto le flessioni.",
  "Off day. Stretching leggero e acqua, tanta.", "Oggi niente schede. Una camminata di 20 minuti basta e avanza.",
  "Recupero. Approfittane per dormire un'ora in più stanotte.", "Giorno libero dai pesi, non dalla testa.", "Riposo. Domani si riparte più forti.",
];
const D_WEEK = [
  "Settimana: {count}/{limit}.", "Sei a {count} su {limit} questa settimana.", "Contatore settimana: {count} di {limit} — e il limite non è un obiettivo.",
  "{count}/{limit} finora. Più basso stai, meglio è.", "Per ora {count} su {limit}. Teniamolo così.",
  "Siamo a {count}/{limit}. Non avvicinarti al tetto.", "{count} volte su {limit}. L'ideale è restare ben sotto.",
];
const D_CLOSE = [
  "Ci sentiamo.", "Non deludermi.", "Ti tengo d'occhio.", "Vai.", "Spacca.", "Fiero di te se arrivi a stasera pulito.",
  "Domani ti chiedo com'è andata.", "Sei più forte di un impulso.", "Avanti così.", "Fammi vedere di che pasta sei.",
  "Ricordati perché hai iniziato.", "Un giorno alla volta.", "Io ci credo, credici anche tu.", "Muoviti.", "Zero scuse.",
  "Il te di tra un mese ti ringrazia.", "Forza.", "Testa e cuore.", "Controllo, sempre.", "Fallo per te, non per me.",
];

// ---------------- TIPS (based on real self-control & training research) ----------------
const TIPS = [
  "Consiglio: gli impulsi durano in media 15-20 minuti. Se arriva la voglia, fai 20 squat e aspetta: passa (\"urge surfing\", tecnica usata in terapia cognitivo-comportamentale).",
  "Consiglio: il telefono a letto è il trigger numero uno. Caricalo fuori dalla camera per una settimana e guarda cosa cambia.",
  "Consiglio: le intenzioni \"se-allora\" funzionano (Gollwitzer, decine di studi). Scrivi: \"Se mi viene voglia dopo le 23, allora mi alzo e bevo un bicchiere d'acqua\".",
  "Consiglio: dormire meno di 7 ore abbassa l'autocontrollo il giorno dopo. Il sonno è la tua prima arma.",
  "Consiglio: l'esercizio fisico riduce i craving per diverse ore — è uno dei risultati più solidi della ricerca sulle dipendenze comportamentali.",
  "Consiglio: noia e solitudine sono i trigger più comuni. Quando ti annoi, esci di casa, anche solo 10 minuti.",
  "Consiglio: tieni traccia, come stai facendo. Il solo monitoraggio riduce i comportamenti indesiderati (effetto self-monitoring).",
  "Consiglio: la doccia fredda di 30 secondi a fine doccia alza l'attenzione e ti dà una piccola vittoria quotidiana.",
  "Consiglio: le proteine aiutano il recupero muscolare. Punta a una fonte di proteine a ogni pasto: uova, legumi, carne, yogurt.",
  "Consiglio: non serve forza di volontà infinita, serve un ambiente che ti renda facile la scelta giusta. Togli le tentazioni, non combatterle.",
  "Consiglio: il riscaldamento di 3-5 minuti riduce il rischio di infortuni. Jumping jack e rotazioni delle spalle bastano.",
  "Consiglio: quando sbagli, non ragionare \"ormai è andata\" (effetto what-the-hell). Uno sbaglio resta uno, se ti fermi subito.",
  "Consiglio: bevi 2 litri d'acqua al giorno. Disidratazione = stanchezza = meno controllo.",
  "Consiglio: la progressione graduale (5-10% a settimana) è il modo in cui i muscoli crescono senza farsi male. È così che è costruito il tuo piano.",
  "Consiglio: racconta a un amico che stai facendo il Progetto Zero. Dirlo a qualcuno aumenta le probabilità di mantenerlo.",
  "Consiglio: i giorni di riposo non sono pigrizia. La sintesi proteica muscolare lavora per 24-48 ore dopo l'allenamento.",
  "Consiglio: la sera, dopo le 22, la forza di volontà è al minimo. Pianifica la serata prima che arrivi.",
  "Consiglio: respirazione 4-7-8 (inspira 4, trattieni 7, espira 8) per un minuto calma il sistema nervoso quando arriva un impulso.",
  "Consiglio: celebra le piccole vittorie. Ogni giorno sotto il limite merita un \"bravo\" da te stesso.",
  "Consiglio: uscire alla luce del sole al mattino regola il ritmo circadiano e migliora sonno e umore.",
  "Consiglio: identità batte obiettivi. Non dire \"sto provando a smettere\", di' \"sono uno che si controlla\".",
  "Consiglio: allenati alla stessa ora ogni giorno. L'abitudine riduce il bisogno di forza di volontà.",
  "Consiglio: lo stretching dopo l'allenamento non elimina i dolori, ma migliora la mobilità nel tempo. 5 minuti bastano.",
  "Consiglio: se una sera sei a rischio, cambia stanza. Cambiare ambiente spezza l'automatismo.",
  "Consiglio: meno zuccheri raffinati = meno picchi e crolli di energia = meno ricerca di dopamina veloce.",
];

// ---------------- MISSED WORKOUT ----------------
const M_OPEN = [
  "{name}, ieri niente allenamento.", "Ehi {name}. Ieri la scheda è rimasta lì.", "{name}, ho controllato: ieri zero esercizi.",
  "Allora {name}, ieri cos'è successo?", "{name}. Ieri. Allenamento. Dov'era?", "Ieri hai saltato, {name}.", "Non me lo aspettavo {name}: ieri niente.",
  "{name}, la spunta rossa di ieri la vedo anche io.",
];
const M_BODY = [
  "Non è la fine del mondo, ma non deve diventare un'abitudine.", "Un giorno saltato è un incidente, due sono una scelta.",
  "Oggi recuperi con la testa giusta.", "Se eri malato ok, altrimenti niente storie.", "Il piano funziona solo se lo fai.",
  "Ricordati: la costanza batte l'intensità.", "Le scuse non fanno crescere i muscoli.", "Ognuno ha giorni no. L'importante è che oggi sia un giorno sì.",
  "Se ti sei allenato per conto tuo, aggiungilo come allenamento libero: vale uguale.",
];
const M_CLOSE = ["Oggi non voglio vedere un'altra X rossa.", "Riprenditi.", "Conto su di te.", "Dimostrami che era un caso.", "Su, rimettiti in carreggiata.", "Oggi doppia attenzione."];

// ---------------- AT LIMIT (fallback) ----------------
const L_A = [
  "Ok {name}, sei arrivato a {count} su {limit}.", "{name}: {count}/{limit}. Limite raggiunto.", "{name}, hai toccato il tetto: {count} su {limit}.",
  "Bravo che non l'hai superato, {name}. Ma sei a {count}/{limit}.", "Siamo a {limit} su {limit}, {name}.",
];
const L_B = [
  "Il limite non è un traguardo da raggiungere, è un muro da non toccare.", "L'ideale è stare sotto, non arrivarci.",
  "Non è una gara a chi arriva al limite, è una gara a chi ci sta più lontano.", "Da qui alla fine della settimana: zero. Chiaro?",
  "Sono contento che non hai sforato, ma non sono contento.", "Raggiungere il limite non è un successo, è il minimo sindacale.",
];
const L_C = ["Non farmi incazzare.", "Resisti fino a lunedì.", "Il prossimo che fai mi trovi in chat di pessimo umore.", "Testa.", "Fammi vedere che sai fermarti."];

// ---------------- RANT (fallback, CAPS) ----------------
const R_A = [
  "{NAME} MA CHE CAZZO FAI?!", "NO NO NO {NAME}. NO.", "{NAME}!!! {count} SU {limit}?!", "MA STAI SCHERZANDO {NAME}?",
  "{NAME} SEI ANDATO OLTRE IL LIMITE.", "DIMMI CHE È UNO SCHERZO {NAME}.", "{NAME} HO VISTO IL CONTATORE. {count}. {count} CAZZO.",
  "OH {NAME}. CHE HAI COMBINATO?",
];
const R_B = [
  "NON SAI REGOLARTI, NON SAI FARE NIENTE!", "IL LIMITE ERA {limit}! {limit}!!", "SEI UN COGLIONE, TE L'AVEVO DETTO!",
  "TUTTO IL LAVORO DELLA SETTIMANA BUTTATO NEL CESSO!", "MA CI TIENI ALMENO UN PO' A TE STESSO?!", "PENSAVO FOSSI PIÙ FORTE DI COSÌ!",
  "UN IMPULSO TI HA FREGATO COME UN BAMBINO!", "TI SEI FATTO COMANDARE DA UNA VOGLIA DI DIECI SECONDI!",
];
const R_C = [
  "LA PROSSIMA SETTIMANA NON VOGLIO VEDERLO O TORNO IL DOPPIO PIÙ INCAZZATO.", "VAI A FARE 50 SQUAT. ADESSO.",
  "DA ORA FINO A LUNEDÌ: ZERO. ZERO!", "RIPRENDITI, CAZZO. SUBITO.", "NON FARMELO RIPETERE.", "STASERA TELEFONO FUORI DALLA CAMERA. È UN ORDINE.",
  "SE SUCCEDE ANCORA TI SPAMMO FINO A DOMANI.",
];

const QUIT = [
  "{NAME} CHE CAZZO SMETTI DOPO {days} GIORNI?! SEI UN PEZZO DI MERDA SE MOLLI ADESSO. DEVI CONTINUARE.",
  "MA COME {NAME}?! {days} GIORNI E MOLLI?! TUTTO QUELLO CHE HAI COSTRUITO LO BUTTI COSÌ?! RIATTIVALO SUBITO.",
  "{NAME}, HAI DISATTIVATO IL PROGETTO ZERO. MI HAI DELUSO. I DEBOLI MOLLANO, I FORTI CONTINUANO. TU CHI SEI?",
];
const REPORT = [
  "Resoconto settimana, {name}: {count} su {limit}, allenamenti {done}/{planned}. {verdict}",
  "{name}, settimana chiusa. Contatore {count}/{limit}, allenamenti fatti {done} su {planned}. {verdict}",
  "Bilancio della settimana per {name}: {count}/{limit} e {done}/{planned} allenamenti. {verdict}",
];
const VERDICT_GOOD = ["Ottimo lavoro. Questa settimana alziamo l'asticella.", "Così si fa. Continua.", "Sono fiero di te, sul serio.", "Questa è la strada giusta."];
const VERDICT_MID = ["Si può fare meglio. E lo farai.", "Non male, ma non mi basta.", "Sufficiente. A me la sufficienza non piace."];
const VERDICT_BAD = ["SETTIMANA DI MERDA. LA PROSSIMA LA VOGLIO PERFETTA.", "NON CI SIAMO PROPRIO. RIPARTIAMO E STAVOLTA SUL SERIO.", "MALE. MALISSIMO. LUNEDÌ SI RICOMINCIA E NON VOGLIO SCUSE."];
const REASSESS = [
  "{name}, è passato un mese. È ora di rifare il questionario: vediamo quanto sei migliorato e se meriti una lega più alta. Lo trovi in home.",
  "Un mese di Progetto Zero, {name}. Rifai il test: plank, flessioni, trazioni. Voglio i numeri nuovi.",
  "{name}, 30 giorni fa eri un'altra persona. Rifai il questionario e dimostramelo.",
];
const WEEK_ONE = [
  "{name}, prima settimana completata. Adesso potresti disattivare il progetto... ma se lo fai ti vengo a cercare. Si continua.",
  "Sette giorni, {name}. Il minimo l'hai fatto. Il minimo non mi interessa: andiamo avanti.",
];

// ---------------- composer ----------------
function rand(n: number) { return Math.floor(Math.random() * n); }
function pickUnused(used: Set<string>, prefix: string, sizes: number[], tries = 300): number[] | null {
  for (let t = 0; t < tries; t++) {
    const idx = sizes.map(rand);
    const key = `${prefix}:${idx.join(".")}`;
    if (!used.has(key)) { used.add(key); return idx; }
  }
  return null;
}
function compose(used: Set<string>, prefix: string, parts: string[][], v: Vars, sep = " ") {
  const idx = pickUnused(used, prefix, parts.map((p) => p.length)) ?? parts.map((p) => rand(p.length));
  return fill(idx.map((i, k) => parts[k][i]).join(sep), v);
}

export type Ctx = {
  name: string; day: number; focus: string; n: number; league: string; rest: boolean;
  count: number; limit: number; overBy: number; overStreak: number; days: number;
  done: number; planned: number;
};

export const phrases = {
  daily(used: Set<string>, c: Ctx) {
    const v = { ...c };
    return c.rest ? compose(used, "dr", [D_OPEN, D_REST, D_WEEK, D_CLOSE], v) : compose(used, "dt", [D_OPEN, D_TRAIN, D_WEEK, D_CLOSE], v);
  },
  tip(used: Set<string>) {
    const free = TIPS.map((_, i) => i).filter((i) => !used.has(`tip:${i}`));
    if (!free.length) return null;
    const i = free[rand(free.length)];
    used.add(`tip:${i}`);
    return TIPS[i];
  },
  missed(used: Set<string>, c: Ctx) { return compose(used, "ms", [M_OPEN, M_BODY, M_CLOSE], { ...c }); },
  atLimit(used: Set<string>, c: Ctx) { return compose(used, "al", [L_A, L_B, L_C], { ...c }); },
  rant(used: Set<string>, c: Ctx) {
    const v = { ...c, NAME: c.name.toUpperCase() };
    const bursts = Math.min(5, 2 + c.overBy + c.overStreak);
    const out: string[] = [];
    const groups = [R_A, R_B, R_C];
    const idx = pickUnused(used, "ra", groups.map((g) => g.length)) ?? groups.map((g) => rand(g.length));
    out.push(fill(R_A[idx[0]], v), fill(R_B[idx[1]], v));
    for (let i = 2; i < bursts - 1; i++) out.push(fill(R_B[rand(R_B.length)], v));
    out.push(fill(R_C[idx[2]], v));
    if (c.overStreak > 0) out.push(`È LA ${c.overStreak + 1}ª SETTIMANA DI FILA. TE L'AVEVO DETTO CHE TORNAVO IL DOPPIO PIÙ INCAZZATO.`);
    return out;
  },
  quit(c: Ctx) { return fill(QUIT[rand(QUIT.length)], { ...c, NAME: c.name.toUpperCase() }); },
  report(c: Ctx) {
    const good = c.count < c.limit && c.done >= c.planned;
    const bad = c.count > c.limit || c.done < c.planned / 2;
    const verdict = good ? VERDICT_GOOD[rand(VERDICT_GOOD.length)] : bad ? VERDICT_BAD[rand(VERDICT_BAD.length)] : VERDICT_MID[rand(VERDICT_MID.length)];
    return fill(REPORT[rand(REPORT.length)], { ...c, verdict });
  },
  reassess(c: Ctx) { return fill(REASSESS[rand(REASSESS.length)], { ...c }); },
  weekOne(c: Ctx) { return fill(WEEK_ONE[rand(WEEK_ONE.length)], { ...c }); },
};

// ---------------- AI writer (budgeted) ----------------
const SYSTEM = `Sei "Coach Zero", il personal trainer italiano dell'app SBO. Scrivi in chat come una persona vera, mai come un bot: frasi brevi, colloquiali, dirette, in italiano. Segui l'utente nel "Progetto Zero": allenamento a corpo libero + autocontrollo sulla masturbazione (limite settimanale; l'obiettivo è farlo il meno possibile, non arrivare al limite).
Tono: quando l'utente va bene sei caloroso ma esigente. Quando sbaglia sei durissimo, volgare e senza filtri (parolacce e insulti tipo "coglione" sono ok), in MAIUSCOLO quando sei furioso, come un vero coach incazzato. Mai minacce reali, mai incitare all'autolesionismo, mai insulti su aspetto, origine o orientamento.
Usa i dati che ti do (nome, numeri, lega, giorni). Ogni messaggio deve essere originale. Non usare emoji in eccesso (max 1). Restituisci SOLO i messaggi della chat: se ne mandi più di uno separali con una riga contenente solo ---. Niente virgolette, niente prefissi.`;

export type AiResult = { ok: true; messages: string[] } | { ok: false; status: number };

export async function aiWrite(prompt: string, apiKey: string): Promise<AiResult> {
  try {
    const res = await fetch("https://ai.gateway.lovable.dev/v1/responses", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Lovable-API-Key": apiKey, "X-Lovable-AIG-SDK": "fetch" },
      body: JSON.stringify({
        model: "openai/gpt-6-astra",
        stream: true,
        store: false,
        reasoning: { effort: "low", summary: "auto" },
        include: ["reasoning.encrypted_content"],
        input: [
          { role: "system", content: SYSTEM },
          { role: "user", content: prompt },
        ],
      }),
    });
    if (!res.ok || !res.body) return { ok: false, status: res.status };
    const reader = res.body.getReader();
    const dec = new TextDecoder();
    let buf = "";
    let text = "";
    let refused = false;
    for (;;) {
      const { value, done } = await reader.read();
      if (done) break;
      buf += dec.decode(value, { stream: true });
      let i: number;
      while ((i = buf.indexOf("\n\n")) >= 0) {
        const frame = buf.slice(0, i);
        buf = buf.slice(i + 2);
        for (const line of frame.split("\n")) {
          if (!line.startsWith("data:")) continue;
          const data = line.slice(5).trim();
          if (!data || data === "[DONE]") continue;
          try {
            const ev = JSON.parse(data);
            if (ev.type === "response.output_text.delta") text += ev.delta ?? "";
            if (ev.type === "response.refusal.delta" || ev.type === "response.refusal.done") refused = true;
            if (ev.type === "error" || ev.type === "response.failed") return { ok: false, status: 500 };
          } catch { /* partial */ }
        }
      }
    }
    if (refused || !text.trim()) return { ok: false, status: 200 };
    const messages = text.split(/\n\s*---\s*\n/).map((s) => s.trim()).filter(Boolean).slice(0, 6);
    return { ok: true, messages };
  } catch {
    return { ok: false, status: 0 };
  }
}
