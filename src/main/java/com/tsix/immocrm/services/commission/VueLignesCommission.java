package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.dto.commission.LigneCommissionCalculeeDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Lignes de commission rendues sur le fil, libellés et réglé ({@link SoldesCommission}) compris. */
@Component
public class VueLignesCommission {

    private static final String LIBELLES = """
            SELECT l.id, f.code, c, i.code, i.description, cat.description, t.description, s.description, e, d.code
            FROM ComCommissionLine l
            LEFT JOIN ComFolder f ON f.id = l.comFolderId
            LEFT JOIN f.cfgCustomer c
            LEFT JOIN f.strStatus s
            LEFT JOIN StkItem i ON i.id = l.stkItemId
            LEFT JOIN StkItemCategory cat ON cat.id = l.stkItemCategoryId
            LEFT JOIN CfgTranche t ON t.id = l.cfgTrancheId
            LEFT JOIN CfgTier e ON e.id = l.cfgEmployeeId
            LEFT JOIN BuyDocument d ON d.id = l.buyDocumentId
            WHERE l.id IN :ids""";

    private final EntityManager em;
    private final SoldesCommission soldes;

    public VueLignesCommission(EntityManager em, SoldesCommission soldes) {
        this.em = em;
        this.soldes = soldes;
    }

    /** Triées par date puis N° de dossier. */
    public List<LigneCommissionCalculeeDto> dtos(List<ComCommissionLine> lignes) {
        em.flush();
        Map<UUID, Object[]> libelles = new HashMap<>();
        LotsTranches.lire(lignes.stream().map(ComCommissionLine::getId).toList(),
                        lot -> em.createQuery(LIBELLES, Object[].class).setParameter("ids", lot).getResultList())
                .forEach(r -> libelles.put((UUID) r[0], r));
        Map<UUID, BigDecimal> regles = soldes.reglesParLigne(lignes);
        return lignes.stream()
                .map(l -> dto(l, libelles.getOrDefault(l.getId(), new Object[10]), regles))
                .sorted(Comparator.comparing(LigneCommissionCalculeeDto::date, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(d -> Objects.toString(d.dossierCode(), "")))
                .toList();
    }

    private static LigneCommissionCalculeeDto dto(ComCommissionLine l, Object[] r, Map<UUID, BigDecimal> regles) {
        BigDecimal montant = SoldesCommission.montant(l);
        BigDecimal regle = regles.getOrDefault(l.getId(), BigDecimal.ZERO);
        return new LigneCommissionCalculeeDto(l.getId(), CalculCommission.jour(l.getDocDate()), l.getCommissionType(),
                montant, l.getCalculationMethod(), l.getComFolderId(), (String) r[1], nom(r[2]), (String) r[3],
                (String) r[4], (String) r[5], (String) r[6], (String) r[7], l.getCfgEmployeeId(), nom(r[8]), regle,
                montant.subtract(regle), l.getBuyDocumentId(), (String) r[9], l.getBuyDocumentId() == null);
    }

    static String nom(Object tiers) {
        return tiers instanceof CfgTier t ? TierNomFormatter.nom(t) : null;
    }
}
