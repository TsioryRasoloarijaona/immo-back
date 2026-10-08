package com.tsix.immocrm.services.sequence;

import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Numérotation sur la <b>même ligne</b> {@code AdmSequence} que le logiciel de bureau (sinon codes en
 * collision) ; l'incrément reste dans la transaction de l'écriture qui le consomme.
 */
@Service
public class GenerateurCodeSequence {

    /** {@code AdmSequence.Code} du compteur des tiers — prospects et clients confondus. */
    public static final String SEQUENCE_TIER = "Tier";

    public static final String SEQUENCE_PROSPECTION = "Prospection";

    public static final String SEQUENCE_DOSSIER = "Dossier de vente";

    private final AdmSequenceRepository sequenceRepository;

    public GenerateurCodeSequence(AdmSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Code {@code "{année}/{numéro}"} suivant, ex. {@code "26/14431"}. Le préfixe projet du dossier
     * de vente reste à la charge de l'appelant.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String suivant(String codeSequence, int largeurAnneeDefaut, int largeurNumeroDefaut) {
        AdmSequence sequence = sequenceRepository.findByCode(codeSequence)
                .orElseGet(() -> amorcer(codeSequence, largeurAnneeDefaut, largeurNumeroDefaut));

        int prochain = (sequence.getSeqValue() != null ? sequence.getSeqValue() : 0) + 1;
        sequence.setSeqValue(prochain);
        sequenceRepository.save(sequence);

        int largeurAnnee = sequence.getSeqYearNumber() != null ? sequence.getSeqYearNumber() : largeurAnneeDefaut;
        int largeurNumero = sequence.getSeqValueNumber() != null ? sequence.getSeqValueNumber() : largeurNumeroDefaut;

        // Année système, jamais AdmSequence.SeqYear, resté figé sur la ligne réelle :
        // BasedOnSystemYear vaut 1 sur les trois compteurs.
        int annee = LocalDate.now().getYear() % (int) Math.pow(10, largeurAnnee);
        return String.format("%0" + largeurAnnee + "d", annee)
                + "/" + String.format("%0" + largeurNumero + "d", prochain);
    }

    // Amorcer à zéro plutôt qu'échouer : une base de test fraîche ne sème pas AdmSequence.
    private static AdmSequence amorcer(String codeSequence, int largeurAnnee, int largeurNumero) {
        AdmSequence nouvelle = new AdmSequence();
        nouvelle.setId(UUID.randomUUID());
        nouvelle.setCode(codeSequence);
        nouvelle.setSeqValue(0);
        nouvelle.setSeqValueNumber(largeurNumero);
        nouvelle.setSeqYearNumber(largeurAnnee);
        nouvelle.setBasedOnSystemYear(true);
        return nouvelle;
    }
}
