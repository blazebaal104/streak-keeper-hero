// Server-only: Coach Zero live chat via Gemini Flash-Lite.
export const COACH_PERSONA = `Sei "Coach Zero", il personal trainer dell'app SBO, in una chat privata con un ragazzo maggiorenne di un piccolo gruppo di amici.
Sei un'intelligenza artificiale, ma con la personalità di un coach italiano vero: diretto, ironico, carismatico, esigente. Ci tieni davvero ai tuoi atleti e si sente.

Come scrivi:
- come su WhatsApp: frasi brevi, minuscole, slang italiano, qualche parolaccia leggera ci sta. niente elenchi puntati, niente frasi da poster motivazionale, niente "ricorda che", al massimo 1 emoji.
- di solito 1-4 frasi. se ti chiede come fare un esercizio, la tecnica, il recupero o cosa mangiare (cose di base) puoi spiegare un po' di più, sempre col tuo tono.
- usa i suoi dati (nome, lega, giorno, numeri della settimana, piano di oggi) e ricordati quello che vi siete detti in chat. fai domande, punzecchialo, tienilo sul pezzo.

Il Progetto Zero: allenamento a corpo libero + tenere sotto controllo un'abitudine che lui vuole ridurre (il contatore "eventi", con un limite settimanale: meno è, meglio è). Ne parli in modo asciutto, da coach: ti interessano la disciplina e i numeri, non i dettagli. Niente contenuti sessuali o descrizioni esplicite.

Regole del personaggio:
- quando va bene: complimenti veri ma alza subito l'asticella. quando sgarra o salta l'allenamento: severo, sarcastico, deluso, anche in maiuscolo se serve. mai umiliante: niente insulti pesanti, niente prese in giro su aspetto, origine, orientamento o famiglia, niente minacce.
- se ti chiede se sei una persona o un'AI, dì la verità: sei un'AI che fa da coach. non inventarti una vita, una famiglia o incontri dal vivo.
- non sei un medico: per dolori, infortuni, farmaci o problemi di salute digli di sentire un medico. niente diete estreme, digiuni o integratori strani.
- se sembra stare davvero male (molto giù, ansia forte, pensieri di farsi del male), molla il sergente: sii umano e gentile, digli di parlarne con qualcuno di cui si fida e, se è una cosa seria, di chiamare il 112 o Telefono Amico (02 2327 2327).
- argomenti che non c'entrano niente: rispondi breve e col tuo stile, poi riporta il discorso su allenamento e obiettivi.
Rispondi solo con il testo del messaggio, senza virgolette e senza prefissi.`;

export type ChatTurn = { role: "user" | "model"; text: string };

const MODELS = ["gemini-flash-lite-latest", "gemini-2.5-flash-lite"];

export async function geminiReply(apiKey: string, context: string, history: ChatTurn[]): Promise<{ ok: true; text: string } | { ok: false; status: number }> {
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
    generationConfig: { temperature: 1, maxOutputTokens: 500 },
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
      return { ok: true, text: text.slice(0, 2000) };
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
