package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.dto.dossier.ActeReservationData.Destinataire;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.modele.ControleAccesFormulaireBarid;
import com.tsix.immocrm.services.modele.DestinatairesBarid;
import com.tsix.immocrm.services.modele.LectureModeles;
import com.tsix.immocrm.services.modele.MiseEnPageModele;
import com.tsix.immocrm.services.rapport.vente.dossiers.RapportVente;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * N° 35 FORMULAIRES DE BARID AL-MAGHRIB, modèle OMRAN {@code ComSaleFormulaireBaridMaroc} (FEATURE-vente-formulaire-barid.md §4) :
 * une ligne par page que le PDF du lot imprimera, dans le même ordre.
 */
@Component
public class FormulairesBaridRapport extends RapportVente {

    public static final String STATUT_ID = "statutId";
    static final String FOND_ABSENT =
            "Le fond du formulaire n'est pas installé : l'impression sortira sans le fond pré-imprimé";
    private static final String DESTINATAIRE = "destinataire";
    private static final String ADRESSE = "adresse";
    private static final String CODE_POSTAL = "codePostal";
    private static final List<ColonneRapport> COLONNES = List.of(ColonneRapport.texte("tranche", "Tranche"),
            ColonneRapport.texte("statut", "Statut"), ColonneRapport.texte("dossier", "Dossier"),
            ColonneRapport.texte("role", "Rôle"), ColonneRapport.texte(DESTINATAIRE, "Destinataire"),
            ColonneRapport.texte(ADRESSE, "Adresse"), ColonneRapport.texte(CODE_POSTAL, "Code postal"),
            ColonneRapport.texte("societe", "Société"));

    private final DestinatairesBarid destinataires;
    private final StrStatusRepository statuts;
    private final LectureModeles modeles;
    private final MiseEnPageModele miseEnPage;

    public FormulairesBaridRapport(DestinatairesBarid destinataires, StrStatusRepository statuts, LectureModeles modeles,
                                   MiseEnPageModele miseEnPage) {
        super(ControleAccesFormulaireBarid.CODE_RAPPORT);
        this.destinataires = destinataires;
        this.statuts = statuts;
        this.modeles = modeles;
        this.miseEnPage = miseEnPage;
    }

    /** Pages imprimables : un destinataire sans nom n'en fait pas. */
    public record Pages(List<Destinataire> retenues, long sansNom) {
    }

    /** Statuts de dossier ni annulés ni désistés, par ordre OMRAN : le modèle filtre sur ces statuts. */
    @Override
    public List<ParametreRapport> parametres() {
        List<ParametreRapport.Option> options = statuts.findByStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID).stream()
                .filter(StrStatus::estActif)
                .sorted(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(StrStatus::getDescription, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(s -> new ParametreRapport.Option(s.getId().toString(), Objects.toString(s.getDescription(), s.getCode())))
                .toList();
        return List.of(ParametreRapport.tranches(), new ParametreRapport(STATUT_ID, "Statuts", TypeParametre.CHOIX,
                false, true, null, options, null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        Pages pages = pages(contexte.tranchesVisibles(), contexte.choix(STATUT_ID));
        List<Map<String, Object>> lignes = pages.retenues().stream().map(FormulairesBaridRapport::ligne).toList();
        List<String> avertissements = new ArrayList<>();
        compter(avertissements, pages.sansNom(), "%d destinataire(s) sans nom : aucune page");
        compter(avertissements, manque(lignes, ADRESSE), "%d page(s) sans adresse");
        compter(avertissements, manque(lignes, CODE_POSTAL), "%d page(s) sans code postal");
        compter(avertissements, pages.retenues().stream().map(Destinataire::titre).filter(FormulairesBaridRapport::vide)
                .count(), "%d page(s) sans titre");
        if (fondAbsent()) {
            avertissements.add(FOND_ABSENT);
        }
        return new ResultatRapport(List.of(new SectionRapport("destinataires", "Pages à imprimer", COLONNES, lignes,
                null, null, List.of())), avertissements);
    }

    /** Seule source des pages, pour le hub comme pour le PDF ; aucune tranche visible = aucune page. */
    public Pages pages(List<UUID> tranchesVisibles, List<String> statutsDemandes) {
        List<Destinataire> tous = destinataires.desTranches(tranchesVisibles,
                statutsDemandes.stream().map(UUID::fromString).toList());
        List<Destinataire> retenues = tous.stream().filter(DestinatairesBarid::imprimable).toList();
        return new Pages(retenues, (long) tous.size() - retenues.size());
    }

    private boolean fondAbsent() {
        return modeles.actifParCode(CategorieModele.DOSSIER_VENTE.name(), ControleAccesFormulaireBarid.CODE_MODELE)
                .flatMap(modeles::versionCourante)
                .map(v -> miseEnPage.lire(v.getMiseEnPage()).fondImageId() == null)
                .orElse(false);
    }

    private static Map<String, Object> ligne(Destinataire d) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put("tranche", d.tranche());
        ligne.put("statut", d.statut());
        ligne.put("dossier", d.dossier());
        ligne.put("role", d.role());
        ligne.put(DESTINATAIRE, Stream.of(d.titre(), d.nom()).filter(Predicate.not(FormulairesBaridRapport::vide))
                .collect(Collectors.joining(" ")));
        ligne.put(ADRESSE, d.adresse());
        ligne.put(CODE_POSTAL, d.codePostal());
        ligne.put("societe", d.societe());
        return ligne;
    }

    private static long manque(List<Map<String, Object>> lignes, String colonne) {
        return lignes.stream().map(l -> (String) l.get(colonne)).filter(FormulairesBaridRapport::vide).count();
    }

    private static void compter(List<String> avertissements, long nombre, String message) {
        if (nombre > 0) {
            avertissements.add(message.formatted(nombre));
        }
    }

    private static boolean vide(String texte) {
        return texte == null || texte.isBlank();
    }
}
