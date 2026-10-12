package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.chantier.LigneMontant;
import com.tsix.immocrm.domain.chantier.Montants;
import com.tsix.immocrm.domain.chantier.SoldeFacture;
import com.tsix.immocrm.dto.chantier.DecompteDetailDto;
import com.tsix.immocrm.dto.chantier.DecompteListItemDto;
import com.tsix.immocrm.dto.chantier.ExtraDecompteDto;
import com.tsix.immocrm.dto.chantier.FactureDecompteDto;
import com.tsix.immocrm.dto.chantier.LigneDecompteDto;
import com.tsix.immocrm.dto.chantier.ReglementFactureDto;
import com.tsix.immocrm.dto.chantier.RetenueDecompteDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLineExtra;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.prj.PrjWorkingOutLineExtraRepository;
import com.tsix.immocrm.repositories.prj.PrjWorkingOutLineRepository;
import com.tsix.immocrm.repositories.prj.PrjWorkingOutRepository;
import com.tsix.immocrm.repositories.prj.PrjWorkingOutRetainedLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.chantier.ChantierMapper.jour;
import static com.tsix.immocrm.services.chantier.ChantierMapper.lire;
import static com.tsix.immocrm.services.chantier.ChantierMapper.vrai;

@Service
@Transactional(readOnly = true)
public class DecompteChantierService {

    static final String DECOMPTE_INTROUVABLE = "Décompte introuvable: ";

    private final PrjWorkingOutRepository decompteRepository;
    private final PrjWorkingOutLineRepository ligneRepository;
    private final PrjWorkingOutLineExtraRepository extraRepository;
    private final PrjWorkingOutRetainedLineRepository retenueRepository;
    private final StlSettlementLineRepository reglementLigneRepository;
    private final ChiffrageLots chiffrage;
    private final PerimetreChantierService perimetreService;

    public DecompteChantierService(PrjWorkingOutRepository decompteRepository,
                                   PrjWorkingOutLineRepository ligneRepository,
                                   PrjWorkingOutLineExtraRepository extraRepository,
                                   PrjWorkingOutRetainedLineRepository retenueRepository,
                                   StlSettlementLineRepository reglementLigneRepository,
                                   ChiffrageLots chiffrage, PerimetreChantierService perimetreService) {
        this.decompteRepository = decompteRepository;
        this.ligneRepository = ligneRepository;
        this.extraRepository = extraRepository;
        this.retenueRepository = retenueRepository;
        this.reglementLigneRepository = reglementLigneRepository;
        this.chiffrage = chiffrage;
        this.perimetreService = perimetreService;
    }

    public Page<DecompteListItemDto> liste(FiltresChantier.FiltreDecomptes filtre, Pageable pageable) {
        PerimetreChantier perimetre = perimetreService.perimetreChantierCourant();
        Pageable traduit = TriChantier.traduire(pageable, TriChantier.DECOMPTES);
        if (perimetre.estVide()) {
            return Page.empty(traduit);
        }
        Page<PrjWorkingOut> page = decompteRepository.findAll(FiltresChantier.decomptes(filtre, perimetre), traduit);
        Map<UUID, PrjWorkingOut> charges = decompteRepository.findAllAvecReferentiels(
                        page.getContent().stream().map(PrjWorkingOut::getId).toList()).stream()
                .collect(Collectors.toMap(PrjWorkingOut::getId, Function.identity()));
        List<PrjWorkingOut> decomptes = page.getContent().stream().map(d -> charges.get(d.getId())).toList();
        Map<UUID, BigDecimal> regle = chiffrage.regleParFacture(decomptes.stream()
                .map(PrjWorkingOut::getBuyDocument).filter(Objects::nonNull).map(BuyDocument::getId).toList());
        List<DecompteListItemDto> contenu = decomptes.stream()
                .map(d -> ChantierMapper.decompteListItem(d, ChiffrageLots.solde(d.getBuyDocument(), regle)))
                .toList();
        return new PageImpl<>(contenu, page.getPageable(), page.getTotalElements());
    }

    public DecompteDetailDto detail(UUID id) {
        PerimetreChantier perimetre = perimetreService.perimetreChantierCourant();
        PrjWorkingOut d = decompteRepository.findAvecReferentiels(id)
                .filter(w -> perimetre.autorise(ChantierMapper.projetId(w.getPrjMarket())))
                .orElseThrow(() -> new ResourceNotFoundException(DECOMPTE_INTROUVABLE + id));
        BuyDocument facture = d.getBuyDocument();
        SoldeFacture solde = facture == null ? SoldeFacture.AUCUNE
                : ChiffrageLots.solde(facture, chiffrage.regleParFacture(List.of(facture.getId())));
        DecompteListItemDto l = ChantierMapper.decompteListItem(d, solde);
        BigDecimal cumulTTC = Montants.arrondi(decompteRepository.cumulTTCJusquA(d.getPrjMarket().getId(), d.getSorting()));
        return new DecompteDetailDto(l.id(), l.code(), l.numero(), l.dateDocument(), l.lotId(), l.lotCode(),
                l.lotDescription(), l.projetId(), l.projetCode(), l.fournisseurId(), l.fournisseurNom(),
                l.montantHT(), l.montantTTC(), l.travauxSupplementaires(), l.retenueGarantie(), l.netAPayer(),
                l.cumulNetRetenueGarantie(), l.dernier(), l.factureId(), l.factureCode(), l.factureStatutCode(),
                l.factureStatutDescription(), l.montantRegle(), l.resteARegler(),
                d.getAmountVat(), Montants.enPourcentage(d.getWarrantyRatio()), d.getWorkingOutAmountTTC(),
                d.getFluctuationsToPay(), d.getAdvanceAmount(), d.getWorkingOutAdvanceAmount(),
                d.getProportionAmount(), d.getInsuranceDECAmount(), d.getInsuranceTRCAmount(),
                d.getNonRefundableRetainer(), d.getRefundableRetainer(), d.getFluctuations(), d.getAmountToBePaid(),
                cumulTTC, ChantierMapper.videEnNull(d.getNotes()),
                d.getUserCreateDateTime(), d.getUserCreate(), d.getUserUpdateDateTime(), d.getUserUpdate(),
                facture == null ? null : facture(facture),
                facture == null ? List.of() : reglements(facture.getId()),
                ligneRepository.findByDecompte(id).stream().map(DecompteChantierService::ligne).toList(),
                extraRepository.findByDecompte(id).stream().map(DecompteChantierService::extra).toList(),
                retenueRepository.findByDecompte(id).stream()
                        .map(r -> new RetenueDecompteDto(r.getId(), r.getDescription(), r.getAmount())).toList());
    }

    private static FactureDecompteDto facture(BuyDocument f) {
        BuyDocumentStatus statut = f.getBuyDocumentStatus();
        return new FactureDecompteDto(f.getId(), f.getCode(), jour(f.getDocDate()), jour(f.getMaturityDate()),
                f.getAmountHT(), f.getAmountTTC(), f.getAmountToPay(), f.getAmountRegul(),
                lire(statut, BuyDocumentStatus::getCode), lire(statut, BuyDocumentStatus::getDescription),
                vrai(lire(statut, BuyDocumentStatus::getIsOkForPayement)));
    }

    private List<ReglementFactureDto> reglements(UUID factureId) {
        return reglementLigneRepository.findByBuyDocumentId(factureId).stream()
                .map(DecompteChantierService::reglement)
                .toList();
    }

    private static ReglementFactureDto reglement(StlSettlementLine ligne) {
        StlSettlement r = ligne.getStlSettlement();
        StlSettlementType type = r.getStlSettlementType();
        return new ReglementFactureDto(r.getId(), ChantierMapper.videEnNull(r.getCode()), jour(r.getDocDate()),
                jour(r.getEcheanceDate()), ligne.getAmount(),
                lire(type, StlSettlementType::getCode), lire(type, StlSettlementType::getDescription),
                !StatutReglement.compteDansLesTotaux(ligne));
    }

    private static LigneDecompteDto ligne(PrjWorkingOutLine l) {
        BigDecimal quantiteMarche = lire(l.getPrjMarketLine(), PrjMarketLine::getQuantity);
        BigDecimal cumulee = Montants.ouZero(l.getQuantityOld()).add(Montants.ouZero(l.getQuantity()));
        return new LigneDecompteDto(l.getId(), l.getSorting(), l.getCode(), l.getDescription(), l.getUnity(),
                quantiteMarche, l.getUnitPriceHT(), l.getQuantityOld(), l.getQuantity(), cumulee,
                l.getCoefficient(), Montants.arrondi(Montants.produit(l.getQuantity(), l.getUnitPriceHT())),
                Montants.pourcentage(cumulee, quantiteMarche));
    }

    private static ExtraDecompteDto extra(PrjWorkingOutLineExtra x) {
        LigneMontant m = new LigneMontant(null, x.getQuantity(), x.getUnitPriceHT(), x.getVatRatio(),
                x.getUnitPriceTTC());
        return new ExtraDecompteDto(x.getId(), x.getCode(), x.getDescription(), x.getUnity(), x.getQuantity(),
                x.getUnitPriceHT(), Montants.enPourcentage(x.getVatRatio()), Montants.arrondi(m.totalHT()),
                Montants.arrondi(m.totalTTC()));
    }
}
