package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.importation.AnalyseImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.BilanImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.ColonneImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.Feuille;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Import d'un classeur (FEATURE-patrimoine-import-export.md §1.3) : lecture hors transaction, puis traduction et
 * écriture par le type ({@link ImportParType}) dans <b>une</b> transaction, annulée à la moindre erreur ou en simulation.
 */
@Service
public class ImportPatrimoineService {

    public static final String ARTICLES = "articles";
    public static final String SOUS_ARTICLES = "sous-articles";
    public static final String PRESENTATIONS = "presentations";
    public static final String TERRAINS = "terrains";
    public static final String LOTISSEMENTS = "lotissements";
    public static final String PREVISIONS = "previsions";
    public static final String ENTITES_JURIDIQUES = "entites-juridiques";
    public static final String PROJETS = "projets";
    public static final String TRANCHES = "tranches";
    public static final String PROSPECTIONS_TERRAIN = "prospections-terrain";

    private final TrancheVisibleRepository tranches;
    private final List<ImportParType<?>> imports;
    private final TransactionTemplate transaction;

    public ImportPatrimoineService(TrancheVisibleRepository tranches, List<ImportParType<?>> imports,
                                   PlatformTransactionManager transactions) {
        this.tranches = tranches;
        this.imports = imports;
        this.transaction = new TransactionTemplate(transactions);
    }

    /** Type inconnu → 404. */
    ImportParType<?> parType(String type) {
        return imports.stream().filter(i -> i.types().contains(type)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Type d'import inconnu : " + type));
    }

    /** Tranche visible si le type l'exige (message OMRAN de {@code ItemList:Import} si absente), sinon nulle. */
    CfgTranche tranche(ImportParType<?> type, UUID trancheId) {
        if (!type.trancheExigee()) {
            return null;
        }
        if (trancheId == null) {
            throw new InvalidRequestException("Vous devez sélectionner une Tranche !");
        }
        return tranches.exigerVisible(trancheId);
    }

    public BilanImportDto importer(String type, UUID trancheId, MultipartFile fichier, String feuille, boolean simulation,
                                   String mapping) {
        ImportParType<?> t = parType(type);
        tranche(t, trancheId);
        Feuille lue = mappee(t, LectureClasseur.lire(fichier, feuille), mapping);
        return transaction.execute(statut -> executer(type, t, trancheId, lue, simulation, statut));
    }

    public List<ColonneImportDto> colonnes(String type) {
        return generique(type).colonnes();
    }

    public AnalyseImportDto analyser(String type, MultipartFile fichier, String feuille) {
        ImportGenerique<?> g = generique(type);
        return MappingImport.analyser(g.catalogue(), LectureClasseur.lire(fichier, feuille));
    }

    /** Types à mapping seulement (FEATURE-patrimoine-import-generique.md) ; les autres → 404. */
    private ImportGenerique<?> generique(String type) {
        if (parType(type) instanceof ImportGenerique<?> g) {
            return g;
        }
        throw new ResourceNotFoundException("Type d'import sans mapping : " + type);
    }

    private static Feuille mappee(ImportParType<?> t, Feuille lue, String mapping) {
        if (t instanceof ImportGenerique<?> g) {
            return MappingImport.appliquer(g.catalogue(), lue, mapping);
        }
        if (mapping != null && !mapping.isBlank()) {
            throw new InvalidRequestException("Le mapping ne s'applique qu'aux entités juridiques, programmes, tranches et "
                    + "prospections terrain.");
        }
        return lue;
    }

    private <C> BilanImportDto executer(String type, ImportParType<C> t, UUID trancheId, Feuille lue, boolean simulation,
                                        TransactionStatus statut) {
        List<String> avertissements = new ArrayList<>();
        C ctx = t.contexte(type, tranche(t, trancheId), lue.entetes(), avertissements);
        Set<String> connus = t.entetesConnus(ctx);
        lue.entetes().stream().filter(e -> !connus.contains(e))
                .forEach(e -> avertissements.add("Colonne « " + e + " » ignorée : en-tête inconnu."));
        List<LigneBilanDto> lignes = t.ecrire(lue.lignes(), ctx);
        int erreurs = compter(lignes, ResultatImport.ERREUR) + compter(lignes, ResultatImport.NON_VERIFIEE);
        boolean ecrit = erreurs == 0 && !simulation;
        if (!ecrit) {
            statut.setRollbackOnly();
        }
        return new BilanImportDto(type, simulation, ecrit, lue.lignes().size(), compter(lignes, ResultatImport.CREATION),
                compter(lignes, ResultatImport.MISE_A_JOUR), compter(lignes, ResultatImport.INCHANGEE), erreurs,
                List.copyOf(avertissements), List.copyOf(lignes));
    }

    private static int compter(List<LigneBilanDto> lignes, ResultatImport resultat) {
        return (int) lignes.stream().filter(l -> l.resultat() == resultat).count();
    }
}
