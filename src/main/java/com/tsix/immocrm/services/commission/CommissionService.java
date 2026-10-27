package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.dto.commission.CommissionTrancheDto;
import com.tsix.immocrm.dto.commission.DemandesCommission.FiltresCommission;
import com.tsix.immocrm.dto.commission.LigneCommissionCalculeeDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.repositories.com.ComCommissionLineRepository;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.TranchesVisiblesRapport;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Population;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Valorisation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lectures de l'écran Commissions : tranches (soldes et ventes) et lignes d'une tranche. */
@Service
public class CommissionService {

    private final CommissionsVisibles visibles;
    private final TranchesVisiblesRapport tranchesVisibles;
    private final ValorisationTranche valorisation;
    private final ComCommissionLineRepository lignes;
    private final SoldesCommission soldes;
    private final VueLignesCommission vue;

    public CommissionService(CommissionsVisibles visibles, TranchesVisiblesRapport tranchesVisibles,
                             ValorisationTranche valorisation, ComCommissionLineRepository lignes, SoldesCommission soldes,
                             VueLignesCommission vue) {
        this.visibles = visibles;
        this.tranchesVisibles = tranchesVisibles;
        this.valorisation = valorisation;
        this.lignes = lignes;
        this.soldes = soldes;
        this.vue = vue;
    }

    /** Tranches en vente visibles ; {@code recherche} comparée sans espaces ni casse au code et au libellé. */
    @Transactional(readOnly = true)
    public List<CommissionTrancheDto> tranches(String recherche) {
        String motif = normaliser(recherche);
        return dtos(tranchesVisibles.visibles(ModuleRapport.VENTE).stream()
                .filter(t -> motif.isEmpty() || normaliser(t.getCode()).contains(motif)
                        || normaliser(t.getDescription()).contains(motif))
                .toList());
    }

    @Transactional(readOnly = true)
    public CommissionTrancheDto tranche(UUID trancheId) {
        return dtos(List.of(visibles.tranche(trancheId))).get(0);
    }

    @Transactional(readOnly = true)
    public List<LigneCommissionCalculeeDto> lignes(UUID trancheId, FiltresCommission f) {
        visibles.tranche(trancheId);
        return vue.dtos(lignes.findByCfgTrancheIdIn(List.of(trancheId))).stream()
                .filter(l -> f.type() == null || f.type().libelle().equals(l.type()))
                .filter(l -> (f.du() == null && f.au() == null) || DossiersCommission.dansLaPeriode(l.date(), f))
                .filter(l -> f.commercialId() == null || f.commercialId().equals(l.commercialId()))
                .filter(l -> f.dossier() == null || normaliser(l.dossierCode()).contains(normaliser(f.dossier())))
                .toList();
    }

    /** Soldes par {@link SoldesCommission}, ventes par {@link ValorisationTranche} ; triées par code. */
    private List<CommissionTrancheDto> dtos(List<CfgTranche> tranches) {
        List<UUID> ids = tranches.stream().map(CfgTranche::getId).toList();
        Map<UUID, Valorisation> ventes = valorisation.pour(ids).tranches().stream()
                .collect(Collectors.toMap(Valorisation::tranche, Function.identity()));
        List<ComCommissionLine> toutes = LotsTranches.lire(ids, lignes::findByCfgTrancheIdIn);
        Map<UUID, BigDecimal> regles = soldes.reglesParLigne(toutes);
        Map<UUID, List<ComCommissionLine>> parTranche = toutes.stream()
                .collect(Collectors.groupingBy(ComCommissionLine::getCfgTrancheId));
        return tranches.stream()
                .sorted(Comparator.comparing(t -> Objects.toString(t.getCode(), "")))
                .map(t -> dto(t, ventes.get(t.getId()), SoldesCommission.solde(parTranche.getOrDefault(t.getId(), List.of()),
                        regles)))
                .toList();
    }

    private static CommissionTrancheDto dto(CfgTranche t, Valorisation v, SoldesCommission.Solde s) {
        BigDecimal stock = v == null ? BigDecimal.ZERO : v.prixTotal();
        BigDecimal realise = v == null ? BigDecimal.ZERO : v.de(Population.VIVANTS).prix();
        int vendus = v == null ? 0 : (int) (v.articles() - v.libres());
        int restants = v == null ? 0 : (int) v.libres();
        return new CommissionTrancheDto(t.getId(), t.getCode(), t.getDescription(), vendus, restants, s.genere(),
                s.regle(), s.reste(), stock, realise, stock.subtract(realise));
    }

    private static String normaliser(String texte) {
        return texte == null ? "" : texte.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
