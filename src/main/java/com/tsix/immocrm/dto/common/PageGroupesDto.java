package com.tsix.immocrm.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Le total général est calculé ici (une requête de plus, sans {@code GROUP BY}) : sommer les
 * groupes d'une page côté front serait faux dès la page 2.
 */
@Schema(description = "Groupes paginés, plus le total général sur tout le périmètre filtré.")
public record PageGroupesDto(

        List<GroupeDto> content,
        int page,
        int size,
        @Schema(description = "Nombre de **groupes**, pas de lignes — c'est ce qui est paginé ici.")
        long totalElements,
        int totalPages,

        @Schema(example = "{\"montantBrut\": 117600.00}")
        Map<String, Object> totaux,

        @Schema(example = "643")
        long nombreTotal) {

    public static PageGroupesDto of(Page<GroupeDto> groupes, Map<String, Object> totaux,
                                    long nombreTotal) {
        return new PageGroupesDto(groupes.getContent(), groupes.getNumber(), groupes.getSize(),
                groupes.getTotalElements(), groupes.getTotalPages(), totaux, nombreTotal);
    }
}
