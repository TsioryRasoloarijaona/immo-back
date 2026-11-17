package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.AvenantSaisieDto;
import com.tsix.immocrm.dto.projet.AvenantSaisieDto.LigneAvenantSaisieDto;
import com.tsix.immocrm.dto.projet.RetenueAvenantDto;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.projet.ValidationBordereau.CAPACITE;
import static com.tsix.immocrm.services.projet.ValidationBordereau.CENT;
import static com.tsix.immocrm.services.projet.ValidationBordereau.NOTES;
import static com.tsix.immocrm.services.projet.ValidationBordereau.TEXTE;
import static com.tsix.immocrm.services.projet.ValidationBordereau.entre;
import static com.tsix.immocrm.services.projet.ValidationBordereau.exiger;
import static com.tsix.immocrm.services.projet.ValidationBordereau.longueur;
import static com.tsix.immocrm.services.projet.ValidationBordereau.sansNul;

/** Corps d'un avenant (FEATURE-lot-avenants-avances.md R3) : toute erreur = 400. */
final class ValidationAvenant {

    private static final int DESIGNATION = 500;

    private ValidationAvenant() {
    }

    /** Les deux opérateurs reçoivent les ids cités et rendent ceux qui existent (catégories, lignes définitives du lot). */
    static void valider(AvenantSaisieDto corps, UnaryOperator<Set<UUID>> categoriesExistantes,
                        UnaryOperator<Set<UUID>> lignesDefinitivesDuLot) {
        exiger(corps.date() != null, "date : obligatoire");
        longueur(corps.referenceInterne(), TEXTE, "referenceInterne");
        longueur(corps.notes(), NOTES, "notes");
        retenue(corps.garantie(), "garantie");
        retenue(corps.prorata(), "prorata");
        retenue(corps.assuranceDEC(), "assuranceDEC");
        retenue(corps.assuranceTRC(), "assuranceTRC");
        exiger(sansNul(corps.lignes()), "lignes : liste obligatoire, sans élément nul");
        corps.lignes().forEach(ValidationAvenant::ligne);
        Set<UUID> categories = corps.lignes().stream().map(LigneAvenantSaisieDto::categorieId).collect(Collectors.toSet());
        exiger(categoriesExistantes.apply(categories).containsAll(categories), "lignes[].categorieId : catégorie inconnue");
        Set<UUID> bordereau = corps.lignes().stream().map(LigneAvenantSaisieDto::ligneBordereauId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        exiger(bordereau.isEmpty() || lignesDefinitivesDuLot.apply(bordereau).containsAll(bordereau),
                "lignes[].ligneBordereauId : ligne du bordereau définitif de ce lot attendue");
    }

    private static void retenue(RetenueAvenantDto r, String champ) {
        if (r != null) {
            exiger(r.taux() == null || entre(r.taux(), CENT), champ + ".taux : entre 0 et 100");
            exiger(r.montant() == null || entre(r.montant(), CAPACITE), champ + ".montant : positif ou nul");
        }
    }

    private static void ligne(LigneAvenantSaisieDto l) {
        exiger(l.ordre() != null, "lignes[].ordre : obligatoire");
        exiger(StringUtils.hasText(l.designation()), "lignes[].designation : obligatoire");
        exiger(l.categorieId() != null, "lignes[].categorieId : obligatoire");
        exiger(entre(l.quantite(), CAPACITE), "lignes[].quantite : obligatoire, positive ou nulle");
        exiger(entre(l.prixUnitaireHT(), CAPACITE), "lignes[].prixUnitaireHT : obligatoire, positif ou nul");
        exiger(entre(l.tauxTVA(), CENT), "lignes[].tauxTVA : obligatoire, entre 0 et 100");
        exiger(l.tauxFODEC() == null || entre(l.tauxFODEC(), CENT), "lignes[].tauxFODEC : entre 0 et 100");
        longueur(l.code(), TEXTE, "lignes[].code");
        longueur(l.designation(), DESIGNATION, "lignes[].designation");
        longueur(l.unite(), TEXTE, "lignes[].unite");
        longueur(l.notes(), NOTES, "lignes[].notes");
    }
}
