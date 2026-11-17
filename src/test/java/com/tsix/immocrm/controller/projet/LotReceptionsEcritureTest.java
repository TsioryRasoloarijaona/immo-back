package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotAvenants;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.CODE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DATE_RECEPTION;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.GENRE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.LIEU;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.RECEPTIONS;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.TIERS_INVITES;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.TYPE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.reception;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.reserve;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.tiersInvite;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.LIGNE_PERIMEE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fiche réception (FEATURE-lot-avenants-avances.md §4.3, R13-R17, §9) : réserves et tiers invités envoyés entiers avec la réception,
 * validation, suppression en cascade, aucun recalcul (prévisions ni échéances), aucun droit propre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotReceptionsEcritureTest {

    private static final String RESERVES_TABLE = FixtureLotAvenants.RESERVES;
    private static final String TIERS_TABLE = FixtureLotAvenants.TIERS_INVITES;
    private static final String RECEPTIONS_TABLE = FixtureLotAvenants.RECEPTIONS;
    private static final String COMPTE_RECEPTIONS = "SELECT COUNT(*) FROM PrjMarketReception WHERE PrjMarketId = ?";
    private static final String DESCRIPTION_COL = "Description";
    private static final String CHANTIER = "Chantier";
    private static final LocalDate JUIN = LocalDate.of(2026, 6, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureLotSousFenetres sf;
    @Autowired private FixtureLotAvenants av;
    @Autowired private FixtureLotPrevisions prev;

    private AvenantsLotSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statut;
    private UUID type;
    private UUID provisoire;
    private UUID definitif;
    private UUID tiers1;
    private UUID tiers2;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        av.tables();
        appels = new AvenantsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        UUID societe = parite.entiteJuridique("RCE-" + jeton);
        projet = parite.projet("RCP-" + jeton, societe);
        statut = parite.statut(ENTREPRENEURS, "RCS-" + jeton);
        type = sf.typeReception("RCT-" + jeton);
        provisoire = sf.genreReception("RCO-" + jeton, false);
        definitif = sf.genreReception("RCD-" + jeton, true);
        tiers1 = lots.fournisseur("RC1-" + jeton);
        tiers2 = lots.fournisseur("RC2-" + jeton);
        admin = connecte(parite.administrateur("lot.rc." + jeton, projet), "Administrateur");
    }

    private UUID lot() {
        rang++;
        return parite.lot(projet, "RC" + rang + "-" + jeton, ENTREPRENEURS, statut);
    }

    private UUID receptionEnBase(UUID lot, UUID genre) {
        rang++;
        return sf.reception(lot, 1, "RCR" + rang + "-" + jeton, JUIN, false, CHANTIER, type, genre);
    }

    /** Réception DEF avec une réserve et un tiers invité. */
    private CorpsJson corps(UUID genre) {
        return reception(1, "R1", JUIN, type, genre,
                List.of(reserve(UUID.randomUUID(), 1, "Fissure", false)),
                List.of(tiersInvite(UUID.randomUUID(), 1, tiers1)));
    }

    private Map<String, Object> enBase(String table, UUID id) {
        List<Map<String, Object>> l = av.parId(table, id);
        assertThat(l).as(table + " " + id).hasSize(1);
        return l.get(0);
    }

    private static UUID id(Reponse r) {
        return UUID.fromString(String.valueOf(r.valeur("$.id")));
    }

    private List<Map<String, Object>> enfants(String table, UUID reception) {
        return av.lignes("SELECT * FROM " + table + " WHERE PrjMarketReceptionId = ?", reception.toString());
    }

    // ------------------------------------------------------------------ POST

    @Test
    void creation_201_receptionReservesEtTiersInsertes_detailRenvoye() throws Exception {
        UUID lot = lot();
        UUID reserve = UUID.randomUUID();
        UUID invite = UUID.randomUUID();
        CorpsJson corps = reception(3, "R1", JUIN, type, definitif,
                List.of(reserve(reserve, 1, "Fissure", true)), List.of(tiersInvite(invite, 1, tiers1)));

        Reponse r = appels.creer(admin, lot, RECEPTIONS, corps);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            UUID cree = id(r);
            Map<String, Object> enBase = enBase(RECEPTIONS_TABLE, cree);
            s.assertThat(enBase).containsEntry("Code", "R1").containsEntry(DESCRIPTION_COL, "Réception R1")
                    .containsEntry("MeetingPlace", CHANTIER).containsEntry("sorting", 3).containsEntry("RequiredBySupplier", false);
            s.assertThat(((Timestamp) enBase.get("DocDate")).toLocalDateTime().toLocalDate()).isEqualTo(JUIN);
            s.assertThat(String.valueOf(enBase.get("PrjMarketId"))).isEqualToIgnoringCase(lot.toString());
            s.assertThat(String.valueOf(enBase.get("PrjMarketReceptionTypeId"))).isEqualToIgnoringCase(type.toString());
            s.assertThat(String.valueOf(enBase.get("PrjMarketReceptionKindId"))).isEqualToIgnoringCase(definitif.toString());
            s.assertThat(enBase(RESERVES_TABLE, reserve)).containsEntry(DESCRIPTION_COL, "Fissure").containsEntry("IsFixed", true);
            s.assertThat(String.valueOf(enBase(RESERVES_TABLE, reserve).get("PrjMarketReceptionId"))).isEqualToIgnoringCase(cree.toString());
            s.assertThat(String.valueOf(enBase(TIERS_TABLE, invite).get("CfgTierId"))).isEqualToIgnoringCase(tiers1.toString());
            s.assertThat(String.valueOf(enBase(TIERS_TABLE, invite).get("PrjMarketReceptionId"))).isEqualToIgnoringCase(cree.toString());
            s.assertThat(r.valeur("$.lotId")).isEqualTo(lot.toString());
            s.assertThat(r.valeur("$.definitive")).isEqualTo(true);
            s.assertThat(r.valeur("$.typeDescription")).isEqualTo("Type RCT-" + jeton);
            s.assertThat(r.valeur("$.genreDescription")).isEqualTo("Genre RCD-" + jeton);
            s.assertThat(r.ids("$.reserves[*].id")).containsExactly(reserve.toString());
            s.assertThat(r.ids("$.tiersInvites[*].tiersId")).containsExactly(tiers1.toString());
            s.assertThat(String.valueOf(r.valeur("$.tiersInvites[0].tiers"))).isNotBlank().isNotEqualTo("null");
        });
    }

    // ------------------------------------------------------------------ R13 : 400

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"codeManquant", "dateManquante", "typeManquant", "typeInconnu", "genreManquant", "genreInconnu",
            "reserveSansDescription", "tiersInconnu", "tiersEnDouble", "codeTropLong", "descriptionTropLongue", "lieuTropLong",
            "reserveTropLongue"})
    void r13_corpsInvalide_400_rienNEstCree(String cas) throws Exception {
        UUID lot = lot();
        CorpsJson res = reserve(UUID.randomUUID(), 1, "Fissure", false);
        CorpsJson invite = tiersInvite(UUID.randomUUID(), 1, tiers1);
        List<CorpsJson> invites = new ArrayList<>(List.of(invite));
        CorpsJson corps = reception(1, "R1", JUIN, type, definitif, List.of(res), invites);
        switch (cas) {
            case "codeManquant" -> corps.avec(CODE, null);
            case "dateManquante" -> corps.avec(DATE_RECEPTION, null);
            case "typeManquant" -> corps.avec(TYPE, null);
            case "typeInconnu" -> corps.avec(TYPE, UUID.randomUUID());
            case "genreManquant" -> corps.avec(GENRE, null);
            case "genreInconnu" -> corps.avec(GENRE, UUID.randomUUID());
            case "reserveSansDescription" -> res.avec(DESCRIPTION, null);
            case "tiersInconnu" -> invite.avec(AvenantsLotSupport.TIERS, UUID.randomUUID());
            case "tiersEnDouble" -> invites.add(tiersInvite(UUID.randomUUID(), 2, tiers1));
            case "codeTropLong" -> corps.avec(CODE, "C".repeat(256));
            case "descriptionTropLongue" -> corps.avec(DESCRIPTION, "D".repeat(256));
            case "lieuTropLong" -> corps.avec(LIEU, "L".repeat(256));
            case "reserveTropLongue" -> res.avec(DESCRIPTION, "R".repeat(256));
            default -> throw new IllegalArgumentException(cas);
        }
        corps.avec(TIERS_INVITES, invites);

        Reponse r = appels.creer(admin, lot, RECEPTIONS, corps);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(cas + " : " + r.corps()).isEqualTo(400);
            s.assertThat(av.compter(COMPTE_RECEPTIONS, lot.toString())).isZero();
        });
    }

    // R13 (témoin) : le corps de base des cas invalides est accepté.
    @Test
    void r13_temoin_corpsDeBase_201() throws Exception {
        Reponse r = appels.creer(admin, lot(), RECEPTIONS, corps(definitif));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    // ------------------------------------------------------------------ PUT : réserves et tiers envoyés entiers

    @Test
    void put_reservesEtTiers_insertionModificationSuppression_unSeulEnvoi() throws Exception {
        UUID lot = lot();
        UUID rec = receptionEnBase(lot, provisoire);
        UUID gardee = av.reserve(rec, 1, "Fissure", false);
        UUID retiree = av.reserve(rec, 2, "Peinture", false);
        UUID inviteGarde = av.tiersInvite(rec, 1, tiers1);
        UUID inviteRetire = av.tiersInvite(rec, 2, tiers2);
        UUID nouvelleReserve = UUID.randomUUID();
        UUID nouvelInvite = UUID.randomUUID();
        CorpsJson corps = reception(1, "R2", JUIN, type, definitif,
                List.of(reserve(gardee, 1, "Fissure levée", true), reserve(nouvelleReserve, 2, "Joint", false)),
                List.of(tiersInvite(inviteGarde, 1, tiers1), tiersInvite(nouvelInvite, 2, tiers2)));

        Reponse r = appels.modifier(admin, lot, RECEPTIONS, rec, corps);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase(RECEPTIONS_TABLE, rec)).containsEntry("Code", "R2");
            s.assertThat(enBase(RESERVES_TABLE, gardee)).containsEntry(DESCRIPTION_COL, "Fissure levée").containsEntry("IsFixed", true);
            s.assertThat(av.parId(RESERVES_TABLE, retiree)).isEmpty();
            s.assertThat(enBase(RESERVES_TABLE, nouvelleReserve)).containsEntry(DESCRIPTION_COL, "Joint");
            s.assertThat(av.parId(TIERS_TABLE, inviteGarde)).hasSize(1);
            s.assertThat(av.parId(TIERS_TABLE, inviteRetire)).isEmpty();
            s.assertThat(String.valueOf(enBase(TIERS_TABLE, nouvelInvite).get("CfgTierId"))).isEqualToIgnoringCase(tiers2.toString());
            s.assertThat(r.valeur("$.definitive")).isEqualTo(true);
            s.assertThat(r.ids("$.reserves[*].id")).containsExactlyInAnyOrder(gardee.toString(), nouvelleReserve.toString());
        });
    }

    @Test
    void put_reserveDUneAutreReception_400_reserveEtrangereIntacte() throws Exception {
        UUID lot = lot();
        UUID rec = receptionEnBase(lot, provisoire);
        UUID etrangere = av.reserve(receptionEnBase(lot, provisoire), 1, "Fissure", false);

        Reponse r = appels.modifier(admin, lot, RECEPTIONS, rec,
                reception(1, "R1", JUIN, type, provisoire, List.of(reserve(etrangere, 1, "Volée", false)), List.of()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(enBase(RESERVES_TABLE, etrangere)).containsEntry(DESCRIPTION_COL, "Fissure");
        });
    }

    @Test
    void put_tiersInviteDUneAutreReception_400() throws Exception {
        UUID lot = lot();
        UUID rec = receptionEnBase(lot, provisoire);
        UUID autre = receptionEnBase(lot, provisoire);
        UUID etranger = av.tiersInvite(autre, 1, tiers1);

        Reponse r = appels.modifier(admin, lot, RECEPTIONS, rec,
                reception(1, "R1", JUIN, type, provisoire, List.of(), List.of(tiersInvite(etranger, 1, tiers2))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(String.valueOf(enBase(TIERS_TABLE, etranger).get("PrjMarketReceptionId"))).isEqualToIgnoringCase(autre.toString());
        });
    }

    // ------------------------------------------------------------------ R14 : suppression

    @Test
    void r14_suppression_204_reservesEtTiersSupprimes_autreReceptionIntacte() throws Exception {
        UUID lot = lot();
        UUID rec = receptionEnBase(lot, definitif);
        av.reserve(rec, 1, "Fissure", false);
        av.tiersInvite(rec, 1, tiers1);
        UUID autre = receptionEnBase(lot, definitif);
        UUID reserveAutre = av.reserve(autre, 1, "Joint", false);

        Reponse r = appels.supprimer(admin, lot, RECEPTIONS, rec);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(204);
            s.assertThat(av.parId(RECEPTIONS_TABLE, rec)).isEmpty();
            s.assertThat(enfants(RESERVES_TABLE, rec)).isEmpty();
            s.assertThat(enfants(TIERS_TABLE, rec)).isEmpty();
            s.assertThat(av.parId(RESERVES_TABLE, reserveAutre)).hasSize(1);
        });
    }

    // ------------------------------------------------------------------ GET détail

    @Test
    void lecture_detail_genreProvisoire_nonDefinitive_sousGrilles() throws Exception {
        UUID lot = lot();
        UUID rec = receptionEnBase(lot, provisoire);
        UUID res = av.reserve(rec, 1, "Fissure", true);
        av.tiersInvite(rec, 1, tiers2);

        Reponse r = appels.lire(admin, lot, RECEPTIONS, rec);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.valeur("$.definitive")).as(r.corps()).isEqualTo(false);
            s.assertThat(r.valeur("$.genreId")).isEqualTo(provisoire.toString());
            s.assertThat(r.valeur("$.dateReception")).isEqualTo("2026-06-30");
            s.assertThat(r.valeur("$.reserves[0].id")).isEqualTo(res.toString());
            s.assertThat(r.valeur("$.reserves[0].levee")).isEqualTo(true);
            s.assertThat(r.valeur("$.tiersInvites[0].tiersId")).isEqualTo(tiers2.toString());
        });
    }

    // ------------------------------------------------------------------ ordre 2 : réception d'un autre lot

    @Test
    void ordre2_receptionDUnAutreLot_404_getPutDelete() throws Exception {
        UUID lot = lot();
        UUID propre = receptionEnBase(lot, provisoire);
        UUID etrangere = receptionEnBase(lot(), provisoire);

        Reponse temoin = appels.lire(admin, lot, RECEPTIONS, propre);
        Reponse lecture = appels.lire(admin, lot, RECEPTIONS, etrangere);
        Reponse modification = appels.modifier(admin, lot, RECEPTIONS, etrangere, corps(provisoire));
        Reponse suppression = appels.supprimer(admin, lot, RECEPTIONS, etrangere);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
            s.assertThat(lecture.statut()).isEqualTo(404);
            s.assertThat(modification.statut()).isEqualTo(404);
            s.assertThat(suppression.statut()).isEqualTo(404);
            s.assertThat(av.parId(RECEPTIONS_TABLE, etrangere)).hasSize(1);
        });
    }

    // ------------------------------------------------------------------ R15 : aucun recalcul

    @Test
    void r15_creationReceptionDefinitive_previsionsNonRecalculees() throws Exception {
        prev.preparer();
        UUID lot = lot();
        prev.planning(lot, PrevisionsLotSupport.DEBUT_2026, PrevisionsLotSupport.DUREE_90, PrevisionsLotSupport.DEBUT_2026,
                PrevisionsLotSupport.DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0", "0.1", "0");
        prev.multiFournisseur(lot, false);
        prev.lignePerimee(lot);

        Reponse r = appels.creer(admin, lot, RECEPTIONS, corps(definitif));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(prev.lignesPaiement(lot)).as("ligne périmée conservée : pas de recalcul")
                    .extracting(l -> String.valueOf(l.get(DESCRIPTION_COL))).contains(LIGNE_PERIMEE);
        });
    }

    // R15 : une réception DEF qui déplace la date R10 ne recalcule pas les échéances existantes.
    @Test
    void r15_creationReceptionDefinitive_echeancesExistantesNonRecalculees() throws Exception {
        UUID lot = lot();
        LocalDate echeance = LocalDate.of(2026, 3, 31);
        UUID existante = sf.echeanceRg(lot, 1, "Échéance 1", 30, echeance, "0.5");

        Reponse r = appels.creer(admin, lot, RECEPTIONS, corps(definitif));

        Map<String, Object> enBase = enBase(FixtureLotAvenants.ECHEANCES, existante);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(((Timestamp) enBase.get("EcheanceDate")).toLocalDateTime().toLocalDate()).isEqualTo(echeance);
            s.assertThat(enBase).containsEntry("Delay", 30);
        });
    }

    // ------------------------------------------------------------------ R16 : aucun droit propre ; ordre 1

    @Test
    void r16_compteSansAucunDroit_creationSurLotVisible_201_lotInvisible_404() throws Exception {
        UUID visible = lot();
        UUID autreProjet = parite.projet("RCX-" + jeton, parite.entiteJuridique("RCY-" + jeton));
        UUID invisible = parite.lot(autreProjet, "RCH-" + jeton, ENTREPRENEURS, statut);
        RequestPostProcessor sansDroit = connecte(parite.avecDroits("lot.rc1." + jeton, List.of(), projet), "Commercial");

        Reponse r = appels.creer(sansDroit, visible, RECEPTIONS, corps(provisoire));
        Reponse cache = appels.creer(sansDroit, invisible, RECEPTIONS, corps(provisoire));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(cache.statut()).as(cache.corps()).isEqualTo(404);
            s.assertThat(av.compter(COMPTE_RECEPTIONS, invisible.toString())).isZero();
        });
    }

    // ------------------------------------------------------------------ R17 : audit

    @Test
    void r17_creation_colonnesDAuditDeLaReceptionEtDesReserves() throws Exception {
        UUID reserve = UUID.randomUUID();

        Reponse r = appels.creer(admin, lot(), RECEPTIONS,
                reception(1, "R1", JUIN, type, definitif, List.of(reserve(reserve, 1, "Fissure", false)), List.of()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            Map<String, Object> rec = enBase(RECEPTIONS_TABLE, id(r));
            Map<String, Object> res = enBase(RESERVES_TABLE, reserve);
            s.assertThat(String.valueOf(rec.get("UserCreate"))).isNotBlank().isNotEqualTo("null");
            s.assertThat(rec.get("UserCreateDateTime")).isNotNull();
            s.assertThat(String.valueOf(res.get("UserCreate"))).isNotBlank().isNotEqualTo("null");
            s.assertThat(res.get("UserCreateDateTime")).isNotNull();
        });
    }

    @Test
    void r17_modification_colonnesDAuditDeLaReceptionEtDeLaReserveModifiee() throws Exception {
        UUID lot = lot();
        UUID rec = receptionEnBase(lot, provisoire);
        UUID reserve = av.reserve(rec, 1, "Fissure", false);

        Reponse r = appels.modifier(admin, lot, RECEPTIONS, rec,
                reception(1, "R1", JUIN, type, provisoire, List.of(reserve(reserve, 1, "Fissure levée", true)), List.of()));

        Map<String, Object> enBase = enBase(RECEPTIONS_TABLE, rec);
        Map<String, Object> res = enBase(RESERVES_TABLE, reserve);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(String.valueOf(enBase.get("UserUpdate"))).isNotBlank().isNotEqualTo("null");
            s.assertThat(enBase.get("UserUpdateDateTime")).isNotNull();
            s.assertThat(String.valueOf(res.get("UserUpdate"))).isNotBlank().isNotEqualTo("null");
            s.assertThat(res.get("UserUpdateDateTime")).isNotNull();
        });
    }
}
