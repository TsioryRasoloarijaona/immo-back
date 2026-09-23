package com.tsix.immocrm.controller.suivi;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/suivi-actions[/groupes]} — l'écran OMRAN « Suivi des actions ». Voir
 * {@code instructions/FEATURE-suivi-actions.md}.
 *
 * <p>Chaque test tire un {@code UserName} unique et filtre dessus : la base de test est partagée
 * entre les classes, et c'est exactement le filtre de l'écran OMRAN.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SuiviActionControllerTest {

    private static final UUID ID_CATEGORIE_PROSPECTION = UUID.fromString("69F8E7D6-1111-1111-1111-1AA8B1F02008");
    private static final String URL = "/api/suivi-actions";

    @Autowired private MockMvc mockMvc;
    @Autowired private ComActionMessageRepository messageRepository;
    @Autowired private ComActionMessageCategoryRepository categorieRepository;
    @Autowired private ComActionMessageTypeRepository typeRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private StkItemTypeRepository itemTypeRepository;
    @Autowired private StkItemCategoryRepository itemCategoryRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;

    private String utilisateur;
    private ComActionMessageCategory prospection;
    private ComActionMessageCategory dossier;
    private ComActionMessageType appel;
    private ComActionMessageType visite;
    private CfgProject projet;
    private CfgTranche tranche;
    private CfgTier tiers;

    @BeforeEach
    void preparer() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        utilisateur = "Commercial " + s;
        prospection = categorieRepository.findById(ID_CATEGORIE_PROSPECTION).orElseGet(() -> {
            ComActionMessageCategory c = new ComActionMessageCategory();
            c.setId(ID_CATEGORIE_PROSPECTION);
            c.setCode("Prospection");
            c.setDescription("Prospection");
            return categorieRepository.save(c);
        });
        dossier = new ComActionMessageCategory();
        dossier.setId(UUID.randomUUID());
        dossier.setCode("DOSSIER-" + s);
        dossier.setDescription("Dossier de vente " + s);
        categorieRepository.save(dossier);
        appel = type("Téléphone Sortant " + s);
        visite = type("Visite " + s);
        projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-SUIVI-" + s);
        projet.setDescription("Programme " + s);
        projectRepository.save(projet);
        tranche = newTranche("TRS-" + s, projet);
        tiers = new CfgTier();
        tiers.setId(UUID.randomUUID());
        tiers.setCode("TIER-SUIVI-" + s);
        tiers.setDescription("Prospect " + s);
        tierRepository.save(tiers);
    }

    private ComActionMessageType type(String libelle) {
        ComActionMessageType t = new ComActionMessageType();
        t.setId(UUID.randomUUID());
        t.setCode(libelle);
        t.setDescription(libelle);
        return typeRepository.save(t);
    }

    private CfgTranche newTranche(String code, CfgProject p) {
        CfgTranche t = new CfgTranche();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        t.setDescription("Tranche " + code);
        t.setCfgProject(p);
        return trancheRepository.save(t);
    }

    private ComProspection newProspection(CfgTranche t) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-SUIVI-" + UUID.randomUUID());
        p.setStkHierarchy(t);
        return prospectionRepository.save(p);
    }

    private ComActionMessage action(ComActionMessageCategory categorie, ComActionMessageType type,
                                    ComProspection p, StkItem article, LocalDateTime date) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(date);
        m.setUserName(utilisateur);
        m.setComActionMessageCategory(categorie);
        m.setComActionMessageType(type);
        m.setComProspection(p);
        m.setStkItem(article);
        m.setCfgTier(tiers);
        m.setIsSystem(false);
        m.setIsUnpaid(false);
        m.setAlertMessage("note " + date);
        return messageRepository.saveAndFlush(m);
    }

    private StkItem newArticle(CfgTranche t) {
        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SUIVI-" + UUID.randomUUID());
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(itemType);
        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SUIVI-" + UUID.randomUUID());
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(itemCategory);
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SUIVI-" + UUID.randomUUID().toString().substring(0, 6));
        vocationRepository.save(vocation);
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setStkVocation(vocation);
        article.setCode("LOT-SUIVI-" + UUID.randomUUID().toString().substring(0, 6));
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkHierarchy(t);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        return itemRepository.save(article);
    }

    // ---------------------------------------------------------------- liste

    /** Les colonnes de la grille OMRAN, tranche résolue par la prospection (catégorie Prospection). */
    @Test
    void liste_colonnesDeLaGrilleOmran() throws Exception {
        ComProspection p = newProspection(tranche);
        ComActionMessage m = action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 21, 10, 30));

        mockMvc.perform(get(URL).param("utilisateur", utilisateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(m.getId().toString()))
                .andExpect(jsonPath("$.content[0].utilisateur").value(utilisateur))
                .andExpect(jsonPath("$.content[0].typeAction").value(appel.getDescription()))
                .andExpect(jsonPath("$.content[0].dateAction").value("2026-09-21T10:30:00"))
                .andExpect(jsonPath("$.content[0].categorie").value("Prospection"))
                .andExpect(jsonPath("$.content[0].message").value("note 2026-09-21T10:30"))
                .andExpect(jsonPath("$.content[0].tiersNom").value(tiers.getDescription()))
                .andExpect(jsonPath("$.content[0].prospectionCode").value(p.getCode()))
                .andExpect(jsonPath("$.content[0].trancheCode").value(tranche.getCode()))
                .andExpect(jsonPath("$.content[0].programme").value(projet.getDescription()))
                .andExpect(jsonPath("$.content[0].rang").value(1))
                .andExpect(jsonPath("$.content[0].alerteActive").value(false))
                .andExpect(jsonPath("$.content[0].etat").value("Prospection"))
                // Hors défaut : non servi sans ?colonnes=.
                .andExpect(jsonPath("$.content[0].creePar").doesNotExist());
    }

    /** Hors catégorie Prospection, la tranche vient de l'article — la règle de la vue. */
    @Test
    void trancheResolue_parLArticleHorsCategorieProspection() throws Exception {
        CfgTranche autre = newTranche("TRA-" + UUID.randomUUID().toString().substring(0, 6), projet);
        // La prospection porte `tranche`, l'article `autre` : c'est `autre` qui doit sortir.
        action(dossier, appel, newProspection(tranche), newArticle(autre), LocalDateTime.of(2026, 9, 21, 11, 0));

        mockMvc.perform(get(URL).param("utilisateur", utilisateur))
                .andExpect(jsonPath("$.content[0].trancheCode").value(autre.getCode()));
        mockMvc.perform(get(URL).param("utilisateur", utilisateur).param("trancheId", autre.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(URL).param("utilisateur", utilisateur).param("trancheId", tranche.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /** {@code DocDate >= début AND DocDate < fin} — la fin est exclue, comme dans la requête OMRAN. */
    @Test
    void periode_finExclue() throws Exception {
        ComProspection p = newProspection(tranche);
        action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 21, 0, 0));
        action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 21, 23, 59));
        action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 22, 0, 0));

        mockMvc.perform(get(URL).param("utilisateur", utilisateur)
                        .param("dateDebut", "2026-09-21T00:00:00").param("dateFin", "2026-09-22T00:00:00"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /** 1 = la plus récente, par tiers et par type ; une action système ne compte pas. */
    @Test
    void rang_parTiersEtParType() throws Exception {
        ComProspection p = newProspection(tranche);
        ComActionMessage ancienne = action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 1, 9, 0));
        ComActionMessage recente = action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 2, 9, 0));
        ComActionMessage autreType = action(prospection, visite, p, null, LocalDateTime.of(2026, 9, 3, 9, 0));
        ComActionMessage systeme = action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 4, 9, 0));
        systeme.setIsSystem(true);
        messageRepository.saveAndFlush(systeme);

        String json = mockMvc.perform(get(URL).param("utilisateur", utilisateur).param("colonnes", "rang"))
                .andReturn().getResponse().getContentAsString();
        Map<String, Integer> rangs = new java.util.HashMap<>();
        List<Map<String, Object>> lignes = JsonPath.read(json, "$.content");
        lignes.forEach(l -> rangs.put((String) l.get("id"), (Integer) l.get("rang")));

        assertThat(rangs.get(recente.getId().toString())).isEqualTo(1);
        assertThat(rangs.get(ancienne.getId().toString())).isEqualTo(2);
        assertThat(rangs.get(autreType.getId().toString())).isEqualTo(1);
    }

    @Test
    void selectionDeColonnes_neSertQueLesChampsDemandes() throws Exception {
        action(prospection, appel, newProspection(tranche), null, LocalDateTime.of(2026, 9, 21, 10, 0));

        String json = mockMvc.perform(get(URL).param("utilisateur", utilisateur)
                        .param("colonnes", "utilisateur,creePar"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> ligne = JsonPath.read(json, "$.content[0]");
        assertThat(ligne.keySet()).containsExactlyInAnyOrder("id", "utilisateur", "creePar");
    }

    @Test
    void colonneInconnue_400() throws Exception {
        mockMvc.perform(get(URL).param("colonnes", "inexistante")).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- groupes

    /**
     * Le cas d'usage de l'écran : les appels exacts par jour d'un commercial. La somme des
     * effectifs retombe sur le total de la liste aux mêmes filtres.
     */
    @Test
    void groupes_appelsParJour() throws Exception {
        ComProspection p = newProspection(tranche);
        action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 21, 9, 0));
        action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 21, 16, 0));
        action(prospection, appel, p, null, LocalDateTime.of(2026, 9, 22, 10, 0));
        action(prospection, visite, p, null, LocalDateTime.of(2026, 9, 22, 11, 0));

        String json = mockMvc.perform(get(URL + "/groupes").param("utilisateur", utilisateur)
                        .param("typeId", appel.getId().toString())
                        .param("groupBy", "dateAction").param("granularite", "JOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> effectifs = JsonPath.read(json, "$.content[*].nombre");
        assertThat(effectifs).containsExactlyInAnyOrder(2, 1);

        mockMvc.perform(get(URL).param("utilisateur", utilisateur).param("typeId", appel.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void groupes_colonneCalculee_400() throws Exception {
        mockMvc.perform(get(URL + "/groupes").param("groupBy", "rang")).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "/groupes").param("groupBy", "message")).andExpect(status().isBadRequest());
    }

    @Test
    void catalogue_publieLesColonnes() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/suivi-actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='trancheCode')].groupable").value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$[?(@.nom=='rang')].groupable").value(org.hamcrest.Matchers.hasItem(false)));
    }

    // ---------------------------------------------------------------- périmètre

    @Test
    void perimetre_actionHorsProjetAffecte_invisible() throws Exception {
        CfgProject autreProjet = new CfgProject();
        autreProjet.setId(UUID.randomUUID());
        autreProjet.setCode("PRJ-SUIVI-B-" + UUID.randomUUID());
        projectRepository.save(autreProjet);
        CfgTranche trancheB = newTranche("TRB-" + UUID.randomUUID().toString().substring(0, 6), autreProjet);
        action(prospection, appel, newProspection(tranche), null, LocalDateTime.of(2026, 9, 21, 9, 0));
        action(prospection, appel, newProspection(trancheB), null, LocalDateTime.of(2026, 9, 21, 10, 0));

        AdmUser commercial = newUser("suivi.perim." + UUID.randomUUID(), null);
        AdmUserHabilitation h = new AdmUserHabilitation();
        h.setId(UUID.randomUUID());
        h.setAdmUser(commercial);
        h.setCfgProject(projet);
        habilitationRepository.save(h);

        mockMvc.perform(get(URL).param("utilisateur", utilisateur).with(connecte(commercial.getId(), commercial.getCode())))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].trancheCode").value(tranche.getCode()));
        mockMvc.perform(get(URL + "/groupes").param("utilisateur", utilisateur).param("groupBy", "utilisateur")
                        .with(connecte(commercial.getId(), commercial.getCode())))
                .andExpect(jsonPath("$.content[0].nombre").value(1));
    }

    // ---------------------------------------------------------------- correctif auteur (§6)

    /** Compte présent dans la base d'écriture : sa description, et le lien au compte. */
    @Test
    void auteur_compteConnu_descriptionEtAdmUserId() throws Exception {
        AdmUser compte = newUser("auteur." + UUID.randomUUID(), "Auteur Connu");
        ComProspection p = newProspection(tranche);

        mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"appel\"}")
                        .with(connecte(compte.getId(), compte.getCode())))
                .andExpect(status().isCreated());

        ComActionMessage ecrite = derniereInteraction(p);
        assertThat(ecrite.getUserName()).isEqualTo("Auteur Connu");
        assertThat(ecrite.getAdmUser().getId()).isEqualTo(compte.getId());
    }

    /**
     * Le bug des 13 actions anonymes : un compte absent de la base d'écriture. L'action garde un
     * auteur ({@code UserName} = l'identifiant du token en mono-base), sans {@code AdmUserId} — la
     * clé étrangère l'interdit. Avant le correctif : {@code UserName} nul.
     */
    @Test
    void auteur_compteAbsentDeLaBaseDEcriture_gardeUnNom() throws Exception {
        ComProspection p = newProspection(tranche);

        mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"appel\"}")
                        .with(connecte(UUID.randomUUID(), "fantome")))
                .andExpect(status().isCreated());

        ComActionMessage ecrite = derniereInteraction(p);
        assertThat(ecrite.getUserName()).isEqualTo("fantome");
        assertThat(ecrite.getAdmUser()).isNull();
        assertThat(ecrite.getDescription()).endsWith(" fantome");
    }

    private ComActionMessage derniereInteraction(ComProspection p) {
        return messageRepository.findAll().stream()
                .filter(m -> m.getComProspection() != null && p.getId().equals(m.getComProspection().getId()))
                .findFirst().orElseThrow();
    }

    private AdmUser newUser(String code, String description) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code);
        user.setDescription(description);
        user.setDisabled(false);
        return userRepository.save(user);
    }

    private static RequestPostProcessor connecte(UUID admUserId, String username) {
        AuthenticatedUser principal = new AuthenticatedUser(admUserId, username, null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }
}
