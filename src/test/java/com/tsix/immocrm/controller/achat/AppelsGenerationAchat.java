package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Routes, corps et messages exacts du lot A2b-2b « générer une pièce d'achat » (FEATURE-achat-generation.md §3-§4). */
final class AppelsGenerationAchat {

    // §4 G2 — messages OMRAN exacts (DocumentEdition.cs l.593, Invoice.cs l.857, 871, 875, 911, 915).
    static final String BC_NON_VALIDE_POUR_BL =
            "Vous ne pouvez pas ajouter un bon de livraison pour un bon de commande qui n'est pas validé !";
    static final String BC_NON_VALIDE_POUR_FACTURE =
            "Vous ne pouvez pas ajouter une facture pour un bon de commande qui n'est pas validé !";
    static final String FOURNISSEURS_DIFFERENTS = "Veuillez choisir des bons de livraison générer pour le même fournisseur";
    static final String BC_DIFFERENTS = "Veuillez choisir des bons de livraison générer du même bon de commande";
    static final String BL_DEJA_FACTURE = "Vous ne pouvez pas générer une facture sur un bon de livraison déja facturé.";
    static final String BL_NON_VALIDE = "Vous ne pouvez pas ajouter une facture pour un bon de livraison qui n'est pas validé !";
    // §4 G5 (décision Q-B) et G3 (Q-8).
    static final String TOUT_LIVRE = "Toutes les quantités de ce bon de commande sont déjà livrées.";
    static final String NON_CONFIGUREE = "Génération non configurée pour la gestion";
    // §3 libellés de GenerationPieceAchatDto.
    static final String GENERER_BC = "Générer Bon de commande";
    static final String GENERER_BL = "Générer Bon de livraison";
    static final String GENERER_FACTURE = "Générer Facture";

    static final String SOURCES = "sources";
    static final String LIGNE_BORDEREAU = "ligneBordereauId";
    static final String GENERATIONS = "$.actions.generations";

    private AppelsGenerationAchat() {
    }

    /** Messages de {@code QuantitesBonCommande} (inchangés, §2). */
    static String depasse(String code) {
        return "La quantité de l'article '" + code + "' dépasse celle définie dans le bon de commande !";
    }

    static String absentDuBc(String code) {
        return "L'article '" + code + "' n'existe pas dans le bon de commande !";
    }

    /** {@code GET …/modele?nature=…&sources=a,b}. */
    static MockHttpServletRequestBuilder modele(String nature, UUID... sources) {
        return get(AppelsSaisieAchat.MODELE).param("nature", nature)
                .param(SOURCES, Arrays.stream(sources).map(UUID::toString).collect(Collectors.joining(",")));
    }

    static CorpsJson avecSources(CorpsJson corps, UUID... sources) {
        return corps.avec(SOURCES, List.of(sources));
    }

    /** Valeurs d'un champ des lignes du modèle dont {@code ligneBordereauId} vaut {@code bordereau}. */
    static List<Object> champDesLignesDuBordereau(String json, UUID bordereau, String champ) {
        return JsonPath.read(json, "$.lignes[?(@.ligneBordereauId == '" + bordereau + "')]." + champ);
    }

    /** Valeurs d'un champ des lignes du modèle de code donné. */
    static List<Object> champDesLignesDeCode(String json, String code, String champ) {
        return JsonPath.read(json, "$.lignes[?(@.code == '" + code + "')]." + champ);
    }

    static BigDecimal decimal(Object valeur) {
        return new BigDecimal(valeur.toString());
    }

    /** {@code natureCible} des générations de la fiche. */
    static List<String> naturesGenerees(String ficheJson) {
        return JsonPath.read(ficheJson, GENERATIONS + "[*].natureCible");
    }
}
