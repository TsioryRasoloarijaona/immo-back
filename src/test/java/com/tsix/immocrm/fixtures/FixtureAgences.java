package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

/**
 * Agences de vente (FEATURE-agences-vente.md §3, §8) : configuration du script {@code 206_agences-vente.sql} posée si
 * absente (yesRemsTest ne l'a pas), agences, vendeurs, liens « Vendeur d'agence » et comptes {@code Agence}. JDBC dans la
 * transaction du test ({@code @Transactional}) ; écritures JPA en attente vidées avant chaque ordre (la CI porte les FK).
 */
@Component
public class FixtureAgences {

    public static final String ROLE_AGENCE = "Agence";
    public static final String RELATION_VENDEUR = "Vendeur d'agence";
    public static final String NATURE_AGENCE = "AGV";

    private static final UUID TYPE_CNT = UUID.fromString("37E49449-5066-46B8-B7EF-5D69F57F2BB1");
    private static final UUID TYPE_EMP = UUID.fromString("EEB3FCFF-15BE-484D-B785-923D7587E649");
    private static final UUID NATURE_COM = UUID.fromString("4994A7D0-0303-4B8E-A929-91FF2E0EA20B");

    private final JdbcTemplate jdbc;
    private final EntityManager em;

    public FixtureAgences(DataSource dataSource, EntityManager em) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.em = em;
    }

    // ------------------------------------------------------------------ configuration (script 206)

    public UUID typeContacts() {
        return assurer("SELECT PKey FROM CfgTierType WHERE Code = ? ORDER BY PKey ASC", "CNT",
                () -> jdbc.update("INSERT INTO CfgTierType (PKey, Code, Description) VALUES (?, 'CNT', 'Contacts')",
                        TYPE_CNT.toString()), TYPE_CNT);
    }

    private UUID typePersonnels() {
        return assurer("SELECT PKey FROM CfgTierType WHERE Code = ? ORDER BY PKey ASC", "EMP",
                () -> jdbc.update("INSERT INTO CfgTierType (PKey, Code, Description) VALUES (?, 'EMP', 'Personnels')",
                        TYPE_EMP.toString()), TYPE_EMP);
    }

    public UUID natureAgence() {
        UUID type = typeContacts();
        UUID id = UUID.randomUUID();
        return assurer("SELECT PKey FROM CfgTierKind WHERE Code = ? ORDER BY PKey ASC", NATURE_AGENCE,
                () -> jdbc.update("""
                        INSERT INTO CfgTierKind (PKey, Code, Description, Sorting, CfgTierTypeId)
                        VALUES (?, ?, N'Agence de vente', 99, ?)""", id.toString(), NATURE_AGENCE, type.toString()), id);
    }

    private UUID natureCommercial() {
        UUID type = typePersonnels();
        return assurer("SELECT PKey FROM CfgTierKind WHERE PKey = ?", NATURE_COM.toString(),
                () -> jdbc.update("""
                        INSERT INTO CfgTierKind (PKey, Code, Description, CfgTierTypeId)
                        VALUES (?, 'COM', N'Commercial', ?)""", NATURE_COM.toString(), type.toString()), NATURE_COM);
    }

    public UUID relationVendeur() {
        UUID id = UUID.randomUUID();
        return assurer("SELECT PKey FROM CfgRelationType WHERE Code = ? ORDER BY PKey ASC", RELATION_VENDEUR,
                () -> jdbc.update("INSERT INTO CfgRelationType (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(),
                        RELATION_VENDEUR, RELATION_VENDEUR), id);
    }

    public UUID roleAgence() {
        UUID id = UUID.randomUUID();
        return assurer("SELECT PKey FROM AdmRole WHERE Code = ? ORDER BY PKey ASC", ROLE_AGENCE,
                () -> jdbc.update("""
                        INSERT INTO AdmRole (PKey, Code, Description, WithSequenceFreeRestriction)
                        VALUES (?, ?, N'Agence de vente', 1)""", id.toString(), ROLE_AGENCE), id);
    }

    private UUID assurer(String select, String cle, Runnable insertion, UUID idInsere) {
        em.flush();
        List<UUID> existants = jdbc.queryForList(select, UUID.class, cle);
        if (!existants.isEmpty()) {
            return existants.getFirst();
        }
        insertion.run();
        return idInsere;
    }

    // ------------------------------------------------------------------ tiers et liens

    /** Fiche d'agence : type {@code CNT}, nature {@code AGV} ; code, libellé et raison sociale = {@code code}. */
    public UUID agence(String code) {
        UUID type = typeContacts();
        UUID nature = natureAgence();
        return tiers(code, type, nature);
    }

    /** Contact {@code CNT} sans nature : n'est pas une agence. */
    public UUID contactOrdinaire(String code) {
        return tiers(code, typeContacts(), null);
    }

    /** Vendeur : fiche commerciale (type {@code EMP}, nature {@code COM}). */
    public UUID vendeur(String code) {
        UUID type = typePersonnels();
        UUID nature = natureCommercial();
        return tiers(code, type, nature);
    }

    private UUID tiers(String code, UUID type, UUID nature) {
        em.flush();
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO CfgTier (PKey, Code, Description, SocialReason, FirstName, LastName, CfgTierTypeId, CfgTierKindId)
                VALUES (?, ?, ?, ?, N'Prénom', ?, ?, ?)""", id.toString(), code, "Tiers " + code, "Tiers " + code,
                "Nom " + code, type.toString(), nature == null ? null : nature.toString());
        return id;
    }

    /** Lien « Vendeur d'agence » : tiers = agence, contact = vendeur ; rend {@code CfgTierContact.PKey}. */
    public UUID lier(UUID agence, UUID vendeur) {
        UUID relation = relationVendeur();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgTierContact (PKey, CfgTierId, CfgContactId, CfgRelationTypeId) VALUES (?, ?, ?, ?)",
                id.toString(), agence.toString(), vendeur.toString(), relation.toString());
        return id;
    }

    public int liensDuVendeur(UUID vendeur) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM CfgTierContact WHERE CfgContactId = ?", Integer.class,
                vendeur.toString());
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------------ comptes

    /**
     * Compte actif portant le rôle {@code Agence} (ligne {@code AdmRole} partagée) et les {@code autresRoles} ;
     * {@code vendeur} nul = compte sans fiche employé. Le principal se pose avec {@code FixtureDossierVente.connecte}.
     */
    public AdmUser compteAgence(String code, UUID vendeur, String... autresRoles) {
        UUID roleAgence = roleAgence();
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code + "-" + UUID.randomUUID());
        user.setDescription("Compte " + code);
        user.setDisabled(false);
        user.setCfgEmployee(vendeur == null ? null : em.getReference(CfgTier.class, vendeur));
        em.persist(user);
        role(user, em.getReference(AdmRole.class, roleAgence));
        for (String codeRole : autresRoles) {
            AdmRole autre = new AdmRole();
            autre.setId(UUID.randomUUID());
            autre.setCode(codeRole);
            em.persist(autre);
            role(user, autre);
        }
        em.flush();
        return user;
    }

    private void role(AdmUser user, AdmRole role) {
        AdmUserRole lien = new AdmUserRole();
        lien.setId(UUID.randomUUID());
        lien.setAdmUser(user);
        lien.setAdmRole(role);
        em.persist(lien);
    }

    // ------------------------------------------------------------------ patrimoine et dossiers

    /** {@code ComFolder.CfgCommercialId} : le vendeur du dossier. */
    public void commercialDuDossier(UUID dossier, UUID vendeur) {
        em.flush();
        jdbc.update("UPDATE ComFolder SET CfgCommercialId = ? WHERE PKey = ?", vendeur.toString(), dossier.toString());
        em.clear();
    }

    /** {@code StkItem.ParentId}. */
    public void parent(UUID sousArticle, UUID parent) {
        em.flush();
        jdbc.update("UPDATE StkItem SET ParentId = ? WHERE PKey = ?", parent.toString(), sousArticle.toString());
        em.clear();
    }

    /** {@code StkItem.IsUnderItemOrigin = 1} : la liste des sous-articles ne retient que {@code IsUnderItem} et cette origine. */
    public void origineSousArticle(UUID sousArticle) {
        em.flush();
        jdbc.update("UPDATE StkItem SET IsUnderItemOrigin = 1 WHERE PKey = ?", sousArticle.toString());
        em.clear();
    }

    /** Tranche sans projet (R-V4). */
    public CfgTranche trancheSansProjet(String code) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setDescription("Tranche " + code);
        em.persist(tranche);
        em.flush();
        return tranche;
    }
}
