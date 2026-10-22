package com.tsix.immocrm.domain.rapport;

import java.util.List;

/** Résultat d'un rapport ; {@code avertissements} : écarts assumés ou données manquantes, affichés tels quels. */
public record ResultatRapport(List<SectionRapport> sections, List<String> avertissements) {

    public ResultatRapport {
        sections = sections == null ? List.of() : List.copyOf(sections);
        avertissements = avertissements == null ? List.of() : List.copyOf(avertissements);
    }
}
