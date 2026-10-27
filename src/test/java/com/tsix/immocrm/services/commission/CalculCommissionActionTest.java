package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.commission.TypeCommission;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.ComSalesCommission;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.services.commission.CalculCommission.Fait;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Niveaux « action commerciale » de la priorité R-S (FEATURE-vente-commissions.md §2, réponse 1 du manager : action =
 * {@code ComProspection.ComActionId} de la prospection du dossier), absents des tests d'acceptation verrouillés.
 */
class CalculCommissionActionTest {

    private static final LocalDate JOUR = LocalDate.of(2024, 2, 23);

    private final CfgTier commercial = tiers();
    private final ComAction action = action();
    private final StkItemCategory categorie = categorie();
    private final Fait fait = new Fait(commercial.getId(), action.getId(), categorie.getId(), JOUR);

    private static CfgTier tiers() {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        return t;
    }

    private static ComAction action() {
        ComAction a = new ComAction();
        a.setId(UUID.randomUUID());
        return a;
    }

    private static StkItemCategory categorie() {
        StkItemCategory c = new StkItemCategory();
        c.setId(UUID.randomUUID());
        return c;
    }

    private static ComSalesCommission regle(CfgTier personne, ComAction a, StkItemCategory c, String montant) {
        ComSalesCommission r = new ComSalesCommission();
        r.setId(UUID.randomUUID());
        r.setCfgTier(personne);
        r.setComAction(a);
        r.setStkItemCategory(c);
        r.setSaleCommissionBasedOnAmount(true);
        r.setSaleCommissionAmount(new BigDecimal(montant));
        r.setUserCreateDateTime(LocalDateTime.of(2024, 1, 1, 0, 0));
        return r;
    }

    private BigDecimal montantRetenu(List<ComSalesCommission> regles) {
        ComSalesCommission r = CalculCommission.regle(regles, fait).orElseThrow();
        return CalculCommission.montant(r, TypeCommission.CONCRETISATION, false, BigDecimal.TEN).valeur();
    }

    @Test
    void commercialActionCategorie_avantCommercialAction() {
        BigDecimal m = montantRetenu(List.of(regle(commercial, action, null, "100"),
                regle(commercial, action, categorie, "200")));

        assertThat(m).isEqualByComparingTo("200");
    }

    @Test
    void commercialAction_avantCommercialCategorie() {
        BigDecimal m = montantRetenu(List.of(regle(commercial, null, categorie, "100"),
                regle(commercial, action, null, "200")));

        assertThat(m).isEqualByComparingTo("200");
    }

    @Test
    void commercialSeul_avantActionCategorie() {
        BigDecimal m = montantRetenu(List.of(regle(null, action, categorie, "100"), regle(commercial, null, null, "200")));

        assertThat(m).isEqualByComparingTo("200");
    }

    @Test
    void actionCategorie_avantActionSeule() {
        BigDecimal m = montantRetenu(List.of(regle(null, action, null, "100"), regle(null, action, categorie, "200")));

        assertThat(m).isEqualByComparingTo("200");
    }

    @Test
    void actionSeule_avantCategorieSeule() {
        BigDecimal m = montantRetenu(List.of(regle(null, null, categorie, "100"), regle(null, action, null, "200")));

        assertThat(m).isEqualByComparingTo("200");
    }

    @Test
    void regleDUneAutreAction_ignoree_repliSurLaRegleSansCritere() {
        BigDecimal m = montantRetenu(List.of(regle(null, null, null, "100"), regle(commercial, action(), categorie, "900")));

        assertThat(m).isEqualByComparingTo("100");
    }

    @Test
    void dossierSansAction_regleAvecAction_ignoree() {
        Fait sansAction = new Fait(commercial.getId(), null, categorie.getId(), JOUR);

        assertThat(CalculCommission.regle(List.of(regle(null, action, null, "100")), sansAction)).isEmpty();
    }

    /** Réponse 2 : à priorité et fenêtre égales, la règle la plus ancienne gagne. */
    @Test
    void departage_plusAncienneRegle() {
        ComSalesCommission recente = regle(null, action, null, "100");
        recente.setUserCreateDateTime(LocalDateTime.of(2024, 6, 1, 0, 0));
        ComSalesCommission ancienne = regle(null, action, null, "200");

        assertThat(montantRetenu(List.of(recente, ancienne))).isEqualByComparingTo("200");
    }
}
