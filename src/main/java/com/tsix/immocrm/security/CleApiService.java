package com.tsix.immocrm.security;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Génération, hachage et résolution des clés d'API (FEATURE-cle-api-n8n.md §2). SHA-256 et non BCrypt : 256 bits
 * aléatoires rendent le dictionnaire inutile, et un hash déterministe permet la recherche par égalité à chaque requête.
 */
@Service
public class CleApiService {

    public static final String PREFIXE = "ak_";
    /** Longueur de {@code ClePrefixe} : {@code ak_} + 8 caractères. */
    private static final int LONGUEUR_PREFIXE_AFFICHE = PREFIXE.length() + 8;
    /** Ne touche {@code DernierUsage} qu'une fois par minute — pas une écriture par requête n8n. */
    private static final int SECONDES_ENTRE_DEUX_TRACES = 60;

    private final AppApiClientRepository clientRepository;
    private final AdmUserRoleRepository userRoleRepository;
    private final SecureRandom aleatoire = new SecureRandom();

    public CleApiService(AppApiClientRepository clientRepository, AdmUserRoleRepository userRoleRepository) {
        this.clientRepository = clientRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /** Une clé neuve, en clair — à montrer une seule fois. */
    public String genererCle() {
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        return PREFIXE + Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    public static String hacher(String cle) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(cle.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    public static String prefixeAffiche(String cle) {
        return cle.length() <= LONGUEUR_PREFIXE_AFFICHE ? cle : cle.substring(0, LONGUEUR_PREFIXE_AFFICHE);
    }

    /** Le client, son principal et sa portée, résolus d'une clé en clair. */
    public record ClientResolu(AppApiClient client, AuthenticatedUser principal, List<String> roles,
                               Set<PorteeCleApi> portees) {
    }

    /**
     * Vide si la clé est inconnue, révoquée ou son compte désactivé : l'appelant rend 401 sans dire lequel.
     * Rôles relus dans {@code AdmUserRole} à chaque requête, pas figés à la création de la clé.
     */
    @Transactional(readOnly = true)
    public Optional<ClientResolu> resoudre(String cle) {
        if (cle == null || !cle.startsWith(PREFIXE)) {
            return Optional.empty();
        }
        return clientRepository.findByCleHash(hacher(cle.trim()))
                .filter(c -> Boolean.TRUE.equals(c.getActif()))
                .filter(c -> !Boolean.TRUE.equals(c.getAdmUser().getDisabled()))
                .map(c -> {
                    AdmUser compte = c.getAdmUser();
                    List<String> roles = userRoleRepository.findByAdmUserId(compte.getId()).stream()
                            .map(ur -> ur.getAdmRole() != null ? ur.getAdmRole().getCode() : null)
                            .filter(Objects::nonNull)
                            .toList();
                    AuthenticatedUser principal = new AuthenticatedUser(
                            compte.getId(),
                            compte.getCode(),
                            compte.getCfgEmployee() != null ? compte.getCfgEmployee().getId() : null,
                            false,
                            true,
                            0);
                    return new ClientResolu(c, principal, roles, PorteeCleApi.depuisColonne(c.getPortees()));
                });
    }

    public void tracerUsage(AppApiClient client) {
        LocalDateTime maintenant = LocalDateTime.now();
        if (client.getDernierUsage() == null
                || client.getDernierUsage().plusSeconds(SECONDES_ENTRE_DEUX_TRACES).isBefore(maintenant)) {
            clientRepository.toucher(client.getId(), maintenant);
        }
    }
}
