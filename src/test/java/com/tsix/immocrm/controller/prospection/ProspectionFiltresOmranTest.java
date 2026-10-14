package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtres des listes à parité OMRAN — voir {@code instructions/FEATURE-prospection-filtres.md}.
 * Chaque test isole ses lignes par un préfixe aléatoire porté par le code : la base de test est partagée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionFiltresOmranTest {

    private static final String PROSPECTIONS = "/api/prospections";
    private static final String PRE_PROSPECTIONS = "/api/pre-prospections";
    private static final String TOTAL = "$.totalElements";
    private static final String SEARCH = "search";
    private static final UUID ENTITE_PROSPECTION = UUID.fromString("e7c73f70-1111-40d9-aa78-a2825c42b601");
    // Vocation vente du légataire : la vue ne lit ComFolder que pour elle (sinon RntFolder).
    private static final UUID VOCATION_VENTE = UUID.fromString("7E729AD8-A030-4D20-91CF-9D8E9D89C318");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComProspectionRepository prospections;
    @Autowired
    private ComProspectionKindRepository etapes;
    @Autowired
    private CfgTierRepository tiers;
    @Autowired
    private ComFolderRepository dossiers;
    @Autowired
    private StrStatusRepository statuts;
    @Autowired
    private StkVocationRepository vocations;
    @Autowired
    private FixtureDossierVente fixture;

    private final String u = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    private final String prefixe = "FLT" + u;

    // ---------------------------------------------------------------- fabrique

    private ComProspection prospection(String suffixe, CfgTier prospect) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(prefixe + "-" + suffixe);
        p.setDocDate(LocalDateTime.of(2020, 3, 10, 9, 0));
        p.setIsClosed(false);
        p.setStrEntityId(ENTITE_PROSPECTION);
        p.setCfgProspect(prospect);
        return p;
    }

    private ComProspection preProspection(String suffixe, CfgTranche tranche) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(prefixe + "-" + suffixe);
        p.setDocDate(LocalDateTime.of(2020, 3, 10, 9, 0));
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setTempDescription("Lead " + suffixe);
        p.setStkHierarchy(tranche);
        return p;
    }

    private CfgTier tiers(String description, String prenom, String nom) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode(prefixe + "-T-" + UUID.randomUUID());
        t.setDescription(description);
        t.setFirstName(prenom);
        t.setLastName(nom);
        return tiers.save(t);
    }

    private ComProspection enregistrer(ComProspection p) {
        return prospections.saveAndFlush(p);
    }

    private StkVocation vocationVente() {
        return vocations.findById(VOCATION_VENTE).orElseGet(() -> {
            StkVocation v = new StkVocation();
            v.setId(VOCATION_VENTE);
            v.setCode("VENTE");
            return vocations.save(v);
        });
    }

    private void dossier(ComProspection p, boolean annule) {
        StrStatus s = new StrStatus();
        s.setId(UUID.randomUUID());
        s.setCode(prefixe + "-ST");
        s.setAnnulation(annule);
        s.setResiliation(false);
        statuts.save(s);
        ComFolder f = new ComFolder();
        f.setId(UUID.randomUUID());
        f.setCode(prefixe + "-D-" + UUID.randomUUID().toString().substring(0, 6));
        f.setStkVocation(vocationVente());
        f.setStrStatus(s);
        f.setComProspection(p);
        dossiers.saveAndFlush(f);
    }

    // ---------------------------------------------------------------- recherche (§1)

    @Test
    void recherche_trouveLeNomDuProspectLie_sansTempDescription() throws Exception {
        ComProspection p = enregistrer(prospection("A", tiers("Dupont" + u, null, null)));

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, "dupont" + u))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].id").value(p.getId().toString()));
    }

    @Test
    void recherche_ignoreLesEspaces_0612_34_trouve_061234() throws Exception {
        String chiffres = "%08d".formatted(ThreadLocalRandom.current().nextInt(100_000_000));
        CfgTier t = tiers(null, null, null);
        t.setMobile("06" + chiffres);
        tiers.save(t);
        ComProspection p = enregistrer(prospection("B", t));
        String saisie = "06 " + chiffres.substring(0, 2) + " " + chiffres.substring(2, 4) + " " + chiffres.substring(4);

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, saisie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + p.getId() + "')]").exists());
    }

    @Test
    void recherche_prenomEtNomDansLesDeuxOrdres() throws Exception {
        ComProspection p = enregistrer(prospection("C", tiers(null, "Amine" + u, "Zeroual" + u)));

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, "Zeroual" + u + " Amine" + u))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].id").value(p.getId().toString()));
        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, "amine" + u + "zeroual" + u))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1));
    }

    @Test
    void recherche_parCin() throws Exception {
        CfgTier t = tiers(null, null, null);
        t.setIDNumber("CIN" + u);
        tiers.save(t);
        ComProspection p = enregistrer(prospection("D", t));

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, "cin" + u))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].id").value(p.getId().toString()));
    }

    @Test
    void recherche_preProspection_parTempField() throws Exception {
        ComProspection p = preProspection("E", null);
        p.setTempField1("Champ libre " + u);
        enregistrer(p);

        mockMvc.perform(get(PRE_PROSPECTIONS).param(SEARCH, "champlibre" + u))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].id").value(p.getId().toString()));
    }

    @Test
    void recherche_listeEtGroupesComptentPareil() throws Exception {
        ComProspectionKind etape = new ComProspectionKind();
        etape.setId(UUID.randomUUID());
        etape.setCode(prefixe + "-ETAPE");
        etapes.save(etape);
        CfgTier t = tiers("Martin" + u, null, null);
        ComProspection a = prospection("F1", t);
        a.setComProspectionKind(etape);
        ComProspection b = prospection("F2", t);
        b.setComProspectionKind(etape);
        enregistrer(a);
        enregistrer(b);

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, "martin" + u))
                .andExpect(jsonPath(TOTAL).value(2));
        mockMvc.perform(get(PROSPECTIONS + "/groupes").param("groupBy", "etape").param(SEARCH, "martin" + u))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='" + prefixe + "-ETAPE')].nombre").value(2));
    }

    // ---------------------------------------------------------------- aboutie (§2)

    @Test
    void aboutie_ouiEtNon_sontComplementaires() throws Exception {
        ComProspection aboutie = prospection("G1", null);
        aboutie.setStkVocation(vocationVente());
        ComProspection annulee = prospection("G2", null);
        annulee.setStkVocation(vocationVente());
        ComProspection sansDossier = prospection("G3", null);
        dossier(enregistrer(aboutie), false);
        dossier(enregistrer(annulee), true);
        enregistrer(sansDossier);

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe).param("aboutie", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].id").value(aboutie.getId().toString()));
        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe).param("aboutie", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(2))
                .andExpect(jsonPath("$.content[?(@.id=='" + aboutie.getId() + "')]").doesNotExist());
    }

    // ---------------------------------------------------------------- période (§3)

    private ComProspection[] troisArchivages() {
        ComProspection mars = prospection("H1", null);
        mars.setClosingDate(LocalDateTime.of(2020, 3, 15, 18, 30));
        ComProspection avril = prospection("H2", null);
        avril.setClosingDate(LocalDateTime.of(2020, 4, 2, 8, 0));
        ComProspection sansDate = prospection("H3", null);
        return new ComProspection[]{enregistrer(mars), enregistrer(avril), enregistrer(sansDate)};
    }

    @Test
    void periodeSurDateArchivage_finSansHeure_inclutLeJourEntier() throws Exception {
        ComProspection[] p = troisArchivages();

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe).param("periodeSur", "ARCHIVAGE")
                        .param("dateDebut", "2020-03-01").param("dateFin", "2020-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].id").value(p[0].getId().toString()));
    }

    @Test
    void dateRenseignee_ouiEtNon_surDateArchivage_ignoreLesBornes() throws Exception {
        ComProspection[] p = troisArchivages();

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe).param("periodeSur", "ARCHIVAGE")
                        .param("dateRenseignee", "OUI").param("dateDebut", "2030-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(2));
        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe).param("periodeSur", "ARCHIVAGE")
                        .param("dateRenseignee", "NON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].id").value(p[2].getId().toString()));
    }

    @Test
    void dateDocument_finSansHeure_inclutLeJourEntier_etDateHeureAcceptee() throws Exception {
        ComProspection p = prospection("I", null);
        p.setDocDate(LocalDateTime.of(2020, 5, 20, 22, 15));
        enregistrer(p);

        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe).param("dateDebut", "2020-05-20")
                        .param("dateFin", "2020-05-20"))
                .andExpect(jsonPath(TOTAL).value(1));
        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe).param("dateDebut", "2020-05-20T00:00:00")
                        .param("dateFin", "2020-05-20T22:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(0));
    }

    @Test
    void dateIllisible_400() throws Exception {
        mockMvc.perform(get(PROSPECTIONS).param("dateFin", "20/05/2020"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- non affectés (§4) et projet (§5)

    @Test
    void preProspections_nonAffectes_ajouteLesLignesSansTranche() throws Exception {
        CfgTranche cochee = fixture.tranche(prefixe + "T1", true);
        CfgTranche autre = fixture.tranche(prefixe + "T2", true);
        ComProspection dans = enregistrer(preProspection("J1", cochee));
        ComProspection sans = enregistrer(preProspection("J2", null));
        enregistrer(preProspection("J3", autre));

        mockMvc.perform(get(PRE_PROSPECTIONS).param(SEARCH, prefixe).param("trancheId", cochee.getId().toString())
                        .param("inclureSansTranche", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(2))
                .andExpect(jsonPath("$.content[?(@.id=='" + dans.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + sans.getId() + "')]").exists());
        mockMvc.perform(get(PRE_PROSPECTIONS).param(SEARCH, prefixe).param("trancheId", cochee.getId().toString()))
                .andExpect(jsonPath(TOTAL).value(1));
    }

    @Test
    void preProspections_nonAffectes_sansTrancheCochee_neRendQueLesLignesSansTranche() throws Exception {
        CfgTranche tranche = fixture.tranche(prefixe + "T3", true);
        enregistrer(preProspection("L1", tranche));
        ComProspection sans = enregistrer(preProspection("L2", null));

        mockMvc.perform(get(PRE_PROSPECTIONS).param(SEARCH, prefixe).param("inclureSansTranche", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].id").value(sans.getId().toString()));
        mockMvc.perform(get(PRE_PROSPECTIONS + "/groupes").param("groupBy", "etape").param(SEARCH, prefixe)
                        .param("inclureSansTranche", "true").param("inclureSansValeur", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath("$.content[0].nombre").value(1));
    }

    @Test
    void preProspections_projetIdSurGroupes_memeTotalQueLaListe() throws Exception {
        ComProspectionKind etape = new ComProspectionKind();
        etape.setId(UUID.randomUUID());
        etape.setCode(prefixe + "-ETAPE");
        etapes.save(etape);
        CfgTranche visee = fixture.tranche(prefixe + "P1", true);
        CfgTranche autre = fixture.tranche(prefixe + "P2", true);
        ComProspection a = preProspection("K1", visee);
        a.setComProspectionKind(etape);
        ComProspection b = preProspection("K2", autre);
        b.setComProspectionKind(etape);
        enregistrer(a);
        enregistrer(b);
        String projet = visee.getCfgProject().getId().toString();

        mockMvc.perform(get(PRE_PROSPECTIONS).param(SEARCH, prefixe).param("projetId", projet))
                .andExpect(jsonPath(TOTAL).value(1));
        mockMvc.perform(get(PRE_PROSPECTIONS + "/groupes").param("groupBy", "etape").param(SEARCH, prefixe)
                        .param("projetId", projet))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='" + prefixe + "-ETAPE')].nombre").value(1));
    }

    // ---------------------------------------------------------------- support (§7)

    @Test
    void supportsOrigine_listePlate() throws Exception {
        mockMvc.perform(get("/api/referentiels/supports-origine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ---------------------------------------------------------------- dernier échange (§6)

    @Test
    void communicationDepuis_formatDate_200() throws Exception {
        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe).param("communicationDepuis", "2026-01-01")
                        .param("communicationJusqua", "2026-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    void communicationDepuis_dateHeureAvecDecalage_200() throws Exception {
        mockMvc.perform(get(PROSPECTIONS).param(SEARCH, prefixe)
                        .param("communicationDepuis", "2026-01-01T00:00:00.000Z")
                        .param("communicationJusqua", "2026-01-31T23:59:59+01:00"))
                .andExpect(status().isOk());
    }
}
