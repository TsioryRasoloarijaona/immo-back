package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dossier.edition.ModifierDossierRequest;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * B5 de FEATURE-dossier-modification.md : remise verrouillée sur un article sans premier plafond. Testé au niveau du
 * service, car le groupe PRIX (Administrateur) est toujours au deuxième niveau : le refus n'est pas joignable en HTTP.
 */
@SpringBootTest
@Transactional
class VerrouRemiseTest {

    @Autowired private PrixEdition prix;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private StkItemRepository stkItemRepository;

    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        dossier = fixture.dossier("DOS-VER", fixture.tranche("VER-TR", true), StatutDossier.CONCRETISATION);
        StkItem article = dossier.getStkItem();
        article.setDiscountMax(BigDecimal.ZERO);
        article.setDiscountMax2(BigDecimal.ZERO);
        stkItemRepository.save(article);
    }

    private static ModifierDossierRequest requete(String champ, Object valeur) throws ReflectiveOperationException {
        RecordComponent[] composants = ModifierDossierRequest.class.getRecordComponents();
        Class<?>[] types = new Class<?>[composants.length];
        Object[] valeurs = new Object[composants.length];
        for (int i = 0; i < composants.length; i++) {
            types[i] = composants[i].getType();
            valeurs[i] = composants[i].getName().equals(champ) ? valeur : null;
        }
        return ModifierDossierRequest.class.getDeclaredConstructor(types).newInstance(valeurs);
    }

    @Test
    void remiseNonNulle_auPremierNiveau_403MessageOmran() throws Exception {
        ModifierDossierRequest r = requete("remisePourcentage", new BigDecimal("5"));

        assertThatThrownBy(() -> prix.verifierVerrou(dossier, r, PlafondRemise.Niveau.PREMIER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Vous ne pouvez pas appliquer une remise pour cet article (0,00 %) !");
    }

    @Test
    void prixDeVenteSousLePrixAvantRemise_sansDroit_403() throws Exception {
        ModifierDossierRequest r = requete("prixVenteHT", new BigDecimal("950000"));

        assertThatThrownBy(() -> prix.verifierVerrou(dossier, r, PlafondRemise.Niveau.AUCUN))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void remiseNulle_auPremierNiveau_acceptee() throws Exception {
        ModifierDossierRequest r = requete("remiseMontantHT", BigDecimal.ZERO);

        assertThatCode(() -> prix.verifierVerrou(dossier, r, PlafondRemise.Niveau.PREMIER)).doesNotThrowAnyException();
    }

    @Test
    void remiseNonNulle_auDeuxiemeNiveau_acceptee() throws Exception {
        ModifierDossierRequest r = requete("remisePourcentage", new BigDecimal("5"));

        assertThatCode(() -> prix.verifierVerrou(dossier, r, PlafondRemise.Niveau.DEUXIEME))
                .doesNotThrowAnyException();
    }
}
