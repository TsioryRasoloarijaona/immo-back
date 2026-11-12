package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

/**
 * Statuts de pièces d'achat éditables (FEATURE-statuts-achat-parite.md) : images {@code CfgImage}, types de documents
 * {@code AdmAttachedFileType}, comptes porteurs ou non du droit OMRAN de l'écran. Parents insérés d'abord (FK de la CI).
 */
@Component
public class FixtureStatutsAchat {

    public static final String DROIT_STATUTS = "Yes.Buy.Front.Config.BuyDocumentStatus";
    /** Rôle du jeton : jamais Administrateur, les droits viennent d'{@code AdmRight} seul. */
    public static final String ROLE_JWT = "Assistante Commerciale";
    public static final String ADMINISTRATEUR = "Administrateur";

    /** Octets d'en-tête PNG ; OMRAN ne contrôle pas le contenu d'une image. */
    public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    // Absente de yesRemsTest et de la CI avant le lot ; définition de yesRemsProd.
    private static final String TABLE_DOCUMENTS = """
            IF OBJECT_ID('dbo.BuyDocumentStatusDocumentToAttach', 'U') IS NULL
            CREATE TABLE dbo.BuyDocumentStatusDocumentToAttach (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, BuyDocumentStatusId uniqueidentifier NULL,
                IsMandatory bit NULL, WithTitle bit NULL,
                UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                AdmAttachedFileTypeId uniqueidentifier NULL)""";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureDossierVente dossiers;

    public FixtureStatutsAchat(EntityManager em, JdbcTemplate jdbc, FixtureDossierVente dossiers) {
        this.em = em;
        this.jdbc = jdbc;
        this.dossiers = dossiers;
    }

    /** Idempotent ; dans la transaction du test (annulé avec elle) ou validé hors transaction (test d'audit). */
    public void tables() {
        jdbc.execute(TABLE_DOCUMENTS);
    }

    public UUID image(String code, byte[] octets) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO CfgImage (PKey, Code, ImageData) VALUES (?, ?, ?)", id.toString(), code, octets);
        return id;
    }

    /** Type de document à joindre : Code = Description. */
    public UUID typeDocument(String libelle) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO AdmAttachedFileType (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), libelle,
                libelle);
        return id;
    }

    /** Ligne {@code BuyDocumentStatusDocumentToAttach} posée en SQL (la table doit exister : {@link #tables()}). */
    public void documentAJoindre(UUID statut, UUID type, boolean obligatoire, boolean avecTitre) {
        em.flush();
        jdbc.update("""
                        INSERT INTO BuyDocumentStatusDocumentToAttach (PKey, BuyDocumentStatusId, AdmAttachedFileTypeId,
                            IsMandatory, WithTitle) VALUES (?, ?, ?, ?, ?)""",
                UUID.randomUUID().toString(), statut.toString(), type.toString(), obligatoire, avecTitre);
    }

    /** Compte dont un rôle en base s'appelle Administrateur (seul ce rôle a tous les droits OMRAN). */
    public RequestPostProcessor administrateur(String jeton) {
        AdmUser compte = dossiers.compte("k2.adm." + jeton, ADMINISTRATEUR);
        em.flush();
        return FixtureDossierVente.connecte(compte, ADMINISTRATEUR);
    }

    /** Compte à rôle propre (jamais Administrateur) portant exactement ces droits {@code AdmRight}. */
    public RequestPostProcessor compte(String jeton, String... droits) {
        String role = "Statuts achat " + jeton + "-" + UUID.randomUUID().toString().substring(0, 6);
        AdmUser compte = dossiers.compte("k2." + jeton, role);
        em.flush();
        String roleId = jdbc.queryForObject("SELECT CAST(AdmRoleId AS varchar(36)) FROM AdmUserRole WHERE AdmUserId = ?",
                String.class, compte.getId().toString());
        for (String droit : droits) {
            jdbc.update("INSERT INTO AdmRight (PKey, Code, Description, AdmRoleId) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID().toString(), droit, droit, roleId);
        }
        return FixtureDossierVente.connecte(compte, ROLE_JWT);
    }

    public int documentsDuStatut(UUID statut) {
        em.flush();
        return compter("SELECT COUNT(*) FROM BuyDocumentStatusDocumentToAttach WHERE BuyDocumentStatusId = ?", statut);
    }

    public int statutsParId(UUID statut) {
        em.flush();
        return compter("SELECT COUNT(*) FROM BuyDocumentStatus WHERE PKey = ?", statut);
    }

    /** Statuts dont le code ou la description vaut l'une des valeurs (comparaison de la base, sans casse). */
    public int statutsNommes(String code, String description) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM BuyDocumentStatus WHERE LTRIM(RTRIM(Code)) = ? OR Description = ?",
                Integer.class, code, description);
        return n == null ? 0 : n;
    }

    private int compter(String sql, UUID id) {
        Integer n = jdbc.queryForObject(sql, Integer.class, id.toString());
        return n == null ? 0 : n;
    }
}
