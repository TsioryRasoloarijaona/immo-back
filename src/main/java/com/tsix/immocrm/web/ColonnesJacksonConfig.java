package com.tsix.immocrm.web;

import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/**
 * Enregistre le filtre de sélection de colonnes en mode « tout sérialiser ».
 *
 * <p><strong>Sans ce bean, l'application casse en production, pas seulement sur la liste.</strong>
 * Jackson lève une erreur dès qu'il rencontre un {@code @JsonFilter} dont l'identifiant n'est
 * enregistré nulle part. Or {@link ProspectionListItemDto} est aussi embarqué par
 * {@code HotlineFicheDto} ({@code GET /api/hotline/recherche}) et servi par
 * {@code GET /api/pre-prospections} — deux endpoints qui n'ont rien à voir avec la sélection de
 * colonnes et qui tomberaient en 500.
 *
 * <p>Le comportement par défaut est donc « aucune colonne masquée ». Seul
 * {@link ColonnesResponseAdvice} le remplace, requête par requête, quand {@code ?colonnes=} est
 * présent — sans jamais toucher à cette configuration partagée.
 *
 * <p>{@code setFailOnUnknownId(false)} est une ceinture de sécurité pour un futur DTO filtré
 * qu'on oublierait d'enregistrer ici : il sera sérialisé en entier plutôt que de faire tomber la
 * réponse.
 */
@Configuration
public class ColonnesJacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer filtreColonnes() {
        return builder -> builder.filterProvider(new SimpleFilterProvider()
                .setFailOnUnknownId(false)
                .addFilter(ProspectionListItemDto.FILTRE_COLONNES,
                        new SelectionColonnesPropertyFilter()));
    }
}
