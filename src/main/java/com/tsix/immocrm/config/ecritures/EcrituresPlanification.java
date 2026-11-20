package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les tiers, les lots et les types de tâche que les planifications citent (FEATURE-n9-planification.md N9b). */
public class EcrituresPlanification implements TablesEcrites {

    @Override
    public int rang() {
        return 1220;
    }

    @Override
    public List<String> tables() {
        return List.of("CfgGanttProject", "CfgGanttOwner", "CfgGanttTask");
    }
}
