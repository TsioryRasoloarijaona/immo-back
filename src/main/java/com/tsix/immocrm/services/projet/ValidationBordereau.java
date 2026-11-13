package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import com.tsix.immocrm.dto.projet.GroupeSaisieDto;
import com.tsix.immocrm.dto.projet.LigneSaisieDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/** Corps d'un bordereau entier (FEATURE-lot-ecritures.md R4, R5) : toute erreur = 400. */
final class ValidationBordereau {

    private static final int TEXTE = 255;
    private static final int UNITE = 50;
    private static final int NOTES = 1000;
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private ValidationBordereau() {
    }

    /** {@code categoriesExistantes} reçoit les catégories citées et rend celles qui existent. */
    static void valider(BordereauSaisieDto corps, boolean estimatif, UnaryOperator<Set<UUID>> categoriesExistantes) {
        if (corps == null || !sansNul(corps.groupes()) || !sansNul(corps.lignes())) {
            throw new InvalidRequestException("groupes, lignes : listes obligatoires, sans élément nul");
        }
        Map<UUID, UUID> parents = new HashMap<>();
        corps.groupes().forEach(g -> groupe(g, parents));
        parents.keySet().forEach(g -> sansCycle(g, parents));
        Set<UUID> ids = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (LigneSaisieDto l : corps.lignes()) {
            ligne(l, ids, parents.keySet());
            if (estimatif && StringUtils.hasText(l.code()) && !codes.add(l.code())) {
                throw new InvalidRequestException("Le code %s doit être unique pour les lignes du bordereaux !".formatted(l.code()));
            }
        }
        Set<UUID> categories = corps.lignes().stream().map(LigneSaisieDto::categorieId).collect(Collectors.toSet());
        exiger(categoriesExistantes.apply(categories).containsAll(categories), "lignes[].categorieId : catégorie inconnue");
    }

    private static void groupe(GroupeSaisieDto g, Map<UUID, UUID> parents) {
        exiger(g.id() != null && !parents.containsKey(g.id()), "groupes[].id : obligatoire et unique");
        parents.put(g.id(), g.parentId());
        exiger(g.ordre() != null, "groupes[].ordre : obligatoire");
        exiger(StringUtils.hasText(g.description()), "groupes[].description : obligatoire");
        longueur(g.code(), TEXTE, "groupes[].code");
        longueur(g.description(), TEXTE, "groupes[].description");
        longueur(g.notes(), NOTES, "groupes[].notes");
    }

    private static void sansCycle(UUID depart, Map<UUID, UUID> parents) {
        UUID p = parents.get(depart);
        for (int pas = 0; p != null; pas++) {
            exiger(parents.containsKey(p), "groupes[].parentId : groupe absent de l'envoi");
            exiger(pas < parents.size() && !p.equals(depart), "groupes[].parentId : cycle de groupes");
            p = parents.get(p);
        }
    }

    private static void ligne(LigneSaisieDto l, Set<UUID> ids, Set<UUID> groupes) {
        exiger(l.id() != null && ids.add(l.id()), "lignes[].id : obligatoire et unique");
        exiger(l.groupeId() == null || groupes.contains(l.groupeId()), "lignes[].groupeId : groupe absent de l'envoi");
        exiger(l.ordre() != null, "lignes[].ordre : obligatoire");
        exiger(StringUtils.hasText(l.designation()), "lignes[].designation : obligatoire");
        exiger(l.categorieId() != null, "lignes[].categorieId : obligatoire");
        exiger(entre(l.quantite(), null), "lignes[].quantite : obligatoire, positive ou nulle");
        exiger(entre(l.prixUnitaireHT(), null), "lignes[].prixUnitaireHT : obligatoire, positif ou nul");
        exiger(entre(l.tauxTVA(), CENT), "lignes[].tauxTVA : obligatoire, entre 0 et 100");
        exiger(l.tauxFODEC() == null || entre(l.tauxFODEC(), CENT), "lignes[].tauxFODEC : entre 0 et 100");
        longueur(l.code(), TEXTE, "lignes[].code");
        longueur(l.designation(), TEXTE, "lignes[].designation");
        longueur(l.unite(), UNITE, "lignes[].unite");
        longueur(l.notes(), NOTES, "lignes[].notes");
    }

    private static boolean sansNul(List<?> liste) {
        return liste != null && liste.stream().allMatch(Objects::nonNull);
    }

    private static boolean entre(BigDecimal valeur, BigDecimal max) {
        return valeur != null && valeur.signum() >= 0 && (max == null || valeur.compareTo(max) <= 0);
    }

    private static void longueur(String texte, int max, String champ) {
        exiger(texte == null || texte.length() <= max, champ + " : " + max + " caractères au plus");
    }

    private static void exiger(boolean condition, String message) {
        if (!condition) {
            throw new InvalidRequestException(message);
        }
    }
}
