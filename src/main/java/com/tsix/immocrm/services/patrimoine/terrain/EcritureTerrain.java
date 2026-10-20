package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.domain.CalculPrixArticle;
import com.tsix.immocrm.domain.CalculPrixArticle.PrixLot;
import com.tsix.immocrm.dto.patrimoine.TerrainEcritureDto;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.lnd.LndVocation;
import com.tsix.immocrm.entities.stk.StkCostCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkOrientation;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.patrimoine.ChampsAVider;
import com.tsix.immocrm.services.patrimoine.EcritureArticle;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.ArticleService.estOccupe;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;
import static com.tsix.immocrm.services.patrimoine.terrain.TerrainsVisibles.NATURE_TERRAIN;
import static com.tsix.immocrm.services.patrimoine.terrain.TerrainsVisibles.exigerSousTerrainsAutorises;

/** Écritures de la fiche terrain (FEATURE-patrimoine-terrains.md §1.2), dans la transaction de {@link TerrainService}. */
@Component
class EcritureTerrain {

    private static final String ENTITE_TERRAIN = "LandProject";
    private static final Set<String> OBLIGATOIRES = Set.of("code", "description", "entiteId", "paysId", "villeId",
            "natureId", "vocationId", "categorieId", "typeId", "estTermine", "surfaceProvisoire", "surfaceDefinitive",
            "prixM2ProvisoireHT", "prixM2ProvisoireTTC", "prixM2DefinitifHT", "prixM2DefinitifTTC", "tauxTva",
            "prixAchatM2", "prixAchat");

    private final EntityManager em;
    private final StkItemRepository items;
    private final StkKindRepository natures;
    private final StrEntityRepository entites;
    private final StrStatusRepository statuts;
    private final TracePatrimoine trace;

    EcritureTerrain(EntityManager em, StkItemRepository items, StkKindRepository natures, StrEntityRepository entites,
                    StrStatusRepository statuts, TracePatrimoine trace) {
        this.em = em;
        this.items = items;
        this.natures = natures;
        this.entites = entites;
        this.statuts = statuts;
        this.trace = trace;
    }

    StkItem creer(TerrainEcritureDto d) {
        StkKind terrain = natureTerrain(d.natureId());
        UUID entite = entiteTerrain();
        StrStatus statut = statuts.findByStrConfigEntityId(entite).stream()
                .min(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new ConfigurationManquanteException("Aucun statut pour l'entité LandProject."));
        StkItem t = nouveau(terrain, statut, entite);
        t.setIsFinished(ou(d.estTermine(), true));
        appliquer(t, d, true, ChampsAVider.de(d, null, OBLIGATOIRES));
        return enregistrer(t, true);
    }

    void modifier(StkItem t, TerrainEcritureDto d) {
        ChampsAVider v = ChampsAVider.de(d, d.champsAVider(), OBLIGATOIRES);
        natureTerrain(d.natureId());
        if (t.getParent() != null) {
            exigerSousTerrainsAutorises(t.getParent());
        }
        if (estOccupe(t) && modifieUnChampProtege(t, d)) {
            throw new ConflictException(("Le terrain %s est affecté à un dossier : ses surfaces, prix et titre foncier "
                    + "ne se modifient plus.").formatted(t.getCode()));
        }
        if (d.estTermine() != null && !d.estTermine().equals(t.getIsFinished())) {
            if (t.getParent() != null || t.getLndLandId() != null) {
                throw new InvalidRequestException("estTermine : verrouillé pour un sous-terrain ou un terrain issu "
                        + "d'une prospection foncière.");
            }
            t.setIsFinished(d.estTermine());
        }
        appliquer(t, d, false, v);
        enregistrer(t, false);
    }

    /** {@code LandProjectItem.cs:CreateUnderLandProject} : seuls code, description, surfaces et prix sont saisis. */
    StkItem creerSousTerrain(StkItem parent, TerrainEcritureDto d) {
        exigerSousTerrainsAutorises(parent);
        StkItem s = nouveau(parent.getStkKind(), parent.getStrStatus(), parent.getStrEntityId());
        s.setParent(parent);
        s.setIsFinished(true);
        s.setCfgCompany(parent.getCfgCompany());
        s.setStkVocation(parent.getStkVocation());
        s.setStkItemCategory(parent.getStkItemCategory());
        s.setStkItemType(parent.getStkItemType());
        s.setAdmCountry(parent.getAdmCountry());
        s.setAdmCity(parent.getAdmCity());
        s.setLndLevel(parent.getLndLevel());
        s.setLndVocation(parent.getLndVocation());
        s.setStkOrientation(parent.getStkOrientation());
        s.setStkCostCategory(parent.getStkCostCategory());
        s.setAddress(parent.getAddress());
        s.setGmapLink(parent.getGmapLink());
        identifier(s, d);
        valoriser(s, d, true);
        return enregistrer(s, true);
    }

    // ------------------------------------------------------------------ saisie

    private void appliquer(StkItem t, TerrainEcritureDto d, boolean creation, ChampsAVider v) {
        if (creation || d.entiteId() != null) {
            t.setCfgCompany(societe(d.entiteId()));
        }
        identifier(t, d);
        if (creation || d.paysId() != null) {
            t.setAdmCountry(reference(AdmCountry.class, d.paysId(), "paysId"));
        }
        if (creation || d.villeId() != null) {
            t.setAdmCity(reference(AdmCity.class, d.villeId(), "villeId"));
        }
        if (creation || d.vocationId() != null) {
            t.setStkVocation(reference(StkVocation.class, d.vocationId(), "vocationId"));
        }
        if (creation || d.categorieId() != null) {
            StkItemCategory c = reference(StkItemCategory.class, d.categorieId(), "categorieId");
            t.setStkItemCategory(deNatureTerrain(c, c.getStkKind(), "categorieId"));
        }
        if (creation || d.typeId() != null) {
            StkItemType type = reference(StkItemType.class, d.typeId(), "typeId");
            t.setStkItemType(deNatureTerrain(type, type.getStkKind(), "typeId"));
        }
        t.setLndLevel(v.reference("niveauId", d.niveauId(), t.getLndLevel(), id -> reference(LndLevel.class, id, "niveauId")));
        t.setLndVocation(v.reference("vocationTerrainId", d.vocationTerrainId(), t.getLndVocation(),
                id -> reference(LndVocation.class, id, "vocationTerrainId")));
        t.setStkOrientation(v.reference("orientationId", d.orientationId(), t.getStkOrientation(),
                id -> reference(StkOrientation.class, id, "orientationId")));
        t.setStkCostCategory(v.reference("categorieChargeId", d.categorieChargeId(), t.getStkCostCategory(),
                id -> reference(StkCostCategory.class, id, "categorieChargeId")));
        t.setCfgSupplier(v.reference("fournisseurId", d.fournisseurId(), t.getCfgSupplier(),
                id -> reference(CfgTier.class, id, "fournisseurId")));
        t.setPurchaseDate(v.ou("dateAchat", d.dateAchat(), t.getPurchaseDate()));
        t.setBuildingPermissionDate(v.ou("datePermisConstruire", d.datePermisConstruire(), t.getBuildingPermissionDate()));
        t.setTitleDate(v.ou("dateTitreFoncier", d.dateTitreFoncier(), t.getTitleDate()));
        t.setTitleNumber(texte(v.texte("titreFoncier", d.titreFoncier()), t.getTitleNumber(), COURT, "Le titre foncier"));
        t.setTitleName(texte(v.texte("intituleTitreFoncier", d.intituleTitreFoncier()), t.getTitleName(), COURT,
                "L'intitulé du titre foncier"));
        t.setAddress(texte(v.texte("adresse", d.adresse()), t.getAddress(), 500, "L'adresse"));
        t.setGmapLink(texte(v.texte("lienGoogle", d.lienGoogle()), t.getGmapLink(), 1000, "Le lien Google"));
        t.setNotes(texte(v.texte("notes", d.notes()), t.getNotes(), 1000, "Les notes"));
        t.setAnomalies(texte(v.texte("anomalies", d.anomalies()), t.getAnomalies(), 1000, "Les anomalies"));
        valoriser(t, d, creation);
    }

    /** Code unique dans la société, parmi les terrains (collation sans casse). */
    private void identifier(StkItem t, TerrainEcritureDto d) {
        String code = obligatoire(d.code(), t.getCode(), "Le code");
        UUID societe = Champs.de(t.getCfgCompany(), CfgCompany::getId);
        if (items.existsByCfgCompany_IdAndCodeAndStkKind_CodeAndIdNot(societe, code, NATURE_TERRAIN, t.getId())) {
            throw new ConflictException("Le code %s est déjà pris par un terrain de la société.".formatted(code));
        }
        t.setCode(code);
        t.setDescription(obligatoire(d.description(), t.getDescription(), "La description"));
    }

    /** Prix au m² HT ou TTC par surface, TVA, coûts d'achat ({@code LandProjectItem.cs:3339-3382}). */
    private static void valoriser(StkItem t, TerrainEcritureDto d, boolean creation) {
        BigDecimal provisoire = surface(d.surfaceProvisoire(), t.getAreaFloor(), "provisoire");
        if (provisoire == null) {
            throw new InvalidRequestException("La surface provisoire est obligatoire.");
        }
        BigDecimal definitive = surface(d.surfaceDefinitive(), creation ? provisoire : t.getArea(), "définitive");
        BigDecimal tva = ou(fraction(d.tauxTva(), "Le taux de TVA"), ou(t.getVat(), BigDecimal.ZERO));
        boolean recalcul = creation || Stream.of(d.surfaceProvisoire(), d.surfaceDefinitive(), d.tauxTva(),
                d.prixM2ProvisoireHT(), d.prixM2ProvisoireTTC(), d.prixM2DefinitifHT(), d.prixM2DefinitifTTC())
                .anyMatch(Objects::nonNull);
        t.setAreaFloor(provisoire);
        t.setArea(definitive);
        t.setVat(tva);
        if (recalcul) {
            PrixLot p = prix(provisoire, d.prixM2ProvisoireHT(), d.prixM2ProvisoireTTC(), t.getComSaleUnitCostFloorHT(), tva,
                    "provisoire");
            t.setComSaleUnitCostFloorHT(p.prixM2HT());
            t.setComSaleUnitCostFloor(p.prixM2TTC());
            t.setSalePriceFloorHT(p.prixHT());
            t.setSalePriceFloor(p.prixTTC());
            PrixLot f = prix(definitive, d.prixM2DefinitifHT(), d.prixM2DefinitifTTC(), t.getComSaleUnitCostHT(), tva,
                    "définitif");
            t.setComSaleUnitCostHT(f.prixM2HT());
            t.setComSaleUnitCost(f.prixM2TTC());
            t.setSalePriceHT(f.prixHT());
            t.setSalePrice(f.prixTTC());
        }
        acheter(t, d, definitive);
    }

    private static PrixLot prix(BigDecimal surface, BigDecimal ht, BigDecimal ttc, BigDecimal htActuel, BigDecimal tva,
                                String libelle) {
        String nom = "Prix au m² " + libelle;
        if (ht != null && ttc != null) {
            throw new InvalidRequestException(nom + " : saisir le HT ou le TTC, pas les deux.");
        }
        return ttc != null
                ? CalculPrixArticle.depuisTTC(surface, positif(ttc, nom + " TTC"), tva)
                : CalculPrixArticle.depuisHT(surface, ou(positif(ht, nom + " HT"), ou(htActuel, BigDecimal.ZERO)), tva);
    }

    private static void acheter(StkItem t, TerrainEcritureDto d, BigDecimal surface) {
        if (d.prixAchatM2() != null && d.prixAchat() != null) {
            throw new InvalidRequestException("Prix d'achat : saisir le prix au m² ou le prix total, pas les deux.");
        }
        if (d.prixAchatM2() != null) {
            t.setPurchaseUnitCost(positif(d.prixAchatM2(), "Le prix d'achat au m²"));
            t.setPurchaseCost(d.prixAchatM2().multiply(surface).setScale(6, RoundingMode.HALF_UP));
        } else if (d.prixAchat() != null) {
            t.setPurchaseCost(positif(d.prixAchat(), "Le prix d'achat"));
            if (surface.signum() != 0) {
                t.setPurchaseUnitCost(d.prixAchat().divide(surface, 6, RoundingMode.HALF_UP));
            }
        }
    }

    private static BigDecimal surface(BigDecimal saisie, BigDecimal actuelle, String libelle) {
        if (saisie != null && saisie.signum() <= 0) {
            throw new InvalidRequestException(
                    "La valeur de la supérficie " + libelle + " doit être strictement supérieure à 0 !");
        }
        return ou(saisie, actuelle);
    }

    private static boolean modifieUnChampProtege(StkItem t, TerrainEcritureDto d) {
        return change(d.surfaceProvisoire(), t.getAreaFloor()) || change(d.surfaceDefinitive(), t.getArea())
                || change(d.prixM2ProvisoireHT(), t.getComSaleUnitCostFloorHT())
                || change(d.prixM2ProvisoireTTC(), t.getComSaleUnitCostFloor())
                || change(d.prixM2DefinitifHT(), t.getComSaleUnitCostHT())
                || change(d.prixM2DefinitifTTC(), t.getComSaleUnitCost())
                || change(d.tauxTva(), t.getVat())
                || (d.titreFoncier() != null && !d.titreFoncier().trim().equals(Objects.toString(t.getTitleNumber(), "")));
    }

    private static boolean change(BigDecimal saisie, BigDecimal actuel) {
        return saisie != null && (actuel == null || saisie.compareTo(actuel) != 0);
    }

    // ------------------------------------------------------------------ références

    /** Nature Terrain implicite ; une autre nature saisie → 400 (§8 ter n° 3). */
    private StkKind natureTerrain(UUID saisie) {
        StkKind terrain = natures.findFirstByCode(NATURE_TERRAIN)
                .orElseThrow(() -> new ConfigurationManquanteException("Nature TER absente de StkKind."));
        if (saisie != null && !saisie.equals(terrain.getId())) {
            throw new InvalidRequestException("natureId : un terrain est de nature Terrain.");
        }
        return terrain;
    }

    private UUID entiteTerrain() {
        return entites.findByCodeIn(List.of(ENTITE_TERRAIN)).stream().findFirst().map(StrEntity::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("StrEntity LandProject absente."));
    }

    private CfgCompany societe(UUID id) {
        CfgCompany societe = reference(CfgCompany.class, id, "entiteId");
        if (!Boolean.TRUE.equals(societe.getIsActive())) {
            throw new InvalidRequestException("entiteId : la société %s est inactive.".formatted(societe.getCode()));
        }
        return societe;
    }

    /** Le serveur vérifie la nature des références (écart P1b à ne pas reproduire). */
    private static <T> T deNatureTerrain(T reference, StkKind nature, String champ) {
        if (!NATURE_TERRAIN.equalsIgnoreCase(Champs.de(nature, StkKind::getCode))) {
            throw new InvalidRequestException(champ + " : la référence n'est pas de nature Terrain.");
        }
        return reference;
    }

    private <T> T reference(Class<T> classe, UUID id, String champ) {
        if (id == null) {
            throw new InvalidRequestException(champ + " est obligatoire.");
        }
        T trouvee = em.find(classe, id);
        if (trouvee == null) {
            throw new InvalidRequestException("%s : référence inconnue (%s).".formatted(champ, id));
        }
        return trouvee;
    }

    // ------------------------------------------------------------------ ligne

    private static StkItem nouveau(StkKind nature, StrStatus statut, UUID entite) {
        StkItem t = new StkItem();
        t.setId(UUID.randomUUID());
        t.setStkKind(nature);
        t.setStrStatus(statut);
        t.setStrEntityId(entite);
        t.setIsUnderItem(false);
        t.setIsUnderItemOrigin(false);
        t.setIsTitled(true);
        EcritureArticle.valeursOmran(t);
        return t;
    }

    private StkItem enregistrer(StkItem t, boolean creation) {
        TracePatrimoine.Trace m = trace.maintenant();
        if (creation) {
            t.setUserCreate(m.auteur());
            t.setUserCreateDateTime(m.le());
        }
        t.setUserUpdate(m.auteur());
        t.setUserUpdateDateTime(m.le());
        return items.save(t);
    }
}
