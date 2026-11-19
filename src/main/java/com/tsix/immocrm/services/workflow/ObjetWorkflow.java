package com.tsix.immocrm.services.workflow;

import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;

import java.util.UUID;

/** Objet porté par un workflow {@code Str*} (lot, demande interne d'achat) : statut et entité relus en base. */
public interface ObjetWorkflow {

    UUID getId();

    StrEntity getStrEntity();

    StrStatus getStrStatus();

    void setStrStatus(StrStatus statut);
}
