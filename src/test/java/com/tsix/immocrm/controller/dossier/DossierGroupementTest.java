package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/groupes} et le dépliage d'un groupe — voir
 * {@code instructions/FEATURE-groupement-listes.md}.
 *
 * <p><strong>Chaque test isole son jeu de données par {@code ?search=}</strong> sur un préfixe de
 * code qui lui est propre. Ce n'est pas qu'une commodité : la base de test porte les fixtures des
 * autres classes, et surtout cela fait passer <em>le même filtre</em> à la liste et à son
 * groupement — ce que l'invariant de {@link #sommeDesEffectifs_egaleLeTotalDeLaListe()} vérifie.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierGroupementTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private StrStatusRepository strStatusRepository;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private CfgTierRepository tierRepository;

    private StkVocation vocation;

    @BeforeEach
    void setUp() {
        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-GRP-TEST");
        vocationRepository.save(vocation);
    }

    /**
     * {@code save()} renvoie l'instance <em>gérée</em> et non celle qu'on lui passe : l'identifiant
     * étant assigné à la main, Spring Data part sur un {@code merge}. Réutiliser la variable locale
     * laisserait une instance transiente accrochée au dossier.
     */
    private CfgTier newTier(String code, String raisonSociale, String prenom, String nom) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setSocialReason(raisonSociale);
        tier.setFirstName(prenom);
        tier.setLastName(nom);
        return tierRepository.save(tier);
    }

    private StrStatus newStatut(String code) {
        StrStatus statut = new StrStatus();
        statut.setId(UUID.randomUUID());
        statut.setCode(code);
        return strStatusRepository.save(statut);
    }

    private ComFolder newFolder(String code, StrStatus statut, BigDecimal prix) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        folder.setStrStatus(statut);
        folder.setPrice(prix);
        return folderRepository.save(folder);
    }

    // ------------------------------------------------------------------
    // Cas nominal
    // ------------------------------------------------------------------

    @Test
    void groupeParStatut_rendUnGroupeParStatutAvecEffectifEtSomme() throws Exception {
        StrStatus reservation = newStatut("GRP-RESERVATION");
        StrStatus concretisation = newStatut("GRP-CONCRETISATION");

        newFolder("GRPA-1", reservation, new BigDecimal("1000000.00"));
        newFolder("GRPA-2", reservation, new BigDecimal("350000.00"));
        newFolder("GRPA-3", concretisation, new BigDecimal("950000.00"));

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "statut")
                        .param("agregats", "prix")
                        .param("search", "GRPA-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-RESERVATION')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-RESERVATION')].agregats.prix").value(1350000.00))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-CONCRETISATION')].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-CONCRETISATION')].agregats.prix").value(950000.00));
    }

    /**
     * Les centimes doivent survivre à la somme.
     *
     * <p>L'agrégat passe par un {@code COALESCE(SUM(...), 0)} — le zéro servant à ce qu'un groupe
     * entièrement nul rende 0 et non {@code null}. Si ce littéral entier tirait le type du résultat
     * vers lui, la somme serait arrondie à l'unité : un dossier à 350 000,50 DH deviendrait
     * 350 000 ou 350 001 sans que rien ne le signale. 190,75 est choisi pour que l'arrondi soit
     * visible dans les deux sens.
     */
    @Test
    void sommeDesMontants_conserveLesCentimes() throws Exception {
        StrStatus statut = newStatut("GRP-CENTIMES");
        newFolder("GRPK-1", statut, new BigDecimal("100.50"));
        newFolder("GRPK-2", statut, new BigDecimal("90.25"));

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "statut")
                        .param("agregats", "prix")
                        .param("search", "GRPK-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].agregats.prix").value(190.75));
    }

    @Test
    void sansAgregats_seulLEffectifEstRenvoye() throws Exception {
        newFolder("GRPB-1", newStatut("GRP-SANS-AGG"), new BigDecimal("500000.00"));

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "statut")
                        .param("search", "GRPB-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value(1))
                .andExpect(jsonPath("$.content[0].agregats").isEmpty());
    }

    // ------------------------------------------------------------------
    // L'invariant du §5 : groupes et liste portent les mêmes lignes
    // ------------------------------------------------------------------

    /**
     * Si ce test casse, c'est que la {@code Specification} du groupement et celle de la liste ont
     * divergé — un {@code INNER JOIN} implicite qui écarte les lignes à association nulle, ou un
     * périmètre commercial appliqué d'un seul côté.
     */
    @Test
    void sommeDesEffectifs_egaleLeTotalDeLaListe() throws Exception {
        StrStatus a = newStatut("GRP-INV-A");
        StrStatus b = newStatut("GRP-INV-B");
        newFolder("GRPC-1", a, new BigDecimal("100.00"));
        newFolder("GRPC-2", a, new BigDecimal("200.00"));
        newFolder("GRPC-3", b, new BigDecimal("300.00"));
        // Sans statut : le groupe à clé nulle, celui que COUNT(DISTINCT) ne compte pas.
        newFolder("GRPC-4", null, new BigDecimal("400.00"));

        mockMvc.perform(get("/api/dossiers").param("search", "GRPC-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));

        // 3 groupes : A, B, et celui des dossiers sans statut. `inclureSansValeur=true` est
        // indispensable ici : c'est ce qui rend la somme des effectifs comparable au total de la
        // liste. Sans lui l'invariant ne tient pas — et ne doit pas tenir.
        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "statut")
                        .param("agregats", "prix")
                        .param("inclureSansValeur", "true")
                        .param("search", "GRPC-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-INV-A')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-INV-B')].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle == null)].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle == null)].agregats.prix").value(400.00));
    }

    // ------------------------------------------------------------------
    // Dépliage
    // ------------------------------------------------------------------

    @Test
    void depliage_rendExactementLesLignesComptees() throws Exception {
        StrStatus vise = newStatut("GRP-DEPLI-VISE");
        StrStatus autre = newStatut("GRP-DEPLI-AUTRE");
        ComFolder attendu = newFolder("GRPD-1", vise, new BigDecimal("100.00"));
        ComFolder exclu = newFolder("GRPD-2", autre, new BigDecimal("200.00"));

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "GRPD-")
                        .param("groupeColonne", "statut")
                        .param("groupeValeur", "GRP-DEPLI-VISE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + attendu.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + exclu.getId() + "')]").doesNotExist());
    }

    /** Le groupe à clé nulle doit s'ouvrir comme les autres, sinon il est comptable mais mort. */
    @Test
    void depliage_sansValeur_ouvreLeGroupeDesLignesSansValeur() throws Exception {
        ComFolder sansStatut = newFolder("GRPE-1", null, new BigDecimal("100.00"));
        ComFolder avecStatut = newFolder("GRPE-2", newStatut("GRP-E-PRESENT"), new BigDecimal("200.00"));

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "GRPE-")
                        .param("groupeColonne", "statut"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + sansStatut.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + avecStatut.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Groupement par tiers : la clé est l'identifiant, le libellé est dérivé
    // ------------------------------------------------------------------

    @Test
    void groupeParClient_libelleSuitLaPrioriteDuFormatteurDeNom() throws Exception {
        // Raison sociale ET nom/prénom : TierNomFormatter doit préférer la raison sociale, et
        // l'en-tête de groupe doit dire exactement ce que dit la colonne « Client » de la ligne.
        CfgTier societe = newTier("TIER-GRP-SOC", "AYAD ESTATE SARL", "Ignoré", "Ignoré");

        ComFolder folder = newFolder("GRPF-1", null, new BigDecimal("750000.00"));
        folder.setCfgCustomer(societe);
        folderRepository.save(folder);

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "clientNom")
                        .param("agregats", "prix")
                        .param("search", "GRPF-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].cle").value(societe.getId().toString()))
                .andExpect(jsonPath("$.content[0].libelle").value("AYAD ESTATE SARL"))
                .andExpect(jsonPath("$.content[0].nombre").value(1));
    }

    /** La clé rendue pour un tiers est un UUID : le dépliage doit savoir la reconvertir. */
    @Test
    void depliageParClient_reconvertitLaCleEnIdentifiant() throws Exception {
        CfgTier client = newTier("TIER-GRP-DEPLI", null, null, "BENALI");

        ComFolder duClient = newFolder("GRPG-1", null, new BigDecimal("100.00"));
        duClient.setCfgCustomer(client);
        folderRepository.save(duClient);
        ComFolder sansClient = newFolder("GRPG-2", null, new BigDecimal("200.00"));

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "GRPG-")
                        .param("groupeColonne", "clientNom")
                        .param("groupeValeur", client.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + duClient.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + sansClient.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Dates : la granularité, sans laquelle le groupement est une liste plate
    // ------------------------------------------------------------------

    @Test
    void groupeParDate_regroupeAuMoisParDefaut() throws Exception {
        ComFolder janvierA = newFolder("GRPH-1", null, new BigDecimal("100.00"));
        janvierA.setDocDate(LocalDateTime.of(2025, 1, 5, 10, 0));
        folderRepository.save(janvierA);
        ComFolder janvierB = newFolder("GRPH-2", null, new BigDecimal("200.00"));
        janvierB.setDocDate(LocalDateTime.of(2025, 1, 28, 16, 30));
        folderRepository.save(janvierB);
        ComFolder fevrier = newFolder("GRPH-3", null, new BigDecimal("300.00"));
        fevrier.setDocDate(LocalDateTime.of(2025, 2, 3, 9, 0));
        folderRepository.save(fevrier);

        // Deux dates distinctes en janvier, un seul groupe : c'est tout l'objet de la granularité.
        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "dateDocument")
                        .param("agregats", "prix")
                        .param("search", "GRPH-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==202501)].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==202501)].libelle").value("janvier 2025"))
                .andExpect(jsonPath("$.content[?(@.cle==202501)].agregats.prix").value(300.00))
                .andExpect(jsonPath("$.content[?(@.cle==202502)].nombre").value(1));
    }

    @Test
    void groupeParDate_granulariteAnnee() throws Exception {
        ComFolder a = newFolder("GRPI-1", null, new BigDecimal("100.00"));
        a.setDocDate(LocalDateTime.of(2025, 1, 5, 10, 0));
        folderRepository.save(a);
        ComFolder b = newFolder("GRPI-2", null, new BigDecimal("200.00"));
        b.setDocDate(LocalDateTime.of(2025, 11, 20, 10, 0));
        folderRepository.save(b);

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "dateDocument")
                        .param("granularite", "ANNEE")
                        .param("search", "GRPI-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].cle").value(2025))
                .andExpect(jsonPath("$.content[0].libelle").value("2025"))
                .andExpect(jsonPath("$.content[0].nombre").value(2));
    }

    @Test
    void depliageParDate_retombeSurLeMemeGroupe() throws Exception {
        ComFolder janvier = newFolder("GRPJ-1", null, new BigDecimal("100.00"));
        janvier.setDocDate(LocalDateTime.of(2025, 1, 5, 10, 0));
        folderRepository.save(janvier);
        ComFolder fevrier = newFolder("GRPJ-2", null, new BigDecimal("200.00"));
        fevrier.setDocDate(LocalDateTime.of(2025, 2, 5, 10, 0));
        folderRepository.save(fevrier);

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "GRPJ-")
                        .param("groupeColonne", "dateDocument")
                        .param("groupeValeur", "202501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + janvier.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + fevrier.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Refus
    // ------------------------------------------------------------------

    @Test
    void groupBy_surColonneInconnue_rend400() throws Exception {
        mockMvc.perform(get("/api/dossiers/groupes").param("groupBy", "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupBy_absent_rend400() throws Exception {
        mockMvc.perform(get("/api/dossiers/groupes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agregats_surColonneNonAgregeable_rend400() throws Exception {
        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "statut")
                        .param("agregats", "statut"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Ignorer {@code groupeValeur} orphelin renverrait la liste entière alors que le client croit
     * avoir ouvert un groupe — une page plausible mais fausse.
     */
    @Test
    void groupeValeur_sansGroupeColonne_rend400() throws Exception {
        mockMvc.perform(get("/api/dossiers").param("groupeValeur", "GRP-RESERVATION"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupeColonne_inconnue_rend400() throws Exception {
        mockMvc.perform(get("/api/dossiers")
                        .param("groupeColonne", "colonneQuiNExistePas")
                        .param("groupeValeur", "x"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Le groupe sans valeur est écarté par défaut
    // ------------------------------------------------------------------

    /**
     * Le comportement demandé : « juste les lignes qui existent ». Sur une colonne peu renseignée,
     * le groupe vide écraserait tous les autres — 11 186 prospections sans dernière visite sur
     * 14 118, mesuré. Il reste récupérable par {@code inclureSansValeur=true}.
     */
    @Test
    void groupeSansValeur_ecarteParDefaut_recuperableSurDemande() throws Exception {
        StrStatus statut = newStatut("GRP-SV-PRESENT");
        newFolder("GRPL-1", statut, new BigDecimal("100.00"));
        newFolder("GRPL-2", null, new BigDecimal("200.00"));

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "statut")
                        .param("search", "GRPL-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.cle == null)]").doesNotExist());

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "statut")
                        .param("inclureSansValeur", "true")
                        .param("search", "GRPL-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle == null)].nombre").value(1));
    }

    // ------------------------------------------------------------------
    // Toute colonne est groupable — y compris celles écartées au lot précédent
    // ------------------------------------------------------------------

    /**
     * Un montant est clé de groupe <strong>et</strong> total. Il avait été refusé sur sa
     * cardinalité (190 prix distincts sur 358 dossiers) ; c'est au client de juger, pas au
     * catalogue — un portefeuille filtré sur une tranche n'a que quelques prix distincts.
     */
    @Test
    void groupeParMontant_estGroupableEtAgregeable() throws Exception {
        StrStatus statut = newStatut("GRP-MT");
        newFolder("GRPM-1", statut, new BigDecimal("500000.00"));
        newFolder("GRPM-2", statut, new BigDecimal("500000.00"));
        newFolder("GRPM-3", statut, new BigDecimal("750000.00"));

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "prix")
                        .param("agregats", "prix")
                        .param("search", "GRPM-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==500000.00)].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==500000.00)].agregats.prix").value(1000000.00))
                .andExpect(jsonPath("$.content[?(@.cle==750000.00)].nombre").value(1));
    }

    /** Les identifiants aussi : même partition que leur jumelle `*Nom`, clés en UUID. */
    @Test
    void groupeParIdentifiant_estGroupable() throws Exception {
        CfgTier client = newTier("TIER-GRP-ID", null, null, "ZAHIDI");
        ComFolder folder = newFolder("GRPN-1", null, new BigDecimal("100.00"));
        folder.setCfgCustomer(client);
        folderRepository.save(folder);

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "clientId")
                        .param("search", "GRPN-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].cle").value(client.getId().toString()));
    }

    @Test
    void groupeParCode_estGroupable() throws Exception {
        newFolder("GRPO-1", null, new BigDecimal("100.00"));

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "code")
                        .param("search", "GRPO-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].cle").value("GRPO-1"));
    }
}
