package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculDecompte;
import com.tsix.immocrm.domain.projet.CalculDecompte.Assurance;
import com.tsix.immocrm.domain.projet.CalculDecompte.Ligne;
import com.tsix.immocrm.domain.projet.CalculDecompte.Resultat;
import com.tsix.immocrm.domain.projet.CalculDecompte.Taux;
import com.tsix.immocrm.dto.projet.CreerDecompteRequest;
import com.tsix.immocrm.dto.projet.DecompteDetailDto;
import com.tsix.immocrm.dto.projet.EnregistrerDecompteRequest;
import com.tsix.immocrm.dto.projet.EnregistrerDecompteRequest.LigneSaisie;
import com.tsix.immocrm.dto.projet.LigneDecompteDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLineExtra;
import com.tsix.immocrm.entities.prj.PrjWorkingOutRetainedLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOutStockLine;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.dossier.ReferencesOmran;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.projet.VerrouDecompte.MODIFIER;
import static com.tsix.immocrm.services.projet.VerrouDecompte.PAR_MONTANT;
import static com.tsix.immocrm.services.projet.VerrouDecompte.RECALCULER;
import static com.tsix.immocrm.services.projet.VerrouDecompte.SUPPRIMER;

/**
 * Écritures d'un décompte (FEATURE-decomptes-parite.md §3-§7, réponses Q5-Q11) : 404 (hors périmètre), puis 403 (droit
 * OMRAN), puis 409 (verrouillé), puis 400, tout lu avant la première écriture ; montants par {@link CalculDecompte}.
 */
@Service
@Transactional
public class ActionsDecompteService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DecomptesProjetService lecture;
    private final VerrouDecompte verrou;
    private final DonneesDecompte donnees;
    private final EcritureDecompte ecriture;
    private final ReferencesOmran references;

    public ActionsDecompteService(DecomptesProjetService lecture, VerrouDecompte verrou, DonneesDecompte donnees,
                                  EcritureDecompte ecriture, ReferencesOmran references) {
        this.lecture = lecture;
        this.verrou = verrou;
        this.donnees = donnees;
        this.ecriture = ecriture;
        this.references = references;
    }

    /** {@code W:NewWorkingOut} l.1950-2035 ; ni lignes (lot sans décomposition) ni lignes d'appro (aucun PrjStockLine). */
    public DecompteDetailDto creer(UUID lotId, CreerDecompteRequest demande) {
        PrjMarket lot = lecture.lotVisible(lotId);
        verrou.exiger(MODIFIER);
        PrjWorkingOut dernier = donnees.precedent(lotId, null);
        if (dernier != null && Boolean.TRUE.equals(dernier.getFinalWorkingOut())) {
            throw new ConflictException("Vous ne pouvez pas ajouter un décompte pour un lot contenant un décompte final.");
        }
        LocalDateTime date = demande.dateDocument() == null ? null : demande.dateDocument().atStartOfDay();
        verifierDate(date, lot, dernier);
        PrjWorkingOut w = new PrjWorkingOut();
        w.setId(UUID.randomUUID());
        w.setPrjMarket(lot);
        int numero = dernier == null || dernier.getSorting() == null ? 1 : dernier.getSorting() + 1;
        w.setSorting(numero);
        w.setDocRef(String.valueOf(numero));
        w.setNotes("DECOMPTE N° " + numero);
        w.setDocDate(date);
        w.setCfgTier(lot.getCfgSupplier());
        w.setFinalWorkingOut(false);
        w.setAdvanceBasedOnAmount(false);
        w.setWarrantyBasedOnAmount(false);
        w.setDecBasedOnAmount(false);
        w.setTrcBasedOnAmount(false);
        w.setProrataBasedOnAmount(false);
        reprendreDuLot(w, lot, null);
        reprendreDuPrecedent(w, lot, dernier);
        w.setStlCurrencyId(references.deviseParDefaut());
        w.setCode(ecriture.codeSuivant());
        libeller(w);
        ecriture.creer(w);
        calculerEtEcrire(w);
        return lecture.decompte(w.getId());
    }

    /** Le corps est la fiche relue : seuls les champs saisissables sont lus, null = inchangé. */
    public DecompteDetailDto enregistrer(UUID id, EnregistrerDecompteRequest demande) {
        PrjWorkingOut w = modifiable(id, MODIFIER);
        PrjMarket lot = w.getPrjMarket();
        if (changeParMontant(w, demande)) {
            verrou.exiger(PAR_MONTANT);
        }
        LocalDateTime date = demande.dateDocument() == null ? w.getDocDate() : demande.dateDocument().atStartOfDay();
        verifierDate(date, lot, donnees.precedent(lot.getId(), w.getSorting()));
        CfgTier tiers = demande.fournisseurId() == null ? w.getCfgTier() : donnees.tiers(demande.fournisseurId());
        List<LigneSaisie> saisies = demande.lignes() == null ? List.of() : demande.lignes();
        boolean rechargees = saisies.stream().anyMatch(s -> s.id() == null);
        List<PrjWorkingOutLine> cibles = rechargees ? recharger(w).lignes()
                : donnees.enfants(PrjWorkingOutLine.class, w.getId());
        Map<PrjWorkingOutLine, LigneSaisie> parLigne = associer(cibles, saisies, rechargees);
        w.setDocDate(date);
        w.setCfgTier(tiers);
        libeller(w);
        appliquerSaisie(w, demande);
        parLigne.forEach((l, s) -> {
            boolean changee = fixerQuantite(lot, l, s.quantiteSaisie() == null ? l.getQuantityOrigin() : s.quantiteSaisie(),
                    s.coefficient() == null ? l.getCoefficient() : s.coefficient());
            if (changee && !rechargees) {
                ecriture.modifier(l);
            }
        });
        if (rechargees) {
            remplacerLignes(w, cibles);
        }
        calculerEtEcrire(w);
        return lecture.decompte(id);
    }

    /** {@code W:SetAsLastOne} l.2071-2082 : lignes rechargées, cumul porté à la quantité du bordereau, décompte final. */
    public DecompteDetailDto definirDernier(UUID id) {
        PrjWorkingOut w = modifiable(id, MODIFIER);
        Rechargement r = recharger(w);
        r.lignes().forEach(l -> {
            BigDecimal total = z(r.quantitesMarche().get(l.getPrjMarketLineId()));
            if (z(l.getQuantityOld()).compareTo(total) < 0) {
                fixerQuantite(w.getPrjMarket(), l, total.subtract(z(l.getQuantityOld())), BigDecimal.ONE);
            }
        });
        remplacerLignes(w, r.lignes());
        w.setFinalWorkingOut(true);
        calculerEtEcrire(w);
        return lecture.decompte(id);
    }

    /** {@code W:ReloadCumulLines} l.2063-2069 : seule QuantityOld change (WL:AffectOldValues n'appelle pas le setter). */
    public DecompteDetailDto rechargerCumuls(UUID id) {
        PrjWorkingOut w = modifiable(id, MODIFIER);
        Map<UUID, BigDecimal> anciennes = donnees.quantitesAnterieures(w.getPrjMarket().getId(), w.getSorting());
        donnees.enfants(PrjWorkingOutLine.class, id).forEach(l -> {
            BigDecimal ancienne = anciennes.getOrDefault(l.getPrjMarketLineId(), BigDecimal.ZERO);
            if (l.getPrjMarketLineId() != null && differe(ancienne, l.getQuantityOld())) {
                l.setQuantityOld(ancienne);
                ecriture.modifier(l);
            }
        });
        calculerEtEcrire(w);
        return lecture.decompte(id);
    }

    /** {@code W:ReinitialiseFromMarket} l.2193-2204 puis recalcul ; seulement non verrouillé (Q9). */
    public DecompteDetailDto recalculer(UUID id) {
        PrjWorkingOut w = modifiable(id, RECALCULER, MODIFIER);
        reprendreDuLot(w, w.getPrjMarket(), w.getSorting());
        calculerEtEcrire(w);
        return lecture.decompte(id);
    }

    /** Enfants NO_ACTION (retenues, appro) supprimés avant le décompte (Q5). */
    public void supprimer(UUID id) {
        PrjWorkingOut w = modifiable(id, SUPPRIMER);
        ecriture.supprimer(donnees.enfants(PrjWorkingOutRetainedLine.class, id));
        ecriture.supprimer(donnees.enfants(PrjWorkingOutStockLine.class, id));
        ecriture.supprimer(donnees.enfants(PrjWorkingOutLine.class, id));
        ecriture.supprimer(donnees.enfants(PrjWorkingOutLineExtra.class, id));
        ecriture.supprimer(List.of(w));
    }

    /** Aperçu de {@code W:ReloadLines} : rien n'est écrit (Q10). */
    @Transactional(readOnly = true)
    public List<LigneDecompteDto> lignesRechargees(UUID id) {
        Rechargement r = recharger(modifiable(id, MODIFIER));
        return r.lignes().stream()
                .map(l -> DecomptesProjetService.ligneDecompte(l, r.quantitesMarche().get(l.getPrjMarketLineId())))
                .toList();
    }

    /** Lignes de {@code W:ReloadLines} (non persistées) et quantité du bordereau par ligne de bordereau. */
    private record Rechargement(List<PrjWorkingOutLine> lignes, Map<UUID, BigDecimal> quantitesMarche) {
    }

    private PrjWorkingOut modifiable(UUID id, String... droits) {
        PrjWorkingOut w = lecture.visible(id);
        verrou.exiger(droits);
        String motif = verrou.motif(w);
        if (motif != null) {
            throw new ConflictException("Décompte en lecture seule : " + motif + ".");
        }
        return w;
    }

    /** {@code W:Description} l.266-276 et {@code W:SupplierDescription} l.1821-1835 (raison sociale du tiers). */
    private static void libeller(PrjWorkingOut w) {
        w.setDescription("Décompte N° " + w.getCode() + " du " + JOUR.format(w.getDocDate()));
        if (w.getCfgTier() != null) {
            w.setSupplierDescription(TierNomFormatter.nom(w.getCfgTier()));
        }
    }

    /** {@code W:Verif} l.2084-2105. */
    private static void verifierDate(LocalDateTime date, PrjMarket lot, PrjWorkingOut precedent) {
        if (date == null) {
            throw new InvalidRequestException("La date du décompte est obligatoire.");
        }
        if (precedent != null && precedent.getDocDate() != null && date.isBefore(precedent.getDocDate())) {
            throw new InvalidRequestException("La date de ce décompte doit être supérieure à la date du dernier décompte : "
                    + JOUR.format(precedent.getDocDate()) + ".");
        }
        if (lot.getServiceOrderDate() != null && date.isBefore(lot.getServiceOrderDate())) {
            throw new InvalidRequestException("La date du décompte doit être supérieure ou égale à la date d'ordre de service"
                    + " du lot : " + JOUR.format(lot.getServiceOrderDate()) + ".");
        }
    }

    /** Ratios et plafonds du lot ; restes d'assurance sur les décomptes de numéro inférieur à {@code avant} (null = tous). */
    private void reprendreDuLot(PrjWorkingOut w, PrjMarket lot, Integer avant) {
        w.setAdvanceRatio(lot.getAdvanceRatio());
        w.setWarrantyRatio(lot.getWarrantyRatio());
        w.setProportionRatio(lot.getProportionRatio());
        w.setInsuranceDECRatio(lot.getInsuranceDECRatio());
        w.setInsuranceTRCRatio(lot.getInsuranceTRCRatio());
        w.setInsuranceDECAmountMax(lot.getInsuranceDECAmount());
        w.setInsuranceTRCAmountMax(lot.getInsuranceTRCAmount());
        w.setInsuranceDECRest(reste(lot.getInsuranceDECAmount(), donnees.somme("insuranceDECAmount", lot.getId(), w.getId(), avant)));
        w.setInsuranceTRCRest(reste(lot.getInsuranceTRCAmount(), donnees.somme("insuranceTRCAmount", lot.getId(), w.getId(), avant)));
    }

    /** {@code W:1961-1963}, {@code 2010-2014} et {@code Utils.cs:GetAdvanceRestByMarket / GetWorkingOutAdvanceRestByMarket}. */
    private void reprendreDuPrecedent(PrjWorkingOut w, PrjMarket lot, PrjWorkingOut dernier) {
        w.setAdvanceRest(resteAvance(lot.getId(), w.getId(), DonneesDecompte.AVANCE_MARCHE, "advanceAmount"));
        BigDecimal resteAvanceDecompte = resteAvance(lot.getId(), w.getId(), DonneesDecompte.AVANCE_DECOMPTE,
                "workingOutAdvanceAmount");
        w.setWorkingOutAdvanceAmountRestAtCreation(resteAvanceDecompte);
        w.setWorkingOutAdvanceAmount(resteAvanceDecompte);
        w.setStockPileRatio(dernier != null && dernier.getStockPileRatio() != null ? dernier.getStockPileRatio()
                : lot.getStockPileRatio());
        if (dernier != null) {
            w.setStockAmountOld(dernier.getStockAmount());
            w.setStockAmountOldHT(dernier.getStockAmountHT());
            w.setFluctuationsOld(dernier.getFluctuations());
            w.setFluctuations(dernier.getFluctuations());
            w.setFluctuationsOldHT(dernier.getFluctuationsHT());
            w.setFluctuationsHT(dernier.getFluctuationsHT());
        }
    }

    private BigDecimal resteAvance(UUID lot, UUID id, String typePiece, String champ) {
        BigDecimal pieces = donnees.pieces(lot, typePiece, id);
        return pieces == null ? BigDecimal.ZERO : pieces.subtract(donnees.somme(champ, lot, id, null));
    }

    private static BigDecimal reste(BigDecimal montantLot, BigDecimal deja) {
        return z(montantLot).subtract(deja).max(BigDecimal.ZERO);
    }

    /** Un indicateur « basé sur le montant » change, ou un montant saisi change sous cet indicateur : droit A_0152_22. */
    private static boolean changeParMontant(PrjWorkingOut w, EnregistrerDecompteRequest d) {
        return change(d.avanceSurMontant(), w.getAdvanceBasedOnAmount(), d.avanceAmortie(), w.getAdvanceAmount())
                || change(d.retenueGarantieSurMontant(), w.getWarrantyBasedOnAmount(), d.retenueGarantie(), w.getWarrantyAmount())
                || change(d.assuranceDECSurMontant(), w.getDecBasedOnAmount(), d.assuranceDEC(), w.getInsuranceDECAmount())
                || change(d.assuranceTRCSurMontant(), w.getTrcBasedOnAmount(), d.assuranceTRC(), w.getInsuranceTRCAmount())
                || change(d.compteProrataSurMontant(), w.getProrataBasedOnAmount(), d.compteProrata(), w.getProportionAmount());
    }

    private static boolean change(Boolean indicateur, Boolean stocke, BigDecimal montant, BigDecimal montantStocke) {
        boolean avant = Boolean.TRUE.equals(stocke);
        boolean apres = indicateur == null ? avant : indicateur;
        return apres != avant || apres && differe(montant, montantStocke);
    }

    private void appliquerSaisie(PrjWorkingOut w, EnregistrerDecompteRequest d) {
        si(d.referenceInterne(), w::setDocRef);
        si(d.notes(), w::setNotes);
        si(d.avanceSurMontant(), w::setAdvanceBasedOnAmount);
        si(d.retenueGarantieSurMontant(), w::setWarrantyBasedOnAmount);
        si(d.assuranceDECSurMontant(), w::setDecBasedOnAmount);
        si(d.assuranceTRCSurMontant(), w::setTrcBasedOnAmount);
        si(d.compteProrataSurMontant(), w::setProrataBasedOnAmount);
        si(ratio(d.tauxAvance()), w::setAdvanceRatio);
        si(ratio(d.tauxRetenueGarantie()), w::setWarrantyRatio);
        si(ratio(d.tauxAssuranceDEC()), w::setInsuranceDECRatio);
        si(ratio(d.tauxAssuranceTRC()), w::setInsuranceTRCRatio);
        si(ratio(d.tauxCompteProrata()), w::setProportionRatio);
        si(ratio(d.tauxApprovisionnement()), w::setStockPileRatio);
        si(d.avanceAmortie(), w::setAdvanceAmount);
        si(d.retenueGarantie(), w::setWarrantyAmount);
        si(d.assuranceDEC(), w::setInsuranceDECAmount);
        si(d.assuranceTRC(), w::setInsuranceTRCAmount);
        si(d.compteProrata(), w::setProportionAmount);
        si(d.fluctuations(), w::setFluctuations);
        si(d.regularisation(), w::setAmountRegul);
        si(d.dernier(), w::setFinalWorkingOut);
        // Setters OMRAN : plafond appliqué seulement quand la valeur change (W:856-877, W:1100-1147).
        if (differe(d.avanceSurDecompte(), w.getWorkingOutAdvanceAmount())) {
            w.setWorkingOutAdvanceAmount(d.avanceSurDecompte().min(z(w.getWorkingOutAdvanceAmountRestAtCreation())));
        }
        if (differe(d.remboursementCompteProrata(), w.getProrataRefundAmount())) {
            w.setProrataRefundAmount(d.remboursementCompteProrata().min(donnees.resteProrata(w)));
        }
    }

    /** Ligne visée par chaque saisie (par id, ou par ligne de bordereau pour un aperçu rechargé) ; inconnue → 400. */
    private static Map<PrjWorkingOutLine, LigneSaisie> associer(List<PrjWorkingOutLine> cibles, List<LigneSaisie> saisies,
                                                                boolean rechargees) {
        Function<PrjWorkingOutLine, UUID> cle = rechargees ? PrjWorkingOutLine::getPrjMarketLineId : PrjWorkingOutLine::getId;
        Map<UUID, PrjWorkingOutLine> parCle = cibles.stream().collect(Collectors.toMap(cle, Function.identity(), (a, b) -> a));
        Map<PrjWorkingOutLine, LigneSaisie> parLigne = new IdentityHashMap<>();
        for (LigneSaisie s : saisies) {
            UUID cleSaisie = rechargees ? s.ligneMarcheId() : s.id();
            PrjWorkingOutLine l = cleSaisie == null ? null : parCle.get(cleSaisie);
            if (l == null) {
                throw new InvalidRequestException("Ligne de décompte inconnue : " + cleSaisie + ".");
            }
            parLigne.put(l, s);
        }
        return parLigne;
    }

    private Rechargement recharger(PrjWorkingOut w) {
        List<PrjMarketLine> bordereau = donnees.bordereau(w.getPrjMarket().getId());
        return new Rechargement(donnees.lignesRechargees(w, bordereau),
                bordereau.stream().collect(Collectors.toMap(PrjMarketLine::getId, m -> z(m.getQuantity()), (a, b) -> a)));
    }

    /** {@code W:ReloadLines} écrit : les lignes du décompte sont remplacées. */
    private void remplacerLignes(PrjWorkingOut w, List<PrjWorkingOutLine> nouvelles) {
        ecriture.supprimer(donnees.enfants(PrjWorkingOutLine.class, w.getId()));
        nouvelles.forEach(l -> {
            l.setId(UUID.randomUUID());
            ecriture.creer(l);
        });
    }

    /** {@code WL} l.141-185 : quantité de la période depuis la quantité saisie (cumul ou exécutée selon le lot). */
    private static boolean fixerQuantite(PrjMarket lot, PrjWorkingOutLine l, BigDecimal saisie, BigDecimal coefficient) {
        BigDecimal coef = coefficient == null ? BigDecimal.ONE : coefficient;
        BigDecimal ancienne = z(l.getQuantityOld());
        BigDecimal quantite = Boolean.TRUE.equals(lot.getExecutedQuantityBasedInsert()) ? z(saisie).multiply(coef)
                : z(saisie).add(ancienne).multiply(coef).subtract(ancienne);
        boolean changee = differe(saisie, l.getQuantityOrigin()) || differe(coef, l.getCoefficient())
                || differe(quantite, l.getQuantity());
        l.setQuantityOrigin(saisie);
        l.setCoefficient(coef);
        l.setQuantity(quantite);
        return changee;
    }

    private void calculerEtEcrire(PrjWorkingOut w) {
        UUID id = w.getId();
        Resultat r = CalculDecompte.calculer(new CalculDecompte.Entree(
                donnees.enfants(PrjWorkingOutLine.class, id).stream()
                        .map(l -> new Ligne(l.getQuantity(), l.getUnitPriceHT(), l.getFodecRatio(), l.getVatRatio())).toList(),
                donnees.enfants(PrjWorkingOutLineExtra.class, id).stream()
                        .map(e -> new Ligne(e.getQuantity(), e.getUnitPriceHT(), e.getFodecRatio(), e.getVatRatio())).toList(),
                donnees.enfants(PrjWorkingOutStockLine.class, id).stream()
                        .map(s -> new Ligne(s.getQuantity(), s.getUnitPriceHT(), s.getFodecRatio(), s.getVatRatio())).toList(),
                donnees.enfants(PrjWorkingOutRetainedLine.class, id).stream().map(PrjWorkingOutRetainedLine::getAmount)
                        .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add),
                donnees.precedents(w), donnees.pieces(w.getPrjMarket().getId(), DonneesDecompte.AVANCE_MARCHE, id),
                new Taux(Boolean.TRUE.equals(w.getAdvanceBasedOnAmount()), w.getAdvanceRatio(), w.getAdvanceAmount()),
                new Taux(Boolean.TRUE.equals(w.getWarrantyBasedOnAmount()), w.getWarrantyRatio(), w.getWarrantyAmount()),
                new Taux(Boolean.TRUE.equals(w.getProrataBasedOnAmount()), w.getProportionRatio(), w.getProportionAmount()),
                new Assurance(new Taux(Boolean.TRUE.equals(w.getDecBasedOnAmount()), w.getInsuranceDECRatio(),
                        w.getInsuranceDECAmount()), w.getInsuranceDECAmountMax(), w.getInsuranceDECRest()),
                new Assurance(new Taux(Boolean.TRUE.equals(w.getTrcBasedOnAmount()), w.getInsuranceTRCRatio(),
                        w.getInsuranceTRCAmount()), w.getInsuranceTRCAmountMax(), w.getInsuranceTRCRest()),
                w.getWorkingOutAdvanceAmount(), w.getProrataRefundAmount(), w.getStockPileRatio(), w.getStockAmountOld(),
                w.getStockAmountOldHT(), w.getFluctuations(), w.getFluctuationsHT(), w.getFluctuationsOld(),
                w.getFluctuationsOldHT()));
        ecrire(w, r);
        ecriture.modifier(w);
    }

    private static void ecrire(PrjWorkingOut w, Resultat r) {
        w.setAmountHT(r.ht());
        w.setAmountVat(r.tva());
        w.setAmountTTC(r.ttc());
        w.setExtraAmount(r.extras());
        w.setStockAmount(r.approvisionnement());
        w.setStockAmountHT(r.approvisionnementHT());
        w.setStockAmountToPay(r.approvisionnementAPayer());
        w.setStockAmountToPayHT(r.approvisionnementAPayerHT());
        w.setFluctuationsToPay(r.fluctuationsAPayer());
        w.setFluctuationsToPayHT(r.fluctuationsAPayerHT());
        w.setAdvanceAmount(r.avance().montant());
        w.setAdvanceRatio(r.avance().taux());
        w.setWarrantyAmount(r.garantie().montant());
        w.setWarrantyRatio(r.garantie().taux());
        w.setProportionAmount(r.prorata().montant());
        w.setProportionRatio(r.prorata().taux());
        w.setInsuranceDECAmount(r.dec().montant());
        w.setInsuranceDECRatio(r.dec().taux());
        w.setInsuranceTRCAmount(r.trc().montant());
        w.setInsuranceTRCRatio(r.trc().taux());
        w.setNonRefundableRetainer(r.nonRemboursables());
        w.setRefundableRetainer(r.remboursables());
        w.setAmountToBePaid(r.montantBrut());
        w.setWorkingOutAmountTTC(r.travauxDuMois());
        w.setAmountTTCAfterDeductions(r.apresDeductions());
        w.setAmountToPay(r.netAPayer());
    }

    private static BigDecimal ratio(BigDecimal pourcentage) {
        return pourcentage == null ? null : pourcentage.movePointLeft(2);
    }

    private static boolean differe(BigDecimal saisi, BigDecimal stocke) {
        return saisi != null && (stocke == null || saisi.compareTo(stocke) != 0);
    }

    private static <T> void si(T valeur, Consumer<T> setter) {
        if (valeur != null) {
            setter.accept(valeur);
        }
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
