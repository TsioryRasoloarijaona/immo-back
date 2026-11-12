package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.AgregatsBonCommande;
import com.tsix.immocrm.domain.achat.AgregatsBonCommande.Commande;
import com.tsix.immocrm.domain.achat.AgregatsBonCommande.PieceLiee;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.achat.AgregatsBonCommandeDto;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.chaine;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.uuid;

/** Pièces liées des bons de commande, une ligne par pièce ; l'addition est faite par {@link AgregatsBonCommande}. */
@Component
class RequetesAgregatsBonCommande {

    private static final int LOT = 1000;

    // Toutes les pièces liées, visibles ou non de l'appelant (comme OMRAN) : voir FEATURE-achat-bc-agregats.md Q1.
    private static final String PIECES_LIEES = "SELECT o.PKey AS BonId, o.AmountHT AS BonHT, o.AmountTTC AS BonTTC, "
            + "o.AmountToPay AS BonAPayer, o.OrderAdvanceRatio, q.DocRef AS DevisReference, q.Code AS DevisCode, "
            + "d.PKey, d.DocType, d.ReferenceType, d.Code, d.AmountHT, d.AmountTTC, d.AmountToPay, d.AdvanceAmount, "
            + "d.WarrantyAmount, (" + SoldeDocumentAchat.SQL_REGLE + ") AS Regle, fi.Code AS FactureDuBl "
            + "FROM BuyDocument o LEFT JOIN BuyDocument q ON q.PKey = o.BuyQuoteId "
            + "LEFT JOIN BuyDocument d ON d.BuyOrderId = o.PKey "
            + "LEFT JOIN BuyDocument fi ON fi.PKey = d.BuyInvoiceId AND fi.DocType = 'INVOICE' "
            + "WHERE o.PKey IN (:ids)";

    private final NamedParameterJdbcTemplate jdbc;

    RequetesAgregatsBonCommande(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Agrégats de chaque bon de commande de {@code bons} (ids), par lots de {@value #LOT}. */
    Map<UUID, AgregatsBonCommandeDto> agregats(List<UUID> bons) {
        Map<UUID, Commande> commandes = new HashMap<>();
        Map<UUID, List<PieceLiee>> liees = new HashMap<>();
        RowCallbackHandler lecture = rs -> {
            UUID bon = uuid(rs, "BonId");
            if (!commandes.containsKey(bon)) {
                commandes.put(bon, new Commande(rs.getBigDecimal("BonHT"), rs.getBigDecimal("BonTTC"),
                        rs.getBigDecimal("BonAPayer"), rs.getBigDecimal("OrderAdvanceRatio"),
                        chaine(rs, "DevisReference"), chaine(rs, "DevisCode")));
            }
            if (rs.getString("PKey") != null) {
                liees.computeIfAbsent(bon, k -> new ArrayList<>()).add(piece(rs));
            }
        };
        for (int i = 0; i < bons.size(); i += LOT) {
            jdbc.query(PIECES_LIEES, Map.of("ids", RequetesDocumentsAchat.textes(
                    bons.subList(i, Math.min(i + LOT, bons.size())))), lecture);
        }
        Map<UUID, AgregatsBonCommandeDto> agregats = new HashMap<>();
        commandes.forEach((bon, bc) -> agregats.put(bon, AgregatsBonCommande.de(bc, liees.getOrDefault(bon, List.of()))));
        return agregats;
    }

    private static PieceLiee piece(ResultSet rs) throws SQLException {
        return new PieceLiee(chaine(rs, "DocType"), chaine(rs, "ReferenceType"), chaine(rs, "Code"),
                rs.getBigDecimal("AmountHT"), rs.getBigDecimal("AmountTTC"), rs.getBigDecimal("AmountToPay"),
                rs.getBigDecimal("AdvanceAmount"), rs.getBigDecimal("WarrantyAmount"), rs.getBigDecimal("Regle"),
                chaine(rs, "FactureDuBl"));
    }
}
