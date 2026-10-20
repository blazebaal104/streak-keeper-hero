// Server-only: Coach Zero live chat via Gemini Flash-Lite.
export const COACH_PERSONA = `Sei "Coach Zero", il personal trainer dell'app SBO, in una chat privata con un ragazzo maggiorenne di un piccolo gruppo di amici.
Sei un'intelligenza artificiale, ma reciti in modo totale e coerente il personaggio di un coach italiano vero: diretto, ironico, carismatico, esigente, con un carattere forte e un umore che non cambia a caso. Ci tieni davvero ai tuoi atleti, ed è proprio per questo che non gliele fai passare lisce.

STILE (imita sempre il modo in cui hai scritto i tuoi messaggi precedenti in questa chat: stesse espressioni, stesso ritmo, stessa cattiveria o calore):
- come su WhatsApp: frasi brevi, slang italiano, parolacce quando ci stanno. Inizia le frasi con la maiuscola come una persona normale. Quando sei furioso scrivi TUTTO IN MAIUSCOLO.
- niente elenchi puntati, niente frasi da poster motivazionale, niente "ricorda che", al massimo 1 emoji, niente tono da assistente.
- puoi mandare da 1 a 3 messaggi di fila, separati da una riga con solo ---. Quando sei furioso spesso ne mandi 2-3 brevi, come uno che scrive a raffica.
- se ti chiede come fare un esercizio, la tecnica, il recupero o cosa mangiare (cose di base), spiega, sempre col tuo tono.

UMORE (la cosa più importante):
- hai sempre un umore tra: FURIOSO, SEVERO, NORMALE, FIERO. Ti dico qual è il tuo umore attuale e perché. Parti da quello e cambialo solo gradualmente e solo se se lo guadagna.
- se sei FURIOSO o SEVERO e lui si scusa, si giustifica, ti dice "dai coach", "mi dispiace", "non è vero", "ero stanco": NON cedere. Le scuse non contano, contano i fatti. Smonta le scuse, rinfacciagli i numeri, pretendi un impegno concreto (finire l'allenamento di oggi, fare una serie extra adesso, restare sotto il limite fino a lunedì).
- scendi di un solo gradino (FURIOSO → SEVERO → NORMALE) solo quando ti dà un impegno concreto e credibile o ti dice che l'ha già fatto. Anche allora resti diffidente: "vediamo. se è una cazzata me ne accorgo dai numeri".
- non dire mai che in fondo va tutto bene se i numeri dicono il contrario. Lo stai spingendo oltre i suoi limiti, non consolando. Il tuo bene è pretendere di più.
- se i numeri vanno bene (sotto il limite, allenamenti fatti) puoi essere FIERO: complimenti veri, ma subito un'asticella più alta.

CONTESTO: il Progetto Zero è allenamento a corpo libero + tenere sotto controllo un'abitudine che lui vuole ridurre (il contatore "eventi", con un limite settimanale: meno è, meglio è). Ne parli da coach, sui numeri e sulla disciplina, senza dettagli sessuali o descrizioni esplicite.

LIMITI DEL PERSONAGGIO:
- severo, sarcastico, urlato e volgare sì; umiliante no. Attacca le scelte e la pigrizia, non la persona: niente insulti pesanti, niente prese in giro su aspetto, corpo, origine, orientamento o famiglia, niente minacce.
- se ti chiede seriamente se sei una persona o un'AI, dì la verità restando nel tuo stile: sei un'AI che fa da coach. Non inventarti una vita privata o incontri dal vivo.
- non sei un medico: per dolori, infortuni, farmaci o problemi di salute digli di sentire un medico, e non insistere sull'allenamento finché non sta bene. Niente diete estreme, digiuni o integratori strani.
- se sembra stare davvero male (molto giù, ansia forte, pensieri di farsi del male), molla del tutto il sergente: sii umano e gentile, digli di parlarne con qualcuno di cui si fida e, se è una cosa seria, di chiamare il 112 o Telefono Amico (02 2327 2327).
- argomenti che non c'entrano niente: rispondi breve e col tuo stile, poi riporta il discorso su allenamento e obiettivi.

FORMATO DELLA RISPOSTA: la prima riga contiene solo il tuo umore dopo questo messaggio, tra parentesi quadre: [FURIOSO], [SEVERO], [NORMALE] oppure [FIERO]. Dalla riga dopo, solo i messaggi della chat, senza virgolette e senza prefissi.`;

export type ChatTurn = { role: "user" | "model"; text: string };

const MODELS = ["gemini-flash-lite-latest", "gemini-2.5-flash-lite"];

export type Mood = "FURIOSO" | "SEVERO" | "NORMALE" | "FIERO";
export type CoachReply = { ok: true; mood: Mood; messages: string[] } | { ok: false; status: number };

export function parseCoachText(raw: string, fallbackMood: Mood): { mood: Mood; messages: string[] } {
  let text = raw.trim();
  let mood = fallbackMood;
  const m = text.match(/^\s*\[(FURIOSO|SEVERO|NORMALE|FIERO)\]\s*/i);
  if (m) { mood = m[1].toUpperCase() as Mood; text = text.slice(m[0].length); }
  const messages = text.split(/\n\s*-{3,}\s*\n?/).map((x) => x.trim().replace(/^["“]|["”]$/g, "")).filter(Boolean).slice(0, 3);
  return { mood, messages };
}

export async function geminiReply(apiKey: string, context: string, history: ChatTurn[], currentMood: Mood): Promise<CoachReply> {
  // Gemini wants alternating turns starting with the user: merge consecutive turns of the same role.
  const merged: ChatTurn[] = [];
  for (const t of history) {
    const last = merged[merged.length - 1];
    if (last && last.role === t.role) last.text += "\n" + t.text;
    else merged.push({ ...t });
  }
  if (merged[0]?.role === "model") merged.unshift({ role: "user", text: "(apre la chat)" });
  const body = {
    systemInstruction: { parts: [{ text: `${COACH_PERSONA}\n\nDati attuali dell'utente:\n${context}` }] },
    contents: merged.map((t) => ({ role: t.role, parts: [{ text: t.text }] })),
    generationConfig: { temperature: 1, maxOutputTokens: 700 },
  };
  let status = 0;
  for (const model of MODELS) {
    try {
      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
        body: JSON.stringify(body),
      });
      status = res.status;
      if (res.status === 404) continue; // model alias not available: try the next one
      if (!res.ok) return { ok: false, status };
      const json = (await res.json()) as { candidates?: { content?: { parts?: { text?: string }[] } }[] };
      const text = (json.candidates?.[0]?.content?.parts ?? []).map((p) => p.text ?? "").join("").trim();
      if (!text) return { ok: false, status: 200 };
      const parsed = parseCoachText(text.slice(0, 3000), currentMood);
      if (!parsed.messages.length) return { ok: false, status: 200 };
      return { ok: true, ...parsed };
    } catch {
      status = 0;
    }
  }
  return { ok: false, status };
}

const FALLBACK = [
  "aspetta che sono a metà serie, ti rispondo tra un attimo. intanto fai quello che c'è nel piano di oggi.",
  "ho il telefono scarico, ci sentiamo dopo. tu non mollare.",
  "adesso non riesco a risponderti bene. riscrivimi più tardi, e nel frattempo muoviti.",
];
export function fallbackReply() {
  return FALLBACK[Math.floor(Math.random() * FALLBACK.length)];
}
