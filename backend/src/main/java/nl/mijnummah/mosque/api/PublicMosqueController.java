package nl.mijnummah.mosque.api;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
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
    @Operation(summary = "Actieve moskeeën, alfabetisch")
    PageResponse<MosqueResponse> list(@Parameter(description = "Zoekt in de naam")
                                      @RequestParam(required = false) String q,
                                      @Parameter(description = "Alleen moskeeën in deze plaats")
                                      @RequestParam(required = false) String city,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.unsorted());
        return PageResponse.of(mosqueService.publicSearch(q, city, pageable), MosqueResponse::from);
    }

    @GetMapping("/cities")
    @Operation(summary = "Plaatsen met minstens één actieve moskee, alfabetisch")
    List<String> cities() {
        return mosqueService.publicCities();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Profiel van een actieve moskee")
    MosqueResponse get(@PathVariable String slug) {
        return MosqueResponse.from(mosqueService.publicProfile(slug));
    }
}
