package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.VerificationDatesLot;
import com.tsix.immocrm.domain.projet.VerificationDatesLot.DateNommee;
import com.tsix.immocrm.dto.projet.LotDetailDto;
import com.tsix.immocrm.dto.projet.ModifierLotRequest;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.droits.DroitsOmran;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.domain.projet.MontantsLot.fraction;
import static com.tsix.immocrm.services.projet.DecomptesProjetService.jour;

/** Enregistrer la fiche d'un lot (FEATURE-lot-fiche-parite.md E6, R8, R11, R12) : 404 → 403 → 409 → 400, puis écriture. */
@Service
public class ModificationLot {

    private static final String DROIT_ESTIMATION = "Yes.Prj.Front.Market.MarketMonitoring.ManualEstimationAdministrator";
    private static final String HT = "montantForfaitaireHT";
    private static final String TTC = "montantForfaitaireTTC";
    private static final String DESCRIPTION = "description";
    private static final String CATEGORIE = "categorieId";
    private static final String TYPE = "typeId";
    private static final int ECHELLE = 6;

    private record DateLot(String nom, String libelle, Function<ModifierLotRequest, LocalDate> saisie,
                           Function<PrjMarket, LocalDateTime> enBase, BiConsumer<PrjMarket, LocalDateTime> ecrire) {
    }

    private record Reference(String nom, Class<?> classe, Function<ModifierLotRequest, UUID> id) {
    }

    private record Texte(String nom, int max, Function<ModifierLotRequest, String> valeur) {
    }

    /** Ordre OMRAN de {@code MarketMonitoring.Verif}. */
    private static final List<DateLot> CHAINE = List.of(
            new DateLot("dateLancementAppelOffres", "Lancement de l'appel d'offres",
                    ModifierLotRequest::dateLancementAppelOffres, PrjMarket::getLaunchOfTenderDate, PrjMarket::setLaunchOfTenderDate),
            new DateLot("dateRetraitDossiers", "Retrait du dossier", ModifierLotRequest::dateRetraitDossiers,
                    PrjMarket::getWithdrawalTenderDocumentsDate, PrjMarket::setWithdrawalTenderDocumentsDate),
            new DateLot("dateEcheanceRemiseOffres", "Échéance de remise des offres", ModifierLotRequest::dateEcheanceRemiseOffres,
                    PrjMarket::getTenderMaturityDate, PrjMarket::setTenderMaturityDate),
            new DateLot("dateReportAppelOffres", "Report de l'appel d'offres", ModifierLotRequest::dateReportAppelOffres,
                    PrjMarket::getTendersResortDate, PrjMarket::setTendersResortDate),
            new DateLot("dateOuverturePlis", "Ouverture des plis", ModifierLotRequest::dateOuverturePlis,
                    PrjMarket::getBidOpeningDate, PrjMarket::setBidOpeningDate),
            new DateLot("dateRapportDepouillement", "Rapport de dépouillement", ModifierLotRequest::dateRapportDepouillement,
                    PrjMarket::getTendersReportDate, PrjMarket::setTendersReportDate),
            new DateLot("dateAdjudication", "Adjudication", ModifierLotRequest::dateAdjudication,
                    PrjMarket::getTenderDate, PrjMarket::setTenderDate),
            new DateLot("dateSignatureMarche", "Signature du marché", ModifierLotRequest::dateSignatureMarche,
                    PrjMarket::getContractSignatureDate, PrjMarket::setContractSignatureDate),
            new DateLot("dateOrdreService", "Ordre de service", ModifierLotRequest::dateOrdreService,
                    PrjMarket::getServiceOrderDate, PrjMarket::setServiceOrderDate),
            new DateLot("dateLancementTravaux", "Lancement des travaux", ModifierLotRequest::dateLancementTravaux,
                    PrjMarket::getWorkLaunchDate, PrjMarket::setWorkLaunchDate),
            new DateLot("dateAchevementTravaux", "Achèvement des travaux", ModifierLotRequest::dateAchevementTravaux,
                    PrjMarket::getWorkCompletionDate, PrjMarket::setWorkCompletionDate),
            new DateLot("dateRecollement", "Recollement", ModifierLotRequest::dateRecollement,
                    PrjMarket::getReattachmentDate, PrjMarket::setReattachmentDate));

    /** Estimation manuelle : vrai si la valeur envoyée est celle déjà en base (pas un changement, pas de droit exigé). */
    private static final Map<String, BiPredicate<PrjMarket, ModifierLotRequest>> ESTIMATION_INCHANGEE = Map.of(
            HT, (m, r) -> memeMontantAffiche(r.montantForfaitaireHT(), m.getEstimatedPriceHTManual()),
            TTC, (m, r) -> memeMontantAffiche(r.montantForfaitaireTTC(), m.getEstimatedPriceTTCManual()),
            "dateDebutBase", (m, r) -> Objects.equals(r.dateDebutBase(), jour(m.getStartDateManual())),
            "delaiBaseJours", (m, r) -> Objects.equals(r.delaiBaseJours(), m.getDelayManual()),
            "dateFinBase", (m, r) -> Objects.equals(r.dateFinBase(), jour(m.getEndDateManual())));

    private static final List<Reference> REFERENCES = List.of(
            new Reference("fournisseurId", CfgTier.class, ModifierLotRequest::fournisseurId),
            new Reference("deviseId", StlCurrency.class, ModifierLotRequest::deviseId),
            new Reference(CATEGORIE, StlCategory.class, ModifierLotRequest::categorieId),
            new Reference(TYPE, PrjMarketType.class, ModifierLotRequest::typeId),
            new Reference("typePaiementId", StlSettlementType.class, ModifierLotRequest::typePaiementId));

    private static final List<Texte> TEXTES = List.of(
            new Texte(DESCRIPTION, 255, ModifierLotRequest::description),
            new Texte("causeResiliation", 500, ModifierLotRequest::causeResiliation),
            new Texte("descriptionDetaillee", 500, ModifierLotRequest::descriptionDetaillee),
            new Texte("notes", 1000, ModifierLotRequest::notes));

    private final LotsProjetService lots;
    private final DroitsOmran droits;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final JsonMapper json;
    private final Clock clock;
    private final Map<String, BiConsumer<PrjMarket, ModifierLotRequest>> ecritures;

    public ModificationLot(LotsProjetService lots, DroitsOmran droits, UtilisateurAffiche utilisateur, EntityManager em,
                           JsonMapper json, Clock clock) {
        this.lots = lots;
        this.droits = droits;
        this.utilisateur = utilisateur;
        this.em = em;
        this.json = json;
        this.clock = clock;
        this.ecritures = ecritures();
    }

    @Transactional
    public LotDetailDto modifier(UUID id, JsonNode corps) {
        PrjMarket m = lots.visible(id);
        Set<String> presents = presents(corps);
        ModifierLotRequest r = lire(corps);
        Set<String> estimationChangee = ESTIMATION_INCHANGEE.entrySet().stream()
                .filter(e -> presents.contains(e.getKey()) && !e.getValue().test(m, r)).map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
        if (!estimationChangee.isEmpty()) {
            droits.exiger(DROIT_ESTIMATION);
        }
        if (m.getStrStatus() != null && Boolean.TRUE.equals(m.getStrStatus().getLectureSeule())) {
            throw new ConflictException("Le statut du lot est en lecture seule : la fiche ne peut pas être modifiée.");
        }
        controler(m, r, presents);
        BigDecimal tvaAvant = m.getDefaultVat();
        ecritures.forEach((nom, ecrire) -> {
            if (presents.contains(nom)) {
                ecrire.accept(m, r);
            }
        });
        estimation(m, r, estimationChangee, !memeMontant(tvaAvant, m.getDefaultVat()));
        m.setUserUpdate(utilisateur.courant().nom());
        m.setUserUpdateDateTime(LocalDateTime.now(clock));
        em.flush();
        return lots.lot(id);
    }

    private static Set<String> presents(JsonNode corps) {
        if (corps == null || !corps.isObject()) {
            throw new InvalidRequestException("corps : objet JSON attendu");
        }
        return corps.properties().stream().map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
    }

    private ModifierLotRequest lire(JsonNode corps) {
        try {
            return json.readerFor(ModifierLotRequest.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(corps);
        } catch (JacksonException e) {
            throw new InvalidRequestException("corps : valeur illisible (dates au format AAAA-MM-JJ, nombres décimaux)");
        }
    }

    private void controler(PrjMarket m, ModifierLotRequest r, Set<String> presents) {
        presents.stream().filter(n -> !ecritures.containsKey(n) && !HT.equals(n) && !TTC.equals(n)).findFirst()
                .ifPresent(n -> {
                    throw new InvalidRequestException(n + " : champ non modifiable par la fiche");
                });
        if (presents.contains(DESCRIPTION) && !StringUtils.hasText(r.description())) {
            throw new InvalidRequestException("description : obligatoire");
        }
        TEXTES.stream().filter(t -> presents.contains(t.nom())).forEach(t -> {
            String v = t.valeur().apply(r);
            if (v != null && v.trim().length() > t.max()) {
                throw new InvalidRequestException(t.nom() + " : " + t.max() + " caractères au plus");
            }
        });
        REFERENCES.stream().filter(ref -> presents.contains(ref.nom())).forEach(ref -> controlerReference(ref, r));
        List<DateNommee> chaine = CHAINE.stream().map(d -> new DateNommee(d.libelle(),
                presents.contains(d.nom()) ? d.saisie().apply(r) : jour(d.enBase().apply(m)))).toList();
        VerificationDatesLot.premiereErreur(chaine).ifPresent(message -> {
            throw new InvalidRequestException(message);
        });
    }

    private void controlerReference(Reference ref, ModifierLotRequest r) {
        UUID ident = ref.id().apply(r);
        boolean obligatoire = CATEGORIE.equals(ref.nom()) || TYPE.equals(ref.nom());
        if (ident == null ? obligatoire : em.find(ref.classe(), ident) == null) {
            throw new InvalidRequestException(ref.nom() + (ident == null ? " : obligatoire" : " : introuvable"));
        }
    }

    private Map<String, BiConsumer<PrjMarket, ModifierLotRequest>> ecritures() {
        Map<String, BiConsumer<PrjMarket, ModifierLotRequest>> e = new HashMap<>();
        e.put(DESCRIPTION, (m, r) -> m.setDescription(r.description().trim()));
        e.put("fournisseurId", (m, r) -> m.setCfgSupplier(r.fournisseurId() == null ? null
                : em.find(CfgTier.class, r.fournisseurId())));
        e.put("deviseId", (m, r) -> m.setStlCurrencyId(r.deviseId()));
        e.put(CATEGORIE, (m, r) -> m.setStlCategory(em.find(StlCategory.class, r.categorieId())));
        e.put(TYPE,(m, r) -> m.setPrjMarketType(em.find(PrjMarketType.class, r.typeId())));
        e.put("dureeGarantieMois", (m, r) -> m.setWarrantyInMonths(r.dureeGarantieMois()));
        e.put("tauxAvancePct", (m, r) -> m.setAdvanceRatio(fraction(r.tauxAvancePct())));
        e.put("tauxRetenueGarantiePct", (m, r) -> m.setWarrantyRatio(fraction(r.tauxRetenueGarantiePct())));
        e.put("tauxCompteProrataPct", (m, r) -> m.setProportionRatio(fraction(r.tauxCompteProrataPct())));
        e.put("tauxAssuranceDECPct", (m, r) -> m.setInsuranceDECRatio(fraction(r.tauxAssuranceDECPct())));
        e.put("montantAssuranceDEC", (m, r) -> m.setInsuranceDECAmount(r.montantAssuranceDEC()));
        e.put("tauxAssuranceTRCPct", (m, r) -> m.setInsuranceTRCRatio(fraction(r.tauxAssuranceTRCPct())));
        e.put("montantAssuranceTRC", (m, r) -> m.setInsuranceTRCAmount(r.montantAssuranceTRC()));
        e.put("tvaDefautPct", (m, r) -> m.setDefaultVat(fraction(r.tvaDefautPct())));
        e.put("delaiPaiementJours", (m, r) -> m.setPaymentDelay(r.delaiPaiementJours()));
        e.put("dateDebutBase", (m, r) -> m.setStartDateManual(debut(r.dateDebutBase())));
        e.put("delaiBaseJours", (m, r) -> m.setDelayManual(r.delaiBaseJours()));
        e.put("dateFinBase", (m, r) -> m.setEndDateManual(debut(r.dateFinBase())));
        e.put("variationMassePct", (m, r) -> m.setMassChangeRatio(fraction(r.variationMassePct())));
        e.put("tauxRemboursementApproPct", (m, r) -> m.setStockPileRatio(fraction(r.tauxRemboursementApproPct())));
        e.put("totalPoints", (m, r) -> m.setTotalPoints(r.totalPoints()));
        e.put("typePaiementId", (m, r) -> m.setStlSettlementTypeId(r.typePaiementId()));
        e.put("remiseSurTravauxSupp", (m, r) -> m.setApplyDiscountOnAdditionalWork(vrai(r.remiseSurTravauxSupp())));
        e.put("retenueSurTravauxSupp", (m, r) -> m.setApplyWithholdingTaxOnAdditionalWork(vrai(r.retenueSurTravauxSupp())));
        e.put("saisieSurQuantites", (m, r) -> m.setExecutedQuantityBasedInsert(vrai(r.saisieSurQuantites())));
        e.put("avancementParLigne", (m, r) -> m.setCanEditWorkingOutLineByAchivement(vrai(r.avancementParLigne())));
        e.put("restitutionGarantieBloquee", (m, r) -> m.setBlokWarrantyRestitution(vrai(r.restitutionGarantieBloquee())));
        e.put("dateResiliation", (m, r) -> m.setCancellationDate(debut(r.dateResiliation())));
        e.put("causeResiliation", (m, r) -> m.setCancellationCause(r.causeResiliation()));
        e.put("descriptionDetaillee", (m, r) -> m.setDescriptionLong(r.descriptionDetaillee()));
        e.put("notes", (m, r) -> m.setNotes(r.notes()));
        CHAINE.forEach(d -> e.put(d.nom(), (m, r) -> d.ecrire().accept(m, debut(d.saisie().apply(r)))));
        return Map.copyOf(e);
    }

    /**
     * Setters OMRAN ({@code MarketMonitoring.cs}), déclenchés seulement par une valeur qui change : HT → TVA = HT × DefaultVat ;
     * TTC → HT = TTC / (1 + DefaultVat) ; DefaultVat → TVA et TTC recalculés depuis le HT stocké.
     */
    private static void estimation(PrjMarket m, ModifierLotRequest r, Set<String> changes, boolean tvaChangee) {
        BigDecimal taux = Objects.requireNonNullElse(m.getDefaultVat(), BigDecimal.ZERO);
        if (changes.contains(HT)) {
            depuisHT(m, r.montantForfaitaireHT(), taux);
        } else if (changes.contains(TTC)) {
            BigDecimal ttc = r.montantForfaitaireTTC();
            depuisHT(m, ttc == null ? null : ttc.divide(BigDecimal.ONE.add(taux), ECHELLE, RoundingMode.HALF_UP), taux);
            m.setEstimatedPriceTTCManual(ttc);
        } else if (tvaChangee && m.getEstimatedPriceHTManual() != null) {
            depuisHT(m, m.getEstimatedPriceHTManual(), taux);
        }
    }

    private static void depuisHT(PrjMarket m, BigDecimal ht, BigDecimal taux) {
        m.setEstimatedPriceHTManual(ht);
        if (ht == null) {
            m.setEstimatedVatAmountManual(null);
            m.setEstimatedPriceTTCManual(null);
        } else {
            BigDecimal tva = ht.multiply(taux).setScale(ECHELLE, RoundingMode.HALF_UP);
            m.setEstimatedVatAmountManual(tva);
            m.setEstimatedPriceTTCManual(ht.add(tva));
        }
    }

    private static boolean memeMontant(BigDecimal saisi, BigDecimal enBase) {
        if (saisi == null || enBase == null) {
            return saisi == null && enBase == null;
        }
        return saisi.compareTo(enBase) == 0;
    }

    /** La fiche expose le forfait arrondi à 2 décimales : le renvoyer tel quel n'est pas un changement. */
    private static boolean memeMontantAffiche(BigDecimal saisi, BigDecimal enBase) {
        return memeMontant(saisi, enBase) || enBase != null && memeMontant(saisi, MontantsLot.arrondi(enBase));
    }

    private static LocalDateTime debut(LocalDate jour) {
        return jour == null ? null : jour.atStartOfDay();
    }

    private static Boolean vrai(Boolean valeur) {
        return Boolean.TRUE.equals(valeur);
    }
}
