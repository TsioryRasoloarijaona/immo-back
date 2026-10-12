package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.domain.chantier.LigneMontant;
import com.tsix.immocrm.domain.chantier.Montants;
import com.tsix.immocrm.domain.chantier.Totaux;
import com.tsix.immocrm.dto.chantier.BordereauDto;
import com.tsix.immocrm.dto.chantier.DecompteListItemDto;
import com.tsix.immocrm.dto.chantier.LigneBordereauDto;
import com.tsix.immocrm.dto.chantier.LotDetailDto;
import com.tsix.immocrm.dto.chantier.LotListItemDto;
import com.tsix.immocrm.dto.chantier.TotauxBordereauDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjMarketLineGrouping;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.prj.PrjMarketLineGroupingRepository;
import com.tsix.immocrm.repositories.prj.PrjMarketLineRepository;
import com.tsix.immocrm.repositories.prj.PrjMarketRepository;
import com.tsix.immocrm.repositories.prj.PrjWorkingOutRepository;
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

@Service
@Transactional(readOnly = true)
public class LotChantierService {

    static final String LOT_INTROUVABLE = "Lot introuvable: ";

    private final PrjMarketRepository lotRepository;
    private final PrjMarketLineRepository ligneRepository;
    private final PrjMarketLineGroupingRepository groupeRepository;
    private final PrjWorkingOutRepository decompteRepository;
    private final ChiffrageLots chiffrage;
    private final ProjetChantierService projetService;
    private final PerimetreChantierService perimetreService;

    public LotChantierService(PrjMarketRepository lotRepository, PrjMarketLineRepository ligneRepository,
                              PrjMarketLineGroupingRepository groupeRepository,
                              PrjWorkingOutRepository decompteRepository, ChiffrageLots chiffrage,
                              ProjetChantierService projetService, PerimetreChantierService perimetreService) {
        this.lotRepository = lotRepository;
        this.ligneRepository = ligneRepository;
        this.groupeRepository = groupeRepository;
        this.decompteRepository = decompteRepository;
        this.chiffrage = chiffrage;
        this.projetService = projetService;
        this.perimetreService = perimetreService;
    }

    public Page<LotListItemDto> lotsDuProjet(FiltresChantier.FiltreLots filtre, Pageable pageable) {
        projetService.projetVisible(filtre.projetId());
        Page<PrjMarket> page = lotRepository.findAll(FiltresChantier.lots(filtre),
                TriChantier.traduire(pageable, TriChantier.LOTS));
        List<PrjMarket> lots = dansLOrdre(page.getContent());
        Map<UUID, LotChiffre> chiffres = chiffrage.chiffrer(lots);
        List<LotListItemDto> contenu = lots.stream()
                .map(lot -> ChantierMapper.lotListItem(lot, chiffres.get(lot.getId())))
                .toList();
        return new PageImpl<>(contenu, page.getPageable(), page.getTotalElements());
    }

    public LotDetailDto detail(UUID id) {
        PrjMarket lot = lotVisible(id);
        return ChantierMapper.lotDetail(lot, chiffrage.chiffrer(List.of(lot)).get(id));
    }

    /** Les totaux passent par {@link Totaux#deLignes} : le même calcul que le montant marché du lot. */
    public BordereauDto bordereau(UUID id) {
        lotVisible(id);
        List<PrjMarketLine> lignes = ligneRepository.findBordereau(id);
        List<UUID> groupeIds = lignes.stream().map(PrjMarketLine::getPrjMarketLineGroupingId)
                .filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> groupes = groupeRepository.findAllById(groupeIds).stream()
                .collect(Collectors.toMap(PrjMarketLineGrouping::getId,
                        g -> Objects.toString(g.getDescription(), Objects.toString(g.getCode(), ""))));
        List<LigneMontant> montants = lignes.stream().map(LotChantierService::montant).toList();
        List<LigneBordereauDto> dtos = lignes.stream()
                .map(l -> ligneBordereau(l, groupes.get(l.getPrjMarketLineGroupingId())))
                .toList();
        Totaux totaux = Totaux.deLignes(montants);
        return new BordereauDto(id, dtos,
                new TotauxBordereauDto(totaux.ht(), totaux.tva(), totaux.ttc(), totaux.nbLignes()));
    }

    public List<DecompteListItemDto> decomptes(UUID id) {
        lotVisible(id);
        List<PrjWorkingOut> decomptes = decompteRepository.findByLot(id);
        Map<UUID, BigDecimal> regle = chiffrage.regleParFacture(decomptes.stream()
                .map(PrjWorkingOut::getBuyDocument).filter(Objects::nonNull).map(BuyDocument::getId).toList());
        return decomptes.stream()
                .map(d -> ChantierMapper.decompteListItem(d, ChiffrageLots.solde(d.getBuyDocument(), regle)))
                .toList();
    }

    /** Inconnu ou hors périmètre : même 404. */
    PrjMarket lotVisible(UUID id) {
        PerimetreChantier perimetre = perimetreService.perimetreChantierCourant();
        return lotRepository.findAllAvecReferentiels(List.of(id)).stream()
                .filter(lot -> perimetre.autorise(ChantierMapper.projetId(lot)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(LOT_INTROUVABLE + id));
    }

    private List<PrjMarket> dansLOrdre(List<PrjMarket> page) {
        Map<UUID, PrjMarket> charges = lotRepository.findAllAvecReferentiels(
                        page.stream().map(PrjMarket::getId).toList()).stream()
                .collect(Collectors.toMap(PrjMarket::getId, Function.identity()));
        return page.stream().map(lot -> charges.get(lot.getId())).toList();
    }

    private static LigneMontant montant(PrjMarketLine l) {
        return new LigneMontant(null, l.getQuantity(), l.getUnitPriceHT(), l.getVatRatio(), l.getUnitPriceTTC());
    }

    private static LigneBordereauDto ligneBordereau(PrjMarketLine l, String groupe) {
        LigneMontant m = montant(l);
        return new LigneBordereauDto(l.getId(), l.getSorting(), groupe, l.getCode(), l.getDescription(),
                l.getUnity(), l.getQuantity(), l.getUnitPriceHT(), Montants.enPourcentage(l.getVatRatio()),
                Montants.arrondi(m.totalHT()), Montants.arrondi(m.totalTTC()));
    }
}
