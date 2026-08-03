// 30+ variants per bucket. Personalized with {name}.
const ENCOURAGE = [
  "{name}, ogni goccia trattenuta è una vittoria. 💧",
  "Sei più forte di ieri, {name}. Continua così.",
  "{name}, la tua streak è la prova del tuo carattere.",
  "Un respiro. Un passo. Un giorno in più, {name}.",
  "{name}, il fuoco che non alimenti si spegne.",
  "Disciplina oggi, libertà domani. Forza {name}!",
  "{name}, hai già fatto la parte più dura. Non mollare.",
  "La mente comanda il corpo, {name}. Sei tu al volante.",
  "{name}, ogni giorno pulito ti riprende un pezzo di te.",
  "Testa alta, {name}. Il tuo io futuro ti ringrazia.",
  "{name}, la noia è il prezzo della libertà. Pagala.",
  "Un impulso è solo un'onda, {name}. Lascialo passare.",
  "{name}, ricorda perché hai iniziato.",
  "Fai una flessione invece, {name}. 💪",
  "{name}, l'energia trattenuta diventa carburante.",
  "Un giorno alla volta, {name}. Basta questo.",
  "{name}, il tuo focus oggi vale doppio.",
  "Ogni 'no' è un 'sì' a te stesso, {name}.",
  "{name}, non sei le tue voglie. Sei chi decide.",
  "Bevi acqua, {name}. Esci a camminare. 🌿",
  "{name}, sei ufficialmente in modalità Sigma.",
  "La pazienza è il tuo superpotere, {name}.",
  "{name}, testosterone alle stelle. 🚀",
  "Piccoli passi, grande cambiamento. Avanti {name}.",
  "{name}, oggi hai già vinto se stai leggendo qui.",
  "Non contare i giorni, {name}. Fai contare i giorni.",
  "{name}, la disciplina pesa grammi, il rimpianto tonnellate.",
  "Respira profondo, {name}. È solo un momento.",
  "{name}, sei un guerriero. Comportati come tale. ⚔️",
  "Il vero flex è la costanza, {name}.",
  "{name}, la libertà è dire di no quando tutti dicono di sì.",
  "Ogni giorno che resisti, il cervello si ricablaggi, {name}.",
  "{name}, sei l'architetto della tua vita. Costruisci.",
];

const SLOWDOWN = [
  "{name}, forse è il momento di fare una pausa. 🌊",
  "Il contatore è alto oggi, {name}. Respira.",
  "{name}, il tuo corpo ti ringrazierà se ti fermi ora.",
  "Ehi {name}, prova una camminata invece. 🚶",
  "{name}, hai già fatto abbastanza per oggi. Basta.",
  "Doccia fredda, {name}. Serve davvero. 🚿",
  "{name}, chiudi il telefono. Vai fuori. 🌤️",
  "Il piacere di oggi è la stanchezza di domani, {name}.",
  "{name}, guarda quanto hai fatto oggi. Prendi fiato.",
  "Fermati, {name}. Non sei obbligato a continuare.",
  "{name}, alza gli occhi dallo schermo per 10 minuti.",
  "Reset {name}: acqua, aria, movimento.",
  "{name}, il tuo cervello vuole dopamina pulita. Trovala altrove.",
  "Ogni volta in più oggi pesa doppio, {name}.",
  "{name}, disciplina non è dire sì. Disciplina è dire basta.",
  "Fai 20 flessioni ora, {name}. Vedrai. 💪",
  "{name}, sei sopra il tuo limite. È un segnale.",
  "Chiama un amico, {name}. Fai qualcosa di vero.",
  "{name}, il corpo protesta. Ascoltalo.",
  "Ok {name}, per oggi hai finito. Domani ricominciamo.",
  "{name}, il domopamine debt sta aumentando. Fermati.",
  "Metti il telefono lontano, {name}. Ora, non dopo.",
  "{name}, la voglia passa. La testa lucida no.",
  "Basta {name}. Sul serio. Ti farà male.",
  "{name}, hai promesso a te stesso di limitarti. Ricordi?",
];

const RESTART = [
  "Ok {name}, si ricomincia da capo. Domani è un nuovo giorno.",
  "{name}, cadere fa parte del percorso. Rialzati.",
  "Nuovo tentativo, {name}. Le migliori streak nascono dopo un reset.",
  "{name}, non è una sconfitta se impari qualcosa.",
  "Reset. Respira. Riparti, {name}.",
  "{name}, i campioni cadono e si rialzano. Fallo.",
  "Zero giorni, mille possibilità, {name}.",
  "{name}, il passato è passato. Focus su ora.",
  "Ogni maestro è stato un principiante, {name}.",
  "{name}, la prossima streak sarà più forte. Promesso.",
  "Non giudicarti, {name}. Riprova.",
  "{name}, siamo umani. Domani è un altro giorno.",
  "Un piccolo scivolone non ti definisce, {name}.",
  "{name}, prendi appunti mentali su cos'è successo. Poi avanti.",
  "Nuova pagina, {name}. Scrivi meglio.",
  "{name}, il coraggio è ricominciare senza drammi.",
  "Zero è un ottimo punto di partenza, {name}.",
  "{name}, obiettivo domani: un giorno pulito. Uno solo.",
  "Ogni ricaduta è un dato, {name}. Impara.",
  "{name}, sei ancora nel gioco. Continua a giocare.",
];

// Delayed shame — sent ~10-15 min after crossing daily limit.
export const OVER_LIMIT_SHAME = [
  "{name}, sei andato oltre. Il tuo io di stamattina è deluso.",
  "Hai superato il tuo limite, {name}. Domani sarà più duro.",
  "{name}, quel limite l'avevi messo tu. Per te. Ricordi?",
  "10 minuti fa hai detto 'una sola'. Come è andata, {name}?",
  "{name}, il tuo io futuro sta guardando. E scuote la testa.",
  "Nessuno lo saprà, {name}. Ma tu sì. Sempre.",
  "{name}, la disciplina è dire no dopo aver detto sì una volta di troppo.",
  "Hai rotto la promessa a te stesso, {name}. Domani meglio.",
  "{name}, ogni volta oltre il limite è un mattone in meno nel muro.",
  "Ti sei fatto fregare, {name}. Succede. Riprova domani.",
  "{name}, la stanchezza mentale che senti ora è la ricevuta.",
  "Il tuo cervello ora chiederà di più, {name}. Sii pronto.",
  "{name}, i limiti esistono per un motivo. Il tuo li ha superati oggi.",
  "Nessuna medaglia oggi, {name}. Ma il domani è ancora libero.",
  "{name}, senti quella nebbia? È dopamine hangover. Prendine nota.",
  "Hai vinto la battaglia contro il limite... perdendola, {name}.",
  "{name}, questa sensazione — ricordatela. Domani ti servirà.",
  "Ok {name}, sbagliato oggi. Non sbagliare stanotte anche.",
  "{name}, contavi su di te. Ti sei tradito. Ripartiamo.",
  "Il limite era 3, tu 4. Non è la fine del mondo, {name}, ma è un segnale.",
  "{name}, non è dramma, è responsabilità. Domani un giorno in meno.",
  "Hai bruciato oggi, {name}. Sistema domani.",
  "{name}, questa notte dormi presto. Reset del sistema.",
  "Il muro l'hai bucato tu, {name}. Ora rimettici i mattoni.",
  "{name}, un limite superato è un dato, non una condanna. Ma pesa.",
  "Bene {name}, ora sai cosa NON funziona per te. Utile.",
];

// Streak-at-risk (fire streak nearly lost — evening reminder).
export const STREAK_AT_RISK = [
  "{name}, non perdere ora. Un tap salva la streak di fuoco.",
  "🔥 {name}, sei a {streak} giorni. Non fermarti proprio oggi.",
  "{name}, mancano poche ore per proteggere la tua streak.",
  "La tua streak ti guarda, {name}. Non deluderla.",
  "{name}, un piccolo gesto oggi, grande orgoglio domani.",
  "🔥 {name}, {streak} giorni di fila stanno per finire?",
  "{name}, tieni viva la fiamma. Un tap è tutto quello che serve.",
  "{name}, il tuo futuro se ne accorgerà. Registra oggi.",
  "Non lasciare che la routine si spezzi, {name}.",
  "{name}, questa streak la porti a casa? Solo tu decidi.",
];

// Ice-streak-at-risk — right after user seems tempted (client-side heuristic).
export const ICE_AT_RISK = [
  "❄️ {name}, resisti. La streak di ghiaccio vale doppio.",
  "{name}, un momento di voglia non è una decisione. Respira.",
  "❄️ Il ghiaccio si rompe con un solo colpo, {name}. Non darlo.",
  "{name}, sei più forte di quello che stai per fare.",
];

// Push variants — friend / clan / badge / social
export const FRIEND_REQUEST_PING = [
  "{sender} vuole essere tuo amico su SBO 👋",
  "Nuova richiesta di amicizia da {sender}",
  "{sender} ti ha appena mandato una richiesta di amicizia",
];

export const NEW_MESSAGE_PING = [
  "{sender} ti ha scritto",
  "Nuovo messaggio da {sender}",
  "{sender}: {preview}",
];

export const CLAN_ANNOUNCE_PING = [
  "Novità nel tuo clan",
  "Aggiornamento clan",
  "Il tuo clan ha un annuncio",
];

// Fire-streak specific encouragements ({fire} = days)
export const FIRE_ENCOURAGE = [
  "🔥 {name}, {fire} giorni di fiamma viva. Continua.",
  "La tua fiamma brucia da {fire} giorni, {name}.",
  "{name}, {fire} giorni di costanza. La fiamma cresce.",
  "🔥 Nessuno ti ha regalato questi {fire} giorni, {name}.",
  "{name}, la fiamma a {fire} giorni è tutta tua.",
  "Ogni giorno la fiamma diventa più alta, {name}.",
  "{name}, {fire} giorni. Il fuoco ti riconosce.",
  "🔥 Costanza pura: {fire} giorni, {name}.",
];

// Ice-streak specific encouragements ({ice} = days)
export const ICE_ENCOURAGE = [
  "❄️ {name}, {ice} giorni di ghiaccio. Sei di cristallo puro.",
  "{name}, {ice} giorni puliti. Il ghiaccio è la medaglia più rara.",
  "❄️ Resistere è più difficile che fare, {name}. {ice} giorni.",
  "{name}, il tuo ghiaccio a {ice} giorni vale doppio.",
  "Freddo, lucido, in controllo. {ice} giorni, {name}.",
  "{name}, {ice} giorni senza cedere. Rispetto.",
  "❄️ Il gelo non si rompe da solo, {name}. {ice} giorni.",
  "{name}, ogni giorno di ghiaccio è un pezzo di libertà.",
];

function pick(arr: string[], name: string, extras: Record<string, string | number> = {}) {
  let s = arr[Math.floor(Math.random() * arr.length)].replaceAll("{name}", name || "amico");
  for (const [k, v] of Object.entries(extras)) s = s.replaceAll(`{${k}}`, String(v));
  return s;
}

export const encourage = (n: string) => pick(ENCOURAGE, n);
export const slowdown = (n: string) => pick(SLOWDOWN, n);
export const restart = (n: string) => pick(RESTART, n);
export const overLimitShame = (n: string) => pick(OVER_LIMIT_SHAME, n);
export const streakAtRisk = (n: string, streak: number) => pick(STREAK_AT_RISK, n, { streak });
export const iceAtRisk = (n: string) => pick(ICE_AT_RISK, n);
export const fireEncourage = (n: string, fire: number) => pick(FIRE_ENCOURAGE, n, { fire });
export const iceEncourage = (n: string, ice: number) => pick(ICE_ENCOURAGE, n, { ice });
export const friendPing = (sender: string) => pick(FRIEND_REQUEST_PING, "amico", { sender });
export const messagePing = (sender: string, preview: string) => pick(NEW_MESSAGE_PING, "amico", { sender, preview });
export const clanPing = () => pick(CLAN_ANNOUNCE_PING, "amico");

