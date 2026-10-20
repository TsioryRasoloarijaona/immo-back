package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.CalculPrixArticle;
import com.tsix.immocrm.domain.CalculPrixArticle.PrixLot;
import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.ArticleService.estOccupe;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/**
 * Écritures d'articles et de sous-articles, appelées par {@link ArticleService} dans sa transaction
 * (FEATURE-patrimoine-ecriture.md §5). {@code ComFolderId}/{@code RntFolderId} ne sont jamais écrits ici.
 */
@Component
class EcritureArticle {

    private static final String PAS_LIBRE = "Ce sous article n'est pas libre.";
    private static final String NATURE_TERRAIN = "TER";

    private final StkItemRepository items;
    private final CfgTrancheRepository tranches;
    private final ComFolderRepository dossiers;
    private final ReferencesArticle references;
    private final TracePatrimoine trace;
    private final FicheArticle fiche;

    EcritureArticle(StkItemRepository items, CfgTrancheRepository tranches, ComFolderRepository dossiers,
                    ReferencesArticle references, TracePatrimoine trace, FicheArticle fiche) {
        this.items = items;
        this.tranches = tranches;
        this.dossiers = dossiers;
        this.references = references;
        this.trace = trace;
        this.fiche = fiche;
    }

    StkItem creer(ArticleEcritureDto d) {
        StkItem article = nouveau(trancheSaisie(d), references.vocation(d.vocationId()), false);
        article.setIsTitled(ou(d.estTitre(), true));
        saisir(article, d, true);
        return article;
    }

    /** Écran Sous-articles, OMRAN {@code UnderItem()} : sous-article libre, ou rattaché si {@code parentId}. */
    StkItem creerLibre(ArticleEcritureDto d) {
        CfgTranche tranche = trancheSaisie(d);
        StkVocation vocation = references.vocation(d.vocationId());
        StkItem parent = d.parentId() == null ? null : parentPour(d.parentId(), tranche, vocation);
        exigerParent(parent, d.toujoursAffecte());
        StkItem sous = nouveau(tranche, vocation, true);
        sous.setParent(parent);
        sous.setIsTitled(ou(d.estTitre(), false));
        sous.setAlwaysAffected(ou(d.toujoursAffecte(), false));
        saisir(sous, d, true);
        if (parent != null) {
            reecrireParent(parent);
        }
        return sous;
    }

    /** Un rattaché ne se modifie que depuis la fiche de son parent ({@code UnderItemList.cs}) ; {@code parentId} = affecter. */
    void modifierLibre(StkItem sous, ArticleEcritureDto d) {
        if (sous.getParent() != null) {
            throw new ConflictException(("Cet article est attaché à l'article parent %s il sera edité en lecture seule. "
                    + "Pour le modifier il faut l'ouvrir à partir de la fiche de son article parent !")
                    .formatted(sous.getParent().getCode()));
        }
        if (FicheArticle.vidages(d).contient("parentId")) {
            throw new InvalidRequestException("parentId : un sous-article se désaffecte depuis la fiche de son article.");
        }
        StkVocation vocation = d.vocationId() == null ? sous.getStkVocation() : references.vocation(d.vocationId());
        StkItem parent = d.parentId() == null ? null : parentPour(d.parentId(), sous.getStkHierarchy(), vocation);
        exigerParent(parent, d.toujoursAffecte());
        modifier(sous, d);
        if (parent != null) {
            affecter(parent, sous, ou(d.toujoursAffecte(), false));
        }
    }

    private CfgTranche trancheSaisie(ArticleEcritureDto d) {
        if (d.trancheId() == null) {
            throw new InvalidRequestException("La tranche est obligatoire.");
        }
        return tranches.trancheVisible(d.trancheId())
                .orElseThrow(() -> new InvalidRequestException("Tranche inconnue : " + d.trancheId()));
    }

    /** OMRAN ne contrôle ni la tranche ni la vocation du parent : nous oui (FEATURE-patrimoine-sous-articles.md §0, R4-3). */
    private StkItem parentPour(UUID parentId, CfgTranche tranche, StkVocation vocation) {
        StkItem parent = items.findVisible(parentId)
                .orElseThrow(() -> new InvalidRequestException("Article parent inconnu : " + parentId));
        if (!Objects.equals(Champs.de(parent.getStkHierarchy(), CfgTranche::getId), Champs.de(tranche, CfgTranche::getId))) {
            throw new InvalidRequestException("L'article parent " + parent.getCode() + " n'est pas de la tranche du sous-article.");
        }
        exigerPrincipal(parent);
        exigerMemeVocation(parent, vocation);
        refuserSiOccupe(parent);
        return parent;
    }

    /** Un rattachement nouveau exige la vocation du parent (R4-3) ; les couples existants ne sont pas repris. */
    private static void exigerMemeVocation(StkItem parent, StkVocation vocation) {
        if (!Objects.equals(Champs.de(parent.getStkVocation(), StkVocation::getId), Champs.de(vocation, StkVocation::getId))) {
            throw new InvalidRequestException("L'article parent " + parent.getCode() + " n'a pas la vocation du sous-article.");
        }
    }

    private static void exigerParent(StkItem parent, Boolean toujoursAffecte) {
        if (parent == null && Boolean.TRUE.equals(toujoursAffecte)) {
            throw new InvalidRequestException("Vous ne pouvez pas toujours affecter un sous article sans article parent !");
        }
    }

    StkItem creerSousArticle(StkItem parent, ArticleEcritureDto d) {
        refuserTrancheEtVocation(d);
        exigerPrincipal(parent);
        refuserSiOccupe(parent);
        StkItem sous = nouveau(parent.getStkHierarchy(), parent.getStkVocation(), true);
        sous.setParent(parent);
        sous.setStkBuilding(parent.getStkBuilding());
        sous.setIsTitled(ou(d.estTitre(), false));
        sous.setAlwaysAffected(ou(d.toujoursAffecte(), false));
        saisir(sous, d, true);
        reecrireParent(parent);
        return sous;
    }

    void modifier(StkItem article, ArticleEcritureDto d) {
        if (NATURE_TERRAIN.equalsIgnoreCase(Champs.de(article.getStkKind(), StkKind::getCode))) {
            throw new InvalidRequestException("Les terrains se modifient depuis la fiche terrain.");
        }
        if (Boolean.TRUE.equals(article.getIsUnderItem()) && article.getParent() != null) {
            refuserTrancheEtVocation(d);
        }
        UUID trancheActuelle = Champs.de(article.getStkHierarchy(), CfgTranche::getId);
        if (d.trancheId() != null && !d.trancheId().equals(trancheActuelle)) {
            throw new InvalidRequestException("La tranche d'un article ne se modifie pas.");
        }
        ChampsAVider v = FicheArticle.vidages(d);
        if (estOccupe(article) && (modifieUnChampProtege(article, d) || v.contient("remiseMax1")
                || v.contient("remiseMax2"))) {
            throw new ConflictException(("L'article %s est affecté au dossier %s : ses prix, surfaces, titre foncier et "
                    + "classification ne se modifient plus.").formatted(article.getCode(), dossierOccupant(article)));
        }
        if (d.vocationId() != null) {
            article.setStkVocation(references.vocation(d.vocationId()));
        }
        // Avec parentId (écran Sous-articles), « toujours affecté » suit l'affectation qui vient après.
        if (d.toujoursAffecte() != null && d.parentId() == null) {
            exigerParent(article.getParent(), d.toujoursAffecte());
            article.setAlwaysAffected(d.toujoursAffecte());
        }
        boolean prixRecalcules = saisir(article, d, false, v);
        if (prixRecalcules && article.getParent() != null) {
            reecrireParent(article.getParent());
        }
    }

    private boolean saisir(StkItem a, ArticleEcritureDto d, boolean creation) {
        return saisir(a, d, creation, FicheArticle.vidages(d));
    }

    /** Saisie complète puis enregistrement ; les lignes de la fiche (vues, contenances) suivent l'article. */
    private boolean saisir(StkItem a, ArticleEcritureDto d, boolean creation, ChampsAVider v) {
        boolean presentationChangee = fiche.appliquer(a, d, v);
        boolean prixRecalcules = appliquer(a, d, creation, v);
        enregistrer(a, creation);
        fiche.lignes(a, d, presentationChangee);
        return prixRecalcules;
    }

    private static void refuserTrancheEtVocation(ArticleEcritureDto d) {
        if (d.trancheId() != null) {
            throw new InvalidRequestException("La tranche d'un sous-article est celle de son article.");
        }
        if (d.vocationId() != null) {
            throw new InvalidRequestException("vocationId : la vocation d'un sous-article est celle de son article.");
        }
    }

    /** {@code BuildItem:AffectUnderItem} : même tranche, sous-article libre, article non occupé. */
    void affecter(StkItem parent, StkItem sous, boolean toujoursAffecte) {
        exigerPrincipal(parent);
        exigerMemeVocation(parent, sous.getStkVocation());
        boolean libre = Objects.equals(Champs.de(sous.getStkHierarchy(), CfgTranche::getId),
                Champs.de(parent.getStkHierarchy(), CfgTranche::getId))
                && Boolean.TRUE.equals(sous.getIsUnderItem()) && sous.getParent() == null
                && Boolean.FALSE.equals(sous.getAlwaysAffected()) && !estOccupe(sous);
        if (!libre) {
            throw new ConflictException(PAS_LIBRE);
        }
        refuserSiOccupe(parent);
        sous.setParent(parent);
        sous.setAlwaysAffected(toujoursAffecte);
        enregistrer(sous, false);
        reecrireParent(parent);
    }

    /** {@code BuildItem:CancelAffectUnderItem}. */
    void desaffecter(StkItem parent, StkItem sous) {
        if (sous.getParent() == null || !sous.getParent().getId().equals(parent.getId())) {
            throw new ConflictException("Le sous-article %s n'est pas rattaché à l'article %s."
                    .formatted(sous.getCode(), parent.getCode()));
        }
        if (sous.getComFolderId() != null) {
            throw new ConflictException("Vous ne pouvez pas désaffecter un sous article ayant un dossier de vente !");
        }
        if (sous.getRntFolderId() != null) {
            throw new ConflictException("Vous ne pouvez pas désaffecter un sous article ayant un dossier de location !");
        }
        refuserSiOccupe(parent);
        sous.setParent(null);
        sous.setAlwaysAffected(false);
        enregistrer(sous, false);
        reecrireParent(parent);
    }

    // ------------------------------------------------------------------ saisie

    /** Rend vrai si les prix de l'article ont été recalculés. */
    private boolean appliquer(StkItem a, ArticleEcritureDto d, boolean creation, ChampsAVider v) {
        boolean sous = Boolean.TRUE.equals(a.getIsUnderItem());
        UUID trancheId = Champs.de(a.getStkHierarchy(), CfgTranche::getId);
        a.setCode(codeLibre(obligatoire(d.code(), a.getCode(), "Le code"), trancheId, a.getId()));
        a.setDescription(obligatoire(d.description(), a.getDescription(), "La description"));
        if (creation || d.categorieId() != null) {
            a.setStkItemCategory(references.categorie(d.categorieId(), sous));
        }
        if (creation || d.typeId() != null) {
            a.setStkItemType(references.type(d.typeId(), sous));
        }
        if (d.immeubleId() != null || (creation && a.getStkBuilding() == null)) {
            a.setStkBuilding(references.immeuble(d.immeubleId()));
        }
        if (creation || d.etageId() != null) {
            a.setStkFloor(references.etage(d.etageId()));
        }
        a.setTitleNumber(texte(d.titreFoncier(), a.getTitleNumber(), COURT, "Le titre foncier"));
        a.setIsTitled(ou(d.estTitre(), a.getIsTitled()));
        a.setAddress(texte(d.adresse(), a.getAddress(), 500, "L'adresse"));
        a.setNotes(texte(d.notes(), a.getNotes(), 1000, "Les notes"));
        a.setDiscountMax(v.ou("remiseMax1", fraction(d.remiseMax1(), "Remise max 1"), a.getDiscountMax()));
        a.setDiscountMax2(v.ou("remiseMax2", fraction(d.remiseMax2(), "Remise max 2"), a.getDiscountMax2()));
        a.setInteriorArea(v.ou("surfaceInterieure", positif(d.surfaceInterieure(), "Surface intérieure"),
                a.getInteriorArea()));
        a.setWallsArea(v.ou("surfaceMurs", positif(d.surfaceMurs(), "Surface des murs"), a.getWallsArea()));
        a.setCommonPartsArea(v.ou("surfacePartiesCommunes", positif(d.surfacePartiesCommunes(),
                "Surface des parties communes"), a.getCommonPartsArea()));
        boolean recalcules = appliquerPrix(a, d, creation);
        if (recalcules) {
            recalculerPrixDeVente(a);
        }
        return recalcules;
    }

    private boolean appliquerPrix(StkItem a, ArticleEcritureDto d, boolean creation) {
        BigDecimal surfaceProvisoire = surface(d.surfaceProvisoire(), ou(a.getAreaFloor(), BigDecimal.ONE), "provisoire");
        BigDecimal surfaceDefinitive = surface(d.surfaceDefinitive(), creation ? surfaceProvisoire : a.getArea(), "définitive");
        BigDecimal tva = ou(fraction(d.tauxTva(), "Le taux de TVA"), a.getVat());
        exclusifs(d.prixM2ProvisoireHT(), d.prixM2ProvisoireTTC(), "provisoire");
        exclusifs(d.prixM2DefinitifHT(), d.prixM2DefinitifTTC(), "définitif");
        // Valeur renvoyée identique = pas de saisie : un article occupé ne voit jamais ses prix réécrits (§5.4.4).
        boolean tvaSaisie = change(d.tauxTva(), a.getVat());
        Saisie provisoire = new Saisie(d.prixM2ProvisoireHT(), d.prixM2ProvisoireTTC(),
                creation || change(d.prixM2ProvisoireHT(), a.getComSaleUnitCostFloorHT())
                        || change(d.prixM2ProvisoireTTC(), a.getComSaleUnitCostFloor()),
                tvaSaisie, change(d.surfaceProvisoire(), a.getAreaFloor()));
        Saisie definitive = new Saisie(d.prixM2DefinitifHT(), d.prixM2DefinitifTTC(),
                creation || change(d.prixM2DefinitifHT(), a.getComSaleUnitCostHT())
                        || change(d.prixM2DefinitifTTC(), a.getComSaleUnitCost()),
                tvaSaisie, change(d.surfaceDefinitive(), a.getArea()));
        a.setAreaFloor(surfaceProvisoire);
        a.setArea(surfaceDefinitive);
        a.setVat(tva);
        if (provisoire.recalcule()) {
            PrixLot lot = nouveauxPrix(provisoire(a), surfaceProvisoire, tva, provisoire);
            a.setComSaleUnitCostFloorHT(lot.prixM2HT());
            a.setComSaleUnitCostFloor(lot.prixM2TTC());
            a.setSalePriceFloorHT(lot.prixHT());
            a.setSalePriceFloor(lot.prixTTC());
        }
        if (definitive.recalcule()) {
            PrixLot lot = nouveauxPrix(definitif(a), surfaceDefinitive, tva, definitive);
            a.setComSaleUnitCostHT(lot.prixM2HT());
            a.setComSaleUnitCost(lot.prixM2TTC());
            a.setSalePriceHT(lot.prixHT());
            a.setSalePrice(lot.prixTTC());
        }
        return provisoire.recalcule() || definitive.recalcule();
    }

    /** Ce qui change réellement pour une surface : un prix au m² saisi, la TVA, la surface (valeur renvoyée égale = rien). */
    private record Saisie(BigDecimal ht, BigDecimal ttc, boolean prix, boolean tva, boolean surface) {
        boolean recalcule() {
            return prix || tva || surface;
        }
    }

    /**
     * Prix saisi : calcul complet. Sinon comme les setters OMRAN ({@code Item.cs}) : la TVA garde le HT au m² et refait le
     * TTC ; la surface garde les prix au m² stockés et refait les totaux.
     */
    private static PrixLot nouveauxPrix(PrixLot actuel, BigDecimal surface, BigDecimal tva, Saisie s) {
        if (s.prix()) {
            return prix(surface, s.ht(), s.ttc(), actuel.prixM2HT(), tva);
        }
        PrixLot resultat = actuel;
        if (s.tva()) {
            PrixLot ttc = CalculPrixArticle.depuisHT(surface, actuel.prixM2HT(), ou(tva, BigDecimal.ZERO));
            resultat = new PrixLot(actuel.prixM2HT(), ttc.prixM2TTC(), actuel.prixHT(), ttc.prixTTC());
        }
        if (s.surface()) {
            BigDecimal prixTTC = s.tva() ? resultat.prixTTC() : fois(resultat.prixM2TTC(), surface);
            resultat = new PrixLot(resultat.prixM2HT(), resultat.prixM2TTC(), fois(resultat.prixM2HT(), surface), prixTTC);
        }
        return resultat;
    }

    private static BigDecimal fois(BigDecimal prixM2, BigDecimal surface) {
        return prixM2.multiply(surface).setScale(6, RoundingMode.HALF_UP);
    }

    /** OMRAN : un seul sens de saisie par surface (setters {@code ComSaleUnitCostHT} / {@code ComSaleUnitCost}). */
    private static void exclusifs(BigDecimal saisieHT, BigDecimal saisieTTC, String libelle) {
        if (saisieHT != null && saisieTTC != null) {
            throw new InvalidRequestException("Prix au m² " + libelle + " : saisir le HT ou le TTC, pas les deux.");
        }
        positif(saisieHT, "Prix au m² " + libelle + " HT");
        positif(saisieTTC, "Prix au m² " + libelle + " TTC");
    }

    private static PrixLot prix(BigDecimal surface, BigDecimal saisieHT, BigDecimal saisieTTC, BigDecimal htActuel,
                                BigDecimal tva) {
        BigDecimal taux = ou(tva, BigDecimal.ZERO);
        return saisieTTC != null
                ? CalculPrixArticle.depuisTTC(surface, saisieTTC, taux)
                : CalculPrixArticle.depuisHT(surface, ou(saisieHT, ou(htActuel, BigDecimal.ZERO)), taux);
    }

    /** Messages d'OMRAN {@code Item:AfterVerifMadatories}. */
    private static BigDecimal surface(BigDecimal saisie, BigDecimal actuelle, String libelle) {
        if (saisie != null && saisie.signum() <= 0) {
            throw new InvalidRequestException(
                    "La valeur de la supérficie " + libelle + " doit être strictement supérieure à 0 !");
        }
        return ou(saisie, actuelle);
    }

    private static boolean modifieUnChampProtege(StkItem a, ArticleEcritureDto d) {
        return change(d.code(), a.getCode())
                || change(d.titreFoncier(), a.getTitleNumber())
                || change(d.categorieId(), Champs.de(a.getStkItemCategory(), StkItemCategory::getId))
                || change(d.typeId(), Champs.de(a.getStkItemType(), StkItemType::getId))
                || change(d.vocationId(), Champs.de(a.getStkVocation(), StkVocation::getId))
                || change(d.surfaceProvisoire(), a.getAreaFloor())
                || change(d.surfaceDefinitive(), a.getArea())
                || change(d.prixM2ProvisoireHT(), a.getComSaleUnitCostFloorHT())
                || change(d.prixM2ProvisoireTTC(), a.getComSaleUnitCostFloor())
                || change(d.prixM2DefinitifHT(), a.getComSaleUnitCostHT())
                || change(d.prixM2DefinitifTTC(), a.getComSaleUnitCost())
                || change(d.tauxTva(), a.getVat())
                || change(d.remiseMax1(), a.getDiscountMax())
                || change(d.remiseMax2(), a.getDiscountMax2());
    }

    private static boolean change(Object saisie, Object actuel) {
        if (saisie == null) {
            return false;
        }
        if (saisie instanceof BigDecimal nombre) {
            return !(actuel instanceof BigDecimal valeur) || nombre.compareTo(valeur) != 0;
        }
        if (saisie instanceof String texte) {
            return !texte.trim().equals(Objects.toString(actuel, ""));
        }
        return !saisie.equals(actuel);
    }

    // ------------------------------------------------------------------ règles

    private String codeLibre(String code, UUID trancheId, UUID id) {
        items.findFirstByStkHierarchyIdAndCode(trancheId, code)
                .filter(existant -> !existant.getId().equals(id))
                .ifPresent(existant -> {
                    throw new ConflictException("Le code %s est déjà pris dans la tranche par l'article %s — %s."
                            .formatted(code, existant.getCode(), existant.getDescription()));
                });
        return code;
    }

    private static void exigerPrincipal(StkItem parent) {
        if (Boolean.TRUE.equals(parent.getIsUnderItem()) || parent.getParent() != null) {
            throw new InvalidRequestException("Un sous-article se rattache à un article principal, pas à "
                    + parent.getCode() + ".");
        }
    }

    private void refuserSiOccupe(StkItem article) {
        if (estOccupe(article)) {
            throw new ConflictException("L'article %s est affecté au dossier %s : ses sous-articles ne se modifient plus."
                    .formatted(article.getCode(), dossierOccupant(article)));
        }
    }

    private String dossierOccupant(StkItem article) {
        StkItem porteur = article.getComFolderId() == null && article.getRntFolderId() == null && article.getParent() != null
                ? article.getParent() : article;
        if (porteur.getComFolderId() == null) {
            return "de location " + dossiers.codeLocation(porteur.getRntFolderId())
                    .orElse(porteur.getRntFolderId().toString());
        }
        return dossiers.findById(porteur.getComFolderId()).map(ComFolder::getCode)
                .orElse(porteur.getComFolderId().toString());
    }

    /** Le parent est réécrit avec ses sous-articles, comme OMRAN réenregistre le parent. */
    void reecrireParent(StkItem parent) {
        recalculerPrixDeVente(parent);
        enregistrer(parent, false);
    }

    /** Prix de vente = prix de l'article + ceux de ses sous-articles rattachés (sous-article : les siens seuls). */
    private void recalculerPrixDeVente(StkItem article) {
        List<StkItem> sousArticles = items.findByParentId(article.getId());
        PrixLot definitif = CalculPrixArticle.avecSousArticles(definitif(article),
                sousArticles.stream().map(EcritureArticle::definitif).toList());
        PrixLot provisoire = CalculPrixArticle.avecSousArticles(provisoire(article),
                sousArticles.stream().map(EcritureArticle::provisoire).toList());
        article.setComSalePriceHT(definitif.prixHT());
        article.setComSalePrice(definitif.prixTTC());
        article.setComSalePriceFloorHT(provisoire.prixHT());
        article.setComSalePriceFloor(provisoire.prixTTC());
    }

    private static PrixLot definitif(StkItem i) {
        return new PrixLot(zero(i.getComSaleUnitCostHT()), zero(i.getComSaleUnitCost()), zero(i.getSalePriceHT()),
                zero(i.getSalePrice()));
    }

    private static PrixLot provisoire(StkItem i) {
        return new PrixLot(zero(i.getComSaleUnitCostFloorHT()), zero(i.getComSaleUnitCostFloor()),
                zero(i.getSalePriceFloorHT()), zero(i.getSalePriceFloor()));
    }

    private static BigDecimal zero(BigDecimal valeur) {
        return ou(valeur, BigDecimal.ZERO);
    }

    private void enregistrer(StkItem article, boolean creation) {
        TracePatrimoine.Trace t = trace.maintenant();
        if (creation) {
            article.setUserCreate(t.auteur());
            article.setUserCreateDateTime(t.le());
        }
        article.setUserUpdate(t.auteur());
        article.setUserUpdateDateTime(t.le());
        items.save(article);
    }

    // ------------------------------------------------------------------ ligne neuve

    /** Ligne modèle ART344 : aucune colonne qu'OMRAN lit en non nullable ne reste NULL (§2.4, §5.3). */
    private StkItem nouveau(CfgTranche tranche, StkVocation vocation, boolean sous) {
        StkItem a = new StkItem();
        a.setId(UUID.randomUUID());
        a.setStkHierarchy(tranche);
        a.setStkVocation(vocation);
        a.setStkKind(references.natureBatiment());
        a.setStrEntityId(references.entiteArticle());
        a.setIsUnderItem(sous);
        a.setIsUnderItemOrigin(sous);
        a.setAlwaysAffected(false);
        a.setIsAvailableForSaleInWebSite(false);
        a.setIsAvailableForRentInWebSite(false);
        a.setAlinmentStop(0);
        a.setNeighborStop(0);
        a.setMaxHeight(0);
        a.setCautionNumber(0);
        textesVides(a);
        zeros(a);
        return a;
    }

    private static void textesVides(StkItem a) {
        a.setContenance("");
        a.setElectricityRef("");
        a.setElectricityMeter("");
        a.setElectricityMeterIndex("");
        a.setElectricityMeterPolice("");
        a.setGasMeter("");
        a.setGasMeterIndex("");
        a.setGasMeterPolice("");
        a.setWaterMeterPolice("");
        a.setWaterMeter("");
        a.setWaterMeterIndex("");
        a.setAnomalies("");
        a.setRegistryNumber("");
        a.setRegistryPlace("");
        a.setImmatriculationCPF("");
        a.setWaterRef("");
        a.setGasRef("");
        a.setTitleNumberMother("");
        a.setTitleName("");
        a.setTitleNotes("");
        a.setPropertyCalled("");
        a.setFractionDivided("");
        a.setFractionDividedLand("");
        a.setGroupLandQuota("");
    }

    private static void zeros(StkItem a) {
        BigDecimal z = BigDecimal.ZERO;
        a.setComSaleMinAdvance(z);
        a.setComSalePrice(z);
        a.setComSalePriceHT(z);
        a.setComSalePriceFloor(z);
        a.setComSalePriceFloorHT(z);
        a.setCos(z);
        a.setCuf(z);
        a.setAmountHT(z);
        a.setDividedPart(z);
        a.setUndividedPart(z);
        a.setVat(z);
        a.setAreaCos(z);
        a.setAreaCuf(z);
        a.setDiscountMax(z);
        a.setDiscountMax2(z);
        a.setInteriorArea(z);
        a.setWallsArea(z);
        a.setCommonPartsArea(z);
        a.setMapLatitude(z);
        a.setMapLongitude(z);
    }
}
