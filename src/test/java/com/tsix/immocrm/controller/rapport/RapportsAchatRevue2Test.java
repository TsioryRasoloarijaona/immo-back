package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ACHAT;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.lignesAvec;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.lignesMentionnant;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.porteMontant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.DEPENSES_PAR_MOIS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.ECHEANCIER_CREDITS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 2e revue N7b (FEATURE-n7-etats-achat.md réponse 8 : « D3 s'applique à toutes les entrées de R12 qui portent un projet ou
 * une société (règlements, …) ») : un décaissement qui règle une pièce d'un projet non habilité ne doit pas compter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsAchatRevue2Test {

    private static final String DECAISSEMENTS = "decaissements";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat fixture;

    private String jeton;
    private UUID s1;
    private UUID pa;
    private UUID pb;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("N7V");
        fixture.rapports();
        s1 = achat.societe("S1-" + jeton);
        pa = achat.projet("PA-" + jeton, s1);
        pb = achat.projet("PB-" + jeton, s1);
        nom = "Fournisseur " + jeton;
        fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("N7V-" + jeton, false, s1, pa);
    }

    private static LocalDate jour(int mois, int jour) {
        return LocalDate.of(FixtureRapportsAchat.ANNEE, mois, jour);
    }

    private void affecter(UUID reglement, UUID piece, String montant) {
        em.createNativeQuery("""
                        INSERT INTO StlSettlementLine (PKey, StlSettlementId, BuyDocumentId, Amount)
                        VALUES (:id, :reglement, :piece, :montant)""")
                .setParameter("id", UUID.randomUUID()).setParameter("reglement", reglement)
                .setParameter("piece", piece).setParameter("montant", new BigDecimal(montant))
                .executeUpdate();
    }

    @Test
    void r12_decaissements_reglementDUnePieceDeProjetNonHabilite_exclu() throws Exception {
        UUID pieceVisible = fixture.facturePayable(jeton + "-FA", LocalDate.of(2077, 1, 5), fournisseur, nom, s1, pa, "9");
        UUID pieceCachee = fixture.facturePayable(jeton + "-FB", LocalDate.of(2077, 1, 5), fournisseur, nom, s1, pb, "8000");
        affecter(fixture.reglement(jeton + "-RA", "PRJ", false, "9", "IN_WORK", null, jour(6, 1), s1), pieceVisible, "9");
        affecter(fixture.reglement(jeton + "-RB", "PRJ", false, "8000", "IN_WORK", null, jour(6, 1), s1), pieceCachee,
                "8000");

        List<Map<String, Object>> mois = lignesAvec(new AppelsRapports(mockMvc).sections(get(resultat(ACHAT,
                DEPENSES_PAR_MOIS)).with(connecte(compte, FixtureRapportsAchat.role()))
                .param(ANNEE, String.valueOf(FixtureRapportsAchat.ANNEE))), DECAISSEMENTS);

        assertThat(mois).hasSize(12);
        assertThat(somme(mois, DECAISSEMENTS)).as("seul le règlement de la pièce du projet habilité PA")
                .isEqualByComparingTo("9");
    }

    @Test
    void r15_deuxPretsHomonymes_restentDeuxLignes() throws Exception {
        String libelle = "Homonyme " + jeton;
        FixtureRapportsAchat.Pret premier = fixture.pret("L1-" + jeton, s1, null);
        FixtureRapportsAchat.Pret second = fixture.pret("L2-" + jeton, s1, null);
        em.createNativeQuery("UPDATE BuyLoan SET Description = :libelle WHERE PKey IN (:premier, :second)")
                .setParameter("libelle", libelle).setParameter("premier", premier.id())
                .setParameter("second", second.id()).executeUpdate();
        fixture.echeance(premier, jour(4, 10), "11");
        fixture.echeance(second, jour(4, 20), "22");

        List<Map<String, Object>> lignes = lignesMentionnant(new AppelsRapports(mockMvc).sections(get(resultat(ACHAT,
                ECHEANCIER_CREDITS)).with(connecte(compte, FixtureRapportsAchat.role()))
                .param(ANNEE, String.valueOf(FixtureRapportsAchat.ANNEE))), libelle);

        assertThat(lignes).as("une ligne par prêt, même libellé").hasSize(2)
                .anySatisfy(l -> assertThat(porteMontant(l, "11")).isTrue())
                .anySatisfy(l -> assertThat(porteMontant(l, "22")).isTrue());
    }
}
