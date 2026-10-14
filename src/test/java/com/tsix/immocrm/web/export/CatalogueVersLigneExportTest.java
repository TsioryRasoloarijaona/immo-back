package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.Colonne;
import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.dto.encaissement.EncaissementDto;
import com.tsix.immocrm.dto.prospect.ProspectListItemDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.suivi.SuiviActionDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExportTableur lit chaque cellule par le nom de la colonne sur la composante du record : un nom de
 * catalogue sans composante homonyme sort une colonne vide, sans erreur.
 */
class CatalogueVersLigneExportTest {

    static Stream<Arguments> catalogues() {
        return Stream.of(
                Arguments.of(ColonneProspection.values(), ProspectionListItemDto.class),
                Arguments.of(ColonneProspect.values(), ProspectListItemDto.class),
                Arguments.of(ColonneEncaissement.values(), EncaissementDto.class),
                Arguments.of(ColonneSuiviAction.values(), SuiviActionDto.class));
    }

    @ParameterizedTest
    @MethodSource("catalogues")
    void chaqueColonneDuCatalogue_aUneComposanteDuRecordExporte(Colonne[] catalogue, Class<?> ligne) {
        List<String> composantes = Arrays.stream(ligne.getRecordComponents()).map(RecordComponent::getName).toList();

        List<String> sansComposante = Arrays.stream(catalogue).map(Colonne::nom)
                .filter(nom -> !composantes.contains(nom)).toList();

        assertThat(sansComposante).as("colonnes de %s sans composante dans %s", catalogue[0].getClass()
                .getSimpleName(), ligne.getSimpleName()).isEmpty();
    }
}
