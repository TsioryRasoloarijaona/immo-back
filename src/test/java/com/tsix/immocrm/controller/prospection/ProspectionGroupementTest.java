package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionVisites;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/prospections/groupes} et {@code GET /api/pre-prospections/groupes} — voir
 * {@code instructions/FEATURE-groupement-listes.md}.
 *
 * <p>Ne rejoue pas ce que {@code DossierGroupementTest} couvre déjà (granularité des dates,
 * libellé des tiers, substitution du groupe nul) : le moteur est partagé. Ce qui est testé ici,
 * c'est ce qui est <strong>propre à ce catalogue</strong> — le discriminant
 * prospection/pré-prospection, et le refus des colonnes servies par la vue légataire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionGroupementTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComProspectionKindRepository kindRepository;
    @Autowired
    private ComProspectionOriginRepository originRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private ComActionMessageRepository actionMessageRepository;
    @Autowired
    private ComActionMessageTypeRepository typeRepository;
    @Autowired
    private ComActionMessageCategoryRepository categorieRepository;
    @Autowired
    private CfgProjectRepository projectRepository;
    @Autowired
    private CfgTrancheRepository trancheRepository;

    /** Identifiants en dur de la vue légataire — voir {@link ComProspectionVisites}. */
    private static final UUID ID_TYPE_VISITE =
            UUID.fromString("fea94cec-b244-4091-9cec-a9b2cffb917c");
    private static final UUID ID_CATEGORIE_PROSPECTION =
            UUID.fromString("69f8e7d6-1111-1111-1111-1aa8b1f02008");

    private ComProspectionKind newEtape(String code) {
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(code);
        return kindRepository.save(kind);
    }

    private ComProspectionOrigin newOrigine(String code) {
        ComProspectionOrigin origin = new ComProspectionOrigin();
        origin.setId(UUID.randomUUID());
        origin.setCode(code);
        return originRepository.save(origin);
    }

    private CfgTier newProspect(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName("Nom-" + code);
        return tierRepository.save(tier);
    }

    /** Qualifiée : porte un prospect, donc relève de {@code /api/prospections}. */
    private ComProspection newProspection(String code, ComProspectionKind etape, CfgTier prospect) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setComProspectionKind(etape);
        p.setCfgProspect(prospect);
        return prospectionRepository.save(p);
    }

    /**
     * Une interaction de type « visite », telle que {@link ComProspectionVisites} la reconnaît.
     *
     * <p>Les deux identifiants sont ceux, en dur, de la vue légataire — ce sont eux qui définissent
     * « visite » dans tout le système. Les fixtures de référentiel sont créées avec ces PKey
     * exacts : une visite sur un autre type ne serait pas comptée, ce qui est le comportement
     * voulu mais rendrait le test faussement vert.
     */
    private void visite(ComProspection prospection, LocalDateTime quand) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(quand);
        m.setComProspection(prospection);
        m.setComActionMessageType(typeVisite());
        m.setComActionMessageCategory(categorieProspection());
        m.setIsSystem(false);
        actionMessageRepository.save(m);
    }

    private ComActionMessageType typeVisite() {
        return typeRepository.findById(ID_TYPE_VISITE).orElseGet(() -> {
            ComActionMessageType t = new ComActionMessageType();
            t.setId(ID_TYPE_VISITE);
            t.setCode("VISITE");
            return typeRepository.save(t);
        });
    }

    private ComActionMessageCategory categorieProspection() {
        return categorieRepository.findById(ID_CATEGORIE_PROSPECTION).orElseGet(() -> {
            ComActionMessageCategory c = new ComActionMessageCategory();
            c.setId(ID_CATEGORIE_PROSPECTION);
            c.setCode("PROSPECTION");
            return categorieRepository.save(c);
        });
    }

    /** Brute : ni prospect ni commercial, donc relève de {@code /api/pre-prospections}. */
    private ComProspection newPreProspection(String code, ComProspectionOrigin origine) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setComProspectionOrigin(origine);
        return prospectionRepository.save(p);
    }

    // ------------------------------------------------------------------
    // Prospections
    // ------------------------------------------------------------------

    @Test
    void groupeParEtape_rendUnGroupeParEtape() throws Exception {
        ComProspectionKind contact = newEtape("GRP-P-CONTACT");
        ComProspectionKind visite = newEtape("GRP-P-VISITE");
        CfgTier prospect = newProspect("TIER-GRP-P");

        newProspection("GRPP-1", contact, prospect);
        newProspection("GRPP-2", contact, prospect);
        newProspection("GRPP-3", visite, prospect);

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "etape")
                        .param("search", "GRPP-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-P-CONTACT')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-P-VISITE')].nombre").value(1));
    }

    @Test
    void sommeDesEffectifs_egaleLeTotalDeLaListe() throws Exception {
        ComProspectionKind etape = newEtape("GRP-P-INV");
        CfgTier prospect = newProspect("TIER-GRP-P-INV");
        newProspection("GRPQ-1", etape, prospect);
        newProspection("GRPQ-2", etape, prospect);
        // Sans étape : le groupe à clé nulle, que COUNT(DISTINCT) ne compte pas de lui-même.
        newProspection("GRPQ-3", null, prospect);

        mockMvc.perform(get("/api/prospections").param("search", "GRPQ-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // `inclureSansValeur=true` : c'est ce qui rend la somme des effectifs comparable au total
        // de la liste. Le défaut écarte le groupe nul, et l'invariant ne tient alors pas.
        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "etape")
                        .param("inclureSansValeur", "true")
                        .param("search", "GRPQ-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-P-INV')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle == null)].nombre").value(1));
    }

    @Test
    void depliage_rendExactementLesLignesComptees() throws Exception {
        ComProspectionKind vise = newEtape("GRP-P-DEPLI-VISE");
        ComProspectionKind autre = newEtape("GRP-P-DEPLI-AUTRE");
        CfgTier prospect = newProspect("TIER-GRP-P-DEPLI");
        ComProspection attendue = newProspection("GRPR-1", vise, prospect);
        ComProspection exclue = newProspection("GRPR-2", autre, prospect);

        mockMvc.perform(get("/api/prospections")
                        .param("search", "GRPR-")
                        .param("groupeColonne", "etape")
                        .param("groupeValeur", "GRP-P-DEPLI-VISE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + attendue.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + exclue.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Ce qui est propre à ce catalogue : les colonnes de la vue légataire
    // ------------------------------------------------------------------

    /**
     * {@code joursInactivite} agrège toute la catégorie « Prospection » — 46 667 lignes de
     * {@code ComActionMessage}. Mesurée à <b>~1 s à chaud</b> contre 6 ms pour la famille des
     * visites, qui n'en lit que 4 292. Elle reste donc refusée, et le refus doit être un 400
     * lisible plutôt qu'une {@code SQLGrammarException} en 500.
     */
    @Test
    void groupBy_surColonneDeLaVue_tropCouteuse_rend400() throws Exception {
        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "joursInactivite"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "derniereActionDate"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Dernière visite : la colonne de vue rendue groupable par pré-agrégation
    // ------------------------------------------------------------------

    /**
     * Le cas qui a motivé ce lot. {@code derniereVisiteDate} vient de la vue légataire, mais sa
     * formule est un {@code MAX(ComActionMessage.DocDate)} filtré sur le type « visite » — donc
     * calculable sans elle, et cent fois moins cher ({@link ComProspectionVisites}).
     *
     * <p>Deux visites sur la même prospection doivent former <strong>un</strong> groupe, à la date
     * de la plus récente : c'est ce que « dernière visite » veut dire.
     */
    @Test
    void groupeParDerniereVisite_prendLaPlusRecenteParProspection() throws Exception {
        CfgTier prospect = newProspect("TIER-GRP-VIS");
        ComProspection deuxVisites = newProspection("GRPV-1", null, prospect);
        visite(deuxVisites, LocalDateTime.of(2025, 3, 10, 9, 0));
        visite(deuxVisites, LocalDateTime.of(2025, 5, 20, 14, 0));   // la dernière
        ComProspection uneVisite = newProspection("GRPV-2", null, prospect);
        visite(uneVisite, LocalDateTime.of(2025, 5, 20, 8, 0));      // même jour
        newProspection("GRPV-3", null, prospect);                     // aucune visite

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "derniereVisiteDate")
                        .param("granularite", "JOUR")
                        .param("search", "GRPV-"))
                .andExpect(status().isOk())
                // Un seul groupe : le 20/05. Mars n'apparaît pas — ce n'est la DERNIÈRE visite de
                // personne. Et la prospection sans visite est écartée par défaut.
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].cle").value(20250520))
                .andExpect(jsonPath("$.content[0].nombre").value(2));
    }

    /** Le dépliage doit rendre exactement les prospections comptées dans le groupe. */
    @Test
    void depliageParDerniereVisite_rendLesLignesComptees() throws Exception {
        CfgTier prospect = newProspect("TIER-GRP-VIS-D");
        ComProspection visitee = newProspection("GRPW-1", null, prospect);
        visite(visitee, LocalDateTime.of(2025, 6, 2, 10, 0));
        ComProspection autre = newProspection("GRPW-2", null, prospect);
        visite(autre, LocalDateTime.of(2025, 7, 2, 10, 0));

        mockMvc.perform(get("/api/prospections")
                        .param("search", "GRPW-")
                        .param("groupeColonne", "derniereVisiteDate")
                        .param("granularite", "JOUR")
                        .param("groupeValeur", "20250602"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + visitee.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + autre.getId() + "')]").doesNotExist());
    }

    @Test
    void groupeParNbVisites_compteEtSAdditionne() throws Exception {
        CfgTier prospect = newProspect("TIER-GRP-NB");
        ComProspection deux = newProspection("GRPX-1", null, prospect);
        visite(deux, LocalDateTime.of(2025, 1, 5, 9, 0));
        visite(deux, LocalDateTime.of(2025, 2, 5, 9, 0));
        ComProspection une = newProspection("GRPX-2", null, prospect);
        visite(une, LocalDateTime.of(2025, 1, 5, 9, 0));

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "nbVisites")
                        .param("agregats", "nbVisites")
                        .param("search", "GRPX-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==2)].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle==1)].nombre").value(1));
    }

    // ------------------------------------------------------------------
    // Repli : la clé de groupe doit dire ce que la ligne affiche
    // ------------------------------------------------------------------

    /**
     * {@code telephone} est un repli fiche &gt; saisie. Le mappeur utilise
     * {@code StringUtils.hasText}, qui écarte aussi les chaînes blanches — d'où le
     * {@code NULLIF(TRIM(…), '')} de la clé. Une fiche au mobile <em>blanc</em> doit donc tomber
     * dans le même groupe que celle qui n'en a pas du tout, et non former le sien.
     */
    @Test
    void groupeParTelephone_replieSurLaFichePuisLaSaisie_etIgnoreLesBlancs() throws Exception {
        CfgTier avecMobile = newProspect("TIER-GRP-TEL-A");
        avecMobile.setMobile("0600000001");
        tierRepository.save(avecMobile);
        CfgTier mobileBlanc = newProspect("TIER-GRP-TEL-B");
        mobileBlanc.setMobile("   ");
        tierRepository.save(mobileBlanc);

        ComProspection parLaFiche = newProspection("GRPY-1", null, avecMobile);
        ComProspection parLaSaisie = newProspection("GRPY-2", null, mobileBlanc);
        parLaSaisie.setTempTel("0600000002");
        prospectionRepository.save(parLaSaisie);

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "telephone")
                        .param("search", "GRPY-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='0600000001')].nombre").value(1))
                // Le mobile blanc ne fait pas groupe : c'est le numéro saisi qui sort.
                .andExpect(jsonPath("$.content[?(@.cle=='0600000002')].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle=='   ')]").doesNotExist());
    }

    /** Le projet était étiqueté `Source.VUE` mais s'atteint par tranche → projet. */
    @Test
    void groupeParProjet_passeParLaTrancheEtNonParLaVue() throws Exception {
        // save() renvoie l'instance gérée, pas celle passée : l'identifiant étant assigné à la
        // main, Spring Data part sur un merge. Réutiliser les variables locales laisserait des
        // instances transientes accrochées à la prospection.
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-GRP-PROSP");
        projet = projectRepository.save(projet);
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-GRP-PROSP");
        tranche.setCfgProject(projet);
        tranche = trancheRepository.save(tranche);

        ComProspection p = newProspection("GRPZ-1", null, newProspect("TIER-GRP-PRJ"));
        p.setStkHierarchy(tranche);
        prospectionRepository.save(p);

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "projetCode")
                        .param("search", "GRPZ-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='PRJ-GRP-PROSP')].nombre").value(1));
    }

    @Test
    void groupBy_surColonneDeConversion_rend400() throws Exception {
        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "statutDossier"))
                .andExpect(status().isBadRequest());
    }

    /**
     * {@code typeProspection} vient de {@code ComProspection.StrEntityId}, un {@code UUID} brut et
     * non une association : il n'y a rien à joindre vers {@code StrEntity} pour en tirer un
     * libellé. Exclue pour une raison différente des colonnes de vue, même refus.
     */
    @Test
    void groupBy_surTypeProspection_rend400() throws Exception {
        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "typeProspection"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Pré-prospections : même moteur, discriminant inverse
    // ------------------------------------------------------------------

    @Test
    void preProspections_groupeParOrigine() throws Exception {
        ComProspectionOrigin facebook = newOrigine("GRP-PP-FACEBOOK");
        ComProspectionOrigin siteWeb = newOrigine("GRP-PP-SITE");

        newPreProspection("GRPS-1", facebook);
        newPreProspection("GRPS-2", facebook);
        newPreProspection("GRPS-3", siteWeb);

        mockMvc.perform(get("/api/pre-prospections/groupes")
                        .param("groupBy", "origine")
                        .param("search", "GRPS-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-PP-FACEBOOK')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-PP-SITE')].nombre").value(1));
    }

    /**
     * Le discriminant doit valoir des deux côtés : une prospection qualifiée ne doit pas être
     * comptée dans les groupes des pré-prospections, sinon les effectifs annoncent des lignes que
     * le dépliage ne rendra jamais.
     */
    @Test
    void preProspections_neComptentPasLesProspectionsQualifiees() throws Exception {
        ComProspectionOrigin origine = newOrigine("GRP-PP-DISCRIMINANT");
        newPreProspection("GRPT-1", origine);

        ComProspection qualifiee = newProspection("GRPT-2", null, newProspect("TIER-GRPT"));
        qualifiee.setComProspectionOrigin(origine);
        prospectionRepository.save(qualifiee);

        mockMvc.perform(get("/api/pre-prospections/groupes")
                        .param("groupBy", "origine")
                        .param("search", "GRPT-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-PP-DISCRIMINANT')].nombre").value(1));

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "origine")
                        .param("search", "GRPT-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-PP-DISCRIMINANT')].nombre").value(1));
    }

    @Test
    void preProspections_depliage() throws Exception {
        ComProspectionOrigin visee = newOrigine("GRP-PP-DEPLI-VISEE");
        ComProspectionOrigin autre = newOrigine("GRP-PP-DEPLI-AUTRE");
        ComProspection attendue = newPreProspection("GRPU-1", visee);
        ComProspection exclue = newPreProspection("GRPU-2", autre);

        mockMvc.perform(get("/api/pre-prospections")
                        .param("search", "GRPU-")
                        .param("groupeColonne", "origine")
                        .param("groupeValeur", "GRP-PP-DEPLI-VISEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + attendue.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + exclue.getId() + "')]").doesNotExist());
    }
}
