package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.LignesListeDossiers;
import com.tsix.immocrm.domain.rapport.*;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.services.dossier.liste.FiltresListeDossiers;
import com.tsix.immocrm.services.dossier.liste.ListeDossiersService;
import com.tsix.immocrm.services.dossier.liste.StatutsVisiblesListe;
import com.tsix.immocrm.services.rapport.vente.dossiers.RapportVente;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.domain.ColonneDossier.*;

/**
 * N° 1 LISTE DOSSIERS (FEATURE-tdb-vente-rapports-dossiers.md §3.4), modèle OMRAN {@code ComTdbFolderWithClientInfos} :
 * enveloppe de la liste {@code /dossiers} (même périmètre, statuts visibles, ordre OMRAN et montants), sortis compris.
 */
@Component
public class ListeDossiersClientRapport extends RapportVente {

    static final String CODE = "ComTdbFolderWithClientInfos";
    static final String STATUTS_MASQUES = "Statuts masqués pour votre profil";
    static final String AUCUN_DOSSIER = "Aucun dossier pour ces tranches";

    private record Colonne(ColonneRapport rapport, ColonneDossier source, Function<DossierListItemDto, Object> valeur) {
    }

    private static final List<Colonne> COLONNES = List.of(
            texte("code", "Dossier", ColonneDossier.CODE, DossierListItemDto::code),
            texte("article", "Article", ARTICLE_CODE, DossierListItemDto::articleCode),
            texte("client", "Client principal", CLIENT_NOM, DossierListItemDto::clientNom),
            texte("telephone", "Téléphone", CLIENT_TELEPHONE, DossierListItemDto::clientTelephone),
            texte("mobile", "Portable", CLIENT_MOBILE_COMPLET, DossierListItemDto::clientMobileComplet),
            texte("email", "e-mail", CLIENT_EMAIL, DossierListItemDto::clientEmail),
            texte("adresse", "Adresse", CLIENT_ADRESSE, DossierListItemDto::clientAdresse),
            texte("ville", "Ville", CLIENT_VILLE, DossierListItemDto::clientVille),
            texte("pays", "Pays", CLIENT_PAYS, DossierListItemDto::clientPays),
            texte("statut", "Statut", STATUT, DossierListItemDto::statut),
            new Colonne(ColonneRapport.montant("fraisSyndic", "Frais syndic", null), FRAIS_SYNDIC,
                    DossierListItemDto::fraisSyndic),
            new Colonne(ColonneRapport.montant("resteSyndic", "Reste syndic", null), FRAIS_SYNDIC_RESTE,
                    DossierListItemDto::fraisSyndicReste),
            date("dateConcretisation", "Concrétisation", DATE_CONCRETISATION, DossierListItemDto::dateConcretisation),
            date("dateMinute", "Minute", DATE_MINUTE, DossierListItemDto::dateMinute),
            date("dateRemiseCles", "Remise clés", DATE_LIVRAISON_REELLE, DossierListItemDto::dateLivraisonReelle),
            texte("titreFoncier", "N° titre foncier", ARTICLE_TITRE_FONCIER, DossierListItemDto::articleTitreFoncier));

    private static final Set<ColonneDossier> SOURCES = EnumSet.copyOf(COLONNES.stream().map(Colonne::source).toList());

    private final ListeDossiersService liste;
    private final StatutsVisiblesListe statutsVisibles;

    public ListeDossiersClientRapport(ListeDossiersService liste, StatutsVisiblesListe statutsVisibles) {
        super(CODE);
        this.liste = liste;
        this.statutsVisibles = statutsVisibles;
    }

    private static Colonne texte(String nom, String libelle, ColonneDossier source,
                                 Function<DossierListItemDto, Object> valeur) {
        return new Colonne(ColonneRapport.texte(nom, libelle), source, valeur);
    }

    private static Colonne date(String nom, String libelle, ColonneDossier source,
                                Function<DossierListItemDto, Object> valeur) {
        return new Colonne(new ColonneRapport(nom, libelle, TypeColonne.DATE, null, FonctionTotal.AUCUNE, true, 0),
                source, valeur);
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<String> avertissements = new ArrayList<>();
        if (statutsVisibles.pourUtilisateurCourant().isPresent()) {
            avertissements.add(STATUTS_MASQUES);
        }
        List<DossierListItemDto> dossiers = dossiers(contexte.tranchesVisibles());
        if (dossiers.isEmpty()) {
            avertissements.add(AUCUN_DOSSIER);
        }
        List<Map<String, Object>> lignes = dossiers.stream().map(ListeDossiersClientRapport::ligne).toList();
        SectionRapport section = new SectionRapport("dossiers",
                "Liste des dossiers de vente avec les informations du client principal",
                COLONNES.stream().map(Colonne::rapport).toList(), lignes, null, null, List.of());
        return new ResultatRapport(List.of(section), avertissements);
    }

    /** Aucune tranche visible ⇒ rien : la liste lirait une liste de tranches vide comme « pas de filtre ». */
    private List<DossierListItemDto> dossiers(List<UUID> tranches) {
        if (tranches.isEmpty()) {
            return List.of();
        }
        FiltresListeDossiers filtres = new FiltresListeDossiers(null, null, null, tranches, null, null, null, null,
                null, true, LignesListeDossiers.DOSSIERS);
        return liste.toutes(filtres, true, null, SOURCES);
    }

    private static Map<String, Object> ligne(DossierListItemDto dossier) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        COLONNES.forEach(c -> ligne.put(c.rapport().nom(), c.valeur().apply(dossier)));
        return ligne;
    }
}
