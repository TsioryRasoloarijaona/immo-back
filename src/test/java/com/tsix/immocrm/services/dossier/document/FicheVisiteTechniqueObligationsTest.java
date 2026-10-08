package com.tsix.immocrm.services.dossier.document;

import org.openpdf.text.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FicheVisiteTechniqueObligationsTest {

    /** Copie littérale de la concaténation d'origine (avant passage en bloc de texte). */
    private static final String TEXTE_ATTENDU =
            "Chaque propriétaire dispose des parties privatives comprises dans son lot. Il use et jouit "
                    + "librement des parties privatives et des parties communes sous la condition de ne pas "
                    + "porter atteinte ni aux droits des autres copropriétaires ni à la destination de "
                    + "l'immeuble. Chaque occupant se doit de :\n"
                    + "- S'abstenir de tous actes générateurs de troubles de voisinage : (bruits excessifs, "
                    + "mauvaises odeurs, activités contraires à la décence, ne pas détenir des animaux "
                    + "criards, etc.)\n"
                    + "- S'abstenir d'annexer, d'empiéter, de s'approprier, d'encombrer des parties communes "
                    + "(escaliers, terrasses, passages, allées, halls...) par : des chaises, tables, "
                    + "barbecues, échelles, vélos, motos, déblais, etc.\n"
                    + "- S'abstenir d'étendre le linge aux fenêtres des façades\n"
                    + "- S'abstenir de laver le linge à la terrasse\n"
                    + "- S'interdire d'installer toute antenne individuelle de télévision, de radio (sur "
                    + "balcons et/ou sur façade, etc.)\n"
                    + "- S'interdire d'entreprendre des travaux sur les parties communes de l'immeuble, "
                    + "notamment : ne pas toucher à la solidité de l'immeuble, ne pas effectuer de travaux "
                    + "sur la façade, ne pas effectuer de percements de gros murs, ne pas effectuer de "
                    + "travaux affectant l'harmonie de l'immeuble\n"
                    + "- Pour tous les travaux réalisés à l'intérieur de l'appartement par l'acquéreur, le "
                    + "Promoteur dégage toute responsabilité sur les incidences techniques ou dégâts qui "
                    + "pourraient en découler sur l'appartement de ce dernier ou sur celui d'un tiers.\n"
                    + "- S'interdire de modifier les parties privées visibles de l'extérieur (fenêtres, "
                    + "portes, volets, balcons, etc.)\n"
                    + "- Souscrire une police d'assurance personnelle du lot qu'il occupe.\n"
                    + "- Laisser l'accès au syndic dans son appartement en cas de besoin pour des "
                    + "réparations de toute nature qui le concernent ou qui concernent les appartements "
                    + "voisins ou qui concernent un besoin particulier de curage ou de réfection du réseau "
                    + "d'assainissement.\n"
                    + "- Soumettre au Syndic pour validation la liste des travaux qu'il devra réaliser dans "
                    + "son appartement pour s'assurer qu'ils ne portent pas préjudice à l'immeuble.\n"
                    + "- Respecter le règlement et les horaires du Rooftop, salle de sport, laverie.";

    @Test
    @DisplayName("Le paragraphe des obligations est identique, octet pour octet, au texte d'origine")
    void obligations_identiquesAuTexteOrigine() throws Exception {
        Method m = FicheVisiteTechniquePdfRenderer.class.getDeclaredMethod("obligationsParagraph");
        m.setAccessible(true);
        Paragraph p = (Paragraph) m.invoke(null);
        assertThat(p.getContent()).isEqualTo(TEXTE_ATTENDU);
    }
}
