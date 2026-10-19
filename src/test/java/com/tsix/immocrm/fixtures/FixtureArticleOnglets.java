package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lignes du lot P2b (FEATURE-patrimoine-article-onglets.md §8.3-§8.5) : prospections d'article, dossiers de location /
 * syndic, lignes de dossier ({@code ComFolderItem}), transferts produit et historique de statuts. Les tables sans entité
 * (ou dont l'entité ne mappe pas encore les colonnes du lot) s'écrivent en SQL natif ; flush avant chaque ordre (la CI
 * porte les FK, le parent doit être écrit). Aucun paramètre natif nul : seules les colonnes renseignées sont écrites.
 */
@Component
public class FixtureArticleOnglets {

    /** {@code StrEntity} « ComProspection » (prospection de vente), semée par {@code ci/schema/050_views.sql}. */
    public static final UUID PROSPECTION_VENTE = UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601");
    /** {@code StrEntity} « ComPreProspection ». */
    public static final UUID PRE_PROSPECTION = UUID.fromString("B92DF3D8-158C-4E1E-827A-70FEB172A264");
    /** Nature « TERRAIN » de production : un article de cette nature n'est pas visé par l'attachement en masse. */
    public static final UUID NATURE_TERRAIN = UUID.fromString("343710CB-8806-484E-BB2C-9073569C72A4");

    private final EntityManager entityManager;
    private final FixturePatrimoine patrimoine;
    private final StkItemRepository itemRepository;
    private final ComProspectionRepository prospectionRepository;

    public FixtureArticleOnglets(EntityManager entityManager, FixturePatrimoine patrimoine,
                                 StkItemRepository itemRepository, ComProspectionRepository prospectionRepository) {
        this.entityManager = entityManager;
        this.patrimoine = patrimoine;
        this.itemRepository = itemRepository;
        this.prospectionRepository = prospectionRepository;
    }

    // ------------------------------------------------------------------ patrimoine

    /** Article de nature Bâtiment (PKey de production) dans la tranche. */
    public StkItem article(String code, CfgTranche tranche) {
        return article(code, tranche, patrimoine.natureBat(), null);
    }

    public StkItem article(String code, CfgTranche tranche, StkKind nature, StkItem parent) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkItemCategory(patrimoine.categorie("CAT-" + code, nature, parent != null));
        item.setStkItemType(patrimoine.type("TYP-" + code, nature, parent != null));
        item.setStkKind(nature);
        item.setStkVocation(patrimoine.vocationVente());
        item.setStkHierarchy(tranche);
        item.setIsUnderItem(parent != null);
        item.setParent(parent);
        return itemRepository.save(item);
    }

    public StkKind natureTerrain() {
        StkKind existante = entityManager.find(StkKind.class, NATURE_TERRAIN);
        return existante != null ? existante : patrimoine.nature(NATURE_TERRAIN, "TER");
    }

    // ------------------------------------------------------------------ prospections

    public ComProspection prospection(String code, StkItem article, CfgTranche tranche, CfgTier prospect, UUID type) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setCfgProspect(prospect);
        p.setStkItem(article);
        p.setStkHierarchy(tranche);
        p.setStrEntityId(type);
        p.setDocDate(LocalDateTime.of(2026, 2, 1, 9, 0));
        return prospectionRepository.saveAndFlush(p);
    }

    /** PKey des lignes de {@code ComSaleProspectionView} qui portent l'article (la vue OMRAN que l'onglet recalcule). */
    @SuppressWarnings("unchecked")
    public List<String> idsDeLaVueOmran(UUID articleId) {
        entityManager.flush();
        return ((List<Object>) entityManager.createNativeQuery(
                        "SELECT DISTINCT CONVERT(varchar(36), PKey) FROM ComSaleProspectionView WHERE StkItemId = :id")
                .setParameter("id", articleId)
                .getResultList()).stream().map(o -> o.toString().toLowerCase(Locale.ROOT)).toList();
    }

    // ------------------------------------------------------------------ dossiers hors vente

    /** Dossier de location ({@code RntFolder}) sur l'article : aucun dossier de vente vif ne doit le porter (trigger). */
    public UUID location(String code, UUID articleId, UUID statutId, UUID clientId) {
        return inserer("RntFolder", colonnes("Code", code, "StkItemId", articleId, "StrStatusId", statutId,
                "CfgCustomerId", clientId));
    }

    /** Dossier de syndic ({@code SynFolder.StkItemId}). */
    public UUID syndic(String code, UUID articleId, UUID statutId, UUID clientId) {
        return inserer("SynFolder", colonnes("Code", code, "StkItemId", articleId, "StrStatusId", statutId,
                "CfgCustomerId", clientId));
    }

    /** Dossier de syndic réduit aux colonnes présentes partout (yesRemsTest n'a que 5 colonnes) : pour l'historique. */
    public UUID syndicSansArticle(String code) {
        return inserer("SynFolder", colonnes("Code", code));
    }

    /** Ligne de dossier de vente : un sous-article affecté au dossier. */
    public UUID ligneDossier(UUID dossierId, UUID articleId) {
        return inserer("ComFolderItem", colonnes("ComFolderId", dossierId, "StkItemId", articleId, "IsUnderItem", true,
                "Code", "LIGNE-" + articleId.toString().substring(0, 8)));
    }

    /** Transfert produit : {@code StkOldItemId} = ancien article, {@code StkItemId} = nouvel article. */
    public UUID transfertProduit(String code, UUID ancienArticleId, UUID nouvelArticleId, boolean legalise) {
        return inserer("ComSaleWithdrawalProduct", colonnes("Code", code, "StkOldItemId", ancienArticleId,
                "StkItemId", nouvelArticleId, "IsLegalized", legalise));
    }

    // ------------------------------------------------------------------ historique de statuts

    /** Ligne {@code StrFolderStatusHistory} ; une valeur nulle dans {@link LigneHistorique} = colonne non écrite. */
    public UUID historique(LigneHistorique ligne) {
        return inserer("StrFolderStatusHistory", colonnes("DocDate", ligne.date(), "Description", ligne.description(),
                "StkItemId", ligne.articleId(), "ComFolderId", ligne.dossierId(), "RntFolderId", ligne.locationId(),
                "SynFolderId", ligne.syndicId(), "AdmUserId", ligne.compteId(), "CfgTierId", ligne.clientId(),
                "ComSaleWithdrawalProductId", ligne.transfertProduitId()));
    }

    public record LigneHistorique(LocalDateTime date, String description, UUID articleId, UUID dossierId,
                                  UUID locationId, UUID syndicId, UUID compteId, UUID clientId,
                                  UUID transfertProduitId) {

        public static LigneHistorique de(LocalDateTime date, String description, UUID articleId) {
            return new LigneHistorique(date, description, articleId, null, null, null, null, null, null);
        }

        public LigneHistorique dossier(UUID id) {
            return new LigneHistorique(date, description, articleId, id, locationId, syndicId, compteId, clientId,
                    transfertProduitId);
        }

        public LigneHistorique location(UUID id) {
            return new LigneHistorique(date, description, articleId, dossierId, id, syndicId, compteId, clientId,
                    transfertProduitId);
        }

        public LigneHistorique syndic(UUID id) {
            return new LigneHistorique(date, description, articleId, dossierId, locationId, id, compteId, clientId,
                    transfertProduitId);
        }

        public LigneHistorique compte(UUID id) {
            return new LigneHistorique(date, description, articleId, dossierId, locationId, syndicId, id, clientId,
                    transfertProduitId);
        }

        public LigneHistorique client(UUID id) {
            return new LigneHistorique(date, description, articleId, dossierId, locationId, syndicId, compteId, id,
                    transfertProduitId);
        }

        public LigneHistorique transfertProduit(UUID id) {
            return new LigneHistorique(date, description, articleId, dossierId, locationId, syndicId, compteId,
                    clientId, id);
        }
    }

    // ------------------------------------------------------------------ outils

    private static Map<String, Object> colonnes(Object... paires) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        for (int i = 0; i < paires.length; i += 2) {
            if (paires[i + 1] != null) {
                valeurs.put((String) paires[i], paires[i + 1]);
            }
        }
        return valeurs;
    }

    private UUID inserer(String table, Map<String, Object> valeurs) {
        UUID id = UUID.randomUUID();
        StringBuilder noms = new StringBuilder("PKey");
        StringBuilder marques = new StringBuilder(":pkey");
        int rang = 0;
        for (String colonne : valeurs.keySet()) {
            noms.append(", ").append(colonne);
            marques.append(", :p").append(rang++);
        }
        entityManager.flush();
        Query query = entityManager.createNativeQuery(
                "INSERT INTO " + table + " (" + noms + ") VALUES (" + marques + ")");
        query.setParameter("pkey", id);
        rang = 0;
        for (Object valeur : valeurs.values()) {
            query.setParameter("p" + rang++, valeur);
        }
        query.executeUpdate();
        return id;
    }
}
