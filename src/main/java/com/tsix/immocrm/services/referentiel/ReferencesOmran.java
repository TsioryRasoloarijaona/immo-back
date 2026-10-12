package com.tsix.immocrm.services.referentiel;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmModule;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.repositories.adm.AdmModuleRepository;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.stl.StlCurrencyRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Identifiants de référence qu'OMRAN écrit sur ses dossiers, échéances et règlements : résolus par
 * {@code Code}, repli sur la PKey de production (identique entre bases) quand la ligne manque.
 * Voir {@code FEATURE-interop-omran-ecritures.md} §2.4.
 */
@Component
public class ReferencesOmran {

    public static final String CODE_ENTITE_DOSSIER = "ComFolder";
    public static final String CODE_ENTITE_ENCAISSEMENT = "StlSettlementIn";
    public static final String CODE_ENTITE_DECAISSEMENT = "StlSettlementOut";
    public static final String CODE_MODULE_VENTE = "COM";
    public static final String CODE_DEVISE_REPLI = "MAD";
    public static final String PARAMETRE_DEVISE_PAR_DEFAUT = "DefaultCurrency";

    public static final UUID ID_ENTITE_DOSSIER = StatutDossier.CONFIG_ENTITY_ID;
    public static final UUID ID_ENTITE_ENCAISSEMENT = UUID.fromString("D7061C59-A039-4EF8-A6ED-B0E886F8EDFF");
    public static final UUID ID_ENTITE_DECAISSEMENT = UUID.fromString("8287D7A9-2D51-4F2E-8F47-0F1B738F1A11");
    public static final UUID ID_MODULE_VENTE = UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00001");
    public static final UUID ID_DEVISE_MAD = UUID.fromString("24EB36A2-2E81-4A12-AF02-6259CFA25BD6");

    private final StrEntityRepository strEntityRepository;
    private final AdmModuleRepository admModuleRepository;
    private final StlCurrencyRepository currencyRepository;
    private final AdmParamsGuidRepository paramsGuidRepository;

    public ReferencesOmran(StrEntityRepository strEntityRepository, AdmModuleRepository admModuleRepository,
                           StlCurrencyRepository currencyRepository, AdmParamsGuidRepository paramsGuidRepository) {
        this.strEntityRepository = strEntityRepository;
        this.admModuleRepository = admModuleRepository;
        this.currencyRepository = currencyRepository;
        this.paramsGuidRepository = paramsGuidRepository;
    }

    /** {@code StrEntity} « Dossier de vente » : sans elle, le dossier sort de {@code ComSaleFolderView}. */
    public UUID entiteDossier() {
        return entite(CODE_ENTITE_DOSSIER, ID_ENTITE_DOSSIER);
    }

    /** {@code StrEntity} d'un règlement : encaissement si crédit, décaissement sinon. */
    public UUID entiteReglement(boolean credit) {
        return credit
                ? entite(CODE_ENTITE_ENCAISSEMENT, ID_ENTITE_ENCAISSEMENT)
                : entite(CODE_ENTITE_DECAISSEMENT, ID_ENTITE_DECAISSEMENT);
    }

    /** {@code AdmModule} COM, porté par toute échéance et tout règlement de vente. */
    public UUID moduleVente() {
        return premier(admModuleRepository.findByCode(CODE_MODULE_VENTE)).map(AdmModule::getId)
                .orElse(ID_MODULE_VENTE);
    }

    /** Paramètre {@code DefaultCurrency}, sinon la devise MAD. */
    public UUID deviseParDefaut() {
        return paramsGuidRepository.findById(PARAMETRE_DEVISE_PAR_DEFAUT)
                .map(AdmParamsGuid::getCodeValue)
                .or(() -> premier(currencyRepository.findByCode(CODE_DEVISE_REPLI)).map(StlCurrency::getId))
                .orElse(ID_DEVISE_MAD);
    }

    private UUID entite(String code, UUID repli) {
        return premier(strEntityRepository.findByCodeIn(List.of(code))).map(StrEntity::getId).orElse(repli);
    }

    private static <T> Optional<T> premier(List<T> lignes) {
        return lignes.stream().findFirst();
    }
}
