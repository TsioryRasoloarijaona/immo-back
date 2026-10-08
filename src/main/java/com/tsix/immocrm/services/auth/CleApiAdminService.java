package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.dto.auth.CleApiCreeeDto;
import com.tsix.immocrm.dto.auth.CleApiDto;
import com.tsix.immocrm.dto.auth.CreerCleApiRequest;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.CleApiService;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Administration des clés d'API — {@code FEATURE-cle-api-n8n.md} §5. */
@Service
public class CleApiAdminService {

    private static final int LONGUEUR_MAX_NOM = 100;

    private final AppApiClientRepository clientRepository;
    private final AdmUserRepository userRepository;
    private final CleApiService cleApiService;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public CleApiAdminService(AppApiClientRepository clientRepository, AdmUserRepository userRepository,
                              CleApiService cleApiService, UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.clock = clock;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.cleApiService = cleApiService;
        this.utilisateurAffiche = utilisateurAffiche;
    }

    @Transactional(readOnly = true)
    public List<CleApiDto> lister() {
        return clientRepository.findAll(Sort.by(Sort.Direction.DESC, "userCreateDateTime")).stream()
                .map(CleApiAdminService::toDto)
                .toList();
    }

    /**
     * Sans {@code compteCode} : compte de service sans employé, rôle ni habilitation — donc sans
     * restriction de périmètre ; la portée est sa seule limite, d'où « au moins une ».
     */
    @Transactional
    public CleApiCreeeDto creer(CreerCleApiRequest request) {
        String nom = request.nom() != null ? request.nom().trim() : "";
        if (!StringUtils.hasText(nom)) {
            throw new InvalidRequestException("nom est requis");
        }
        if (nom.length() > LONGUEUR_MAX_NOM) {
            throw new InvalidRequestException("nom dépasse " + LONGUEUR_MAX_NOM + " caractères");
        }
        Set<PorteeCleApi> portees = portees(request.portees());
        if (clientRepository.existsByNomIgnoreCase(nom)) {
            throw new ConflictException("Une clé d'API nommée « " + nom + " » existe déjà");
        }

        AdmUser compte;
        if (StringUtils.hasText(request.compteCode())) {
            compte = userRepository.findByCode(request.compteCode().trim())
                    .orElseThrow(() -> new InvalidRequestException(
                            "Compte AdmUser introuvable: " + request.compteCode()));
        } else {
            if (userRepository.findByCode(nom).isPresent()) {
                throw new ConflictException("Un compte AdmUser de code « " + nom
                        + " » existe déjà : fournir compteCode pour l'utiliser");
            }
            compte = creerCompteDeService(nom);
        }

        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom(nom);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(portees));
        client.setActif(true);
        client.setUserCreate(utilisateurAffiche.courant().nom());
        client.setUserCreateDateTime(LocalDateTime.now(clock));
        client = clientRepository.save(client);
        return new CleApiCreeeDto(toDto(client), cle);
    }

    /** Nouveau secret, même compte, même portée ; l'ancien cesse dès le commit. */
    @Transactional
    public CleApiCreeeDto regenerer(UUID id) {
        AppApiClient client = charger(id);
        if (!Boolean.TRUE.equals(client.getActif())) {
            throw new ConflictException("La clé « " + client.getNom() + " » est révoquée : en créer une nouvelle");
        }
        String cle = cleApiService.genererCle();
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client = clientRepository.save(client);
        return new CleApiCreeeDto(toDto(client), cle);
    }

    /** Idempotent : révoquer une clé déjà révoquée ne change rien. */
    @Transactional
    public void revoquer(UUID id) {
        AppApiClient client = charger(id);
        if (!Boolean.TRUE.equals(client.getActif())) {
            return;
        }
        client.setActif(false);
        client.setRevoquePar(utilisateurAffiche.courant().nom());
        client.setRevoqueDateTime(LocalDateTime.now(clock));
        clientRepository.save(client);
    }

    private AppApiClient charger(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clé d'API introuvable: " + id));
    }

    private static Set<PorteeCleApi> portees(List<PorteeCleApi> demandees) {
        if (demandees == null || demandees.isEmpty()) {
            throw new InvalidRequestException("portees est requis : au moins une portée ("
                    + String.join(", ", java.util.Arrays.stream(PorteeCleApi.values()).map(Enum::name).toList()) + ")");
        }
        if (demandees.contains(null)) {
            throw new InvalidRequestException("portees contient une valeur nulle");
        }
        return EnumSet.copyOf(demandees);
    }

    private AdmUser creerCompteDeService(String nom) {
        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        compte.setCode(nom);
        // Description = l'auteur que UtilisateurAffiche écrira dans UserCreate.
        compte.setDescription(nom);
        compte.setDisabled(false);
        compte.setUserCreate(utilisateurAffiche.courant().nom());
        compte.setUserCreateDateTime(LocalDateTime.now(clock));
        return userRepository.save(compte);
    }

    private static CleApiDto toDto(AppApiClient c) {
        AdmUser compte = c.getAdmUser();
        return new CleApiDto(
                c.getId(),
                c.getNom(),
                c.getClePrefixe(),
                compte.getCode(),
                StringUtils.hasText(compte.getDescription()) ? compte.getDescription() : compte.getCode(),
                List.copyOf(PorteeCleApi.depuisColonne(c.getPortees())),
                Boolean.TRUE.equals(c.getActif()),
                c.getDernierUsage(),
                c.getUserCreateDateTime(),
                c.getUserCreate(),
                c.getRevoqueDateTime(),
                c.getRevoquePar());
    }
}
