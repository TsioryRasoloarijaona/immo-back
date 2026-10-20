package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.stk.StkExpense;
import com.tsix.immocrm.entities.stk.StkExpenseType;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.stk.StkExpenseRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.SuppressionArticle;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.terrain.TerrainsVisibles.exigerSousTerrainsAutorises;

/**
 * Suppression d'un terrain (décision 8 bis n° 2) : un frais validé bloque (compte explicite, la FK
 * {@code BuyDocument → StkExpense} manque à yesRemsTest) ; les autres partent avec lui, puis {@link SuppressionArticle}.
 */
@Component
record SuppressionTerrain(EntityManager em, StkItemRepository items, StkExpenseRepository depenses,
                          FraisFactures factures, SuppressionArticle suppression) {

    static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    void supprimer(StkItem terrain) {
        if (terrain.getParent() != null) {
            exigerSousTerrainsAutorises(terrain.getParent());
        }
        List<StkItem> objets = new ArrayList<>(items.findByParentId(terrain.getId()));
        objets.add(terrain);
        if (objets.stream().anyMatch(ArticleService::estOccupe)) {
            throw new ConflictException(("Suppression impossible : le terrain %s ou un sous-terrain est affecté à un "
                    + "dossier.").formatted(terrain.getCode()));
        }
        List<StkExpense> frais = objets.stream()
                .flatMap(o -> depenses.findByStkItemIdOrderByDateAscIdAsc(o.getId()).stream()).toList();
        Map<UUID, BuyDocument> facturees = factures.parFrais(frais);
        frais.stream().filter(f -> FraisFactures.valide(f, facturees)).findFirst().ifPresent(f -> {
            throw new ConflictException("Suppression impossible : frais %s du %s (%s) validé.".formatted(
                    Champs.de(f.getStkExpenseType(), StkExpenseType::getDescription),
                    f.getDate() == null ? "" : f.getDate().format(JOUR), Objects.toString(f.getAmount(), "")));
        });
        frais.forEach(em::remove);
        em.flush();
        suppression.supprimer(terrain);
    }
}
