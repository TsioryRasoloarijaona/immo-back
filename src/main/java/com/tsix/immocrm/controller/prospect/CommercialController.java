package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.dto.prospect.CommercialListItemDto;
import com.tsix.immocrm.services.prospect.CommercialService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/commerciaux")
public class CommercialController {

    private final CommercialService commercialService;

    public CommercialController(CommercialService commercialService) {
        this.commercialService = commercialService;
    }

    @GetMapping
    public List<CommercialListItemDto> findAll() {
        return commercialService.findAll();
    }
}
