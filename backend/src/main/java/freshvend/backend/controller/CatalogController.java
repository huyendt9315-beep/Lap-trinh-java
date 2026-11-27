package freshvend.backend.controller;

import freshvend.backend.service.CatalogService;
import freshvend.backend.service.CatalogService.CatalogItem;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/machines")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping("/{machineId}/catalog")
    public List<CatalogItem> getCatalog(
            @PathVariable Long machineId) {

        return service.getCatalog(machineId);
    }
}