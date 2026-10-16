package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmModule;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.repositories.adm.AdmModuleRepository;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.stl.StlCurrencyRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Identifiants de référentiel qu'OMRAN écrit sur ses dossiers, échéances et règlements : lus par
 * {@code Code}, avec repli sur la PKey de production quand la ligne manque. Voir
 * {@code FEATURE-interop-omran-ecritures.md} §2.4.
 */
@Component
public class ReferencesOmran {

    public static final UUID ENTITE_DOSSIER = StatutDossier.CONFIG_ENTITY_ID;
    public static final UUID ENTITE_REGLEMENT_ENTREE = UUID.fromString("D7061C59-A039-4EF8-A6ED-B0E886F8EDFF");
    public static final UUID ENTITE_REGLEMENT_SORTIE = UUID.fromString("8287D7A9-2D51-4F2E-8F47-0F1B738F1A11");
    public static final UUID ENTITE_OFFRE_VENTE = UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B602");
    public static final UUID MODULE_COMMERCIAL =UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00001");
    public static final UUID DEVISE_MAD = UUID.fromString("24EB36A2-2E81-4A12-AF02-6259CFA25BD6");

    static final String CODE_ENTITE_DOSSIER = "ComFolder";
    static final String CODE_ENTITE_REGLEMENT_ENTREE = "StlSettlementIn";
    static final String CODE_ENTITE_REGLEMENT_SORTIE = "StlSettlementOut";
    static final String CODE_ENTITE_OFFRE_VENTE = "ComQuote";
    static final String CODE_MODULE_COMMERCIAL = "COM";
    static final String CODE_DEVISE_MAD = "MAD";
    static final String PARAMETRE_DEVISE = "DefaultCurrency";

    private final StrEntityRepository strEntityRepository;
    private final AdmModuleRepository moduleRepository;
    private final StlCurrencyRepository deviseRepository;
    private final AdmParamsGuidRepository parametreRepository;

    public ReferencesOmran(StrEntityRepository strEntityRepository, AdmModuleRepository moduleRepository,
                           StlCurrencyRepository deviseRepository, AdmParamsGuidRepository parametreRepository) {
        this.strEntityRepository = strEntityRepository;
        this.moduleRepository = moduleRepository;
        this.deviseRepository = deviseRepository;
        this.parametreRepository = parametreRepository;
    }

    /** {@code StrEntity} « Dossier de vente » ({@code ComFolder.StrEntityId}). */
    public UUID entiteDossier() {
        return entite(CODE_ENTITE_DOSSIER, ENTITE_DOSSIER);
    }

    /** {@code StlSettlementIn} pour un encaissement, {@code StlSettlementOut} pour un décaissement. */
    public UUID entiteReglement(boolean credit) {
        return credit
                ? entite(CODE_ENTITE_REGLEMENT_ENTREE, ENTITE_REGLEMENT_ENTREE)
                : entite(CODE_ENTITE_REGLEMENT_SORTIE, ENTITE_REGLEMENT_SORTIE);
    }

    /** {@code StrEntity} « Offre vente » : sans elle, OMRAN ne liste pas l'offre ({@code StlQuote.StrEntityId}). */
    public UUID entiteOffreVente() {
        return entite(CODE_ENTITE_OFFRE_VENTE, ENTITE_OFFRE_VENTE);
    }

    /** {@code AdmModule} « COM », module des échéances et règlements de vente. */
    public UUID moduleCommercial() {
        return premier(moduleRepository.findByCode(CODE_MODULE_COMMERCIAL).stream().map(AdmModule::getId).toList())
                .orElse(MODULE_COMMERCIAL);
    }

    /** Paramètre {@code DefaultCurrency}, sinon la devise « MAD », sinon sa PKey de production. */
    public UUID deviseParDefaut() {
        return parametreRepository.findById(PARAMETRE_DEVISE).map(AdmParamsGuid::getCodeValue)
                .or(() -> premier(deviseRepository.findByCode(CODE_DEVISE_MAD).stream()
                        .map(StlCurrency::getId).toList()))
                .orElse(DEVISE_MAD);
    }

    private UUID entite(String code, UUID repli) {
        return premier(strEntityRepository.findByCodeIn(List.of(code)).stream().map(StrEntity::getId).toList())
                .orElse(repli);
    }

    private static Optional<UUID> premier(List<UUID> ids) {
        return ids.stream().filter(Objects::nonNull).findFirst();
    }
}
