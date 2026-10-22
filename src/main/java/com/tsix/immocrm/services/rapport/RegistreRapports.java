package com.tsix.immocrm.services.rapport;

import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Tous les {@link Rapport} déclarés, par (module, code) ; un doublon fait échouer le démarrage. */
@Service
public class RegistreRapports {

    private final Map<ModuleRapport, Map<String, Rapport>> parModule = new EnumMap<>(ModuleRapport.class);

    public RegistreRapports(List<Rapport> rapports) {
        for (Rapport rapport : rapports) {
            Rapport autre = parModule.computeIfAbsent(rapport.module(), m -> new HashMap<>())
                    .putIfAbsent(rapport.code(), rapport);
            if (autre != null) {
                throw new IllegalStateException("Deux rapports %s/%s : %s et %s".formatted(rapport.module(),
                        rapport.code(), autre.getClass().getName(), rapport.getClass().getName()));
            }
        }
    }

    public Optional<Rapport> trouver(ModuleRapport module, String code) {
        return Optional.ofNullable(parModule.getOrDefault(module, Map.of()).get(code));
    }

    /** Rapports implémentés du module, par code. */
    public Map<String, Rapport> duModule(ModuleRapport module) {
        return Map.copyOf(parModule.getOrDefault(module, Map.of()));
    }
}
