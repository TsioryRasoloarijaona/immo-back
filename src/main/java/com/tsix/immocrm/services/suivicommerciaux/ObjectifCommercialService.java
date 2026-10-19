package com.tsix.immocrm.services.suivicommerciaux;

import com.tsix.immocrm.entities.app.AppParametre;
import com.tsix.immocrm.repositories.app.AppParametreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Objectif de personnes jointes par jour (FEATURE-suivi-commerciaux.md R7) : ligne {@code AppParametre}, 60 si absente. */
@Service
@Transactional(readOnly = true)
public class ObjectifCommercialService {

    static final String CLE = "objectifPersonnesJointesJour";
    static final int DEFAUT = 60;

    private final AppParametreRepository repository;

    public ObjectifCommercialService(AppParametreRepository repository) {
        this.repository = repository;
    }

    public int objectif() {
        return repository.findByCle(CLE).map(p -> Integer.parseInt(p.getValeur())).orElse(DEFAUT);
    }

    @Transactional
    public int modifierObjectif(int valeur) {
        AppParametre parametre = repository.findByCle(CLE).orElseGet(() -> {
            AppParametre p = new AppParametre();
            p.setId(UUID.randomUUID());
            p.setCle(CLE);
            return p;
        });
        parametre.setValeur(Integer.toString(valeur));
        repository.save(parametre);
        return valeur;
    }
}
