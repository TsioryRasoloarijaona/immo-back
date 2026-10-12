package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Référentiels des onglets de tranche (FEATURE-patrimoine-tranche-onglets.md §7). */
@Component
public class FixtureOngletsTranche {

    /** PKey de production de la nature « TER », créée si absente. */
    public static final UUID NATURE_TERRAIN = UUID.fromString("343710CB-8806-484E-BB2C-9073569C72A4");

    private final FixturePatrimoine patrimoine;
    private final StkKindRepository kindRepository;
    private final StkItemRepository itemRepository;
    private final ComSettlementCategoryRepository categorieRepository;
    private final EntityManager em;

    public FixtureOngletsTranche(FixturePatrimoine patrimoine, StkKindRepository kindRepository,
                                 StkItemRepository itemRepository,
                                 ComSettlementCategoryRepository categorieRepository, EntityManager em) {
        this.patrimoine = patrimoine;
        this.kindRepository = kindRepository;
        this.itemRepository = itemRepository;
        this.categorieRepository = categorieRepository;
        this.em = em;
    }

    public StkKind natureTerrain() {
        return kindRepository.findById(NATURE_TERRAIN).orElseGet(() -> patrimoine.nature(NATURE_TERRAIN, "TER"));
    }

    /** Terrain de vocation Projet, de nature TER (ou {@code nature} si fournie), dans une chaîne neuve. */
    public StkItem terrain(String code, StkKind nature, String vocation) {
        FixturePatrimoine.Chaine chaine = patrimoine.chaine(code);
        StkItem terrain = new StkItem();
        terrain.setId(UUID.randomUUID());
        terrain.setCode(code);
        terrain.setDescription("Terrain " + code);
        terrain.setArea(new BigDecimal("1250.5"));
        terrain.setStkItemType(chaine.type());
        terrain.setStkItemCategory(chaine.categorie());
        terrain.setStkVocation(patrimoine.vocation(vocation));
        terrain.setStkHierarchy(chaine.tranche());
        terrain.setStkKind(nature);
        return itemRepository.save(terrain);
    }

    public StkItem terrain(String code) {
        return terrain(code, natureTerrain(), "PROJET");
    }

    public ComSettlementCategory categorieReglement(String code, boolean credit, boolean fraisDeVente) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription("Frais " + code);
        categorie.setIsCredit(credit);
        categorie.setIsSaleExpense(fraisDeVente);
        return categorieRepository.save(categorie);
    }

    /** Pose (ou remplace) un paramètre OMRAN {@code AdmParamsGuid}, annulé avec la transaction du test. */
    public void parametreGuid(String code, UUID valeur) {
        em.createNativeQuery("DELETE FROM AdmParamsGuid WHERE Code = :code").setParameter("code", code)
                .executeUpdate();
        em.createNativeQuery("INSERT INTO AdmParamsGuid (Code, CodeValue) VALUES (:code, :valeur)")
                .setParameter("code", code).setParameter("valeur", valeur).executeUpdate();
    }
}
