package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.dto.patrimoine.FicheArticleDto;
import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkCostCategory;
import com.tsix.immocrm.entities.stk.StkFacade;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemContenance;
import com.tsix.immocrm.entities.stk.StkItemVue;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkOrientation;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkPresentationTypeContenance;
import com.tsix.immocrm.entities.stk.StkVue;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;
import static com.tsix.immocrm.services.patrimoine.SuperficiesArticle.zero;

/**
 * Champs de la fiche article (FEATURE-patrimoine-article-fiche.md §1, §2, §9) : références, montants, foncier,
 * compteurs, vues et recopie des contenances du type de présentation, dans la transaction de {@link EcritureArticle}.
 */
@Component
class FicheArticle {

    /** Champs du socle que {@code champsAVider} refuse (texte : envoyer {@code ""}, vues : {@code []}). */
    private static final Set<String> SOCLE = Set.of("trancheId", "code", "description", "vocationId", "categorieId",
            "typeId", "immeubleId", "etageId", "surfaceProvisoire", "surfaceDefinitive", "prixM2ProvisoireHT",
            "prixM2ProvisoireTTC", "prixM2DefinitifHT", "prixM2DefinitifTTC", "tauxTva", "titreFoncier", "estTitre",
            "adresse", "notes", "toujoursAffecte", "vueIds");
    private static final int COMPTEUR = 50;
    private static final int LONG = 1000;
    private static final String NATURE_TERRAIN = "TER";
    private static final String PRESENTATION = "presentationId";
    private static final String ARTICLE = "id";
    private static final String VUES = "SELECT v FROM StkItemVue v WHERE v.stkItem.id = :id";
    private static final String CONTENANCES = "SELECT c FROM StkItemContenance c WHERE c.stkItem.id = :id";

    private final EntityManager em;
    private final ReferencesFicheArticle references;
    private final TracePatrimoine trace;

    FicheArticle(EntityManager em, ReferencesFicheArticle references, TracePatrimoine trace) {
        this.em = em;
        this.references = references;
        this.trace = trace;
    }

    // ------------------------------------------------------------------ écriture

    static ChampsAVider vidages(ArticleEcritureDto d) {
        return ChampsAVider.de(d, d.champsAVider(), SOCLE);
    }

    /** Colonnes de la fiche ; rend vrai si le type de présentation change (contenances à recopier par {@link #lignes}). */
    boolean appliquer(StkItem a, ArticleEcritureDto d, ChampsAVider v) {
        boolean presentationChangee = presentation(a, d, v);
        a.setStkOrientation(v.reference("orientationId", d.orientationId(), a.getStkOrientation(),
                id -> references.reference(StkOrientation.class, id, "orientationId")));
        a.setStkCostCategory(v.reference("categorieChargeId", d.categorieChargeId(), a.getStkCostCategory(),
                id -> references.reference(StkCostCategory.class, id, "categorieChargeId")));
        a.setStkFacade(v.reference("facadeId", d.facadeId(), a.getStkFacade(),
                id -> references.reference(StkFacade.class, id, "facadeId")));
        a.setCfgCompanySyndic(v.reference("entiteSyndicId", d.entiteSyndicId(), a.getCfgCompanySyndic(),
                id -> references.reference(CfgCompany.class, id, "entiteSyndicId")));
        montantsEtTextes(a, d, v);
        foncier(a, d, v);
        compteurs(a, d, v);
        return presentationChangee;
    }

    /** Après l'enregistrement de l'article : vues (ensemble remplacé) et contenances du nouveau type de présentation. */
    void lignes(StkItem a, ArticleEcritureDto d, boolean presentationChangee) {
        // À la création, save() fusionne une copie : les lignes visent l'instance gérée, pas l'article saisi.
        StkItem gere = em.getReference(StkItem.class, a.getId());
        if (d.vueIds() != null) {
            remplacerVues(gere, d.vueIds());
        }
        if (presentationChangee && a.getStkPresentationType() != null) {
            recopierContenances(gere, a.getStkPresentationType());
        }
    }

    private boolean presentation(StkItem a, ArticleEcritureDto d, ChampsAVider v) {
        UUID actuelle = Champs.de(a.getStkPresentationType(), StkPresentationType::getId);
        StkPresentationType nouvelle = v.reference(PRESENTATION, d.presentationId(), a.getStkPresentationType(),
                id -> presentationDeLaTranche(a, id));
        a.setStkPresentationType(nouvelle);
        return !Objects.equals(actuelle, Champs.de(nouvelle, StkPresentationType::getId));
    }

    private StkPresentationType presentationDeLaTranche(StkItem a, UUID id) {
        StkPresentationType type = references.reference(StkPresentationType.class, id, PRESENTATION);
        UUID tranche = Champs.de(a.getStkHierarchy(), CfgTranche::getId);
        if (tranche == null || !tranche.equals(Champs.de(type.getStkHierarchy(), CfgTranche::getId))) {
            throw new InvalidRequestException(PRESENTATION + " : le type de présentation n'est pas de la tranche de l'article.");
        }
        return type;
    }

    private static void montantsEtTextes(StkItem a, ArticleEcritureDto d, ChampsAVider v) {
        a.setComSaleMinAdvance(v.ou("avanceMinimum", positif(d.avanceMinimum(), "Avance minimum"), a.getComSaleMinAdvance()));
        a.setAmountHT(v.ou("loyerMensuelHT", positif(d.loyerMensuelHT(), "Loyer mensuel HT"), a.getAmountHT()));
        a.setCautionNumber(v.ou("nombreCautions", positif(d.nombreCautions(), "Nombre de cautions"), a.getCautionNumber()));
        a.setAnomalies(texte(v.texte("anomalies", d.anomalies()), a.getAnomalies(), LONG, "Les anomalies"));
        a.setContenance(texte(v.texte("contenanceLibre", d.contenanceLibre()), a.getContenance(), LONG, "La contenance"));
        a.setIsAvailableForSaleInWebSite(v.ou("disponibleVenteWeb", d.disponibleVenteWeb(),
                a.getIsAvailableForSaleInWebSite()));
        a.setIsAvailableForRentInWebSite(v.ou("disponibleLocationWeb", d.disponibleLocationWeb(),
                a.getIsAvailableForRentInWebSite()));
    }

    private static void foncier(StkItem a, ArticleEcritureDto d, ChampsAVider v) {
        a.setRegistryDate(v.ou("dateEnregistrement", d.dateEnregistrement(), a.getRegistryDate()));
        a.setImmatriculationDate(v.ou("dateImmatriculation", d.dateImmatriculation(), a.getImmatriculationDate()));
        a.setTitleDate(v.ou("dateTitreFoncier", d.dateTitreFoncier(), a.getTitleDate()));
        a.setRegistryNumber(texte(v.texte("numeroEnregistrement", d.numeroEnregistrement()), a.getRegistryNumber(),
                COURT, "Le numéro d'enregistrement"));
        a.setRegistryPlace(texte(v.texte("lieuEnregistrement", d.lieuEnregistrement()), a.getRegistryPlace(), COURT,
                "Le lieu d'enregistrement"));
        a.setImmatriculationCPF(texte(v.texte("cpfImmatriculation", d.cpfImmatriculation()), a.getImmatriculationCPF(),
                COURT, "L'immatriculation CPF"));
        a.setTitleNumberMother(texte(v.texte("titreFoncierMere", d.titreFoncierMere()), a.getTitleNumberMother(), COURT,
                "Le titre foncier mère"));
        a.setTitleName(texte(v.texte("intituleTitreFoncier", d.intituleTitreFoncier()), a.getTitleName(), COURT,
                "L'intitulé du titre foncier"));
        a.setPropertyCalled(texte(v.texte("proprieteDite", d.proprieteDite()), a.getPropertyCalled(), COURT,
                "La propriété dite"));
        a.setFractionDivided(texte(v.texte("fractionDivise", d.fractionDivise()), a.getFractionDivided(), COURT,
                "La fraction divise"));
        a.setFractionDividedLand(texte(v.texte("fractionDiviseTerrain", d.fractionDiviseTerrain()),
                a.getFractionDividedLand(), COURT, "La fraction divise du terrain"));
        a.setDividedPart(v.ou("partiesDivises", positif(d.partiesDivises(), "Parties divises"), a.getDividedPart()));
        a.setUndividedPart(v.ou("partiesIndivises", positif(d.partiesIndivises(), "Parties indivises"),
                a.getUndividedPart()));
        a.setGroupLandQuota(texte(v.texte("quotaTerrainGroupe", d.quotaTerrainGroupe()), a.getGroupLandQuota(), COURT,
                "La quote-part du terrain"));
        a.setTitleNotes(texte(v.texte("notesTitreFoncier", d.notesTitreFoncier()), a.getTitleNotes(), COURT,
                "Les notes du titre foncier"));
    }

    private static void compteurs(StkItem a, ArticleEcritureDto d, ChampsAVider v) {
        a.setElectricityRef(compteur(v, "referenceElectricite", d.referenceElectricite(), a.getElectricityRef()));
        a.setElectricityMeterPolice(compteur(v, "policeElectricite", d.policeElectricite(), a.getElectricityMeterPolice()));
        a.setElectricityMeter(compteur(v, "compteurElectricite", d.compteurElectricite(), a.getElectricityMeter()));
        a.setElectricityMeterIndex(compteur(v, "releveElectricite", d.releveElectricite(), a.getElectricityMeterIndex()));
        a.setWaterRef(compteur(v, "referenceEau", d.referenceEau(), a.getWaterRef()));
        a.setWaterMeterPolice(compteur(v, "policeEau", d.policeEau(), a.getWaterMeterPolice()));
        a.setWaterMeter(compteur(v, "compteurEau", d.compteurEau(), a.getWaterMeter()));
        a.setWaterMeterIndex(compteur(v, "releveEau", d.releveEau(), a.getWaterMeterIndex()));
        a.setGasRef(compteur(v, "referenceGaz", d.referenceGaz(), a.getGasRef()));
        a.setGasMeterPolice(compteur(v, "policeGaz", d.policeGaz(), a.getGasMeterPolice()));
        a.setGasMeter(compteur(v, "compteurGaz", d.compteurGaz(), a.getGasMeter()));
        a.setGasMeterIndex(compteur(v, "releveGaz", d.releveGaz(), a.getGasMeterIndex()));
    }

    private static String compteur(ChampsAVider v, String champ, String saisie, String actuel) {
        return texte(v.texte(champ, saisie), actuel, COMPTEUR, champ);
    }

    /** OMRAN ajoute des doublons à chaque enregistrement (BuildItem.cs:705-715) : ici l'ensemble est remplacé. */
    private void remplacerVues(StkItem a, List<UUID> vueIds) {
        Set<UUID> aCreer = new LinkedHashSet<>(vueIds);
        aCreer.forEach(id -> references.reference(StkVue.class, id, "vueIds"));
        // Ligne gardée : vue voulue vue pour la première fois ; doublon ou vue retirée : supprimée.
        em.createQuery(VUES, StkItemVue.class).setParameter(ARTICLE, a.getId()).getResultList().forEach(ligne -> {
            if (!aCreer.remove(Champs.de(ligne.getStkVue(), StkVue::getId))) {
                em.remove(ligne);
            }
        });
        aCreer.forEach(id -> {
            StkItemVue ligne = nouvelle(new StkItemVue());
            ligne.setStkItem(a);
            ligne.setStkVue(em.getReference(StkVue.class, id));
            em.persist(ligne);
        });
    }

    /** {@code Item.SetContenance} (Item.cs:430-466) : les contenances de l'article deviennent celles du type. */
    private void recopierContenances(StkItem a, StkPresentationType type) {
        em.createQuery(CONTENANCES, StkItemContenance.class).setParameter(ARTICLE, a.getId()).getResultList()
                .forEach(em::remove);
        em.createQuery("SELECT m FROM StkPresentationTypeContenance m WHERE m.stkPresentationType.id = :id",
                        StkPresentationTypeContenance.class)
                .setParameter(ARTICLE, type.getId()).getResultList().forEach(m -> {
                    StkItemContenance ligne = nouvelle(new StkItemContenance());
                    ligne.setStkItem(a);
                    ligne.setStkContenance(m.getStkContenance());
                    ligne.setItemsNumber(m.getItemsNumber());
                    ligne.setOption1(m.getOption1());
                    ligne.setIsWithOption1(m.getIsWithOption1());
                    ligne.setQuantityOption1(m.getQuantityOption1());
                    ligne.setOption2(m.getOption2());
                    ligne.setIsWithOption2(m.getIsWithOption2());
                    ligne.setQuantityOption2(m.getQuantityOption2());
                    ligne.setOption3(m.getOption3());
                    ligne.setIsWithOption3(m.getIsWithOption3());
                    ligne.setQuantityOption3(m.getQuantityOption3());
                    em.persist(ligne);
                });
    }

    /** Ligne neuve avec sa trace OMRAN (à persister par l'appelant). */
    <L extends LigneOmran> L nouvelle(L ligne) {
        TracePatrimoine.Trace t = trace.maintenant();
        ligne.setId(UUID.randomUUID());
        ligne.setUserCreate(t.auteur());
        ligne.setUserCreateDateTime(t.le());
        ligne.setUserUpdate(t.auteur());
        ligne.setUserUpdateDateTime(t.le());
        return ligne;
    }

    // ------------------------------------------------------------------ lecture

    /** {@code sousArticles} : rattachés. Superficies calculées comme les colonnes AreaNet / TitledArea de StkItemView (§9.1). */
    FicheArticleDto lire(StkItem a, List<StkItem> sousArticles) {
        List<UUID> vues = em.createQuery(VUES, StkItemVue.class).setParameter(ARTICLE, a.getId()).getResultList().stream()
                .map(l -> Champs.de(l.getStkVue(), StkVue::getId)).filter(Objects::nonNull).distinct().toList();
        return new FicheArticleDto(
                Champs.de(a.getStkOrientation(), StkOrientation::getId),
                Champs.de(a.getStkCostCategory(), StkCostCategory::getId),
                Champs.de(a.getStkFacade(), StkFacade::getId),
                Champs.de(a.getCfgCompanySyndic(), CfgCompany::getId),
                vues,
                a.getComSaleMinAdvance(), a.getAmountHT(), a.getCautionNumber(), a.getAnomalies(), a.getContenance(),
                a.getIsAvailableForSaleInWebSite(), a.getIsAvailableForRentInWebSite(),
                a.getRegistryDate(), a.getRegistryNumber(), a.getRegistryPlace(), a.getImmatriculationCPF(),
                a.getImmatriculationDate(), a.getTitleNumberMother(), a.getTitleName(), a.getTitleDate(),
                a.getPropertyCalled(), a.getFractionDivided(), a.getFractionDividedLand(), a.getDividedPart(),
                a.getUndividedPart(), a.getGroupLandQuota(), a.getTitleNotes(),
                a.getElectricityRef(), a.getElectricityMeterPolice(), a.getElectricityMeter(), a.getElectricityMeterIndex(),
                a.getWaterRef(), a.getWaterMeterPolice(), a.getWaterMeter(), a.getWaterMeterIndex(),
                a.getGasRef(), a.getGasMeterPolice(), a.getGasMeter(), a.getGasMeterIndex(),
                superficieNette(a, sousArticles), superficieTitree(a, sousArticles));
    }

    private static BigDecimal superficieNette(StkItem a, List<StkItem> sousArticles) {
        if (NATURE_TERRAIN.equals(Champs.de(a.getStkKind(), StkKind::getCode))) {
            return zero(a.getArea()).subtract(zero(a.getEquipementsArea()));
        }
        return zero(a.getArea()).add(somme(sousArticles));
    }

    private static BigDecimal superficieTitree(StkItem a, List<StkItem> sousArticles) {
        return SuperficiesArticle.titree(a,
                somme(sousArticles.stream().filter(s -> Boolean.TRUE.equals(s.getIsTitled())).toList()));
    }

    private static BigDecimal somme(List<StkItem> items) {
        return items.stream().map(i -> zero(i.getArea())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
