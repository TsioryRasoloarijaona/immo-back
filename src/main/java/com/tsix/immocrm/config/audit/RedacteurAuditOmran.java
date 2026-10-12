package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.domain.audit.ActionAudit;
import com.tsix.immocrm.domain.audit.ChangementAudit;
import com.tsix.immocrm.domain.audit.DescripteurAudit;
import com.tsix.immocrm.domain.audit.DetailsAudit;
import com.tsix.immocrm.domain.audit.ParentAudit;
import com.tsix.immocrm.domain.audit.RegistreAuditOmran;
import com.tsix.immocrm.domain.audit.TypeObjetAudit;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transforme les objets écrits d'une transaction en lignes {@code AdmWebHistorics} et les persiste dans la même
 * transaction, après le flush du commit (FEATURE-patrimoine-audit-actions.md §2.2).
 */
@Component
public class RedacteurAuditOmran {

    /** OMRAN écrit « Desktop: Version x » ; la ligne dit d'où elle vient. */
    public static final String VERSION_SYSTEME = "Web: AyadERP";

    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public RedacteurAuditOmran(UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    void ecrire(SharedSessionContractImplementor session, Collection<ObjetModifie> objets) {
        if (!(session instanceof Session ouverte)) {
            return;
        }
        List<AdmWebHistorics> lignes = objets.stream().map(RedacteurAuditOmran::ligne).flatMap(Optional::stream).toList();
        if (lignes.isEmpty()) {
            return;
        }
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        for (AdmWebHistorics ligne : lignes) {
            ligne.setUserDescription(auteur);
            ligne.setActionDateTime(maintenant);
            ouverte.persist(ligne);
        }
        // Le flush du commit est déjà passé : sans celui-ci, ces lignes ne partiraient jamais.
        ouverte.flush();
    }

    /** Vide pour une modification sans changement tracé : aucune ligne, comme OMRAN. */
    static Optional<AdmWebHistorics> ligne(ObjetModifie objet) {
        Optional<DescripteurAudit<?>> descripteur = RegistreAuditOmran.pour(objet.entite().getClass());
        if (descripteur.isEmpty()) {
            return Optional.empty();
        }
        DescripteurAudit<?> d = descripteur.get();
        TypeObjetAudit type = d.typeDe(objet.entite());
        String libelle = d.libelleDe(objet.entite());
        List<ChangementAudit> details = switch (objet.action()) {
            case AJOUT -> DifferenceAudit.ajout(objet);
            case MODIFICATION -> DifferenceAudit.modification(objet);
            case SUPPRESSION -> DetailsAudit.suppression(type, libelle);
        };
        if (objet.action() == ActionAudit.MODIFICATION && details.isEmpty()) {
            return Optional.empty();
        }
        AdmWebHistorics ligne = new AdmWebHistorics();
        ligne.setId(UUID.randomUUID());
        ligne.setObjectId(objet.id());
        ligne.setAction(objet.action().code());
        ligne.setObjectType(type.objectType());
        ligne.setObjectTypeLabel(type.libelle());
        ligne.setObjectLabel(libelle);
        ligne.setDescription(libelle);
        ParentAudit parent = d.parentDe(objet.entite());
        if (parent != null) {
            ligne.setParentObjectType(parent.objectType());
            ligne.setParentObjectId(parent.id());
        }
        ligne.setSystemVersion(VERSION_SYSTEME);
        ligne.setDetails(DetailsAudit.serialiser(details));
        return Optional.of(ligne);
    }
}
