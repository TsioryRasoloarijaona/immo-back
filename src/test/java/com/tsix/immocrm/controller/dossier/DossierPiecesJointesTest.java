package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.controller.auth.AuthController;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.storage.FixturePieceJointe;
import com.tsix.immocrm.storage.StockageMemoire;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/{id}/pieces-jointes[/...]} — AdmAttachedFile has no declared FK to
 * ComFolder; the linkage ({@code CategoryId = ComFolder.PKey}, matched alone with no "Category"
 * prefix filter — see {@code AdmAttachedFileSummaryRepository#findByDossierId}) was confirmed
 * directly against real data. This test pins that down.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierPiecesJointesTest {

    @Autowired
    private StockageMemoire stockageMemoire;

    @Autowired
    private FixturePieceJointe fixturePieceJointe;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private AdmAttachedFileRepository attachedFileRepository;

    @Autowired
    private AdmAttachedFileTypeRepository attachedFileTypeRepository;

    @Autowired
    private AdmUserRepository userRepository;

    @Autowired
    private AdmRoleRepository roleRepository;

    @Autowired
    private AdmUserRoleRepository userRoleRepository;

    @Autowired
    private AppUserCredentialRepository credentialRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    /** Fully onboarded user (no temporary-password/incomplete-profile gate) — see {@link #login}. */
    private AdmUser newActiveUser(String username, String password, String displayName) {
        CfgTier employee = new CfgTier();
        employee.setId(UUID.randomUUID());
        tierRepository.save(employee);

        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(username);
        user.setDescription(displayName);
        user.setDisabled(false);
        user.setCfgEmployee(employee);
        userRepository.save(user);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode(password));
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode("Commercial-TEST-" + UUID.randomUUID());
        roleRepository.save(role);

        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(UUID.randomUUID());
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        userRoleRepository.save(userRole);

        return user;
    }

    private Cookie login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}""".formatted(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie(AuthController.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private ComFolder newFolder(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PJ-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    private AdmAttachedFile newDossierAttachment(UUID categoryId, byte[] bytes, String extension, String description) {
        AdmAttachedFile file = new AdmAttachedFile();
        file.setId(UUID.randomUUID());
        file.setCategoryId(categoryId);
        file.setCategory("Dossier de vente N° TEST du 01/01/2026 *");
        file.setDescription(description);
        file.setExtension(extension);
        return fixturePieceJointe.deposer(file, bytes);
    }

    @Test
    void getPiecesJointes_returnsOnlyThisDossiersAttachments() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-1");
        ComFolder otherFolder = newFolder("DOS-PJ-2");

        AdmAttachedFile matching = newDossierAttachment(folder.getId(), "PDF-CONTENT".getBytes(StandardCharsets.UTF_8),
                ".pdf", "CIN CLIENT TEST");
        newDossierAttachment(otherFolder.getId(), "OTHER".getBytes(StandardCharsets.UTF_8), ".pdf", "Autre dossier");

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + matching.getId() + "')]").exists())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPiecesJointes_includesAttachmentsRegardlessOfCategoryPrefix() throws Exception {
        // Real data disproved the old assumption that Dossier attachments always carry a
        // "Dossier de vente..." Category: a real dossier had 5/6 attachments categorized
        // "Yes.Com.Sales.Sale" instead. findByDossierId now matches on CategoryId alone, so any
        // Category sharing this dossier's CategoryId must come back.
        ComFolder folder = newFolder("DOS-PJ-CATCHECK");

        AdmAttachedFile cheque = new AdmAttachedFile();
        cheque.setId(UUID.randomUUID());
        cheque.setCategoryId(folder.getId());
        cheque.setCategory("Yes.Com.Sales.Sale");
        cheque.setDescription("CHQ 1700000DH TEST");
        cheque.setExtension(".pdf");
        fixturePieceJointe.deposer(cheque, "CHEQUE".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("Yes.Com.Sales.Sale"));
    }

    @Test
    void getPiecesJointes_includesDocumentType() throws Exception {
        AdmAttachedFileType docType = new AdmAttachedFileType();
        docType.setId(UUID.randomUUID());
        docType.setCode("PIECE D'IDENTITE");
        docType.setDescription("PIECE D'IDENTITE");
        attachedFileTypeRepository.save(docType);

        ComFolder folder = newFolder("DOS-PJ-TYPE");
        AdmAttachedFile cin = new AdmAttachedFile();
        cin.setId(UUID.randomUUID());
        cin.setCategoryId(folder.getId());
        cin.setCategory("Dossier de vente N° TEST du 01/01/2026 *");
        cin.setDescription("CIN client");
        cin.setExtension(".pdf");
        cin.setAdmAttachedFileTypeId(docType.getId());
        fixturePieceJointe.deposer(cin, "CIN".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PIECE D'IDENTITE"));
    }

    @Test
    void getPiecesJointes_nullTypeWhenNotSetInDb() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-NOTYPE");
        newDossierAttachment(folder.getId(), "X".getBytes(StandardCharsets.UTF_8), ".pdf", "Sans type");

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").doesNotExist());
    }

    @Test
    void getFichier_redirigeVersUrlPresigneeDuBonObjet() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-3");
        byte[] contenu = "PDF-CONTENT".getBytes(StandardCharsets.UTF_8);
        AdmAttachedFile file = newDossierAttachment(folder.getId(), contenu, ".pdf", "Piece de test");

        // Le binaire ne transite plus par le backend : l'endpoint redirige vers une URL
        // presignee. On verifie que c'est bien l'objet de CETTE piece qui est vise, et que son
        // contenu est celui depose — sans quoi une redirection vers le mauvais fichier
        // passerait le test.
        String cle = fixturePieceJointe.cleAttendue(file);

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes/{pieceId}/fichier", folder.getId(), file.getId()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(cle)));

        org.assertj.core.api.Assertions.assertThat(stockageMemoire.contenu(cle)).isEqualTo(contenu);
    }

    @Test
    void getFichier_wrongDossier_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-4");
        ComFolder otherFolder = newFolder("DOS-PJ-5");
        AdmAttachedFile file = newDossierAttachment(folder.getId(), "X".getBytes(StandardCharsets.UTF_8), ".pdf", "Test");

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes/{pieceId}/fichier", otherFolder.getId(), file.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPiecesJointes_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadPieceJointe_savesFileAndReturnsIt() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-UPLOAD-1");
        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "cin-client.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF-BYTES".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/dossiers/{id}/pieces-jointes", folder.getId())
                        .file(fichier)
                        .param("description", "CIN client"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("CIN client"))
                .andExpect(jsonPath("$.extension").value(".pdf"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.category").value(org.hamcrest.Matchers.startsWith("Dossier de vente N° " + folder.getCode())))
                .andExpect(jsonPath("$.dateCreation").isNotEmpty())
                // @WithMockUser's principal isn't an AuthenticatedUser, so creePar can't be resolved here —
                // see uploadPieceJointe_recordsAuthenticatedUserAsCreePar for the real-login case.
                .andExpect(jsonPath("$.creePar").doesNotExist());

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("CIN client"))
                .andExpect(jsonPath("$[0].category").value(org.hamcrest.Matchers.startsWith("Dossier de vente N° " + folder.getCode())))
                .andExpect(jsonPath("$[0].dateCreation").isNotEmpty());
    }

    @Test
    void uploadPieceJointe_recordsAuthenticatedUserAsCreePar() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-UPLOAD-CREEPAR");
        AdmUser user = newActiveUser("test.pj." + UUID.randomUUID(), "Password123", "Test PIECEJOINTE");
        Cookie cookie = login(user.getCode(), "Password123");

        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "cin-client.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF-BYTES".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/dossiers/{id}/pieces-jointes", folder.getId())
                        .file(fichier)
                        .cookie(cookie)
                        .param("description", "CIN client"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creePar").value("Test PIECEJOINTE"));

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].creePar").value("Test PIECEJOINTE"));
    }

    @Test
    void uploadPieceJointe_withTypeId_setsAndReturnsType() throws Exception {
        AdmAttachedFileType docType = new AdmAttachedFileType();
        docType.setId(UUID.randomUUID());
        docType.setCode("CONTRAT");
        docType.setDescription("CONTRAT");
        attachedFileTypeRepository.save(docType);

        ComFolder folder = newFolder("DOS-PJ-UPLOAD-TYPE");
        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "contrat.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF-BYTES".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/dossiers/{id}/pieces-jointes", folder.getId())
                        .file(fichier)
                        .param("typeId", docType.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CONTRAT"));

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CONTRAT"));
    }

    @Test
    void uploadPieceJointe_unknownTypeId_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-UPLOAD-BADTYPE");
        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "X".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/dossiers/{id}/pieces-jointes", folder.getId())
                        .file(fichier)
                        .param("typeId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadPieceJointe_noDescription_fallsBackToOriginalFilename() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-UPLOAD-2");
        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "contrat-signe.docx", MediaType.APPLICATION_OCTET_STREAM_VALUE, "DOCX-BYTES".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/dossiers/{id}/pieces-jointes", folder.getId())
                        .file(fichier))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("contrat-signe.docx"))
                .andExpect(jsonPath("$.extension").value(".docx"));
    }

    @Test
    void uploadPieceJointe_emptyFile_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-UPLOAD-3");
        MockMultipartFile fichier = new MockMultipartFile("fichier", "vide.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/dossiers/{id}/pieces-jointes", folder.getId())
                        .file(fichier))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadPieceJointe_unknownDossier_returns404() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "X".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/dossiers/{id}/pieces-jointes", UUID.randomUUID())
                        .file(fichier))
                .andExpect(status().isNotFound());
    }
}
