package com.tsix.immocrm.dto.prospection;

/**
 * Body for {@code POST /api/prospections/{id}/interactions}. {@code description} is required;
 * {@code entrante} defaults to {@code true} (incoming) when omitted.
 */
public record CreerInteractionRequest(
        String description,
        Boolean entrante
) {
}
