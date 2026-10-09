package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.FicheReservationData;
import com.tsix.immocrm.exceptions.ConflictException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La fiche de réservation est une pièce remise au client : l'en-tête, la société citée et les
 * clauses imprimées dépendent de la marque, et se tromper de marque n'est pas un défaut cosmétique.
 *
 * <p>Tests unitaires purs — les deux générateurs n'ont aucune dépendance, ni base ni contexte.
 */
class FicheReservationMarqueTest {

    private final FicheReservationPdfRenderer pdf = new FicheReservationPdfRenderer();
    private final FicheReservationDocxRenderer docx = new FicheReservationDocxRenderer();

    private static FicheReservationData fiche(String codeDossier, String entreprise) {
        return new FicheReservationData(
                codeDossier, "Meryem TNIGUER", "LOT SAADAGH 3 IMM 17 OULFA CASABLANCA", "BK407979",
                "IMM06", "ARS0622", "622", "2 ème étage", new BigDecimal("70.00"),
                "AYAD RESIDENCE SIDI MAAROUF", "Casablanca", entreprise,
                "CHÉQUE CIH N° 4764660 du 08/05/2026", new BigDecimal("100000.00"),
                "Meryem TNIGUER", new BigDecimal("880000.00"), LocalDate.of(2026, 9, 7));
    }

    private String texteDocx(byte[] octets) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(octets))) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"))
                    + doc.getTables().stream().map(Object::toString).collect(Collectors.joining("\n"));
        }
    }

    @Test
    @DisplayName("Les deux marques produisent un PDF d'une page, fond de marque estampillé")
    void lesDeuxMarquesProduisentUnPdf() {
        for (String[] cas : new String[][]{{"ARS 2026/00370", "GHRIZA"}, {"APF 2022/00217", "ADMOD"}}) {
            String code = cas[0];
            byte[] rendu = pdf.render(fiche(code, cas[1]));
            assertThat(rendu).startsWith('%', 'P', 'D', 'F');
            // Le fond pèse ~1 Mo : un PDF nettement plus léger signifierait qu'il n'a pas été posé.
            assertThat(rendu)
                    .as("le fond de page doit être embarqué pour " + code)
                    .hasSizeGreaterThan(500_000);
        }
    }

    @Test
    @DisplayName("Une marque non couverte est refusée, jamais rendue sous un autre en-tête")
    void marqueInconnue_refusee() {
        for (String code : new String[]{"MBR 2024/00001", "ART 2023/00050", "", "XX"}) {
            FicheReservationData donnees = fiche(code, "BENACEUR");
            assertThatThrownBy(() -> pdf.render(donnees))
                    .as("marque non couverte : " + code)
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Test
    @DisplayName("Une société qui ne correspond pas à la marque du code fait échouer l'émission")
    void societeIncoherente_refusee() {
        // Code APF (en-tête ADMOD) mais projet rattaché à MAYLINE : le document porterait
        // l'en-tête et les clauses d'ADMOD alors que le vendeur est un autre.
        FicheReservationData donnees = fiche("APF 2022/00217", "MAYLINE");
        assertThatThrownBy(() -> pdf.render(donnees))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Incohérence");
    }

    @Test
    @DisplayName("La clause d'immobilisation 10 % est propre à APF")
    void clauseImmobilisation_seulementApf() throws Exception {
        assertThat(texteDocx(docx.render(fiche("APF 2022/00217", "ADMOD"))))
                .contains("frais d'immobilisation")
                .contains("groupe AYAD DEVELOPPEMENT");

        assertThat(texteDocx(docx.render(fiche("ARS 2026/00370", "GHRIZA"))))
                .doesNotContain("frais d'immobilisation");
    }

    @Test
    @DisplayName("La mention parking et la formule GHRIZA sont propres à ARS")
    void mentionParking_seulementArs() throws Exception {
        String ars = texteDocx(docx.render(fiche("ARS 2026/00370", "GHRIZA")));
        assertThat(ars)
                .contains(GabaritFicheReservation.MENTION_ARS)
                .contains("GHRIZA")
                // La formule ADMOD nommait « SOCIETE {nom} (groupe AYAD DEVELOPPEMENT) » quelle que
                // soit l'entité : appliquée à GHRIZA, elle affirmait un rattachement inexact.
                .doesNotContain("groupe AYAD DEVELOPPEMENT");

        assertThat(texteDocx(docx.render(fiche("APF 2022/00217", "ADMOD"))))
                .doesNotContain(GabaritFicheReservation.MENTION_ARS);
    }

    @Test
    @DisplayName("Le gabarit couvre les quatorze valeurs, pour les deux marques")
    void gabaritsComplets() {
        for (var gabarit : new java.util.List[]{java.util.List.of(GabaritFicheReservation.ARS),
                java.util.List.of(GabaritFicheReservation.APF)}) {
            @SuppressWarnings("unchecked")
            var zones = (java.util.Map<String, GabaritFicheReservation.Zone>) gabarit.get(0);
            assertThat(zones.values()).hasSize(14).allSatisfy(z -> {
                assertThat(z.xCm()).isBetween(0f, GabaritFicheReservation.PAGE_L);
                assertThat(z.yCm()).isBetween(0f, GabaritFicheReservation.PAGE_H);
                assertThat(z.xCm() + z.largeurCm()).isLessThanOrEqualTo(GabaritFicheReservation.PAGE_L);
            });
        }
    }
}
