package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Articles des tests d'export et de fiches PDF (FEATURE-patrimoine-import-export.md §3) : référentiels créés par
 * {@link FixturePatrimoine}, articles en masse insérés en une requête (plafond PDF de 2 000 lignes).
 */
@Component
public class FixtureExportPatrimoine {

    private final EntityManager entityManager;
    private final FixturePatrimoine patrimoine;
    private final StkItemRepository itemRepository;

    public FixtureExportPatrimoine(EntityManager entityManager, FixturePatrimoine patrimoine,
                                   StkItemRepository itemRepository) {
        this.entityManager = entityManager;
        this.patrimoine = patrimoine;
        this.itemRepository = itemRepository;
    }

    /** Catégories et types d'article et de sous-article de nature BAT. */
    public record Referentiel(StkItemCategory categorie, StkItemType type, StkItemCategory categorieSous,
                              StkItemType typeSous) {
    }

    public static String jeton() {
        return "P9C" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    public Referentiel referentiel(String jeton) {
        patrimoine.referentielArticle();
        StkKind nature = patrimoine.natureBat();
        return new Referentiel(patrimoine.categorie("CAT-" + jeton, nature, false),
                patrimoine.type("TYP-" + jeton, nature, false),
                patrimoine.categorie("CATS-" + jeton, nature, true),
                patrimoine.type("TYPS-" + jeton, nature, true));
    }

    /** Article (non sous-article) de la tranche, description « Article {code} ». */
    public StkItem article(Referentiel r, String code, CfgTranche tranche, BigDecimal prixVente) {
        StkItem item = item(code, r.categorie(), r.type(), tranche, false, null);
        item.setComSalePrice(prixVente);
        return itemRepository.save(item);
    }

    /** Sous-article de la tranche (IsUnderItemOrigin = 1, sinon absent de la liste) : libre si {@code parent} est nul. */
    public StkItem sousArticle(Referentiel r, String code, CfgTranche tranche, StkItem parent) {
        StkItem item = item(code, r.categorieSous(), r.typeSous(), tranche, true, parent);
        item.setIsUnderItemOrigin(true);
        return itemRepository.save(item);
    }

    private StkItem item(String code, StkItemCategory categorie, StkItemType type, CfgTranche tranche,
                         boolean sousArticle, StkItem parent) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkItemCategory(categorie);
        item.setStkItemType(type);
        item.setStkKind(patrimoine.natureBat());
        item.setStkVocation(patrimoine.vocationVente());
        item.setStkHierarchy(tranche);
        item.setIsUnderItem(sousArticle);
        item.setAlwaysAffected(false);
        item.setParent(parent);
        return item;
    }

    /** {@code nombre} articles {@code <prefixe>0001…} de la tranche, prix de vente 100 000, en une requête. */
    public void articlesEnMasse(Referentiel r, CfgTranche tranche, String prefixe, int nombre) {
        entityManager.flush();
        entityManager.createNativeQuery("""
                        INSERT INTO StkItem (PKey, Code, Description, StkItemCategoryId, StkItemTypeId, StkKindId,
                            StkVocationId, StkHierarchyId, IsUnderItem, AlwaysAffected, ComSalePrice)
                        SELECT NEWID(), CONCAT(:prefixe, RIGHT(CONCAT(N'0000', t.n), 4)),
                            CONCAT(N'Article ', :prefixe, t.n), :categorie, :type, :nature, :vocation, :tranche, 0, 0, 100000
                        FROM (SELECT TOP (:nombre) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
                              FROM sys.all_objects a CROSS JOIN sys.all_objects b) t""")
                .setParameter("prefixe", prefixe)
                .setParameter("categorie", r.categorie().getId())
                .setParameter("type", r.type().getId())
                .setParameter("nature", patrimoine.natureBat().getId())
                .setParameter("vocation", patrimoine.vocationVente().getId())
                .setParameter("tranche", tranche.getId())
                .setParameter("nombre", nombre)
                .executeUpdate();
    }

    /** Compte au rôle Commercial (sans projet affecté : sans restriction de périmètre). */
    public static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }
}
