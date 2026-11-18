package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * R15 « Echéancier de remboursement des crédits » (FEATURE-n7-etats-achat.md R15, Q5) : par prêt et par mois, Σ des échéances
 * de l'année ; prêt visible si sa société est habilitée et son projet habilité ou nul.
 */
@Component
public class EcheancierCreditsRapport extends RapportAchat {

    private static final String CREDIT = "credit";
    private static final String MONTANT = "montant";

    private final RequetesRapportsAchat requetes;
    private final Clock horloge;

    public EcheancierCreditsRapport(RequetesRapportsAchat requetes, Clock horloge) {
        this.requetes = requetes;
        this.horloge = horloge;
    }

    @Override
    public String code() {
        return "BuyTdbLoanScheduleByYear";
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(LignesMensuelles.annee(horloge));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = annee(contexte);
        List<Map<String, Object>> lignes = requetes.echeances(annee).stream().map(e -> {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(CREDIT, e.pret());
            ligne.put(LignesMensuelles.MOIS, LignesMensuelles.libelleMois(e.mois()));
            ligne.put(MONTANT, e.montant());
            return ligne;
        }).toList();
        return resultat(new SectionRapport("echeancier", "Échéancier de remboursement des crédits — %d".formatted(annee),
                List.of(ColonneRapport.texte(CREDIT, "Crédit"), ColonneRapport.texte(LignesMensuelles.MOIS, "Mois"),
                        ColonneRapport.montant(MONTANT, "Montant", null)), lignes, CREDIT, null, List.of()));
    }
}
