package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.dto.achat.config.StatutAchatConfigDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import com.tsix.immocrm.services.achat.config.ConfigurationAchatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Achat — configuration")
@RestController
@RequestMapping("/api/achat/config")
public class ConfigurationAchatController {

    private final ConfigurationAchatService service;

    public ConfigurationAchatController(ConfigurationAchatService service) {
        this.service = service;
    }

    @GetMapping("/statuts")
    @Operation(summary = "Statuts des pièces d'achat, par ordre puis code (lecture seule, tout compte connecté)")
    public List<StatutAchatConfigDto> statuts() {
        return service.statuts();
    }

    @GetMapping("/transitions")
    @Operation(summary = "Transitions des pièces d'achat par gestion, avec rôles et usage réel (lecture seule)")
    public List<TransitionAchatDto> transitions(
            @Parameter(description = "true = seulement les transitions déjà empruntées (nombrePassages > 0).")
            @RequestParam(defaultValue = "false") boolean utilisees) {
        return service.transitions(utilisees);
    }
}
