package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.adm.AdmModule;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.repositories.adm.AdmModuleRepository;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.stl.StlCurrencyRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Référentiels lus par code, repli sur la PKey de production (FEATURE-interop-omran-ecritures.md §2.4). */
class ReferencesOmranTest {

    private final StrEntityRepository entites = Mockito.mock(StrEntityRepository.class);
    private final AdmModuleRepository modules = Mockito.mock(AdmModuleRepository.class);
    private final StlCurrencyRepository devises = Mockito.mock(StlCurrencyRepository.class);
    private final AdmParamsGuidRepository parametres = Mockito.mock(AdmParamsGuidRepository.class);
    private final ReferencesOmran references = new ReferencesOmran(entites, modules, devises, parametres);

    @Test
    void referentielsAbsents_replisDeProduction() {
        when(entites.findByCodeIn(anyCollection())).thenReturn(List.of());
        when(modules.findByCode(anyString())).thenReturn(List.of());
        when(devises.findByCode(anyString())).thenReturn(List.of());
        when(parametres.findById(anyString())).thenReturn(Optional.empty());

        assertAll(
                () -> assertThat(references.entiteDossier()).isEqualTo(ReferencesOmran.ENTITE_DOSSIER),
                () -> assertThat(references.entiteReglement(true)).isEqualTo(ReferencesOmran.ENTITE_REGLEMENT_ENTREE),
                () -> assertThat(references.entiteReglement(false)).isEqualTo(ReferencesOmran.ENTITE_REGLEMENT_SORTIE),
                () -> assertThat(references.moduleCommercial()).isEqualTo(ReferencesOmran.MODULE_COMMERCIAL),
                () -> assertThat(references.deviseParDefaut()).isEqualTo(ReferencesOmran.DEVISE_MAD));
    }

    @Test
    void referentielsPresents_lusParCode() {
        StrEntity sortie = Mockito.mock(StrEntity.class);
        when(sortie.getId()).thenReturn(UUID.randomUUID());
        when(entites.findByCodeIn(List.of(ReferencesOmran.CODE_ENTITE_REGLEMENT_SORTIE))).thenReturn(List.of(sortie));
        AdmModule module = Mockito.mock(AdmModule.class);
        when(module.getId()).thenReturn(UUID.randomUUID());
        when(modules.findByCode(ReferencesOmran.CODE_MODULE_COMMERCIAL)).thenReturn(List.of(module));
        StlCurrency mad = Mockito.mock(StlCurrency.class);
        when(mad.getId()).thenReturn(UUID.randomUUID());
        when(devises.findByCode(ReferencesOmran.CODE_DEVISE_MAD)).thenReturn(List.of(mad));
        when(parametres.findById(anyString())).thenReturn(Optional.empty());

        assertAll(
                () -> assertThat(references.entiteReglement(false)).isEqualTo(sortie.getId()),
                () -> assertThat(references.moduleCommercial()).isEqualTo(module.getId()),
                () -> assertThat(references.deviseParDefaut()).isEqualTo(mad.getId()));
    }

    @Test
    void deviseParDefaut_parametreDefaultCurrencyPrioritaire() {
        AdmParamsGuid parametre = Mockito.mock(AdmParamsGuid.class);
        UUID devise = UUID.randomUUID();
        when(parametre.getCodeValue()).thenReturn(devise);
        when(parametres.findById(ReferencesOmran.PARAMETRE_DEVISE)).thenReturn(Optional.of(parametre));

        assertThat(references.deviseParDefaut()).isEqualTo(devise);
    }
}
