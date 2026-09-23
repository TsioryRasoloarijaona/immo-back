package com.tsix.immocrm.web;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/**
 * Enregistre un filtre de sélection de colonnes <strong>par liste filtrable</strong>.
 *
 * <p><strong>Sans ce bean, l'application casse en production, pas seulement sur les listes.</strong>
 * Jackson lève une erreur dès qu'il rencontre un {@code @JsonFilter} dont l'identifiant n'est
 * enregistré nulle part. Or {@link ProspectionListItemDto} est aussi embarqué par
 * {@code HotlineFicheDto} ({@code GET /api/hotline/recherche}) et servi par
 * {@code GET /api/pre-prospections} — deux endpoints qui n'ont rien à voir avec la sélection de
 * colonnes et qui tomberaient en 500.
 *
 * <p><strong>Une instance par catalogue, et c'est structurel.</strong> Chaque filtre porte les
 * colonnes par défaut de <em>sa</em> liste. Un filtre unique partagé — le montage d'origine, quand
 * seule la prospection était filtrable — appliquait les défauts de la prospection à tout DTO
 * filtré : la liste des prospects serait revenue presque vide, aucun de ses noms
 * ({@code prenom}, {@code ville}…) ne figurant dans le catalogue de la prospection.
 *
 * <p>{@code setFailOnUnknownId(false)} est une ceinture de sécurité pour un futur DTO filtré qu'on
 * oublierait d'enregistrer ici : il sera sérialisé en entier plutôt que de faire tomber la réponse.
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
                                ColonneSuiviAction.nomsParDefaut(), ColonneSuiviAction.ID.nom())));
    }
}
