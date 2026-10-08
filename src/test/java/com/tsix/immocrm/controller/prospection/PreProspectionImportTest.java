package com.tsix.immocrm.controller.prospection;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import CSV des leads — {@code POST /api/pre-prospections/import} et {@code .../import/apercu}.
 * Voir {@code instructions/FEATURE-creation-pre-prospection.md} §5.
 *
 * <p>Le fichier reprend le format réel de l'export (en-tête et lignes fournis par l'utilisateur),
 * avec des noms suffixés pour ne rencontrer aucun doublon venu d'une autre classe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class PreProspectionImportTest {

    private static final String ENTETE = "Créé,Nom,Adresse e-mail,Source,Formulaire,Canal,Étape,"
            + "Propriétaire,Étiquettes,Téléphone,Numéro de téléphone secondaire,Numéro WhatsApp";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private AdmUserRepository userRepository;

    @Autowired
    private AdmUserHabilitationRepository habilitationRepository;

    private CfgTranche tranche;
    private String suffixe;
    private String formulaire;

    @BeforeEach
    void preparer() {
        suffixe = UUID.randomUUID().toString().substring(0, 8);
        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(("IMP" + suffixe).toUpperCase(Locale.ROOT));
        trancheRepository.save(tranche);
        formulaire = tranche.getCode() + " FORM 02*2026 - GISK";
    }

    /** Trois lignes de l'export réel, le formulaire désignant la tranche du test. */
    private String fichierType() {
        return ENTETE + "\n"
                + "09/23/2026 1:45am,Imane Bennis " + suffixe + ",Imanebennis68@gmail.com,Payé," + formulaire
                + ",E-mail,À évaluer,Unassigned,,+212663434434,,\n"
                + "09/23/2026 12:10am,Abden Sayem " + suffixe + ",abdensayem@hotmail.com,Payé," + formulaire
                + ",E-mail,À évaluer,Unassigned,,+212642463157,,\n"
                + "09/20/2026 2:18am,Majda el haitmy " + suffixe + ",hitmimajda@gmail.com,Payé," + formulaire
                + ",E-mail,À évaluer,Unassigned,,0670770078,,\n";
    }

    private static MockMultipartFile csv(String contenu) {
        return new MockMultipartFile("fichier", "leads.csv", "text/csv", contenu.getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions apercu(String contenu) throws Exception {
        return mockMvc.perform(multipart("/api/pre-prospections/import/apercu").file(csv(contenu)));
    }

    private ResultActions importer(String contenu) throws Exception {
        return mockMvc.perform(multipart("/api/pre-prospections/import").file(csv(contenu))
                .param("trancheId", tranche.getId().toString()));
    }

    @Test
    void apercu_extraitToutEtProposeLaTranche_sansRienEcrire() throws Exception {
        apercu(fichierType())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importe").value(false))
                .andExpect(jsonPath("$.totalLignes").value(3))
                .andExpect(jsonPath("$.nouvelles").value(3))
                .andExpect(jsonPath("$.trancheSuggereeId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.trancheSuggereeCode").value(tranche.getCode()))
                .andExpect(jsonPath("$.formulaires[0]").value(formulaire))
                .andExpect(jsonPath("$.lignes[0].ligne").value(2))
                .andExpect(jsonPath("$.lignes[0].nom").value("Imane Bennis " + suffixe))
                .andExpect(jsonPath("$.lignes[0].dateCreation").value("2026-09-23T01:45:00"))
                .andExpect(jsonPath("$.lignes[1].dateCreation").value("2026-09-23T00:10:00"))
                .andExpect(jsonPath("$.lignes[0].telephone").value("+212663434434"))
                .andExpect(jsonPath("$.lignes[0].email").value("Imanebennis68@gmail.com"))
                .andExpect(jsonPath("$.lignes[0].statut").value("NOUVELLE"))
                .andExpect(jsonPath("$.lignes[0].preProspectionId").value(nullValue()));

        mockMvc.perform(get("/api/pre-prospections").param("search", suffixe))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void importer_creeChaqueLigneSurLaTrancheChoisie() throws Exception {
        String reponse = importer(fichierType())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importe").value(true))
                .andExpect(jsonPath("$.trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.nouvelles").value(3))
                .andExpect(jsonPath("$.lignes[0].code").value(matchesPattern("\\d{2}/\\d{5}")))
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(reponse, "$.lignes[0].preProspectionId");
        ComProspection p = prospectionRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(p.getStrEntityId()).isEqualTo(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        assertThat(p.getStkHierarchy().getId()).isEqualTo(tranche.getId());
        assertThat(p.getDocDate()).isEqualTo(LocalDateTime.of(2026, 9, 23, 1, 45));
        assertThat(p.getTempTel()).isEqualTo("+212663434434");
        assertThat(p.getTempEMail()).isEqualTo("Imanebennis68@gmail.com");
        assertThat(p.getNotes()).contains("formulaire : " + formulaire)
                .contains("Source : Payé · Canal : E-mail · Étape : À évaluer")
                .doesNotContain("Unassigned");

        mockMvc.perform(get("/api/pre-prospections").param("trancheId", tranche.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    /** Réimporter le même export — ou un export qui chevauche le précédent — ne crée rien deux fois. */
    @Test
    void reimport_ignoreLesLeadsDejaImportes() throws Exception {
        importer(fichierType()).andExpect(status().isOk());

        importer(fichierType())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouvelles").value(0))
                .andExpect(jsonPath("$.dejaImportees").value(3))
                .andExpect(jsonPath("$.lignes[0].statut").value("DEJA_IMPORTEE"))
                .andExpect(jsonPath("$.lignes[0].preProspectionId").value(nullValue()));

        mockMvc.perform(get("/api/pre-prospections").param("trancheId", tranche.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    /** Même contact, autre soumission : créée et signalée, dans le fichier comme contre la base. */
    @Test
    void memeContactAutreDate_estUnDoublonCree() throws Exception {
        String ligne = ",Houdette " + suffixe + ",houda.saaidi@gmail.com,Payé," + formulaire
                + ",E-mail,À évaluer,Unassigned,,+212662178116,,\n";
        String fichier = ENTETE + "\n09/21/2026 2:09pm" + ligne + "09/22/2026 9:00am" + ligne
                + "09/21/2026 2:09pm" + ligne;

        importer(fichier)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouvelles").value(1))
                .andExpect(jsonPath("$.doublons").value(1))
                .andExpect(jsonPath("$.dejaImportees").value(1))
                .andExpect(jsonPath("$.lignes[0].dateCreation").value("2026-09-21T14:09:00"))
                .andExpect(jsonPath("$.lignes[1].statut").value("DOUBLON"))
                .andExpect(jsonPath("$.lignes[1].preProspectionId").isNotEmpty())
                .andExpect(jsonPath("$.lignes[1].doublonIds.length()").value(1))
                .andExpect(jsonPath("$.lignes[2].statut").value("DEJA_IMPORTEE"))
                .andExpect(jsonPath("$.lignes[2].message").value(containsString("ligne 2")));
    }

    @Test
    void lignesIllisibles_sontIgnoreesSansBloquerLesAutres() throws Exception {
        String fichier = fichierType()
                + "09/23/2026 3:00am,,vide@test.local,Payé," + formulaire + ",E-mail,,,,+212600000000,,\n"
                + "23/09/2026 03:00,Date Europe " + suffixe + ",x@test.local,Payé," + formulaire + ",,,,,0600000000,,\n";

        importer(fichier)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLignes").value(5))
                .andExpect(jsonPath("$.nouvelles").value(3))
                .andExpect(jsonPath("$.erreurs").value(2))
                .andExpect(jsonPath("$.lignes[3].statut").value("ERREUR"))
                .andExpect(jsonPath("$.lignes[3].message").value("Nom vide"))
                .andExpect(jsonPath("$.lignes[4].message").value(containsString("illisible")));
    }

    /** Un export repassé par Excel en français : BOM, point-virgule, colonnes dans un autre ordre. */
    @Test
    void fichierExcel_pointVirguleEtBom() throws Exception {
        String fichier = "﻿Nom;Téléphone;Créé;Formulaire\n"
                + "\"Bah; Ahmadou " + suffixe + "\";+212639221668;09/19/2026 9:43am;" + formulaire + "\n";

        apercu(fichier)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouvelles").value(1))
                .andExpect(jsonPath("$.lignes[0].nom").value("Bah; Ahmadou " + suffixe))
                .andExpect(jsonPath("$.lignes[0].telephone").value("+212639221668"))
                .andExpect(jsonPath("$.lignes[0].dateCreation").value("2026-09-19T09:43:00"));
    }

    @Test
    void formulairesDePlusieursTranches_pasDeSuggestionEtUnAvertissement() throws Exception {
        CfgTranche autre = new CfgTranche();
        autre.setId(UUID.randomUUID());
        autre.setCode(("IMQ" + suffixe).toUpperCase(Locale.ROOT));
        trancheRepository.save(autre);
        String fichier = fichierType()
                + "09/23/2026 1:45am,Autre " + suffixe + ",a@test.local,Payé," + autre.getCode()
                + " FORM,E-mail,,,,0600000001,,\n";

        apercu(fichier)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheSuggereeId").value(nullValue()))
                .andExpect(jsonPath("$.avertissements[0]").value(containsString("plusieurs tranches")));
    }

    @Test
    void importSansTranche_400_etFichierSansColonneNom_400() throws Exception {
        mockMvc.perform(multipart("/api/pre-prospections/import").file(csv(fichierType())))
                .andExpect(status().isBadRequest());
        apercu("Créé,Email\n09/23/2026 1:45am,a@b.c\n").andExpect(status().isBadRequest());
    }

    /**
     * Un contact qui n'existe que sur un projet hors du périmètre de l'appelant n'est jamais
     * nommé — ni par l'import, ni par la création unitaire. Le réimport reste pourtant
     * idempotent : le lead déjà importé est ignoré, simplement sans dire où il se trouve.
     */
    @Test
    void doublonHorsPerimetre_nEstJamaisNomme() throws Exception {
        CfgProject projetA = newProjet("PRJ-IMP-A-" + suffixe);
        CfgProject projetB = newProjet("PRJ-IMP-B-" + suffixe);
        CfgTranche trancheA = newTranche("TRA-" + suffixe, projetA);
        CfgTranche trancheB = newTranche("TRB-" + suffixe, projetB);

        AdmUser commercial = new AdmUser();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("imp.perim." + suffixe);
        commercial.setDisabled(false);
        userRepository.save(commercial);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(commercial);
        habilitation.setCfgProject(projetA);
        habilitationRepository.save(habilitation);

        String nom = "Cache " + suffixe;
        ComProspection cachee = new ComProspection();
        cachee.setId(UUID.randomUUID());
        cachee.setCode("HORS-PERIM-" + suffixe);
        cachee.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        cachee.setTempDescription(nom);
        cachee.setTempTel("+212611111111");
        cachee.setDocDate(LocalDateTime.of(2026, 9, 21, 14, 9));
        cachee.setStkHierarchy(trancheB);
        prospectionRepository.save(cachee);

        String ligne = "," + nom + ",,Payé,FORM,E-mail,,,,+212611111111,,\n";
        String fichier = ENTETE + "\n09/21/2026 2:09pm" + ligne + "09/22/2026 9:00am" + ligne;

        mockMvc.perform(multipart("/api/pre-prospections/import").file(csv(fichier))
                        .param("trancheId", trancheA.getId().toString())
                        .with(connecte(commercial)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].statut").value("DEJA_IMPORTEE"))
                .andExpect(jsonPath("$.lignes[0].message").value("Lead déjà importé"))
                .andExpect(jsonPath("$.lignes[0].doublonIds", hasSize(0)))
                .andExpect(jsonPath("$.lignes[1].statut").value("NOUVELLE"))
                .andExpect(jsonPath("$.lignes[1].doublonIds", hasSize(0)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(cachee.getId().toString()))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("HORS-PERIM-"))));

        mockMvc.perform(post("/api/pre-prospections").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"%s","telephone":"0611111111","trancheId":"%s"}"""
                                .formatted(nom, trancheA.getId()))
                        .with(connecte(commercial)))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(cachee.getId().toString()))));
    }

    // -- Import sélectif par statut : FEATURE-import-selectif.md

    /**
     * Ligne 2 NOUVELLE, 3 DOUBLON interne (contact de la ligne 2, autre minute), 4 DEJA_IMPORTEE
     * (même lead que la 2), 5 ERREUR (nom vide), 6 NOUVELLE.
     */
    private String fichierMixte() {
        String houda = ",Houda " + suffixe + ",houda." + suffixe + "@test.local,Payé," + formulaire
                + ",E-mail,,,,+212662178116,,\n";
        return ENTETE + "\n"
                + "09/21/2026 2:09pm" + houda
                + "09/22/2026 9:00am" + houda
                + "09/21/2026 2:09pm" + houda
                + "09/23/2026 3:00am,,vide@test.local,Payé," + formulaire + ",E-mail,,,,+212600000000,,\n"
                + "09/23/2026 1:45am,Imane " + suffixe + ",imane." + suffixe + "@test.local,Payé," + formulaire
                + ",E-mail,,,,+212663434434,,\n";
    }

    private ResultActions importerStatuts(String contenu, String... statuts) throws Exception {
        return mockMvc.perform(multipart("/api/pre-prospections/import").file(csv(contenu))
                .param("trancheId", tranche.getId().toString())
                .param("statuts", statuts));
    }

    private long creeesSurLaTranche() {
        return prospectionRepository.count(
                (root, query, cb) -> cb.equal(root.get("stkHierarchy").get("id"), tranche.getId()));
    }

    private static final List<String> STATUTS_MIXTE =
            List.of("NOUVELLE", "DOUBLON", "DEJA_IMPORTEE", "ERREUR", "NOUVELLE");

    @Test
    void statutsNouvelle_neCreeQueLesNouvelles() throws Exception {
        String reponse = importerStatuts(fichierMixte(), "NOUVELLE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutsImportes", hasSize(1)))
                .andExpect(jsonPath("$.statutsImportes[0]").value("NOUVELLE"))
                .andExpect(jsonPath("$.nouvelles").value(2))
                .andExpect(jsonPath("$.doublons").value(1))
                .andExpect(jsonPath("$.creees").value(2))
                .andExpect(jsonPath("$.lignes[0].preProspectionId").isNotEmpty())
                .andExpect(jsonPath("$.lignes[1].statut").value("DOUBLON"))
                .andExpect(jsonPath("$.lignes[1].preProspectionId").value(nullValue()))
                .andExpect(jsonPath("$.lignes[1].code").value(nullValue()))
                .andExpect(jsonPath("$.lignes[1].message")
                        .value(org.hamcrest.Matchers.endsWith(" — non importée (statut non sélectionné)")))
                .andExpect(jsonPath("$.lignes[4].preProspectionId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        List<String> statuts = JsonPath.read(reponse, "$.lignes[*].statut");
        assertThat(statuts).isEqualTo(STATUTS_MIXTE);
        assertThat(creeesSurLaTranche()).isEqualTo(2);
    }

    /** R2 : la DOUBLON interne reste DOUBLON bien que sa NOUVELLE de référence ne soit pas importée. */
    @Test
    void statutsDoublon_neCreeQueLesDoublons_etLaClassificationNeBougePas() throws Exception {
        String reponse = importerStatuts(fichierMixte(), "DOUBLON")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutsImportes[0]").value("DOUBLON"))
                .andExpect(jsonPath("$.statutsImportes", hasSize(1)))
                .andExpect(jsonPath("$.creees").value(1))
                .andExpect(jsonPath("$.nouvelles").value(2))
                .andExpect(jsonPath("$.lignes[0].preProspectionId").value(nullValue()))
                .andExpect(jsonPath("$.lignes[0].message").value("Non importée (statut non sélectionné)"))
                .andExpect(jsonPath("$.lignes[1].statut").value("DOUBLON"))
                .andExpect(jsonPath("$.lignes[1].preProspectionId").isNotEmpty())
                .andExpect(jsonPath("$.lignes[1].message")
                        .value("Même contact que la ligne 2 du fichier (autre soumission)"))
                .andExpect(jsonPath("$.lignes[4].preProspectionId").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        List<String> statuts = JsonPath.read(reponse, "$.lignes[*].statut");
        assertThat(statuts).isEqualTo(STATUTS_MIXTE);
        assertThat(creeesSurLaTranche()).isEqualTo(1);
    }

    @Test
    void sansStatuts_creeNouvellesEtDoublons_commeAvant() throws Exception {
        importer(fichierMixte())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutsImportes[0]").value("NOUVELLE"))
                .andExpect(jsonPath("$.statutsImportes[1]").value("DOUBLON"))
                .andExpect(jsonPath("$.creees").value(3))
                .andExpect(jsonPath("$.lignes[1].doublonIds", hasSize(1)));
        assertThat(creeesSurLaTranche()).isEqualTo(3);
    }

    @Test
    void statutsSeparesParVirgules_acceptes_etRenduDansLOrdre() throws Exception {
        importerStatuts(fichierMixte(), "DOUBLON,NOUVELLE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutsImportes[0]").value("NOUVELLE"))
                .andExpect(jsonPath("$.statutsImportes[1]").value("DOUBLON"))
                .andExpect(jsonPath("$.creees").value(3));
    }

    /** R2 : mêmes statuts ligne à ligne que l'aperçu, quelle que soit la sélection. */
    @Test
    void statutsDeLImport_egalentCeuxDeLApercu() throws Exception {
        String apercu = apercu(fichierMixte())
                .andExpect(jsonPath("$.statutsImportes").value(nullValue()))
                .andExpect(jsonPath("$.creees").value(0))
                .andReturn().getResponse().getContentAsString();
        String importe = importerStatuts(fichierMixte(), "NOUVELLE")
                .andReturn().getResponse().getContentAsString();

        List<String> attendus = JsonPath.read(apercu, "$.lignes[*].statut");
        List<String> obtenus = JsonPath.read(importe, "$.lignes[*].statut");
        assertThat(attendus).isEqualTo(STATUTS_MIXTE);
        assertThat(obtenus).isEqualTo(attendus);
    }

    @Test
    void statutsInvalides_400_sansRienEcrire() throws Exception {
        for (String statuts : List.of("ERREUR", "DEJA_IMPORTEE", "FOO", "", "NOUVELLE,FOO", "NOUVELLE,ERREUR")) {
            importerStatuts(fichierMixte(), statuts)
                    .andExpect(status().isBadRequest());
        }
        assertThat(creeesSurLaTranche()).isZero();
    }

    /**
     * Revue — R2 / R5 : doublonsContact est limité à 50 lignes triées par création décroissante.
     * Plus de 50 soumissions d'un même contact créées par cet import évincent la ligne déjà en base
     * de la page : la dernière ligne (même lead que celle en base) passe de DEJA_IMPORTEE (aperçu)
     * à DOUBLON, et est recréée.
     */
    @Test
    void revue_plusDe50SoumissionsDUnContact_statutIdentiqueALApercu_etRienRecree() throws Exception {
        String contact = ",Houda " + suffixe + ",houda." + suffixe + "@test.local,Payé," + formulaire
                + ",E-mail,,,,+212662178116,,\n";
        importer(ENTETE + "\n" + "09/21/2026 2:09pm" + contact).andExpect(status().isOk());

        StringBuilder fichier = new StringBuilder(ENTETE + "\n");
        for (int m = 0; m < 50; m++) {
            fichier.append(String.format("09/24/2026 1:%02dam", m)).append(contact);
        }
        fichier.append("09/21/2026 2:09pm").append(contact);

        String apercu = apercu(fichier.toString()).andReturn().getResponse().getContentAsString();
        String importe = importer(fichier.toString()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> attendus = JsonPath.read(apercu, "$.lignes[*].statut");
        List<String> obtenus = JsonPath.read(importe, "$.lignes[*].statut");
        assertThat(attendus.get(50)).isEqualTo("DEJA_IMPORTEE");
        assertThat(obtenus).isEqualTo(attendus);
        assertThat(creeesSurLaTranche()).isEqualTo(51);
    }

    private CfgProject newProjet(String code) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        projet.setDescription("Projet " + code);
        return projectRepository.save(projet);
    }

    private CfgTranche newTranche(String code, CfgProject projet) {
        CfgTranche t = new CfgTranche();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        t.setCfgProject(projet);
        return trancheRepository.save(t);
    }

    /** Vrai principal : le périmètre se résout depuis {@code AuthenticatedUser.admUserId()}. */
    private static RequestPostProcessor connecte(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }
}
