package com.tsix.immocrm.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Enveloppe des endpoints {@code /groupes} : la pagination habituelle de {@link PageDto},
 * <strong>plus</strong> le total général du périmètre filtré.
 *
 * <h2>Pourquoi le total général ne peut pas être calculé côté front</h2>
 *
 * <p>C'est la même raison qui a fait exister {@code /groupes} : la réponse est paginée. Sommer les
 * groupes de la page reçue donnerait un total juste sur la page 1 d'un seul groupe et faux partout
 * ailleurs — et faux <em>silencieusement</em>, puisque le nombre aurait l'air plausible. Le total
 * ci-dessous porte sur l'ensemble filtré, pas sur la page.
 *
 * <p>Il coûte une requête supplémentaire par appel : les mêmes filtres, sans {@code GROUP BY}.
 */
@Schema(description = "Groupes paginés, plus le total général sur tout le périmètre filtré.")
public record PageGroupesDto(

        List<GroupeDto> content,
        int page,
        int size,
        @Schema(description = "Nombre de **groupes**, pas de lignes — c'est ce qui est paginé ici.")
        long totalElements,
        int totalPages,

        @Schema(description = """
                Les mêmes agrégats que dans chaque groupe, mais calculés sur **tout le périmètre \
                filtré** : la bande « Total : » de bas de grille, par opposition à celle d'un \
                en-tête de groupe. Indexés par le même jeton. Vide si `agregats` n'est pas fourni.

                Suit `inclureSansValeur` à l'identique : sinon ce total et la somme des groupes \
                désigneraient deux populations différentes.""",
                example = "{\"montantBrut\": 117600.00}")
        Map<String, Object> totaux,

        @Schema(description = "Nombre total de lignes agrégées — l'effectif dont `totaux` est tiré. "
                + "Avec `inclureSansValeur=true`, égale le `totalElements` de la liste non groupée.",
                example = "643")
        long nombreTotal) {

    public static PageGroupesDto of(Page<GroupeDto> groupes, Map<String, Object> totaux,
                                    long nombreTotal) {
        return new PageGroupesDto(groupes.getContent(), groupes.getNumber(), groupes.getSize(),
                groupes.getTotalElements(), groupes.getTotalPages(), totaux, nombreTotal);
    }
}
