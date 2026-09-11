package com.tsix.immocrm.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = """
        Un groupe de la liste repliée — l'équivalent d'un en-tête « Client : Adnane ES-SAIYDY \
        (1 item) » suivi de sa bande « Total : » dans le logiciel de bureau.

        Les groupes sont **repliés** : ils portent l'effectif et les totaux, pas les lignes. \
        Pour obtenir les lignes d'un groupe, rappeler la liste correspondante avec \
        `?groupeColonne=<la colonne groupée>&groupeValeur=<la clé ci-dessous>`.""")
public record GroupeDto(

        @Schema(description = """
                Valeur technique du groupe, à réémettre telle quelle dans `?groupeValeur=` pour \
                déplier le groupe. Selon la colonne groupée : un identifiant (groupement par \
                client/commercial/notaire), un code (statut, tranche, origine), un booléen, ou \
                un entier de période (`202507` pour juillet 2025, `2025` pour l'année).

                **`null` est une valeur de groupe légitime** : ce sont les lignes sans valeur \
                sur la colonne groupée. Le groupe est renvoyé et non masqué, sans quoi la somme \
                des effectifs cesserait de retomber sur le total de la liste.""",
                example = "CONCRETISATION")
        Object cle,

        @Schema(description = """
                Libellé affichable du groupe. Identique à `cle` pour la plupart des colonnes ; \
                dérivé pour les groupements par tiers (priorité raison sociale > description > \
                prénom + nom, exactement comme la colonne correspondante de la liste) et pour \
                les périodes (`juillet 2025`). `null` pour le groupe des lignes sans valeur.""",
                example = "CONCRETISATION")
        String libelle,

        @Schema(description = "Nombre de lignes du groupe — le « (1 item) » de l'écran légataire.",
                example = "12")
        long nombre,

        @Schema(description = """
                Sommes des colonnes demandées via `?agregats=`, indexées par le nom de colonne \
                tel qu'il apparaît dans la liste (`prix`, `totalAPayer`...). Vide si `agregats` \
                n'est pas fourni. Une colonne dont toutes les lignes du groupe sont nulles vaut \
                `0`, jamais `null` — c'est un total, pas une valeur absente.""",
                example = "{\"prix\": 1350000.00}")
        Map<String, BigDecimal> agregats) {
}
