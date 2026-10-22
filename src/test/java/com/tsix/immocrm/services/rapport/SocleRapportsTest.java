package com.tsix.immocrm.services.rapport;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.entities.adm.AdmReportData;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmReportDataRepository;
import com.tsix.immocrm.services.equipe.CommerciauxVisibles;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Règles du socle hors tests verrouillés (FEATURE-tdb-vente-rapports-socle.md « Précisions », B6, B7) : registre,
 * paramètres, totaux, plafond de lignes, export.
 */
class SocleRapportsTest {

    private static final String CODE = "ComTestUnitaire";
    private static final String N = "n";

    // ------------------------------------------------------------------ registre

    @Test
    void registre_doublonModuleCode_echoueAuDemarrage() {
        List<Rapport> rapports = List.of(rapport(c -> null), rapport(c -> null));

        assertThatThrownBy(() -> new RegistreRapports(rapports))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(CODE);
    }

    // ------------------------------------------------------------------ paramètres

    private static ParametreRapport parametre(TypeParametre type, boolean obligatoire, String defaut, Integer min,
                                              Integer max) {
        return new ParametreRapport(N, "N", type, obligatoire, false, defaut, List.of(), min, max);
    }

    private static Map<String, List<String>> valider(ParametreRapport p, String... valeurs) {
        return ParametresRapport.valider(List.of(p), valeurs.length == 0 ? Map.of() : Map.of(N, List.of(valeurs)));
    }

    @Test
    void parametres_obligatoireManquant_400() {
        ParametreRapport p = parametre(TypeParametre.TEXTE, true, null, null, null);

        assertThatThrownBy(() -> valider(p)).isInstanceOf(InvalidRequestException.class).hasMessageContaining(N);
    }

    @Test
    void parametres_absent_valeurParDefautAppliquee() {
        assertThat(valider(parametre(TypeParametre.ANNEE, false, "2025", null, null))).containsEntry(N, List.of("2025"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1899", "2201", "deux"})
    void parametres_anneeHorsBornes_400(String valeur) {
        ParametreRapport p = parametre(TypeParametre.ANNEE, false, null, null, null);

        assertThatThrownBy(() -> valider(p, valeur)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void parametres_anneeAuxBornes_acceptee() {
        ParametreRapport p = parametre(TypeParametre.ANNEE, false, null, null, null);

        assertThat(valider(p, "1900")).containsEntry(N, List.of("1900"));
        assertThat(valider(p, "2200")).containsEntry(N, List.of("2200"));
    }

    /** B7 : un ENTIER sans min/max n'a pas de borne. */
    @Test
    void parametres_entierSansBornes_toutEntierAccepte() {
        ParametreRapport p = parametre(TypeParametre.ENTIER, false, null, null, null);

        assertThat(valider(p, "-3")).containsEntry(N, List.of("-3"));
        assertThat(valider(p, "50000")).containsEntry(N, List.of("50000"));
    }

    /** B7 : bornes min/max incluses d'un ENTIER. */
    @Test
    void parametres_entierHorsMinMax_400_bornesIncluses() {
        ParametreRapport p = parametre(TypeParametre.ENTIER, false, null, 1, 12);

        assertThat(valider(p, "1")).containsEntry(N, List.of("1"));
        assertThat(valider(p, "12")).containsEntry(N, List.of("12"));
        assertThatThrownBy(() -> valider(p, "0")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> valider(p, "13")).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void parametres_booleen_trueFalseSeulement() {
        ParametreRapport p = parametre(TypeParametre.BOOLEEN, false, null, null, null);

        assertThat(valider(p, "TRUE")).containsEntry(N, List.of("TRUE"));
        assertThatThrownBy(() -> valider(p, "oui")).isInstanceOf(InvalidRequestException.class);
    }

    // ------------------------------------------------------------------ totaux

    private static ColonneRapport colonne(FonctionTotal fonction) {
        return new ColonneRapport(N, "N", TypeColonne.NOMBRE, null, fonction, true, null);
    }

    private static SectionRapport section(FonctionTotal fonction, Map<String, Object> imposes, Object... valeurs) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Object v : valeurs) {
            Map<String, Object> ligne = new HashMap<>();
            ligne.put(N, v);
            ligne.put("g", lignes.size() % 2 == 0 ? "pair" : "impair");
            lignes.add(ligne);
        }
        return new SectionRapport("s", "S", List.of(colonne(fonction)), lignes, "g", imposes, List.of());
    }

    @Test
    void totaux_moyenne_ignoreLesValeursNulles() {
        assertThat(TotauxRapport.totaux(section(FonctionTotal.MOYENNE, null, 1, null, new BigDecimal("2"))))
                .containsEntry(N, new BigDecimal("1.5"));
    }

    @Test
    void totaux_minEtMax() {
        assertThat(TotauxRapport.totaux(section(FonctionTotal.MIN, null, 3, 1L, 2))).containsEntry(N, BigDecimal.ONE);
        assertThat(TotauxRapport.totaux(section(FonctionTotal.MAX, null, 3, 1L, 2)))
                .containsEntry(N, BigDecimal.valueOf(3));
    }

    @Test
    void totaux_valeurImposee_lEmporteSurLaSomme() {
        assertThat(TotauxRapport.totaux(section(FonctionTotal.SOMME, Map.of(N, 99), 1, 2))).containsEntry(N, 99);
    }

    @Test
    void totaux_sectionVide_aucunTotal() {
        assertThat(TotauxRapport.totaux(section(FonctionTotal.SOMME, Map.of(N, 99)))).isEmpty();
    }

    @Test
    void sousTotaux_parValeurDuGroupe() {
        Map<String, Map<String, Object>> sousTotaux = TotauxRapport.sousTotaux(section(FonctionTotal.SOMME, null,
                1, 10, 2, 20));

        assertThat(sousTotaux).containsEntry("pair", Map.of(N, BigDecimal.valueOf(3)))
                .containsEntry("impair", Map.of(N, BigDecimal.valueOf(30)));
    }

    // ------------------------------------------------------------------ plafond et registre d'ids

    @Test
    void resultat_sectionDePlusDe20000Lignes_400() {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (int i = 0; i <= 20_000; i++) {
            lignes.add(Map.of(N, i));
        }
        ServiceRapports service = service(rapport(c -> new ResultatRapport(List.of(new SectionRapport("s", "S",
                List.of(colonne(FonctionTotal.SOMME)), lignes, null, null, List.of())), List.of())));

        assertThatThrownBy(() -> service.resultat("vente", CODE, Map.of()))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("affinez");
    }

    @Test
    void resultat_idDeSectionEnDouble_erreurDuRapport() {
        SectionRapport s = new SectionRapport("s", "S", List.of(), List.of(), null, null, List.of());
        ServiceRapports service = service(rapport(c -> new ResultatRapport(List.of(s, s), List.of())));

        assertThatThrownBy(() -> service.resultat("vente", CODE, Map.of())).isInstanceOf(IllegalStateException.class);
    }

    private static ServiceRapports service(Rapport rapport) {
        AdmReportData ligne = new AdmReportData();
        ligne.setId(UUID.randomUUID());
        ligne.setCode(CODE);
        AdmReportDataRepository repository = mock(AdmReportDataRepository.class);
        when(repository.findFirstByTypeInAndCode(any(), anyString())).thenReturn(Optional.of(ligne));
        DroitsRapports droits = mock(DroitsRapports.class);
        when(droits.visibles()).thenReturn(id -> true);
        TranchesVisiblesRapport tranches = mock(TranchesVisiblesRapport.class);
        when(tranches.retenues(any(), any())).thenReturn(List.of());
        return new ServiceRapports(new RegistreRapports(List.of(rapport)), repository, droits, tranches,
                mock(CommerciauxVisibles.class), Clock.systemUTC());
    }

    private static Rapport rapport(Function<ContexteRapport, ResultatRapport> execution) {
        return new Rapport() {
            @Override
            public String code() {
                return CODE;
            }

            @Override
            public ModuleRapport module() {
                return ModuleRapport.VENTE;
            }

            @Override
            public List<ParametreRapport> parametres() {
                return List.of();
            }

            @Override
            public ResultatRapport executer(ContexteRapport contexte) {
                return execution.apply(contexte);
            }
        };
    }

    // ------------------------------------------------------------------ export (B6, noms de feuille, Total)

    @Test
    void export_decimalesNombre_nomsDeFeuilleNettoyesEtDedoublonnes_ligneTotal() throws Exception {
        ColonneRapport surface = new ColonneRapport("surface", "Surface", TypeColonne.NOMBRE, null,
                FonctionTotal.SOMME, true, 2);
        ColonneRapport libelle = ColonneRapport.texte("libelle", "Libellé");
        List<Map<String, Object>> lignes = List.of(Map.of("libelle", "A", "surface", new BigDecimal("12.5")));
        FeuilleExport feuille = new FeuilleExport("Stock [tranche]: a/b", List.of(libelle, surface), lignes,
                Map.of("surface", new BigDecimal("12.5")));
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();

        new ExportTableur(Clock.systemUTC(), null).ecrire(sortie, "T", "auteur", List.of(feuille, feuille));

        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(sortie.toByteArray()))) {
            assertThat(classeur.getSheetName(0)).isEqualTo("Stock tranche ab");
            assertThat(classeur.getSheetName(1)).isEqualTo("Stock tranche ab (2)");
            Sheet s = classeur.getSheetAt(0);
            assertThat(s.getRow(1).getCell(1).getCellStyle().getDataFormatString()).isEqualTo("#,##0.00");
            assertThat(s.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(classeur.getFontAt(s.getRow(2).getCell(1).getCellStyle().getFontIndex()).getBold()).isTrue();
        }
    }

    @Test
    void colonneRapport_decimalesAbsentes_zero() {
        assertThat(colonne(FonctionTotal.SOMME).decimales()).isZero();
    }
}
