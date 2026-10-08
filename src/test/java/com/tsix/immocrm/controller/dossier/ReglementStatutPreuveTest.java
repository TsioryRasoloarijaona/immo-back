package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.storage.CleStockage;
import com.tsix.immocrm.storage.PorteurPieceJointe;
import com.tsix.immocrm.storage.StockageMemoire;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Saisie complète d'un règlement, cycle de vie du statut, et règle « pointé ⇒ preuve de paiement »
 * — voir {@code instructions/FEATURE-reglement-statut-preuve.md}.
 *
 * <p>Deux invariants sont testés ici et ne doivent pas être affaiblis :
 * <ul>
 *   <li><b>{@code POINTED} ⟺ {@code SettlementDate} renseignée</b> : vrai sur les 3 244 règlements
 *       réels sans une seule exception. Aucun chemin de l'API ne doit produire l'un sans l'autre.</li>
 *   <li><b>La règle « pointé ⇒ pièce jointe » ne vaut qu'à la transition</b> : 12 des 14 règlements
 *       {@code POINTED} legacy n'ont aucune pièce jointe, et l'application desktop continue
 *       d'écrire dans cette base sans rien contrôler. Les rendre illisibles serait une régression
 *       — d'où {@link #reglementLegacyPointeSansPieceJointe_resteLisible()}.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementStatutPreuveTest {

    @Autowired
    private StockageMemoire stockageMemoire;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private ComDocumentRepository documentRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;

    @Autowired
    private StlSettlementRepository settlementRepository;

    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private EntityManager entityManager;

    // ---------------------------------------------------------------- fixtures

    private ComFolder newFolder(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-RSP-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    private ComDocument newEcheance(ComFolder folder, String montant) {
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new BigDecimal(montant));
        return documentRepository.save(doc);
    }

    private StlSettlementType newType(String code, boolean referenceObligatoire, boolean banqueObligatoire) {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode(code + "-" + UUID.randomUUID());
        type.setDescription(code);
        type.setIsReferenceMandatory(referenceObligatoire);
        type.setIsBankMandatory(banqueObligatoire);
        return settlementTypeRepository.save(type);
    }

    private CfgTier newTier(String prefixe, String nom) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(prefixe + "-" + UUID.randomUUID());
        tier.setDescription(nom);
        return tierRepository.save(tier);
    }

    private String corps(String montant, StlSettlementType type, CfgTier payeur, String extra) {
        return "{\"montant\":" + montant + ",\"dateEcheance\":\"2026-09-01T10:00:00\""
                + ",\"typeId\":\"" + type.getId() + "\",\"payeurId\":\"" + payeur.getId() + "\""
                + (extra != null && !extra.isEmpty() ? "," + extra : "") + "}";
    }

    /** Crée un règlement via l'API et renvoie son id. */
    private UUID creerReglement(ComFolder folder, ComDocument echeance, String montant) throws Exception {
        StlSettlementType type = newType("VIREMENT", false, false);
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");
        String json = mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps(montant, type, payeur, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }

    private void deposerPreuve(ComFolder folder, UUID reglementId) throws Exception {
        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "cheque.pdf", MediaType.APPLICATION_PDF_VALUE, "contenu-cheque".getBytes());
        mockMvc.perform(multipart("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId).file(fichier))
                .andExpect(status().isCreated());
    }

    // ---------------------------------------------------- création : champs requis

    @Test
    void addReglement_montantNegatifOuNul_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-1");
        ComDocument echeance = newEcheance(folder, "100000");
        StlSettlementType type = newType("VIREMENT", false, false);
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");

        for (String montant : new String[]{"0", "-1500.00"}) {
            mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                            folder.getId(), echeance.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(corps(montant, type, payeur, null)))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * Sans ce contrôle un sur-paiement passait silencieusement : le reste devenait négatif et
     * l'échéance s'affichait au statut calculé "PAYE".
     */
    @Test
    void addReglement_montantDepasseResteAPayer_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-2");
        ComDocument echeance = newEcheance(folder, "100000");
        StlSettlementType type = newType("VIREMENT", false, false);
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps("100000.01", type, payeur, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_montantDepasseLeResteApresUnPremierReglement_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-3");
        ComDocument echeance = newEcheance(folder, "100000");
        creerReglement(folder, echeance, "60000.00");

        StlSettlementType type = newType("VIREMENT", false, false);
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");
        // Reste = 40 000 : 40 000,01 doit passer en 400, 40 000 doit passer en 201.
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps("40000.01", type, payeur, null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps("40000.00", type, payeur, null)))
                .andExpect(status().isCreated());
    }

    @Test
    void addReglement_dateEcheanceManquante_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-4");
        ComDocument echeance = newEcheance(folder, "100000");
        StlSettlementType type = newType("VIREMENT", false, false);
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");

        String body = "{\"montant\":1000,\"typeId\":\"" + type.getId() + "\",\"payeurId\":\"" + payeur.getId() + "\"}";
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_typeIdManquant_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-5");
        ComDocument echeance = newEcheance(folder, "100000");
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");

        String body = "{\"montant\":1000,\"dateEcheance\":\"2026-09-01T10:00:00\",\"payeurId\":\""
                + payeur.getId() + "\"}";
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_payeurIdManquant_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-6");
        ComDocument echeance = newEcheance(folder, "100000");
        StlSettlementType type = newType("VIREMENT", false, false);

        String body = "{\"montant\":1000,\"dateEcheance\":\"2026-09-01T10:00:00\",\"typeId\":\""
                + type.getId() + "\"}";
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * {@code IsBankMandatory} était annoncé au front via {@code TypeReglementDto.banqueObligatoire}
     * mais jamais appliqué côté serveur : un chèque sans banque passait en 201.
     */
    @Test
    void addReglement_banqueObligatoireSansBanque_returns400_puis201AvecBanque() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-7");
        ComDocument echeance = newEcheance(folder, "100000");
        StlSettlementType type = newType("CHEQUE", false, true);
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");
        CfgTier banque = newTier("BANQUE", "BMCE BANK");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps("50000.00", type, payeur, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("50000.00", type, payeur, "\"banqueId\":\"" + banque.getId() + "\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.banqueNom").value("BMCE BANK"));
    }

    /** {@code SettlementDate} n'appartient qu'au passage à POINTED (invariant 1). */
    @Test
    void addReglement_avecDateReglement_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-8");
        ComDocument echeance = newEcheance(folder, "100000");
        StlSettlementType type = newType("VIREMENT", false, false);
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("50000.00", type, payeur, "\"dateReglement\":\"2026-09-01T10:00:00\"")))
                .andExpect(status().isBadRequest());
    }

    /** Aucune preuve ne peut exister avant que le règlement existe : POINTED est donc impossible ici. */
    @Test
    void addReglement_statutPointeALaCreation_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-9");
        ComDocument echeance = newEcheance(folder, "100000");
        StlSettlementType type = newType("VIREMENT", false, false);
        CfgTier payeur = newTier("PAYEUR", "Karim ALAMI");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("50000.00", type, payeur, "\"statut\":\"POINTED\"")))
                .andExpect(status().isBadRequest());

        // IN_WORK explicite reste accepté (c'est la valeur par défaut).
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("50000.00", type, payeur, "\"statut\":\"IN_WORK\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("IN_WORK"));
    }

    // ------------------------------------------------ pièces jointes du règlement

    @Test
    void uploadPreuve_creeLaPieceEtLaRendVisible() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-20");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");

        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "cheque-bmce.pdf", MediaType.APPLICATION_PDF_VALUE, "contenu".getBytes());

        mockMvc.perform(multipart("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId)
                        .file(fichier).param("description", "Chèque BMCE n° 123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Chèque BMCE n° 123"))
                .andExpect(jsonPath("$.extension").value(".pdf"))
                // Category déterminée par IsCredit : les 368 pièces de règlements IsCredit=1
                // réelles portent toutes exactement cette valeur (zéro croisement avec Debit).
                .andExpect(jsonPath("$.category").value("Yes.Stl.Credit.Settlement"));

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Chèque BMCE n° 123"));
    }

    @Test
    void uploadPreuve_fichierVide_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-21");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");

        MockMultipartFile vide = new MockMultipartFile(
                "fichier", "vide.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId).file(vide))
                .andExpect(status().isBadRequest());
    }

    @Test
    void piecesJointes_reglementDunAutreDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-RSP-22A");
        ComFolder folder2 = newFolder("DOS-RSP-22B");
        ComDocument echeance = newEcheance(folder1, "100000");
        UUID reglementId = creerReglement(folder1, echeance, "50000.00");

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder2.getId(), reglementId))
                .andExpect(status().isNotFound());
    }

    /**
     * Aller-retour complet dépôt → relecture, le seul du genre côté règlement.
     *
     * <p>Le dépôt passe par l'API, donc par la vraie chaîne : envoi sur le stockage, puis
     * écriture d'{@code AdmAttachedFile} et d'{@code AppFileObject}. La relecture ne renvoie
     * plus le binaire mais une redirection {@code 302} vers une URL présignée — on vérifie donc
     * que la redirection vise bien l'objet de CETTE pièce, et que le contenu réellement stocké
     * est celui qui a été envoyé. Sans ce second contrôle, une redirection vers un mauvais
     * objet passerait le test.
     */
    @Test
    void getFichierPreuve_redirigeVersLObjetReellementDepose() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-23");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");

        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "avis.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF-BYTES".getBytes());
        String json = mockMvc.perform(multipart("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId).file(fichier))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID pieceId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));

        String cle = CleStockage.construire("test", PorteurPieceJointe.REGLEMENT, reglementId,
                pieceId, ".pdf");

        mockMvc.perform(get(
                        "/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}/fichier",
                        folder.getId(), reglementId, pieceId))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(cle)));

        assertThat(stockageMemoire.contenu(cle)).isEqualTo("PDF-BYTES".getBytes());
    }

    // ------------------------------------------- suppression d'une preuve de paiement

    /** Dépose une preuve et renvoie l'id de la pièce créée. */
    private UUID deposerPreuveEtRendreId(ComFolder folder, UUID reglementId, String nom) throws Exception {
        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", nom, MediaType.APPLICATION_PDF_VALUE, ("contenu-" + nom).getBytes());
        String json = mockMvc.perform(multipart("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId).file(fichier))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }

    private void pointer(ComFolder folder, UUID reglementId) throws Exception {
        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"POINTED\",\"dateReglement\":\"2026-09-05T09:00:00\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void supprimerPreuve_dunReglementNonPointe_returns204() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-50");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");
        UUID pieceId = deposerPreuveEtRendreId(folder, reglementId, "cheque.pdf");

        mockMvc.perform(delete("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}",
                        folder.getId(), reglementId, pieceId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Le cœur de la garde : un règlement pointé ne peut pas se retrouver sans justificatif.
     * Voir {@code FEATURE-suppression-pieces-jointes.md} §6 — la règle ne s'évalue qu'ici, jamais
     * en lecture, et ne concerne que 3 règlements de la base réelle.
     */
    @Test
    void supprimerLaDernierePreuve_dunReglementPointe_returns409() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-51");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");
        UUID pieceId = deposerPreuveEtRendreId(folder, reglementId, "cheque.pdf");
        pointer(folder, reglementId);

        mockMvc.perform(delete("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}",
                        folder.getId(), reglementId, pieceId))
                .andExpect(status().isConflict());

        // La pièce est toujours là : un 409 ne doit rien avoir supprimé au passage.
        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(pieceId.toString()));
    }

    /**
     * La garde ne vise que la DERNIÈRE preuve : avec deux pièces la suppression passe, puis la
     * pièce restante redevient protégée.
     *
     * <p>Les {@code clear()} ne sont pas décoratifs. Le pointage a chargé les
     * {@code AdmAttachedFileSummary} du règlement (contrôle « au moins une pièce ») ; ces
     * instances restent dans le contexte de persistance, et Hibernate rend l'instance mémorisée
     * plutôt que la ligne relue — {@code hasFile}, calculé par SQL Server au chargement, garde
     * donc sa valeur d'avant la suppression. En production le cas n'existe pas, chaque requête
     * HTTP ayant son propre contexte ; ici tout le test tient dans une transaction, et le
     * {@code clear()} rétablit cette condition. Même raison que dans
     * {@code DossierListeFiltresEtRequetesTest#compterRequetes}.
     */
    @Test
    void supprimerUnePreuveParmiDeux_dunReglementPointe_returns204() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-52");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");
        UUID premiere = deposerPreuveEtRendreId(folder, reglementId, "cheque.pdf");
        UUID seconde = deposerPreuveEtRendreId(folder, reglementId, "avis-virement.pdf");
        pointer(folder, reglementId);

        mockMvc.perform(delete("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}",
                        folder.getId(), reglementId, premiere))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), reglementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(seconde.toString()));

        entityManager.clear();

        // La pièce restante est maintenant la dernière : elle passe sous la garde.
        mockMvc.perform(delete("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}",
                        folder.getId(), reglementId, seconde))
                .andExpect(status().isConflict());
    }

    /**
     * L'ordre des contrôles : une pièce inconnue sur un règlement pointé doit donner 404, pas le
     * 409 de la garde — sinon la réponse révélerait l'état du règlement.
     */
    @Test
    void supprimerPreuve_pieceInconnueSurReglementPointe_returns404PasLe409() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-53");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");
        deposerPreuveEtRendreId(folder, reglementId, "cheque.pdf");
        pointer(folder, reglementId);

        mockMvc.perform(delete("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}",
                        folder.getId(), reglementId, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void supprimerPreuve_reglementDunAutreDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-RSP-54A");
        ComFolder folder2 = newFolder("DOS-RSP-54B");
        ComDocument echeance = newEcheance(folder1, "100000");
        UUID reglementId = creerReglement(folder1, echeance, "50000.00");
        UUID pieceId = deposerPreuveEtRendreId(folder1, reglementId, "cheque.pdf");

        mockMvc.perform(delete("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}",
                        folder2.getId(), reglementId, pieceId))
                .andExpect(status().isNotFound());
    }

    /** Un règlement annulé n'a rien à protéger : sa preuve se supprime. */
    @Test
    void supprimerLaDernierePreuve_dunReglementAnnule_returns204() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-55");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");
        UUID pieceId = deposerPreuveEtRendreId(folder, reglementId, "cheque.pdf");

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}",
                        folder.getId(), reglementId, pieceId))
                .andExpect(status().isNoContent());
    }

    // ----------------------------------------------------------- transition de statut

    /** Le cœur de la feature : pointer sans preuve de paiement est refusé. */
    @Test
    void pointer_sansPieceJointe_returns409() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-30");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"POINTED\",\"dateReglement\":\"2026-09-05T09:00:00\"}"))
                .andExpect(status().isConflict());

        // Le règlement n'a pas bougé.
        StlSettlement inchange = settlementRepository.findById(reglementId).orElseThrow();
        assertThat(inchange.getStatus()).isEqualTo("IN_WORK");
        assertThat(inchange.getSettlementDate()).isNull();
    }

    @Test
    void pointer_avecPieceJointe_returns200EtEcritLaDateDeReglement() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-31");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");
        deposerPreuve(folder, reglementId);

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"POINTED\",\"dateReglement\":\"2026-09-05T09:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("POINTED"))
                .andExpect(jsonPath("$.dateReglement").value("2026-09-05T09:00:00"));

        StlSettlement pointe = settlementRepository.findById(reglementId).orElseThrow();
        assertThat(pointe.getStatus()).isEqualTo("POINTED");
        assertThat(pointe.getSettlementDate()).isEqualTo(LocalDateTime.of(2026, 9, 5, 9, 0, 0));
    }

    /** Invariant 1 : POINTED sans SettlementDate n'existe pas en base réelle, et ne doit pas exister ici. */
    @Test
    void pointer_sansDateReglement_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-32");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");
        deposerPreuve(folder, reglementId);

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"POINTED\"}"))
                .andExpect(status().isBadRequest());

        StlSettlement inchange = settlementRepository.findById(reglementId).orElseThrow();
        assertThat(inchange.getStatus()).isEqualTo("IN_WORK");
        assertThat(inchange.getSettlementDate()).isNull();
    }

    @Test
    void annuler_neRenseignePasLaDateDeReglement_etNeDemandePasDePreuve() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-33");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CANCELED"))
                .andExpect(jsonPath("$.dateReglement").doesNotExist());

        assertThat(settlementRepository.findById(reglementId).orElseThrow().getSettlementDate()).isNull();
    }

    @Test
    void transitionDepuisUnEtatTerminal_returns409AvecTransitionsAutorisees() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-34");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");
        deposerPreuve(folder, reglementId);

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"POINTED\",\"dateReglement\":\"2026-09-05T09:00:00\"}"))
                .andExpect(status().isOk());

        // POINTED est terminal : ni re-pointage, ni annulation après coup.
        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"POINTED\",\"dateReglement\":\"2026-09-06T09:00:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.transitionsAutorisees").isArray());

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void statutInconnuOuAbsent_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-35");
        ComDocument echeance = newEcheance(folder, "100000");
        UUID reglementId = creerReglement(folder, echeance, "50000.00");

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"ENCAISSE\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifierStatut_reglementInconnuOuDunAutreDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-RSP-36A");
        ComFolder folder2 = newFolder("DOS-RSP-36B");
        ComDocument echeance = newEcheance(folder1, "100000");
        UUID reglementId = creerReglement(folder1, echeance, "50000.00");

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder1.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder2.getId(), reglementId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ régression

    /**
     * 12 des 14 règlements {@code POINTED} de la base réelle n'ont aucune pièce jointe : la règle
     * « pointé ⇒ preuve » ne vaut qu'à la transition et ne doit jamais rendre ces lignes illisibles.
     */
    @Test
    void reglementLegacyPointeSansPieceJointe_resteLisible() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-40");
        ComDocument echeance = newEcheance(folder, "100000");

        StlSettlement legacy = new StlSettlement();
        legacy.setId(UUID.randomUUID());
        legacy.setDocDate(LocalDateTime.of(2021, 12, 20, 10, 0));
        legacy.setAmount(new BigDecimal("52200.00"));
        legacy.setStatus("POINTED");
        legacy.setSettlementDate(LocalDateTime.of(2021, 12, 22, 0, 0));
        settlementRepository.save(legacy);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(legacy);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("52200.00"));
        settlementLineRepository.save(ligne);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value("POINTED"))
                .andExpect(jsonPath("$[0].dateReglement").value("2021-12-22T00:00:00"));

        // Et sa liste de pièces jointes est vide, pas en erreur.
        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes",
                        folder.getId(), legacy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Une valeur de {@code Status} inconnue (texte libre écrit par l'application desktop) ne doit
     * pas faire échouer la lecture, et est traitée comme {@code IN_WORK} pour la transition.
     */
    @Test
    void statutLegacyInconnuEnBase_estLisibleEtTraiteCommeInWork() throws Exception {
        ComFolder folder = newFolder("DOS-RSP-41");
        ComDocument echeance = newEcheance(folder, "100000");

        StlSettlement legacy = new StlSettlement();
        legacy.setId(UUID.randomUUID());
        legacy.setDocDate(LocalDateTime.of(2022, 3, 1, 10, 0));
        legacy.setAmount(new BigDecimal("10000.00"));
        legacy.setStatus("UNPAID");
        settlementRepository.save(legacy);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(legacy);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("10000.00"));
        settlementLineRepository.save(ligne);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("UNPAID"));

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut",
                        folder.getId(), legacy.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CANCELED"));
    }
}
