package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lignes de référence du lot P1a (FEATURE-patrimoine-tranche-fiche.md §8.7) : tables sans entité JPA avant la
 * feature, d'où l'insertion native. Flush avant chaque ordre : la CI porte les FK, le parent doit être écrit.
 */
@Component
public class FixtureTrancheFiche {

    /** PKey de production de CfgTierType EMP (CfgTierType.Employee) et FOU (fournisseur). */
    public static final UUID TYPE_EMPLOYE = UUID.fromString("EEB3FCFF-15BE-484D-B785-923D7587E649");
    public static final UUID TYPE_FOURNISSEUR = UUID.fromString("00685105-DFD9-497B-8830-DBF0FBE91668");

    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";

    private final EntityManager entityManager;
    private final CfgTierTypeRepository tierTypeRepository;
    private final CfgTierRepository tierRepository;

    public FixtureTrancheFiche(EntityManager entityManager, CfgTierTypeRepository tierTypeRepository,
                               CfgTierRepository tierRepository) {
        this.entityManager = entityManager;
        this.tierTypeRepository = tierTypeRepository;
        this.tierRepository = tierRepository;
    }

    // ------------------------------------------------------------------ tiers

    public CfgTier employe(String code) {
        return tiers(code, typeTiers(TYPE_EMPLOYE, "EMP", "EMPLOYE"));
    }

    public CfgTier fournisseur(String code) {
        return tiers(code, typeTiers(TYPE_FOURNISSEUR, "FOU", "FOURNISSEUR"));
    }

    private CfgTierType typeTiers(UUID id, String code, String description) {
        return tierTypeRepository.findById(id)
                .or(() -> tierTypeRepository.findByCode(code))
                .orElseGet(() -> {
                    CfgTierType type = new CfgTierType();
                    type.setId(id);
                    type.setCode(code);
                    type.setDescription(description);
                    return tierTypeRepository.save(type);
                });
    }

    private CfgTier tiers(String code, CfgTierType type) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setDescription("Tiers " + code);
        tier.setCfgTierType(type);
        return tierRepository.save(tier);
    }

    // ------------------------------------------------------------------ référentiels de la tranche

    public UUID standing(String code) {
        return codeDescription("INSERT INTO CfgTrancheStanding (PKey, Code, Description) VALUES (:id, :code, :description)", code);
    }

    public UUID niveauTerrain(String code) {
        return codeDescription("INSERT INTO LndLevel (PKey, Code, Description) VALUES (:id, :code, :description)", code);
    }

    public UUID planComptable(String code) {
        return codeDescription("INSERT INTO ActAccount (PKey, Code, Description) VALUES (:id, :code, :description)", code);
    }

    public UUID rubrique(String code, int sorting) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkTrancheHeading (PKey, Code, Description, Sorting)
                        VALUES (:id, :code, :description, :sorting)""")
                .setParameter(ID, id).setParameter(CODE, code).setParameter(DESCRIPTION, libelle(code))
                .setParameter("sorting", sorting));
        return id;
    }

    public UUID projetFacturation(String code, UUID entiteId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId)
                        VALUES (:id, :code, :description, :entite)""")
                .setParameter(ID, id).setParameter(CODE, code).setParameter(DESCRIPTION, libelle(code))
                .setParameter("entite", entiteId));
        return id;
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

    // ------------------------------------------------------------------ programme

    /** Zone ; {@code parentId} null = racine. */
    public UUID zone(UUID projetId, UUID parentId, String code, int sorting) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgProjectZone (PKey, CfgProjectId, ParentId, Code, Description, Sorting)
                        VALUES (:id, :projet, :parent, :code, :description, :sorting)""")
                .setParameter(ID, id).setParameter("projet", projetId).setParameter("parent", parentId)
                .setParameter(CODE, code).setParameter(DESCRIPTION, libelle(code)).setParameter("sorting", sorting));
        return id;
    }

    public void habilitationProjet(UUID admUserId, UUID projetId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmUserHabilitation (PKey, AdmUserId, CfgProjectId) VALUES (:id, :user, :projet)""")
                .setParameter(ID, UUID.randomUUID()).setParameter("user", admUserId).setParameter("projet", projetId));
    }

    public void pieceJointe(UUID categoryId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmAttachedFile (PKey, Category, CategoryId, Description)
                        VALUES (:id, N'CfgProject', :categorie, N'Piece P1a')""")
                .setParameter(ID, UUID.randomUUID()).setParameter("categorie", categoryId));
    }

    public void message(UUID categoryId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmMessage (PKey, CategoryId, Description) VALUES (:id, :categorie, N'Message P1a')""")
                .setParameter(ID, UUID.randomUUID()).setParameter("categorie", categoryId));
    }

    /** Article minimal rattaché au programme : les 4 FK non nulles de StkItem pointent sur des lignes réelles. */
    public void articleDuProjet(UUID projetId, UUID typeId, UUID categorieId, UUID vocationId, UUID natureId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkItem (PKey, Code, CfgProjectId, StkItemTypeId, StkItemCategoryId, StkVocationId, StkKindId)
                        VALUES (:id, :code, :projet, :type, :categorie, :vocation, :nature)""")
                .setParameter(ID, UUID.randomUUID()).setParameter(CODE, "P1A-ART-" + projetId.toString().substring(0, 8))
                .setParameter("projet", projetId).setParameter("type", typeId).setParameter("categorie", categorieId)
                .setParameter("vocation", vocationId).setParameter("nature", natureId));
    }

    /** Colonnes lues par {@code defauts-entite} (CfgProject:GetDefaultValuesFromCompany). */
    public void coordonneesEntite(UUID entiteId, UUID signataireId) {
        executer(entityManager.createNativeQuery("""
                        UPDATE CfgCompany SET Address = N'7 bd P1a', City = N'Marrakech', SignatureCity = N'Agadir',
                               Email = N'entite@p1a.ma', Phone = N'0524000000', DescriptionLong = N'Longue P1a',
                               AdmSignatoryId = :signataire
                        WHERE PKey = :id""")
                .setParameter("signataire", signataireId).setParameter(ID, entiteId));
    }

    // ------------------------------------------------------------------ tranche

    public void standingDeLaTranche(UUID trancheId, UUID standingId) {
        executer(entityManager.createNativeQuery("UPDATE CfgTranche SET CfgTrancheStandingId = :ref WHERE PKey = :id")
                .setParameter("ref", standingId).setParameter(ID, trancheId));
    }

    // ------------------------------------------------------------------ lecture

    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}
