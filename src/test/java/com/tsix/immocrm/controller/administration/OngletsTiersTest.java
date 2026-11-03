package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureOngletsTiers;
import com.tsix.immocrm.fixtures.FixturePersonnels;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.CONGES;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.DOSSIERS;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.ONGLETS_COMMUNS;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.REGLEMENTS;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.ids;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.onglet;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.total;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contenu des onglets — FEATURE-admin-onglets-tiers.md, règle testable 1 (lignes du tiers seulement), décision 3 (tri
 * date décroissante), réponses du manager R1 (historique = {@code StrFolderStatusHistory}), R2 (dépenses), R3
 * (règlements {@code TierId} ou {@code TierPayerId}), R6 (dossiers fusionnés), règle 6 (congés sans montant).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OngletsTiersTest {

    private static final LocalDate JANVIER = LocalDate.of(2025, 1, 10);
    private static final LocalDate FEVRIER = LocalDate.of(2025, 2, 10);
    private static final LocalDate MARS = LocalDate.of(2025, 3, 10);
    private static final Pattern CLE_MONTANT = Pattern.compile("(?i).*(montant|amount|salair|salary|prix|total).*");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers tiers;
    @Autowired private FixtureOngletsTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = tiers.jeu();
        fixture.preparer();
    }

    private String page(TypeTiers type, UUID id, String nom) throws Exception {
        return mockMvc.perform(onglet(type.famille(), id, nom, connecte(j.admin()))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static Stream<Arguments> famillesEtOnglets() {
        Stream<Arguments> communs = Stream.of(FOURNISSEURS, CONTACTS, PERSONNELS)
                .flatMap(type -> ONGLETS_COMMUNS.stream().map(o -> Arguments.of(type, o)));
        return Stream.concat(communs, Stream.of(Arguments.of(PERSONNELS, CONGES)));
    }

    private static Stream<String> ongletsDates() {
        return Stream.concat(ONGLETS_COMMUNS.stream().filter(o -> !DOSSIERS.equals(o)), Stream.of(CONGES));
    }

    // ------------------------------------------------------------------ règle 1 : lignes du tiers, et seulement les siennes

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("famillesEtOnglets")
    void regle1_chaqueOnglet_rendLesLignesDuTiers_etSeulementLesSiennes(TypeTiers type, String nom) throws Exception {
        UUID cible = j.tiers(type, "A").inserer();
        UUID autre = j.tiers(type, "B").inserer();
        UUID l1 = fixture.ligne(j, nom, cible, JANVIER);
        UUID l2 = fixture.ligne(j, nom, cible, FEVRIER);
        UUID etrangere = fixture.ligne(j, nom, autre, MARS);

        String corps = page(type, cible, nom);

        assertAll(
                () -> assertThat(ids(corps)).containsExactlyInAnyOrder(l1.toString(), l2.toString())
                        .doesNotContain(etrangere.toString()),
                () -> assertThat(total(corps)).isEqualTo(2));
    }

    // ------------------------------------------------------------------ décision 3 : tri par date décroissante

    @ParameterizedTest
    @MethodSource("ongletsDates")
    void d3_lignesTrieesParDateDecroissante(String nom) throws Exception {
        TypeTiers type = CONGES.equals(nom) ? PERSONNELS : FOURNISSEURS;
        UUID cible = j.tiers(type, "T").inserer();
        UUID janvier = fixture.ligne(j, nom, cible, JANVIER);
        UUID mars = fixture.ligne(j, nom, cible, MARS);
        UUID fevrier = fixture.ligne(j, nom, cible, FEVRIER);

        assertThat(ids(page(type, cible, nom))).containsExactly(mars.toString(), fevrier.toString(), janvier.toString());
    }

    // ------------------------------------------------------------------ R3 : règlements TierId ou TierPayerId, sans doublon

    @Test
    void reponseR3_reglements_tiersOuPayeur_sansDoublon() throws Exception {
        UUID cible = j.tiers(FOURNISSEURS, "R").inserer();
        UUID autre = j.tiers(FOURNISSEURS, "S").inserer();
        UUID commeTiers = fixture.reglement(j, cible, autre, JANVIER);
        UUID commePayeur = fixture.reglement(j, autre, cible, FEVRIER);
        UUID lesDeux = fixture.reglement(j, cible, cible, MARS);
        UUID etranger = fixture.reglement(j, autre, autre, MARS);

        String corps = page(FOURNISSEURS, cible, REGLEMENTS);

        assertAll(
                () -> assertThat(ids(corps)).containsExactlyInAnyOrder(commeTiers.toString(), commePayeur.toString(),
                        lesDeux.toString()).doesNotContain(etranger.toString()),
                () -> assertThat(total(corps)).isEqualTo(3));
    }

    // ------------------------------------------------------------------ R6 : dossiers vente + location + co-acquéreur

    @ParameterizedTest
    @ValueSource(strings = {"FOURNISSEURS", "CONTACTS", "PERSONNELS"})
    void reponseR6_dossiers_fusionVenteLocationCoAcquereur_avecNature(String famille) throws Exception {
        TypeTiers type = TypeTiers.valueOf(famille);
        UUID cible = j.tiers(type, "D").inserer();
        UUID client = j.tiers(type, "E").inserer();
        UUID vente = fixture.dossierVente(j, cible, JANVIER);
        UUID location = fixture.dossierLocation(j, cible, FEVRIER);
        UUID dossierDuClient = fixture.dossierVente(j, client, MARS);
        fixture.coAcquereur(dossierDuClient, cible);
        UUID etranger = fixture.dossierVente(j, client, MARS);

        String corps = page(type, cible, DOSSIERS);

        List<String> natures = JsonPath.read(corps, "$.content[*].nature");
        assertAll(
                () -> assertThat(total(corps)).isEqualTo(3),
                () -> assertThat(ids(corps)).contains(vente.toString(), location.toString())
                        .doesNotContain(etranger.toString()),
                () -> assertThat(natures).doesNotContainNull().doesNotHaveDuplicates().hasSize(3));
    }

    // ------------------------------------------------------------------ règle 6 : congés sans montant ni salaire

    @Test
    void regle6_conges_aucunMontantNiSalaire() throws Exception {
        UUID employe = FixturePersonnels.employe(j, "P").inserer();
        fixture.conge(j, employe, JANVIER);

        String corps = page(PERSONNELS, employe, CONGES);

        FixturePersonnels.assertSansPaie(corps);
        assertThat(cles(JsonPath.parse(corps).read("$.content"))).isNotEmpty()
                .noneMatch(c -> CLE_MONTANT.matcher(c).matches());
    }

    private static List<String> cles(Object noeud) {
        List<String> cles = new ArrayList<>();
        if (noeud instanceof Map<?, ?> objet) {
            objet.forEach((cle, valeur) -> {
                cles.add(String.valueOf(cle));
                cles.addAll(cles(valeur));
            });
        } else if (noeud instanceof Collection<?> liste) {
            liste.forEach(element -> cles.addAll(cles(element)));
        }
        return cles;
    }
}
