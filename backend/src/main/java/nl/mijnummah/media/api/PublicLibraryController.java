package nl.mijnummah.media.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.media.application.PublicMediaService;
import nl.mijnummah.media.domain.MediaCategory;

/** The media libraries of all mosques together, for the central app (D-060). */
@RestController
@RequestMapping("/api/v1/public/media")
@Tag(name = "Publiek: lezingen en media")
class PublicLibraryController {

    static final int MAX_MOSQUES = 100;

    private final PublicMediaService mediaService;
    private final PublicMediaResponses responses;

    PublicLibraryController(PublicMediaService mediaService, PublicMediaResponses responses) {
        this.mediaService = mediaService;
        this.responses = responses;
    }

    @GetMapping
    @Operation(summary = "Lezingen en video's van alle actieve moskeeën, nieuwste eerst",
            description = "Met mosque (herhaalbaar, slug) alleen die moskeeën.")
    PageResponse<MediaResponse> list(@AuthenticationPrincipal AuthenticatedUser viewer,
                                     @Parameter(description = "Slug van een moskee; mag vaker voorkomen")
                                     @RequestParam(name = "mosque", required = false) List<String> mosques,
                                     @Parameter(description = "Zoekt in titel en spreker")
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) MediaCategory category,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.unsorted());
        return responses.page(mediaService.listAcross(limit(mosques), viewer, q, category, pageable));
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Eén media-item van een actieve moskee")
    MediaResponse get(@AuthenticationPrincipal AuthenticatedUser viewer, @PathVariable UUID mediaId) {
        return responses.one(mediaService.get(mediaId, viewer));
    }

    @GetMapping("/{mediaId}/related")
    @Operation(summary = "Gerelateerde media van dezelfde moskee (maximaal 4)")
    List<MediaResponse> related(@AuthenticationPrincipal AuthenticatedUser viewer, @PathVariable UUID mediaId) {
        return responses.list(mediaService.related(mediaId, viewer));
    }

    static List<String> limit(List<String> slugs) {
        return slugs == null ? null : slugs.stream().filter(s -> s != null && !s.isBlank()).distinct()
                .limit(MAX_MOSQUES).toList();
    }
}
