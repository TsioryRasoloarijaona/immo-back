package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * R14 « SUIVI DES BONS DE COMMANDE » (FEATURE-n7-etats-achat.md R14) : une ligne par ligne de BC sans BL, sinon par couple
 * (ligne de BC, ligne de BL de même code article) ; société et projet filtrés par id, BC sans projet gardés (D3).
 */
@Component
public class SuiviBonsCommandeRapport extends RapportAchat {

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String SOCIETE = "societeId";
    private static final String PROJET = "projetId";
    private static final String CODE_BC = "codeBC";
    private static final String CODE_ARTICLE = "codeArticle";
    private static final String QUANTITE = "Quantité";

    private final RequetesRapportsAchat requetes;
    private final Clock clock;

    public SuiviBonsCommandeRapport(RequetesRapportsAchat requetes, Clock clock) {
        this.requetes = requetes;
        this.clock = clock;
    }

    @Override
    public String code() {
        return "BuyOrderAndDeliveryLinesWithQuantity";
    }

    @Override
    public List<ParametreRapport> parametres() {
        LocalDate aujourdhui = LocalDate.now(clock);
        return List.of(ParametreRapport.date(DU, "Du", aujourdhui.withDayOfYear(1)),
                ParametreRapport.date(AU, "Au", aujourdhui), requetes.societes(SOCIETE, "Société"),
                requetes.projets(PROJET, "Projet", false), texte(CODE_BC, "Code BC"), texte(CODE_ARTICLE, "Code article"));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        PerimetreAchat visibles = requetes.visibles(ids(contexte, PROJET)).deSocietes(ids(contexte, SOCIETE));
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(CODE_ARTICLE, "Code article"),
                ColonneRapport.texte("designation", "Désignation"), ColonneRapport.texte(CODE_BC, "Code BC"),
                date("dateBC", "Date BC"), ColonneRapport.texte("codeBL", "Code BL"), date("dateBL", "Date BL"),
                quantite("quantiteCommandee", "Commandée"), quantite("quantiteLivree", "Livrée"),
                quantite("reste", "Reste"));
        return resultat(new SectionRapport("bons-commande", "Suivi des bons de commande", colonnes,
                requetes.bons(visibles, Objects.requireNonNull(contexte.date(DU), DU),
                        Objects.requireNonNull(contexte.date(AU), AU), contexte.texte(CODE_BC),
                        contexte.texte(CODE_ARTICLE)), null, null, List.of()));
    }

    private static List<UUID> ids(ContexteRapport contexte, String nom) {
        return contexte.choix(nom).stream().map(UUID::fromString).toList();
    }

    private static ParametreRapport texte(String nom, String libelle) {
        return new ParametreRapport(nom, libelle, TypeParametre.TEXTE, false, false, null, List.of(), null, null);
    }

    private static ColonneRapport date(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.DATE, null, FonctionTotal.AUCUNE, true, 0);
    }

    private static ColonneRapport quantite(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, QUANTITE, FonctionTotal.AUCUNE, true, 2);
    }
}
