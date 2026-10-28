package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.dto.patrimoine.LotissementDetailDto;
import com.tsix.immocrm.dto.patrimoine.LotissementEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkCostCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkOrientation;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.services.patrimoine.ArticleService.VOCATION_PROJET;
import static com.tsix.immocrm.services.patrimoine.ArticleService.estOccupe;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/**
 * Écriture et fiche d'un lot de terrain (FEATURE-patrimoine-lotissements.md §1.3) : StkItem nature TER d'une tranche,
 * écrit avec les règles de l'article (code, prix, champs de la fiche) dans la transaction de {@code LotissementService}.
 */
@Component
public class EcritureLot {

    private static final String NATURE_TERRAIN = "TER";
    private static final Set<String> NATURES_LOT = Set.of(NATURE_TERRAIN, "LOT DE TERRAIN");
    private static final Set<String> SOCLE = Set.of("trancheId", "code", "description", "vocationId", "categorieId",
            "typeId", "surfaceProvisoire", "surfaceDefinitive", "prixM2ProvisoireHT", "prixM2ProvisoireTTC",
            "prixM2DefinitifHT", "prixM2DefinitifTTC", "tauxTva", "titreFoncier", "estTitre");
    private static final String PROSPECTIONS = "SELECT COUNT(p) FROM ComProspection p WHERE p.stkItem.id = :id";
    private static final String REMISE_1 = "remiseMax1";
    private static final String REMISE_2 = "remiseMax2";

    private final EntityManager em;
    private final EcritureArticle article;
    private final FicheArticle fiche;
    private final ReferencesArticle references;
    private final ReferencesFicheArticle referencesFiche;
    private final StkKindRepository natures;
    private final JsonMapper json;

    EcritureLot(EntityManager em, EcritureArticle article, FicheArticle fiche, ReferencesArticle references,
                ReferencesFicheArticle referencesFiche, StkKindRepository natures, JsonMapper json) {
        this.em = em;
        this.article = article;
        this.fiche = fiche;
        this.references = references;
        this.referencesFiche = referencesFiche;
        this.natures = natures;
        this.json = json;
    }

    /** Pas de statut de workflow : OMRAN n'affiche que l'état Libre / Réservé / Vendu calculé (décision Q1). */
    public StkItem creer(LotissementEcritureDto d) {
        ArticleEcritureDto a = enArticle(d);
        StkItem lot = new StkItem();
        lot.setId(UUID.randomUUID());
        lot.setStkKind(natures.findFirstByCode(NATURE_TERRAIN)
                .orElseThrow(() -> new ConfigurationManquanteException("Nature TER absente de StkKind.")));
        lot.setStrEntityId(references.entiteArticle());
        lot.setIsUnderItem(false);
        lot.setIsUnderItemOrigin(false);
        EcritureArticle.valeursOmran(lot);
        lot.setIsFinished(true);
        lot.setIsTitled(true);
        placer(lot, article.trancheSaisie(a));
        saisir(lot, d, a, true, ChampsAVider.de(d, null, SOCLE));
        return lot;
    }

    public void modifier(StkItem lot, LotissementEcritureDto d) {
        ChampsAVider v = ChampsAVider.de(d, d.champsAVider(), SOCLE);
        ArticleEcritureDto a = enArticle(d);
        boolean autreTranche = d.trancheId() != null
                && !d.trancheId().equals(Champs.de(lot.getStkHierarchy(), CfgTranche::getId));
        if (estOccupe(lot) && (autreTranche || EcritureArticle.modifieUnChampProtege(lot, a) || v.contient(REMISE_1)
                || v.contient(REMISE_2))) {
            throw new ConflictException(("Le lot %s est affecté à un dossier : ses surfaces, prix et titre foncier ne se "
                    + "modifient plus.").formatted(lot.getCode()));
        }
        if (autreTranche) {
            if (em.createQuery(PROSPECTIONS, Long.class).setParameter("id", lot.getId()).getSingleResult() > 0) {
                throw new ConflictException("Le lot %s a des prospections : il ne change plus de tranche."
                        .formatted(lot.getCode()));
            }
            placer(lot, article.trancheSaisie(a));
            lot.setStkPresentationType(null);
        }
        saisir(lot, d, a, false, v);
    }

    private ArticleEcritureDto enArticle(LotissementEcritureDto d) {
        return json.convertValue(d, ArticleEcritureDto.class);
    }

    private static void placer(StkItem lot, CfgTranche tranche) {
        lot.setStkHierarchy(tranche);
        lot.setCfgCompany(Champs.de(tranche, CfgTranche::getCfgProject, CfgProject::getCfgCompany));
    }

    private void saisir(StkItem lot, LotissementEcritureDto d, ArticleEcritureDto a, boolean creation, ChampsAVider v) {
        lot.setCode(article.codeLibre(obligatoire(d.code(), lot.getCode(), "code"),
                Champs.de(lot.getStkHierarchy(), CfgTranche::getId), lot.getId()));
        lot.setDescription(obligatoire(d.description(), lot.getDescription(), "description"));
        if (creation || d.vocationId() != null) {
            lot.setStkVocation(vocation(d.vocationId()));
        }
        if (creation || d.categorieId() != null) {
            lot.setStkItemCategory(classement(StkItemCategory.class, d.categorieId(), "categorieId",
                    StkItemCategory::getStkKind));
        }
        if (creation || d.typeId() != null) {
            lot.setStkItemType(classement(StkItemType.class, d.typeId(), "typeId", StkItemType::getStkKind));
        }
        lot.setTitleNumber(texte(d.titreFoncier(), lot.getTitleNumber(), COURT, "titreFoncier"));
        lot.setIsTitled(ou(d.estTitre(), lot.getIsTitled()));
        // Réponse 7 : vidés à NULL ; les autres textes restent "" (colonnes qu'OMRAN lit non nulles).
        lot.setAddress(v.contient("adresse") ? null : texte(d.adresse(), lot.getAddress(), 500, "adresse"));
        lot.setNotes(v.contient("notes") ? null : texte(d.notes(), lot.getNotes(), 1000, "notes"));
        lot.setDiscountMax(v.ou(REMISE_1, fraction(d.remiseMax1(), REMISE_1), lot.getDiscountMax()));
        lot.setDiscountMax2(v.ou(REMISE_2, fraction(d.remiseMax2(), REMISE_2), lot.getDiscountMax2()));
        lot.setCos(v.ou("cos", positif(d.cos(), "cos"), lot.getCos()));
        lot.setCuf(v.ou("cuf", positif(d.cuf(), "cuf"), lot.getCuf()));
        lot.setAlinmentStop(v.ou("arreteAlignement", positif(d.arreteAlignement(), "arreteAlignement"),
                lot.getAlinmentStop()));
        lot.setNeighborStop(v.ou("retraitVoisin", positif(d.retraitVoisin(), "retraitVoisin"), lot.getNeighborStop()));
        lot.setMaxHeight(v.ou("hauteurMax", positif(d.hauteurMax(), "hauteurMax"), lot.getMaxHeight()));
        lot.setMapLatitude(v.ou("latitude", d.latitude(), lot.getMapLatitude()));
        lot.setMapLongitude(v.ou("longitude", d.longitude(), lot.getMapLongitude()));
        valoriser(lot, d, a, creation);
        boolean presentationChangee = fiche.appliquer(lot, a, v);
        article.enregistrer(lot, creation);
        fiche.lignes(lot, a, presentationChangee);
    }

    /** Messages nommant le champ du fil, puis le calcul de l'article ({@code CalculPrixArticle}). */
    private void valoriser(StkItem lot, LotissementEcritureDto d, ArticleEcritureDto a, boolean creation) {
        strictementPositive(d.surfaceProvisoire(), "surfaceProvisoire");
        strictementPositive(d.surfaceDefinitive(), "surfaceDefinitive");
        exclusifs(d.prixM2ProvisoireHT(), d.prixM2ProvisoireTTC(), "prixM2Provisoire");
        exclusifs(d.prixM2DefinitifHT(), d.prixM2DefinitifTTC(), "prixM2Definitif");
        if (EcritureArticle.appliquerPrix(lot, a, creation)) {
            article.recalculerPrixDeVente(lot);
        }
    }

    private static void strictementPositive(BigDecimal surface, String champ) {
        if (surface != null && surface.signum() <= 0) {
            throw new InvalidRequestException(champ + " : la superficie doit être strictement supérieure à 0.");
        }
    }

    private static void exclusifs(BigDecimal ht, BigDecimal ttc, String champ) {
        if (ht != null && ttc != null) {
            throw new InvalidRequestException("%sHT et %sTTC : saisir l'un ou l'autre, pas les deux.".formatted(champ, champ));
        }
    }

    private StkVocation vocation(UUID id) {
        StkVocation vocation = reference(StkVocation.class, id, "vocationId");
        if (VOCATION_PROJET.equals(vocation.getCode())) {
            throw new InvalidRequestException("vocationId : la vocation PROJET ne s'applique pas à un lot.");
        }
        return vocation;
    }

    /** Question 2 : la fiche OMRAN propose la nature LOT DE TERRAIN, le filtre de liste la nature TER — les deux valent. */
    private <T> T classement(Class<T> classe, UUID id, String champ, Function<T, StkKind> nature) {
        T trouve = reference(classe, id, champ);
        String code = Champs.de(nature.apply(trouve), StkKind::getCode);
        if (code == null || !NATURES_LOT.contains(code.toUpperCase(Locale.ROOT))) {
            throw new InvalidRequestException(champ + " : la référence n'est pas de nature Terrain ou Lot de terrain.");
        }
        return trouve;
    }

    private <T> T reference(Class<T> classe, UUID id, String champ) {
        if (id == null) {
            throw new InvalidRequestException(champ + " est obligatoire.");
        }
        return referencesFiche.reference(classe, id, champ);
    }

    // ------------------------------------------------------------------ lecture

    public LotissementDetailDto detail(StkItem l) {
        CfgTranche tr = l.getStkHierarchy();
        CfgProject prog = Champs.de(tr, CfgTranche::getCfgProject);
        CfgCompany soc = Champs.de(prog, CfgProject::getCfgCompany);
        StkVocation voc = l.getStkVocation();
        StkItemCategory cat = l.getStkItemCategory();
        StkItemType typ = l.getStkItemType();
        StkPresentationType pres = l.getStkPresentationType();
        return new LotissementDetailDto(l.getId(), l.getCode(), l.getDescription(),
                Champs.de(tr, CfgTranche::getId), Champs.de(tr, CfgTranche::getCode), Champs.de(tr, CfgTranche::getDescription),
                Champs.de(prog, CfgProject::getId), Champs.de(prog, CfgProject::getCode),
                Champs.de(prog, CfgProject::getDescription),
                Champs.de(soc, CfgCompany::getId), Champs.de(soc, CfgCompany::getCode), Champs.de(soc, CfgCompany::getDescription),
                Champs.de(voc, StkVocation::getId), Champs.de(voc, StkVocation::getCode),
                Champs.de(voc, StkVocation::getDescription),
                Champs.de(cat, StkItemCategory::getId), Champs.de(cat, StkItemCategory::getCode),
                Champs.de(cat, StkItemCategory::getDescription),
                Champs.de(typ, StkItemType::getId), Champs.de(typ, StkItemType::getCode),
                Champs.de(typ, StkItemType::getDescription),
                Champs.de(pres, StkPresentationType::getId), Champs.de(pres, StkPresentationType::getCode),
                Champs.de(pres, StkPresentationType::getDescription),
                Champs.de(l.getStkOrientation(), StkOrientation::getDescription),
                Champs.de(l.getStkCostCategory(), StkCostCategory::getDescription),
                Champs.de(l.getCfgCompanySyndic(), CfgCompany::getDescription),
                l.getAddress(), l.getNotes(), l.getTitleNumber(), l.getIsTitled(), estOccupe(l),
                l.getAreaFloor(), l.getArea(), l.getComSaleUnitCostFloorHT(), l.getComSaleUnitCostFloor(),
                l.getComSaleUnitCostHT(), l.getComSaleUnitCost(), l.getSalePriceFloorHT(), l.getSalePriceFloor(),
                l.getSalePriceHT(), l.getSalePrice(), l.getVat(), l.getDiscountMax(), l.getDiscountMax2(),
                l.getCos(), l.getCuf(), l.getAlinmentStop(), l.getNeighborStop(), l.getMaxHeight(), l.getMapLatitude(),
                l.getMapLongitude(), l.getUserCreate(), l.getUserCreateDateTime(), l.getUserUpdate(),
                l.getUserUpdateDateTime(), fiche.lire(l, List.of()));
    }
}
