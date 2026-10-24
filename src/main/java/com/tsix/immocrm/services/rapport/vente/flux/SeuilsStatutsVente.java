package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Seuils de statut de dossier : {@code StrStatus.Sorting} du statut pointé par le paramètre {@code AdmParamsGuid}, lu par
 * code, sans repli (un paramètre absent rend vide, l'appelant le signale).
 */
@Component
public class SeuilsStatutsVente {

    public static final String CONCRETISATION = "DefaultComFolderStatusConcretisation";
    public static final String FINALISATION = "DefaultComFolderStatusFinalisation";
    public static final String LIVRAISON = "DefaultComFolderStatusDelevery";
    public static final String RESERVATION = "DefaultComFolderStatusReservation";

    private final AdmParamsGuidRepository parametres;
    private final StrStatusRepository statuts;

    public SeuilsStatutsVente(AdmParamsGuidRepository parametres, StrStatusRepository statuts) {
        this.parametres = parametres;
        this.statuts = statuts;
    }

    public Optional<Integer> concretisation() {
        return sorting(CONCRETISATION);
    }

    public Optional<Integer> finalisation() {
        return sorting(FINALISATION);
    }

    public Optional<Integer> livraison() {
        return sorting(LIVRAISON);
    }

    /** {@code Sorting} du statut désigné par {@code parametre} ; vide si le paramètre, sa valeur ou le statut manque. */
    public Optional<Integer> sorting(String parametre) {
        return valeur(parametre).flatMap(statuts::findById).map(StrStatus::getSorting);
    }

    /** Valeur d'un paramètre {@code AdmParamsGuid} ; vide si absent ou nul. */
    public Optional<UUID> valeur(String parametre) {
        return parametres.findById(parametre).map(AdmParamsGuid::getCodeValue);
    }
}
