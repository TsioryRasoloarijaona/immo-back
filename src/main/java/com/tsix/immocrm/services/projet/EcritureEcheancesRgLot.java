package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.EcheanceRgSaisieDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketWarrantyLine;
import com.tsix.immocrm.services.projet.GrillesLot.Grille;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.ValidationBordereau.CENT;
import static com.tsix.immocrm.services.projet.ValidationBordereau.TEXTE;
import static com.tsix.immocrm.services.projet.ValidationBordereau.entre;
import static com.tsix.immocrm.services.projet.ValidationBordereau.exiger;
import static com.tsix.immocrm.services.projet.ValidationBordereau.longueur;
import static com.tsix.immocrm.services.projet.ValidationBordereau.sansNul;

/** Grille des échéances de remboursement de RG (FEATURE-lot-avenants-avances.md §4.2, R8-R12), envoyée entière. */
@Service
public class EcritureEcheancesRgLot {

    private final LotsProjetService lots;
    private final GrillesLot grilles;
    private final DateReceptionDefinitive dateDefinitive;
    private final PrevisionsPaiementLot previsions;
    private final SousFenetresLotService sousFenetres;

    EcritureEcheancesRgLot(LotsProjetService lots, GrillesLot grilles, DateReceptionDefinitive dateDefinitive,
                           PrevisionsPaiementLot previsions, SousFenetresLotService sousFenetres) {
        this.lots = lots;
        this.grilles = grilles;
        this.dateDefinitive = dateDefinitive;
        this.previsions = previsions;
        this.sousFenetres = sousFenetres;
    }

    @Transactional
    public RemboursementsLotDto enregistrerEcheances(UUID lotId, List<EcheanceRgSaisieDto> corps) {
        PrjMarket lot = lots.visible(lotId);
        exiger(sansNul(corps), "liste obligatoire, sans élément nul");
        corps.forEach(EcritureEcheancesRgLot::valider);
        LocalDate base = dateDefinitive.du(lotId);
        Grille<EcheanceRgSaisieDto, PrjMarketWarrantyLine> g = new Grille<>(PrjMarketWarrantyLine.class, "prjMarketId",
                EcheanceRgSaisieDto::id, PrjMarketWarrantyLine::getId, id -> {
            PrjMarketWarrantyLine l = new PrjMarketWarrantyLine();
            l.setId(id);
            l.setPrjMarketId(lotId);
            return l;
        }, (l, s) -> remplir(l, s, base), l -> Arrays.asList(l.getSorting(), l.getDescription(), l.getDelay(),
                l.getEcheanceDate(), l.getRatio() == null ? null : l.getRatio().stripTrailingZeros()));
        Map<UUID, PrjMarketWarrantyLine> enBase = grilles.enBase(g, lotId);
        grilles.refuserIds(g, corps, enBase, "id");
        grilles.enregistrer(g, corps, enBase, grilles.ecriture());
        grilles.tracer(lot, "Échéances de RG enregistrées");
        previsions.recalculer(lotId);
        return sousFenetres.remboursements(lotId);
    }

    private static void valider(EcheanceRgSaisieDto e) {
        exiger(e.ordre() != null, "ordre : obligatoire");
        exiger(entre(e.pourcentage(), CENT), "pourcentage : obligatoire, entre 0 et 100");
        exiger(e.delaiJours() == null || e.delaiJours() >= 0, "delaiJours : positif ou nul");
        longueur(e.description(), TEXTE, "description");
    }

    /** R11 : le délai prime sur l'échéance envoyée ; sans date de base, les deux sont gardés tels quels. */
    private static void remplir(PrjMarketWarrantyLine l, EcheanceRgSaisieDto s, LocalDate base) {
        LocalDate echeance = s.echeance();
        Integer delai = s.delaiJours();
        if (base != null && delai != null) {
            echeance = base.plusDays(delai);
        } else if (base != null && echeance != null) {
            delai = Math.toIntExact(ChronoUnit.DAYS.between(base, echeance));
        }
        l.setSorting(s.ordre());
        l.setDescription(s.description());
        l.setDelay(delai);
        l.setEcheanceDate(echeance == null ? null : echeance.atStartOfDay());
        l.setRatio(s.pourcentage().movePointLeft(2).setScale(6, RoundingMode.HALF_UP));
    }
}
