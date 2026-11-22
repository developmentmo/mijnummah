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

@RestController
@RequestMapping("/api/v1/public/mosques/{slug}/media")
@Tag(name = "Publiek: lezingen en media")
class PublicMediaController {

    private final PublicMediaService mediaService;
    private final PublicMediaResponses responses;

    PublicMediaController(PublicMediaService mediaService, PublicMediaResponses responses) {
        this.mediaService = mediaService;
        this.responses = responses;
    }

    @GetMapping
    @Operation(summary = "Mediabibliotheek, nieuwste eerst")
    PageResponse<MediaResponse> list(@AuthenticationPrincipal AuthenticatedUser viewer,
                                     @PathVariable String slug,
                                     @Parameter(description = "Zoekt in titel en spreker")
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) MediaCategory category,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.unsorted());
        return responses.page(mediaService.list(slug, viewer, q, category, pageable));
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Eén media-item")
    MediaResponse get(@AuthenticationPrincipal AuthenticatedUser viewer, @PathVariable String slug,
                      @PathVariable UUID mediaId) {
        return responses.one(mediaService.get(slug, mediaId, viewer));
    }

    @GetMapping("/{mediaId}/related")
    @Operation(summary = "Gerelateerde media (zelfde categorie of spreker, maximaal 4)")
    List<MediaResponse> related(@AuthenticationPrincipal AuthenticatedUser viewer, @PathVariable String slug,
                                @PathVariable UUID mediaId) {
        return responses.list(mediaService.related(slug, mediaId, viewer));
    }
}
