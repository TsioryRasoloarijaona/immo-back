package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppUserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserCredentialRepository extends JpaRepository<AppUserCredential, UUID> {

    /**
     * {@code @Transactional(readOnly = true)} n'est pas décoratif ici : il décide de la base.
     *
     * <p>{@code JwtAuthenticationFilter} appelle cette méthode à <b>chaque requête
     * authentifiée</b>, et un filtre n'est pas un service — il ne porte aucune transaction. Or
     * la transactionnalité par défaut de {@code SimpleJpaRepository} ne couvre que les méthodes
     * CRUD héritées : une <b>requête dérivée</b> comme celle-ci s'exécute sans transaction. Le
     * drapeau {@code readOnly} vaut alors {@code false} et le routage de
     * {@code config/DataSourceConfig} envoie la lecture sur la <b>source de vérité</b> —
     * c'est-à-dire, en production, un aller-retour réseau sur chaque requête de l'application.
     *
     * <p>Le symptôme observé avant ce correctif était plus brutal qu'une lenteur : la connexion
     * lisait la copie et trouvait l'utilisateur, le filtre lisait la source et ne le trouvait
     * pas — <b>401 sur toute requête authentifiée</b>, avec un corps vide puisque le rejet
     * venait de Spring Security et non du gestionnaire d'exceptions.
     *
     * <p><b>Conséquence à connaître</b> : la révocation d'un jeton passe par {@code TokenVersion},
     * qui est <i>écrit</i> sur la source. Lire ici la copie ne l'invalide donc pas
     * instantanément — c'est {@code config/RemonteeLecture} qui referme la fenêtre, en
     * recopiant la ligne dès la validation de la transaction qui l'a modifiée.
     */
    @Transactional(readOnly = true)
    Optional<AppUserCredential> findByAdmUserId(UUID admUserId);

    /** Batch variant of {@link #findByAdmUserId} — avoids N+1 when listing several users at once. */
    List<AppUserCredential> findByAdmUserIdIn(List<UUID> admUserIds);
}
