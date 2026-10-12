package com.tsix.immocrm.domain.audit;

import java.util.UUID;

/** {@code ParentObjectType} / {@code ParentObjectId} d'une ligne enfant (ex. terrain d'une tranche). */
public record ParentAudit(String objectType, UUID id) {
}
