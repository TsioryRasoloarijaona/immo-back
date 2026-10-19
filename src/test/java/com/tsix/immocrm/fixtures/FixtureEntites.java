package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lignes du lot P5 (FEATURE-entites-juridiques.md §7) écrites en SQL natif : actionnaires, groupes, répartition et
 * colonnes de {@code CfgCompany} que l'entité JPA ne mappe pas avant la feature. Flush avant chaque ordre (la CI porte
 * les FK, le parent doit être écrit), clear après (la lecture suivante repart de la base).
 */
@Component
public class FixtureEntites {

    /** PKey de production de la devise MAD, semée par {@code ci/schema/040_seed_reference.sql}. */
    public static final UUID DEVISE_MAD = UUID.fromString("24EB36A2-2E81-4A12-AF02-6259CFA25BD6");

    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";

    private final EntityManager entityManager;

    public FixtureEntites(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ------------------------------------------------------------------ devises

    /** Devise MAD créée si absente, à sa PKey de production : jamais deux lignes « MAD ». */
    public UUID deviseMad() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StlCurrency WHERE Code = N'MAD')
                            INSERT INTO StlCurrency (PKey, Code, Description) VALUES (:id, N'MAD', N'MAD')""")
                .setParameter(ID, DEVISE_MAD));
        Object id = entityManager.createNativeQuery(
                        "SELECT TOP 1 CONVERT(varchar(36), PKey) FROM StlCurrency WHERE Code = N'MAD' ORDER BY PKey ASC")
                .getSingleResult();
        return UUID.fromString(id.toString());
    }

    /** Rend la devise MAD introuvable par son code (transaction de test annulée ensuite). */
    public void masquerDeviseMad() {
        executer(entityManager.createNativeQuery(
                "UPDATE StlCurrency SET Code = N'MAD-MASQUEE' WHERE Code = N'MAD'"));
    }

    public UUID devise(String code) {
        return codeDescription("INSERT INTO StlCurrency (PKey, Code, Description) VALUES (:id, :code, :description)",
                code);
    }

    // ------------------------------------------------------------------ actionnaires et groupes

    public UUID actionnaire(String code) {
        return codeDescription("INSERT INTO CfgShareholder (PKey, Code, Description) VALUES (:id, :code, :description)",
                code);
    }

    public UUID groupe(String code) {
        return codeDescription(
                "INSERT INTO CfgGroupShareholder (PKey, Code, Description) VALUES (:id, :code, :description)", code);
    }

    public UUID ligneGroupe(UUID groupeId, UUID actionnaireId, BigDecimal pourcentage) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgGroupShareholderLine (PKey, CfgGroupShareholderId, CfgShareholderId, SharingNumber)
                        VALUES (:id, :groupe, :actionnaire, :pourcentage)""")
                .setParameter(ID, id).setParameter("groupe", groupeId).setParameter("actionnaire", actionnaireId)
                .setParameter("pourcentage", pourcentage));
        return id;
    }

    public UUID repartition(UUID entiteId, UUID actionnaireId, int parts) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgCompanyShareholder (PKey, CfgCompanyId, CfgShareholderId, SharingNumber)
                        VALUES (:id, :entite, :actionnaire, :parts)""")
                .setParameter(ID, id).setParameter("entite", entiteId).setParameter("actionnaire", actionnaireId)
                .setParameter("parts", parts));
        return id;
    }

    // ------------------------------------------------------------------ colonnes de CfgCompany

    /** {@code SharingNumber} (nombre de parts) et {@code TotalSharing} (nombre de répartitions). */
    public void parts(UUID entiteId, int nombreParts, int nombreRepartitions) {
        executer(entityManager.createNativeQuery(
                        "UPDATE CfgCompany SET SharingNumber = :parts, TotalSharing = :total WHERE PKey = :id")
                .setParameter("parts", nombreParts).setParameter("total", nombreRepartitions).setParameter(ID, entiteId));
    }

    public void actif(UUID entiteId, boolean actif) {
        executer(entityManager.createNativeQuery("UPDATE CfgCompany SET IsActive = :actif WHERE PKey = :id")
                .setParameter("actif", actif).setParameter(ID, entiteId));
    }

    /** R3 : drapeau jamais renseigné. */
    public void actifNull(UUID entiteId) {
        executer(entityManager.createNativeQuery("UPDATE CfgCompany SET IsActive = NULL WHERE PKey = :id")
                .setParameter(ID, entiteId));
    }

    public void groupeDeLEntite(UUID entiteId, UUID groupeId) {
        executer(entityManager.createNativeQuery("UPDATE CfgCompany SET CfgGroupShareholderId = :groupe WHERE PKey = :id")
                .setParameter("groupe", groupeId).setParameter(ID, entiteId));
    }

    public void deviseDeLEntite(UUID entiteId, UUID deviseId) {
        executer(entityManager.createNativeQuery("UPDATE CfgCompany SET StlCurrencyId = :devise WHERE PKey = :id")
                .setParameter("devise", deviseId).setParameter(ID, entiteId));
    }

    public void dateCreation(UUID entiteId, String iso) {
        executer(entityManager.createNativeQuery(
                        "UPDATE CfgCompany SET CreationDate = CAST(:date AS datetime) WHERE PKey = :id")
                .setParameter("date", iso).setParameter(ID, entiteId));
    }

    /** Valeurs non nulles : un paramètre natif null est lié en binaire et SQL Server refuse la conversion. */
    public void identifiants(UUID entiteId, String fiscal, String registre) {
        executer(entityManager.createNativeQuery(
                        "UPDATE CfgCompany SET FiscalNumber = :fiscal, CommercialRegister = :registre WHERE PKey = :id")
                .setParameter("fiscal", fiscal).setParameter("registre", registre).setParameter(ID, entiteId));
    }

    /** Les 13 colonnes ajoutées par le lot (§7.2), à des valeurs repérables dans une réponse. */
    public void treizeColonnes(UUID entiteId, TreizeColonnes v) {
        executer(entityManager.createNativeQuery("""
                        UPDATE CfgCompany SET FiscalNumber = :fiscal, CommercialRegister = :registre,
                               CreationDate = CAST(:creation AS datetime), Capital = :capital, Cnss = :cnss, Patent = :patente,
                               ICE = :ice, TotalSharing = :total, SharingNumber = :parts,
                               LiquidationDate = CAST(:liquidation AS datetime), HomePart = :partSiege,
                               CfgGroupShareholderId = :groupe, StlCurrencyId = :devise
                        WHERE PKey = :id""")
                .setParameter("fiscal", v.fiscal()).setParameter("registre", v.registre())
                .setParameter("creation", v.creation()).setParameter("capital", v.capital())
                .setParameter("cnss", v.cnss()).setParameter("patente", v.patente()).setParameter("ice", v.ice())
                .setParameter("total", v.nombreRepartitions()).setParameter("parts", v.nombreParts())
                .setParameter("liquidation", v.liquidation()).setParameter("partSiege", v.partSiege())
                .setParameter("groupe", v.groupeId()).setParameter("devise", v.deviseId())
                .setParameter(ID, entiteId));
    }

    /** Dates au format ISO {@code yyyy-MM-ddTHH:mm:ss} (le seul que SQL Server convertit quelle que soit la langue). */
    public record TreizeColonnes(String fiscal, String registre, String creation, BigDecimal capital, String cnss,
                                 String patente, String ice, int nombreRepartitions, int nombreParts,
                                 String liquidation, BigDecimal partSiege, UUID groupeId, UUID deviseId) {
    }

    // ------------------------------------------------------------------ lecture

    public Tuple ligneEntite(UUID id) {
        return ligne("""
                SELECT Code, Description, FiscalNumber, CommercialRegister, CreationDate, Capital, Cnss, Patent, ICE,
                       TotalSharing, SharingNumber, LiquidationDate, HomePart, IsActive,
                       CONVERT(varchar(36), CfgGroupShareholderId) AS CfgGroupShareholderId,
                       CONVERT(varchar(36), StlCurrencyId) AS StlCurrencyId
                FROM CfgCompany WHERE PKey = :id""", id);
    }

    public List<Tuple> sequences(UUID entiteId) {
        return lignes("""
                SELECT Code, TypeCode, SeqYear, BasedOnSystemYear, SeqYearNumber, SeqValueNumber, SeqValue,
                       Separator, SeqPrefix, SeqSuffix
                FROM AdmSequence WHERE CfgCompanyId = :id""", entiteId);
    }

    public List<Tuple> repartitions(UUID entiteId) {
        return lignes("""
                SELECT CONVERT(varchar(36), CfgShareholderId) AS CfgShareholderId, SharingNumber
                FROM CfgCompanyShareholder WHERE CfgCompanyId = :id""", entiteId);
    }

    public List<Tuple> lignesGroupe(UUID groupeId) {
        return lignes("""
                SELECT CONVERT(varchar(36), CfgShareholderId) AS CfgShareholderId, SharingNumber
                FROM CfgGroupShareholderLine WHERE CfgGroupShareholderId = :id""", groupeId);
    }

    public int entitesDeCode(String code) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery("SELECT COUNT(*) FROM CfgCompany WHERE Code = :code")
                .setParameter(CODE, code).getSingleResult();
        return ((Number) n).intValue();
    }

    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> lignes(String sql, UUID id) {
        entityManager.flush();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(ID, id).getResultList();
    }

    private Tuple ligne(String sql, UUID id) {
        entityManager.flush();
        entityManager.clear();
        return (Tuple) entityManager.createNativeQuery(sql, Tuple.class).setParameter(ID, id).getSingleResult();
    }

    private UUID codeDescription(String sql, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(sql)
                .setParameter(ID, id).setParameter(CODE, code).setParameter(DESCRIPTION, libelle(code)));
        return id;
    }

    public static String libelle(String code) {
        return "Libelle " + code;
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}
