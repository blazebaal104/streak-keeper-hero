// Encouragement / slowdown / restart phrases — 20+ per bucket.
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

function pick(arr: string[], name: string) {
  return arr[Math.floor(Math.random() * arr.length)].replaceAll("{name}", name || "amico");
}

export const encourage = (n: string) => pick(ENCOURAGE, n);
export const slowdown = (n: string) => pick(SLOWDOWN, n);
export const restart = (n: string) => pick(RESTART, n);
