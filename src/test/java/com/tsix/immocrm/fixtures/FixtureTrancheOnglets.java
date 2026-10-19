package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lignes des onglets de la tranche (FEATURE-patrimoine-tranche-onglets.md §2, §4), en SQL natif : les tests ne
 * dépendent pas des entités que le lot crée. Flush avant chaque ordre : la CI porte les FK, le parent doit être écrit.
 */
@Component
public class FixtureTrancheOnglets {

    /** PKey de production de StkKind TER (« TERRAIN »). */
    public static final UUID NATURE_TERRAIN = UUID.fromString("343710CB-8806-484E-BB2C-9073569C72A4");

    private static final String ID = "id";
    private static final String TRANCHE = "tranche";

    private final EntityManager entityManager;
    private final StkKindRepository kindRepository;
    private final FixturePatrimoine patrimoine;

    public FixtureTrancheOnglets(EntityManager entityManager, StkKindRepository kindRepository,
                                 FixturePatrimoine patrimoine) {
        this.entityManager = entityManager;
        this.kindRepository = kindRepository;
        this.patrimoine = patrimoine;
    }

    public StkKind natureTerrain() {
        return kindRepository.findById(NATURE_TERRAIN).orElseGet(() -> patrimoine.nature(NATURE_TERRAIN, "TER"));
    }

    /** Article minimal ; {@code trancheId} null = rattaché à aucune tranche (StkHierarchyId). */
    public UUID article(String code, UUID trancheId, StkKind nature, StkVocation vocation) {
        StkItemCategory categorie = patrimoine.categorie("CAT-" + code, nature, false);
        StkItemType type = patrimoine.type("TYP-" + code, nature, false);
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkItem (PKey, Code, Description, StkHierarchyId, StkItemTypeId, StkItemCategoryId,
                            StkVocationId, StkKindId)
                        VALUES (:id, :code, :description, :tranche, :type, :categorie, :vocation, :nature)""")
                .setParameter(ID, id).setParameter("code", code).setParameter("description", "Article " + code)
                .setParameter(TRANCHE, trancheId).setParameter("type", type.getId())
                .setParameter("categorie", categorie.getId()).setParameter("vocation", vocation.getId())
                .setParameter("nature", nature.getId()));
        return id;
    }

    /** Terrain au sens d'AddLand : nature Terrain, vocation Projet. */
    public UUID terrain(String code) {
        return article(code, null, natureTerrain(), patrimoine.vocationProjet());
    }

    public UUID lienTerrain(UUID trancheId, UUID terrainId, BigDecimal ratio, int sorting) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTrancheStkItem (PKey, CfgTrancheId, StkItemId, Ratio, Sorting)
                        VALUES (:id, :tranche, :terrain, :ratio, :sorting)""")
                .setParameter(ID, id).setParameter(TRANCHE, trancheId).setParameter("terrain", terrainId)
                .setParameter("ratio", ratio).setParameter("sorting", sorting));
        return id;
    }

    public UUID ligneEtage(UUID trancheId, UUID niveauId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTrancheLevel (PKey, CfgTrancheId, LndLevelId) VALUES (:id, :tranche, :niveau)""")
                .setParameter(ID, id).setParameter(TRANCHE, trancheId).setParameter("niveau", niveauId));
        return id;
    }

    public UUID lignePlancher(UUID trancheId, UUID categorieId, BigDecimal surface) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTrancheCoveredFloor (PKey, CfgTrancheId, StkItemCategoryId, Area)
                        VALUES (:id, :tranche, :categorie, :surface)""")
                .setParameter(ID, id).setParameter(TRANCHE, trancheId).setParameter("categorie", categorieId)
                .setParameter("surface", surface));
        return id;
    }

    public UUID categorieReglement(String code, boolean credit, boolean fraisDeVente) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO ComSettlementCategory (PKey, Code, Description, IsCredit, IsSaleExpense)
                        VALUES (:id, :code, :description, :credit, :frais)""")
                .setParameter(ID, id).setParameter("code", code).setParameter("description", "Categorie " + code)
                .setParameter("credit", credit).setParameter("frais", fraisDeVente));
        return id;
    }

    public UUID ligneFrais(UUID trancheId, UUID categorieId, BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTrancheSettlementCategory (PKey, CfgtrancheId, ComSettlementCategoryId, Sorting,
                            AmountHT, VatRatio, AmountTTC)
                        VALUES (:id, :tranche, :categorie, 1, :ht, :tva, :ttc)""")
                .setParameter(ID, id).setParameter(TRANCHE, trancheId).setParameter("categorie", categorieId)
                .setParameter("ht", ht).setParameter("tva", tva).setParameter("ttc", ttc));
        return id;
    }

    /** Paramètre OMRAN {@code AdmParamsGuid} (clé = Code) : posé ou remplacé, annulé avec la transaction du test. */
    public void parametreGuid(String code, UUID valeur) {
        executer(entityManager.createNativeQuery("""
                        IF EXISTS (SELECT 1 FROM AdmParamsGuid WHERE Code = :code)
                            UPDATE AdmParamsGuid SET CodeValue = :valeur WHERE Code = :code
                        ELSE
                            INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (:code, :valeur, NEWID())""")
                .setParameter("code", code).setParameter("valeur", valeur));
    }

    public void habilitationTranche(UUID admUserId, UUID trancheId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmUserHabilitation (PKey, AdmUserId, CfgTrancheId) VALUES (:id, :user, :tranche)""")
                .setParameter(ID, UUID.randomUUID()).setParameter("user", admUserId).setParameter(TRANCHE, trancheId));
    }

    // ------------------------------------------------------------------ lecture

    /** Nombre de lignes, paramètres {@code :id} et {@code :autre}. */
    public int compter(String sql, UUID id, UUID autre) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(ID, id).setParameter("autre", autre).getSingleResult();
        return ((Number) n).intValue();
    }

    /** Valeur décimale d'une colonne, {@code null} si la ligne n'existe pas. */
    @SuppressWarnings("unchecked")
    public BigDecimal decimal(String sql, UUID id) {
        entityManager.flush();
        List<Object> valeurs = entityManager.createNativeQuery(sql).setParameter(ID, id).getResultList();
        return valeurs.isEmpty() ? null : FixturePatrimoine.decimal(valeurs.get(0));
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}
