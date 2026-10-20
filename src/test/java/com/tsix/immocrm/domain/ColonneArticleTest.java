package com.tsix.immocrm.domain;

import com.tsix.immocrm.dto.patrimoine.LigneArticleDto;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColonneArticleTest {

    @Test
    void catalogue_memesNomsEtMemeOrdreQueLigneArticleDto() {
        List<String> composantes = Arrays.stream(LigneArticleDto.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertThat(Colonnes.nomsValides(ColonneArticle.class)).containsExactlyElementsOf(composantes);
    }

    @Test
    void libellesUniques() {
        assertThat(Arrays.stream(ColonneArticle.values()).map(ColonneArticle::libelle).toList())
                .doesNotHaveDuplicates();
    }

    @Test
    void resoudre_ajouteToujoursLId_etRefuseUnNomInconnu() {
        assertThat(ColonneArticle.resoudre(List.of("parentCode")))
                .containsExactly(ColonneArticle.ID, ColonneArticle.PARENT_CODE);
        List<String> inconnue = List.of("inconnue");
        assertThatThrownBy(() -> ColonneArticle.resoudre(inconnue)).isInstanceOf(ColonneInconnueException.class);
    }
}
