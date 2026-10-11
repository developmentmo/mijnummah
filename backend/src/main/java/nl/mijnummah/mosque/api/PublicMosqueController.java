package nl.mijnummah.mosque.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.mosque.application.MosqueService;

@RestController
@RequestMapping("/api/v1/public/mosques")
@Tag(name = "Publiek: moskeeën")
class PublicMosqueController {

    private final MosqueService mosqueService;

    PublicMosqueController(MosqueService mosqueService) {
        this.mosqueService = mosqueService;
    }

    @GetMapping
    @Operation(summary = "Alle actieve moskeeën")
    List<MosqueResponse> list() {
        return mosqueService.publicList().stream().map(MosqueResponse::from).toList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Profiel van een actieve moskee")
    MosqueResponse get(@PathVariable String slug) {
        return MosqueResponse.from(mosqueService.publicProfile(slug));
    }
}
