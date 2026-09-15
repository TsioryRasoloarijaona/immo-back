package com.tsix.immocrm.services.sequence;

import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Numérotation des entités créées par l'API, sur les <b>mêmes compteurs</b> que le logiciel de
 * bureau ({@code AdmSequence}).
 *
 * <p>Extrait parce qu'il y en avait déjà deux copies — {@code DossierService} et
 * {@code ProspectionService} — et que le prospect en réclamait une troisième. Les trois partagent
 * le même mécanisme, relevé dans la trace d'audit d'OMRAN : incrémenter {@code SeqValue} de la
 * ligne, puis formater {@code année/numéro} avec les largeurs que la ligne porte elle-même
 * ({@code SeqYearNumber}, {@code SeqValueNumber}).
 *
 * <h2>Le compteur est partagé avec OMRAN, et c'est voulu</h2>
 *
 * <p>Nous incrémentons la <b>même ligne</b> que le logiciel de bureau, pas un compteur parallèle :
 * les deux applications écrivent dans {@code yesRems} et une numérotation dédoublée produirait des
 * codes en collision. C'est aussi pourquoi l'incrément et la lecture doivent rester dans la même
 * transaction que l'écriture qui les consomme.
 *
 * <h2>Le cas « la ligne n'existe pas »</h2>
 *
 * <p>Amorcer à zéro plutôt qu'échouer. Une base de test fraîche ne sème pas {@code AdmSequence} ;
 * refuser de créer un prospect pour cette raison transformerait une absence de configuration en
 * panne fonctionnelle.
 */
@Service
public class GenerateurCodeSequence {

    /** {@code AdmSequence.Code} du compteur des tiers — prospects et clients confondus. */
    public static final String SEQUENCE_TIER = "Tier";

    /** {@code AdmSequence.Code} du compteur des prospections. */
    public static final String SEQUENCE_PROSPECTION = "Prospection";

    /** {@code AdmSequence.Code} du compteur des dossiers de vente. */
    public static final String SEQUENCE_DOSSIER = "Dossier de vente";

    private final AdmSequenceRepository sequenceRepository;

    public GenerateurCodeSequence(AdmSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Le code suivant de ce compteur, au format {@code "{année}/{numéro}"}.
     *
     * <p>Exemples réels, tous deux vérifiés en base : {@code "26/14431"} pour un tiers
     * ({@code SeqYearNumber = 2}), {@code "26/05935"} pour une prospection. Le dossier de vente
     * utilise une année sur 4 chiffres et préfixe le résultat par le code du projet — ce préfixe
     * reste à la charge de l'appelant, il dépend de l'article et non du compteur.
     *
     * @param codeSequence       {@code AdmSequence.Code} de la ligne à incrémenter
     * @param largeurAnneeDefaut largeur de l'année si la ligne ne la porte pas
     * @param largeurNumeroDefaut largeur du numéro si la ligne ne la porte pas
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

        // Année système, jamais AdmSequence.SeqYear : la ligne réelle porte encore 2021 alors que
        // les codes émis en 2026 commencent par « 26 ». C'est BasedOnSystemYear qui tranche, et il
        // vaut 1 sur les trois compteurs.
        int annee = LocalDate.now().getYear() % (int) Math.pow(10, largeurAnnee);
        return String.format("%0" + largeurAnnee + "d", annee)
                + "/" + String.format("%0" + largeurNumero + "d", prochain);
    }

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
