package com.tsix.immocrm.services.rapport.vente.dossiers;

import com.tsix.immocrm.services.dossier.ParametresVente;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Paramètres {@code AdmParamsGuid} des rapports dossiers (FEATURE-tdb-vente-rapports-dossiers.md §2), lus par
 * {@code Code} : un paramètre absent ajoute un avertissement et vide la section, jamais d'exception.
 */
@Component
public class ParametresDossiers {

    public static final String CREDIT = "DefaultCreditDocumentType";
    public static final String STATUT_NOTAIRE = "DefaultComFolderStatusLawyer";

    private final ParametresVente parametres;

    public ParametresDossiers(ParametresVente parametres) {
        this.parametres = parametres;
    }

    /** Valeur du paramètre ; absente, {@code avertissements} reçoit « paramètre … non défini ». */
    public Optional<UUID> lire(String code, List<String> avertissements) {
        Optional<UUID> valeur = parametres.guid(code);
        if (valeur.isEmpty()) {
            avertissements.add("Paramètre %s non défini : section vide.".formatted(code));
        }
        return valeur;
    }
}
