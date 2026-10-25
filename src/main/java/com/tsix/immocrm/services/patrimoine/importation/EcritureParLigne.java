package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Écriture ligne par ligne des types à une ligne de bilan par ligne Excel (articles, présentations, terrains). */
final class EcritureParLigne {

    private static final Logger log = LoggerFactory.getLogger(EcritureParLigne.class);

    private EcritureParLigne() {
    }

    /** Ligne traduite ; {@code cle} non nulle = clé de rapprochement, une seconde ligne de même clé est refusée (I8). */
    interface LigneImport {
        int numero();

        String pivot();

        String code();

        List<String> erreurs();

        default String cle() {
            return null;
        }
    }

    /** Libellés OMRAN du bilan, {@code %s} = code de la ligne. */
    record Libelles(String creation, String miseAJour, String inchangee, String erreur) {

        String message(ResultatImport resultat, String code) {
            String format = switch (resultat) {
                case CREATION -> creation;
                case MISE_A_JOUR -> miseAJour;
                case INCHANGEE -> inchangee;
                default -> erreur;
            };
            return format.formatted(code);
        }
    }

    static <L extends LigneImport> List<LigneBilanDto> ecrire(EntityManager em, List<L> traduites,
                                                              Function<L, ResultatImport> ecriture, Libelles libelles) {
        List<LigneBilanDto> lignes = new ArrayList<>();
        Set<String> cles = new HashSet<>();
        Integer interrompue = null;
        for (L l : traduites) {
            if (interrompue != null) {
                lignes.add(new LigneBilanDto(l.numero(), l.pivot(), l.code(), ResultatImport.NON_VERIFIEE,
                        "Non vérifiée : import interrompu à la ligne " + interrompue));
                continue;
            }
            try {
                exigerValide(l, cles);
                ResultatImport resultat = ecriture.apply(l);
                em.flush();
                lignes.add(new LigneBilanDto(l.numero(), l.pivot(), l.code(), resultat, libelles.message(resultat, l.code())));
            } catch (InvalidRequestException | ConflictException | ResourceNotFoundException e) {
                // Une écriture refusée après des setters laisserait l'entité sale pour la ligne suivante.
                em.clear();
                lignes.add(erreur(l, libelles, e.getMessage()));
            } catch (PersistenceException | DataAccessException e) {
                log.warn("Import interrompu à la ligne {}", l.numero(), e);
                interrompue = l.numero();
                lignes.add(erreur(l, libelles, "écriture refusée par la base, import interrompu"));
            }
        }
        return lignes;
    }

    private static void exigerValide(LigneImport l, Set<String> cles) {
        if (!l.erreurs().isEmpty()) {
            throw new InvalidRequestException(String.join(" ; ", l.erreurs()));
        }
        if (l.cle() != null && !cles.add(l.cle())) {
            throw new InvalidRequestException("Ligne en double dans le fichier : même clé qu'une ligne précédente");
        }
    }

    private static LigneBilanDto erreur(LigneImport l, Libelles libelles, String detail) {
        return new LigneBilanDto(l.numero(), l.pivot(), l.code(), ResultatImport.ERREUR,
                libelles.message(ResultatImport.ERREUR, l.code()) + " : " + detail);
    }
}
