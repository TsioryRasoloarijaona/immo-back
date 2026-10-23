package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.importation.BilanImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.services.patrimoine.importation.ImportArticles.Contexte;
import com.tsix.immocrm.services.patrimoine.importation.ImportArticles.LigneTraduite;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.Feuille;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Import d'un classeur (FEATURE-patrimoine-import-export.md §1.3) : lecture hors transaction, puis traduction de toutes
 * les lignes et écriture ligne par ligne dans <b>une</b> transaction, annulée à la moindre erreur ou en simulation.
 */
@Service
public class ImportPatrimoineService {

    private static final Logger log = LoggerFactory.getLogger(ImportPatrimoineService.class);
    static final String ARTICLES = "articles";
    static final String SOUS_ARTICLES = "sous-articles";

    private final TrancheVisibleRepository tranches;
    private final ImportArticles importArticles;
    private final EntityManager em;
    private final TransactionTemplate transaction;

    public ImportPatrimoineService(TrancheVisibleRepository tranches, ImportArticles importArticles, EntityManager em,
                                   PlatformTransactionManager transactions) {
        this.tranches = tranches;
        this.importArticles = importArticles;
        this.em = em;
        this.transaction = new TransactionTemplate(transactions);
    }

    /** {@code articles} → faux, {@code sous-articles} → vrai ; autre type → 404. */
    static boolean sousArticles(String type) {
        return switch (type) {
            case ARTICLES -> false;
            case SOUS_ARTICLES -> true;
            default -> throw new ResourceNotFoundException("Type d'import inconnu : " + type);
        };
    }

    /** Message OMRAN de {@code ItemList:Import}. */
    static UUID exigerTranche(UUID trancheId) {
        if (trancheId == null) {
            throw new InvalidRequestException("Vous devez sélectionner une Tranche !");
        }
        return trancheId;
    }

    public BilanImportDto importer(String type, UUID trancheId, MultipartFile fichier, String feuille, boolean simulation) {
        boolean sous = sousArticles(type);
        tranches.exigerVisible(exigerTranche(trancheId));
        Feuille lue = LectureClasseur.lire(fichier, feuille);
        return transaction.execute(statut -> executer(type, sous, trancheId, lue, simulation, statut));
    }

    private BilanImportDto executer(String type, boolean sous, UUID trancheId, Feuille lue, boolean simulation,
                                    TransactionStatus statut) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        List<String> avertissements = new ArrayList<>();
        Contexte ctx = new Contexte(sous, tranche, new ResolutionCodes(em, trancheId), lue.entetes(), avertissements);
        List<String> connus = ctx.champs().stream().map(ChampsArticle.ChampImport::entete).toList();
        lue.entetes().stream().filter(e -> !connus.contains(e))
                .forEach(e -> avertissements.add("Colonne « " + e + " » ignorée : en-tête inconnu."));
        List<LigneTraduite> traduites = lue.lignes().stream().map(l -> importArticles.traduire(l, ctx)).toList();
        Libelles libelles = sous ? Libelles.SOUS_ARTICLE : Libelles.ARTICLE;
        List<LigneBilanDto> lignes = new ArrayList<>();
        Integer interrompue = null;
        for (LigneTraduite l : traduites) {
            if (interrompue != null) {
                lignes.add(new LigneBilanDto(l.numero(), l.pivot(), l.code(), ResultatImport.NON_VERIFIEE,
                        "Non vérifiée : import interrompu à la ligne " + interrompue));
                continue;
            }
            try {
                ResultatImport resultat = importArticles.ecrire(l, ctx);
                em.flush();
                lignes.add(new LigneBilanDto(l.numero(), l.pivot(), l.code(), resultat, libelles.message(resultat, l.code())));
            } catch (InvalidRequestException | ConflictException | ResourceNotFoundException e) {
                // Une écriture refusée après des setters laisserait l'entité sale pour la ligne suivante.
                em.clear();
                lignes.add(erreur(l, libelles, e.getMessage()));
            } catch (PersistenceException | DataAccessException e) {
                log.warn("Import {} interrompu à la ligne {}", type, l.numero(), e);
                interrompue = l.numero();
                lignes.add(erreur(l, libelles, "écriture refusée par la base, import interrompu"));
            }
        }
        return bilan(type, simulation, lignes, avertissements, statut);
    }

    private static LigneBilanDto erreur(LigneTraduite l, Libelles libelles, String detail) {
        return new LigneBilanDto(l.numero(), l.pivot(), l.code(), ResultatImport.ERREUR,
                libelles.message(ResultatImport.ERREUR, l.code()) + " : " + detail);
    }

    private static BilanImportDto bilan(String type, boolean simulation, List<LigneBilanDto> lignes,
                                        List<String> avertissements, TransactionStatus statut) {
        int erreurs = compter(lignes, ResultatImport.ERREUR) + compter(lignes, ResultatImport.NON_VERIFIEE);
        boolean ecrit = erreurs == 0 && !simulation;
        if (!ecrit) {
            statut.setRollbackOnly();
        }
        return new BilanImportDto(type, simulation, ecrit, lignes.size(), compter(lignes, ResultatImport.CREATION),
                compter(lignes, ResultatImport.MISE_A_JOUR), compter(lignes, ResultatImport.INCHANGEE), erreurs,
                List.copyOf(avertissements), List.copyOf(lignes));
    }

    private static int compter(List<LigneBilanDto> lignes, ResultatImport resultat) {
        return (int) lignes.stream().filter(l -> l.resultat() == resultat).count();
    }

    /** Libellés du bilan OMRAN ({@code ImportItems}, {@code ImportUnderItems}). */
    private enum Libelles {
        ARTICLE("de l'article", "l'article"), SOUS_ARTICLE("du sous-article", "le sous-article");

        private final String de;
        private final String le;

        Libelles(String de, String le) {
            this.de = de;
            this.le = le;
        }

        String message(ResultatImport resultat, String code) {
            return switch (resultat) {
                case CREATION -> "Création " + de + " : " + code;
                case MISE_A_JOUR -> "Mise à jour " + de + " : " + code;
                case INCHANGEE -> le + " : " + code + " n'a pas été modifié";
                default -> "Problèmes d'import " + de + " : " + code;
            };
        }
    }
}
