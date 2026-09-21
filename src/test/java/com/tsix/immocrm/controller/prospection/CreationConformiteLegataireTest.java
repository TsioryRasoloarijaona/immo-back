package com.tsix.immocrm.controller.prospection;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSaleProspectionViewRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Conformité des créations au logiciel de bureau — voir
 * {@code instructions/FIX-creation-prospect-prospection.md}.
 *
 * <p>Les deux écarts couverts ici viennent de la trace d'audit d'OMRAN : sa création de prospect
 * incrémente {@code AdmSequence} avant son {@code INSERT}, et sa création de prospection écrit un
 * {@code ComActionMessage} juste après le sien. Nous ne faisions ni l'un ni l'autre.
 *
 * <p>Les corps JSON sont écrits à la main, comme ailleurs dans ce dépôt : Spring Boot 4.1 sérialise
 * avec Jackson&nbsp;3 et n'expose aucun {@code ObjectMapper} Jackson&nbsp;2 injectable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class CreationConformiteLegataireTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComSaleProspectionViewRepository vueRepository;

    @Autowired
    private CfgProjectRepository projectRepository;
    @Autowired
    private CfgTrancheRepository trancheRepository;
    @Autowired
    private ComProspectionOriginRepository prospectionOriginRepository;
    @Autowired
    private ComActionMessageTypeRepository actionMessageTypeRepository;
    @Autowired
    private ComProspectionKindRepository prospectionKindRepository;

    @Autowired
    private EntityManager entityManager;

    // Référentiels obligatoires à la création (Source de contact, Type, Nature, Tranche).
    private CfgTranche tranche;
    private ComProspectionOrigin origine;
    private ComActionMessageType typeContact;
    private ComProspectionKind nature;

    @BeforeEach
    void setUpReferentiels() {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-CC-" + UUID.randomUUID());
        projectRepository.save(project);

        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-CC-" + UUID.randomUUID());
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORI-CC-" + UUID.randomUUID());
        origine.setDescription("Facebook");
        prospectionOriginRepository.save(origine);

        typeContact = new ComActionMessageType();
        typeContact.setId(UUID.randomUUID());
        typeContact.setCode("TYP-CC-" + UUID.randomUUID());
        typeContact.setDescription("Message entrant");
        actionMessageTypeRepository.save(typeContact);

        nature = new ComProspectionKind();
        nature.setId(UUID.randomUUID());
        nature.setCode("NAT-CC-" + UUID.randomUUID());
        nature.setDescription("1 A TRAITER");
        prospectionKindRepository.save(nature);
    }

    // ---------------------------------------------------------------- le code du prospect

    /**
     * Sur 13 913 prospects et clients réels, 12 seulement n'ont pas de code. Chaque prospect créé
     * sans code par l'API rejoignait cette anomalie marginale.
     */
    @Test
    void creationProspect_attribueUnCode() throws Exception {
        String body = """
                {"prenom":"Ines","nom":"ALAMI-CODE-TEST"}""";

        String reponse = mockMvc.perform(post("/api/prospects")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Format du légataire : « 26/14431 » — année sur 2 chiffres, numéro sur 5.
        assertThat(JsonPath.parse(reponse).read("$.code", String.class)).matches("\\d{2}/\\d{5}");
    }

    /** Deux prospects successifs ne partagent pas le même code : le compteur avance. */
    @Test
    void creationProspect_deuxCodesDistincts() throws Exception {
        assertThat(codeDuProspectCree("UN")).isNotEqualTo(codeDuProspectCree("DEUX"));
    }

    private String codeDuProspectCree(String suffixe) throws Exception {
        String body = """
                {"prenom":"Test","nom":"SEQUENCE-%s"}""".formatted(suffixe);
        String reponse = mockMvc.perform(post("/api/prospects")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(reponse).read("$.code", String.class);
    }

    // ---------------------------------------------------------------- l'interaction de création

    /**
     * Le logiciel de bureau écrit systématiquement une interaction juste après la prospection.
     * Sans elle, la prospection naît sans aucun échange et tout ce qui se fonde sur les
     * interactions est faussé dès la création — dernier échange, délai de prise en charge,
     * compteurs d'activité.
     */
    @Test
    void creationProspection_ecritSonInteraction() throws Exception {
        CfgTier prospect = newProspect();
        UUID prospectionId = creerProspection(prospect, "note de creation");

        List<ComActionMessage> interactions = interactionsDe(prospectionId);
        assertThat(interactions).hasSize(1);
        ComActionMessage creation = interactions.get(0);

        // Les notes de la prospection sont reprises telles quelles — mesuré sur 1 507 des 2 634
        // interactions de création réelles, les écarts restants n'étant que des espaces.
        assertThat(creation.getAlertMessage()).isEqualTo("note de creation");
        // Pas une relance : aucune échéance, et désarmée d'emblée.
        assertThat(creation.getAlertValidityDate()).isNull();
        assertThat(creation.getWithoutRevival()).isTrue();
        // Le tiers est repris, comme sur les interactions manuelles.
        assertThat(creation.getCfgTier()).isNotNull();
        assertThat(creation.getCfgTier().getId()).isEqualTo(prospect.getId());
    }

    /**
     * {@code IsSystem = 1}, à l'inverse de ce que pose {@code creerInteraction} pour une saisie
     * humaine. Les deux conventions cohabitent volontairement : {@code ComSaleProspectionView}
     * calcule le dernier échange avec {@code AND IsSystem = 0}, et créer une prospection n'est pas
     * un échange avec le client. C'est le choix du légataire, mesuré sur ses 2 634 interactions de
     * création.
     */
    @Test
    void interactionDeCreation_estExclueDuDernierEchange() throws Exception {
        UUID prospectionId = creerProspection(newProspect(), "note de conformite");
        ComActionMessage creation = interactionsDe(prospectionId).get(0);

        assertThat(creation.getIsSystem()).isTrue();
        assertThat(creation.getIsNotRead()).isFalse();
        // `notes` étant désormais obligatoire à la création, l'interaction reprend toujours la
        // note (le cas "notes absentes → message null" n'est plus atteignable par l'endpoint ;
        // la branche défensive côté service reste, mais n'est plus exercée ici).
        assertThat(creation.getAlertMessage()).isEqualTo("note de conformite");
    }

    // ---------------------------------------------------------------- les drapeaux hérités

    /**
     * <b>Le défaut qui rendait invisible tout ce que le CRM créait.</b>
     *
     * <p>{@code ComSaleProspectionView} — la vue de travail du logiciel de bureau — filtre en
     * {@code IsClosed = 0}. Or {@code NULL = 0} n'est <b>jamais</b> vrai en SQL : une prospection
     * laissée à {@code NULL} était bien écrite en base, et n'apparaissait dans aucune liste.
     *
     * <p>Diagnostiqué en comparant une prospection créée par le CRM à une prospection saisie dans
     * le logiciel : la seconde figurait dans la vue, la première non, et {@code IsClosed} était
     * leur seul écart.
     */
    @Test
    void creationProspection_estOuverteEtNonNulle() throws Exception {
        UUID prospectionId = creerProspection(newProspect(), null);

        ComProspection prospection = prospectionRepository.findById(prospectionId).orElseThrow();

        // isFalse() et non isNotNull() : c'est la distinction entre false et NULL qui compte,
        // et elle seule. Un assert sur la non-nullité laisserait passer le retour du défaut.
        assertThat(prospection.getIsClosed()).isFalse();
        assertThat(prospection.getIsOldCustomer()).isFalse();
    }

    /**
     * Même raison sur le tiers : les écrans du légataire filtrent ses drapeaux en {@code = 0}.
     * C'est ainsi qu'il remplit ses propres lignes — aucune n'a ces colonnes à {@code NULL}.
     */
    @Test
    void creationProspect_poseLesDrapeauxHerites() throws Exception {
        String reponse = mockMvc.perform(post("/api/prospects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"Drapeaux","nom":"HERITES-TEST"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(JsonPath.parse(reponse).read("$.id", String.class));
        CfgTier tier = tierRepository.findById(id).orElseThrow();

        assertThat(tier.getIsOldCustomer()).isFalse();
        assertThat(tier.getIsExtern()).isFalse();
        assertThat(tier.getIsRntCustomer()).isFalse();
        assertThat(tier.getIsVisibleInList()).isFalse();
        assertThat(tier.getIsFundReader()).isFalse();
        // Celui-ci reste à true : c'est un prospect, et c'est le seul de la famille que le
        // légataire ne pose pas à 0. Laissé tel quel faute de savoir ce que la colonne signifie
        // pour lui — à trancher avec le métier, pas ici.
        assertThat(tier.getIsProspect()).isTrue();
    }

    /**
     * <b>Le test qui compte : la prospection existe-t-elle pour le logiciel de bureau ?</b>
     *
     * <p>Assertion sur le résultat métier, pas sur une colonne. {@code ComSaleProspectionView}
     * est la vue sur laquelle s'appuient ses écrans ; une ligne qui n'y figure pas n'existe pour
     * lui nulle part, quelle que soit la qualité du reste.
     *
     * <p>Ce qui l'en écartait : {@code StrEntityId} laissé à {@code NULL}. La vue filtre par
     * {@code StrEntityId <> <pré-prospection>} et l'écran refiltre par
     * {@code StrEntityId = <vente>} — deux comparaisons qu'un {@code NULL} ne satisfait jamais.
     * Diagnostiqué sur la requête réelle du logiciel, relevée dans sa trace d'audit.
     *
     * <p>{@code flush} puis {@code clear} sont indispensables : une vue n'est peuplée qu'au
     * chargement, et sans vidage la prospection n'est encore qu'en session.
     */
    @Test
    void creationProspection_estVisibleDuLogicielDeBureau() throws Exception {
        UUID prospectionId = creerProspection(newProspect(), null);

        entityManager.flush();
        entityManager.clear();

        assertThat(vueRepository.findById(prospectionId))
                .as("La prospection doit figurer dans ComSaleProspectionView, sans quoi elle "
                        + "n'apparait dans aucun ecran du logiciel de bureau.")
                .isPresent();
    }

    /** Le type de document du légataire : « prospection de vente », et non {@code NULL}. */
    @Test
    void creationProspection_porteLeTypeDeDocumentDuLegataire() throws Exception {
        UUID prospectionId = creerProspection(newProspect(), null);

        ComProspection prospection = prospectionRepository.findById(prospectionId).orElseThrow();

        assertThat(prospection.getStrEntityId())
                .isEqualTo(UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601"));
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * Crée une prospection avec les cinq champs obligatoires (Source de contact, Type, Nature,
     * Tranche + Notes). {@code notes} peut être précisé par le test ; à {@code null}, une valeur par
     * défaut non vide est posée — car depuis l'alignement légataire, {@code notes} est obligatoire.
     */
    private UUID creerProspection(CfgTier prospect, String notes) throws Exception {
        String valeurNotes = notes != null ? notes : "note de conformite";
        String body = "{\"prospectId\":\"" + prospect.getId() + "\""
                + ",\"trancheId\":\"" + tranche.getId() + "\""
                + ",\"origineId\":\"" + origine.getId() + "\""
                + ",\"typeContactId\":\"" + typeContact.getId() + "\""
                + ",\"etapeId\":\"" + nature.getId() + "\""
                + ",\"notes\":\"" + valeurNotes + "\"}";
        String reponse = mockMvc.perform(post("/api/prospections")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.parse(reponse).read("$.id", String.class));
    }

    private List<ComActionMessage> interactionsDe(UUID prospectionId) {
        return actionMessageRepository.findAll().stream()
                .filter(m -> m.getComProspection() != null
                        && prospectionId.equals(m.getComProspection().getId()))
                .toList();
    }

    private CfgTier newProspect() {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("CC-TIER-" + UUID.randomUUID());
        t.setFirstName("Prospect");
        t.setLastName("CREATION TEST");
        return tierRepository.save(t);
    }
}
