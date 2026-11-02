package com.sbo.app.nnn

import kotlin.random.Random

/** What Coach Zero yells while you dig through the protections (red stamps + chat bubbles you can't answer). */
object NnnPhrases {
    val stamps = listOf(
        "POTEVI FARCELA", "FERMATI", "MA SEI SERIO?", "NON FARLO", "COME HAI FATTO A ARRIVARE QUI?",
        "TORNA INDIETRO", "DISCIPLINA, CAZZO", "NO. NO. NO.", "PENSACI", "NE VALE LA PENA?",
        "STAI BUTTANDO TUTTO", "VERGOGNA", "IL GRUPPO TI GUARDA", "SEI PIÙ FORTE DI COSÌ", "RESPIRA",
        "MOLLA IL TELEFONO", "VAI A FARE FLESSIONI", "ULTIMA OCCASIONE", "CHE DELUSIONE", "SVEGLIATI",
        "NON È UN GIOCO", "HAI GIURATO", "TRENTA GIORNI", "ANCORA?!", "SMETTILA",
        "NESSUNA SCUSA", "DOCCIA FREDDA. ORA.", "POSA QUEL DITO", "PROPRIO TU?", "CI CREDEVO IN TE",
        "STOP", "RIDICOLO", "PENSA A DICEMBRE", "LA MEDAGLIA…", "NON SEI UN CAMPIONE COSÌ",
    )
    val coach = listOf(
        "Che stai facendo? Ti ho visto. Posa quel dito.",
        "Stai smontando tutto pezzo per pezzo. Impegno vero, peccato che sia nella direzione sbagliata.",
        "Fai 20 flessioni. Adesso. Poi ne riparliamo.",
        "Ogni strato che togli è una scusa in più. Lo sai, vero?",
        "Guarda quanta roba ho messo. L'ho fatto per te.",
        "Se arrivi in fondo lo sa tutto il gruppo. Tutti.",
        "Non rispondermi, tanto non puoi. Ascolta e basta.",
        "Esci da questa schermata e vai a farti una doccia fredda.",
        "Pensa a come ti sentirai fra cinque minuti. Male. Ecco come.",
        "Sei davvero così debole? Non ci credo. Dimostramelo.",
        "Hai resistito fin qui per buttare tutto per un pulsante?",
        "Più premi, più mi incazzo. Continua pure.",
        "Il me di dicembre ti sta già guardando male.",
        "Chiudi l'app. Ora. Non è un consiglio.",
        "Stai sudando su una cassaforte per perdere una sfida. Complimenti.",
        "Ti ricordi cosa mi avevi promesso il primo novembre?",
        "Ogni chiodo che togli è una promessa che rompi.",
        "Ancora uno strato e ancora uno. Non ti stanchi mai di fallire?",
    )

    fun stamp(r: Random) = stamps[r.nextInt(stamps.size)]
    fun coachLine(r: Random) = coach[r.nextInt(coach.size)]

    /** Coach Zero's reaction to a fall (same wording as the server, used for previews and right after the press). */
    private fun giorni(n: Int) = if (n == 1) "1 giorno" else "$n giorni"

    fun reaction(lasted: Int): List<String> = when {
        lasted < 5 -> listOf(
            if (lasted == 0) "NO. NON CI CREDO. IL PRIMO GIORNO. NEANCHE UN GIORNO E HAI MOLLATO." else "NO. NON CI CREDO. ${giorni(lasted).uppercase()}. ${giorni(lasted).uppercase()} E HAI MOLLATO.",
            "MA TI RENDI CONTO? NEANCHE UNA SETTIMANA. NEANCHE CINQUE GIORNI.",
            "TUTTE QUELLE PROTEZIONI. TRAVI, LUCCHETTI, CASSEFORTI. E TU LE HAI SMONTATE UNA PER UNA PUR DI PREMERE.",
            "NON VOGLIO SENTIRE SCUSE. ZERO. LA PROSSIMA VOLTA VOGLIO VEDERE LA SCHIENA DRITTA, NON QUESTA ROBA.",
        )
        lasted < 15 -> listOf(
            "${giorni(lasted).replaceFirstChar { it.uppercase() }}. Non zero, ma neanche lontanamente quello che ti avevo chiesto.",
            "Hai tenuto botta per un po' e poi hai mollato sul più bello. Sono incazzato, sì.",
            "Ripartiamo. Ma la prossima volta voglio vedere il doppio di questi giorni.",
        )
        lasted < 29 -> listOf(
            "${giorni(lasted).replaceFirstChar { it.uppercase() }}. Non te lo dico volentieri, ma sei stato bravo.",
            "C'eri quasi, cazzo. Un altro po' e ti stringevo la mano.",
            "L'anno prossimo arrivi in fondo. Me lo segno.",
        )
        else -> listOf(
            "NO. NO. NO. ${giorni(lasted).uppercase()}. MANCAVA UN GIORNO. UNO.",
            "Avevi la medaglia in mano e l'hai buttata nel cesso all'ultimo metro.",
            "Sono fiero di quanto sei arrivato lontano e incazzato nero per come è finita. Tutte e due le cose insieme.",
            "Ricordatelo il prossimo novembre: l'ultimo giorno è quello che conta.",
        )
    }

    val kickoff = listOf(
        "È il primo novembre. Lo sai cosa vuol dire: NNN. Trenta giorni, zero eventi. Nessuna eccezione.",
        "Ho blindato il pulsante. Travi, lucchetti, casseforti, nastro della polizia. Se lo smonti, lo vedo. E lo vede tutto il gruppo.",
        "Io credo in te, sul serio. Apri l'app per controllare i numeri o per parlare con me, mai per quel pulsante. Arriva fino in fondo e a dicembre facciamo festa.",
    )
    fun victory(year: Int) = listOf(
        "CE L'HAI FATTA. TRENTA GIORNI. NON HO PAROLE, E IO HO SEMPRE PAROLE.",
        "Hai guardato quel pulsante blindato per un mese intero e non l'hai toccato. Questa è disciplina vera, non chiacchiere.",
        "La medaglia e il titolo \"Vincitore NNN $year\" sono tuoi. Mettili in bella vista: te li sei guadagnati. Sono fiero di te, campione.",
    )
    /** Whispered next to the button when you stop tapping: letter by letter, dim, close. */
    val whisper = listOf("premilo…", "nessuno lo saprà…", "solo un attimo…", "dai…", "te lo meriti…", "ancora uno strato…", "chi vuoi prendere in giro…")
    val whisperFinal = listOf("ora…", "è lì. nudo.", "un tocco solo…", "lo vuoi. lo sai.", "premi…")

    val reminders = listOf(
        "Apri SBO per controllare i numeri o parlare con Coach Zero. Non per aggiungere eventi.",
        "Se premi quel pulsante, lo sa tutto il gruppo.",
        "Ogni giorno pulito è un passo verso la medaglia leggendaria.",
    )
}
