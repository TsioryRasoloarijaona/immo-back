package com.tsix.immocrm.web;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.domain.ColonneTranche;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/**
 * Un filtre de colonnes par catalogue, chacun avec ses propres défauts (un filtre partagé viderait les autres listes).
 * Indispensable même hors listes : un {@code @JsonFilter} non enregistré fait tomber en 500 hotline et pré-prospections,
 * qui embarquent {@link ProspectionListItemDto}. {@code setFailOnUnknownId(false)} : un DTO oublié ici sort entier.
 */
@Configuration
public class ColonnesJacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer filtreColonnes() {
        return builder -> builder.filterProvider(new SimpleFilterProvider()
                .setFailOnUnknownId(false)
                .addFilter(ProspectionListItemDto.FILTRE_COLONNES,
                        new SelectionColonnesPropertyFilter(
                                ColonneProspection.nomsParDefaut(), ColonneProspection.ID.nom()))
                .addFilter(ColonneProspect.FILTRE,
                        new SelectionColonnesPropertyFilter(
                                ColonneProspect.nomsParDefaut(), ColonneProspect.ID.nom()))
                .addFilter(ColonneDossier.FILTRE,
                        new SelectionColonnesPropertyFilter(
                                ColonneDossier.nomsParDefaut(), ColonneDossier.ID.nom()))
                .addFilter(ColonneEncaissement.FILTRE,
                        new SelectionColonnesPropertyFilter(
                                ColonneEncaissement.nomsParDefaut(), ColonneEncaissement.ID.nom()))
                .addFilter(ColonneSuiviAction.FILTRE,
                        new SelectionColonnesPropertyFilter(
                                ColonneSuiviAction.nomsParDefaut(), ColonneSuiviAction.ID.nom()))
                .addFilter(ColonneTranche.FILTRE,
                        new SelectionColonnesPropertyFilter(
                                ColonneTranche.nomsParDefaut(), ColonneTranche.ID.nom())));
    }
}
