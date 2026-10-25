package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rapprochement manuel du n° 8 avec la logique du modèle OMRAN {@code ComTdbSalesRythme} (FEATURE-tdb-vente-rapports-rythme.md
 * §1, §9.2) sur yesRemsProd ({@code DB_NAME=yesRemsProd}, retirer {@code @Disabled}) : réservations identiques ; le modèle
 * compte 1 concrétisation de plus en 2025 (sous le seuil) et 1 contrat de plus en 2024 (annulé ou sous MINUTE).
 */
@Disabled("Manuel : données réelles de yesRemsProd, absentes de yesRemsTest et de la CI")
@SpringBootTest
class RythmeRapprochementTest {

    private static final int DEBUT = 2022;
    private static final int FIN = 2026;

    // Population du modèle : dossier de vente avec statut, tranche en vente, société active ; aucun seuil.
    private static final String MODELE = """
            SELECT x.k, YEAR(x.d) AS a, COUNT(*) AS n
            FROM ComFolder f
            JOIN StkVocation v ON v.PKey = f.StkVocationId AND v.Code = 'VENTE'
            JOIN StkItem i ON i.PKey = f.StkItemId
            JOIN CfgTranche t ON t.PKey = i.StkHierarchyId AND t.IsForSale = 1
            JOIN CfgProject p ON p.PKey = t.CfgProjectId
            JOIN CfgCompany c ON c.PKey = p.CfgCompanyId AND c.IsActive = 1
            JOIN StrStatus st ON st.PKey = f.StrStatusId
            CROSS APPLY (VALUES ('R', f.DocDate), ('C', f.AdvanceDate), ('A', f.AgreementSavingDate)) x(k, d)
            WHERE x.d IS NOT NULL GROUP BY x.k, YEAR(x.d)""";

    @Autowired private FluxVenteMensuel flux;
    @Autowired private JdbcTemplate jdbc;

    /** {@code "R|2024"} → nombre de dossiers du modèle (R réservation, C concrétisation, A acte). */
    private Map<String, Long> modele() {
        Map<String, Long> comptes = new HashMap<>();
        jdbc.query(MODELE, rs -> {
            comptes.put(rs.getString("k") + "|" + rs.getInt("a"), rs.getLong("n"));
        });
        return comptes;
    }

    @Test
    void n8_reservationsIdentiques_concretisationsEtContratsAvecLEcartDuSpec() {
        List<UUID> tranches = jdbc.queryForList("""
                SELECT t.PKey FROM CfgTranche t JOIN CfgProject p ON p.PKey = t.CfgProjectId
                JOIN CfgCompany c ON c.PKey = p.CfgCompanyId AND c.IsActive = 1 WHERE t.IsForSale = 1""", String.class)
                .stream().map(UUID::fromString).toList();
        FluxMensuel f = flux.calculer(new DemandeFlux(tranches, DEBUT, FIN,
                Set.of(MesureFlux.RESERVATIONS, MesureFlux.CONCRETISATIONS, MesureFlux.CA_QUANTITE)));
        Map<String, Long> modele = modele();

        SoftAssertions soft = new SoftAssertions();
        for (int a = DEBUT; a <= FIN; a++) {
            soft.assertThat(modele.getOrDefault("R|" + a, 0L) - f.annee(MesureFlux.RESERVATIONS, a).longValue())
                    .as("réservations %d", a).isZero();
            soft.assertThat(modele.getOrDefault("C|" + a, 0L) - f.annee(MesureFlux.CONCRETISATIONS, a).longValue())
                    .as("concrétisations %d", a).isEqualTo(a == 2025 ? 1L : 0L);
            soft.assertThat(modele.getOrDefault("A|" + a, 0L) - f.annee(MesureFlux.CA_QUANTITE, a).longValue())
                    .as("contrats %d", a).isEqualTo(a == 2024 ? 1L : 0L);
        }
        soft.assertAll();
    }
}
