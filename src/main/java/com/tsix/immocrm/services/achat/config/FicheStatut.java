package com.tsix.immocrm.services.achat.config;

import com.tsix.immocrm.dto.achat.config.FicheStatutSaisie;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import jakarta.persistence.EntityManager;

import java.util.UUID;

/** Saisie contrôlée (400) d'une fiche statut, avant toute écriture ; {@code requeteType} compte le type par {@code :id}. */
public record FicheStatut(Integer ordre, String code, String description, UUID type, String couleur, String notes) {

    private static final int NOTES = 1000;

    public static FicheStatut lire(EntityManager em, FicheStatutSaisie d, String requeteType) {
        Integer ordre = exiger(d.ordre(), "Ordre");
        String code = SaisiePatrimoine.obligatoire(SaisiePatrimoine.sansBlancsAuxBords(d.code()), null, "Code");
        String description = SaisiePatrimoine.obligatoire(SaisiePatrimoine.sansBlancsAuxBords(d.description()), null,
                "Description");
        UUID type = exiger(d.typeId(), "Type");
        verifierExiste(em, requeteType, type, "Type de statut inconnu.");
        if (d.dureeVieJours() < 0) {
            throw new InvalidRequestException("Durée de vie : un nombre de jours positif ou nul.");
        }
        String couleur = CatalogueStatutAchat.couleur(d.couleur());
        if (d.imageId() != null) {
            verifierExiste(em, "SELECT COUNT(*) FROM CfgImage WHERE PKey = :id", d.imageId(), "Image inconnue.");
        }
        return new FicheStatut(ordre, code, description, type, couleur,
                SaisiePatrimoine.texte(d.notes(), null, NOTES, "Notes"));
    }

    public static <T> T exiger(T valeur, String libelle) {
        if (valeur == null) {
            throw new InvalidRequestException(libelle + " est obligatoire.");
        }
        return valeur;
    }

    public static void verifierExiste(EntityManager em, String requete, UUID id, String message) {
        if (((Number) em.createNativeQuery(requete).setParameter("id", id).getSingleResult()).intValue() == 0) {
            throw new InvalidRequestException(message);
        }
    }
}
