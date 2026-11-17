package com.tsix.immocrm.services.projet;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.FacturesDecomptes.date;

/** R10 : dernière réception de genre définitif, sinon fin du lot, sinon null (FEATURE-lot-avenants-avances.md). */
@Component
public class DateReceptionDefinitive {

    private static final String SQL = """
            SELECT COALESCE((SELECT MAX(r.DocDate) FROM PrjMarketReception r
                             JOIN PrjMarketReceptionKind k ON k.PKey = r.PrjMarketReceptionKindId
                             WHERE r.PrjMarketId = :lot AND k.IsFinal = 1),
                            (SELECT m.EndDate FROM PrjMarket m WHERE m.PKey = :lot))""";

    private final EntityManager em;

    DateReceptionDefinitive(EntityManager em) {
        this.em = em;
    }

    public LocalDate du(UUID lot) {
        return date(em.createNativeQuery(SQL).setParameter("lot", lot).getSingleResult());
    }
}
