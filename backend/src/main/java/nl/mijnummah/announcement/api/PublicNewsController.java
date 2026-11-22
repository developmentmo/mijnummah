package nl.mijnummah.announcement.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.announcement.application.AnnouncementService;
import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;

/** News of all mosques together, for the central app (D-060). */
@RestController
@RequestMapping("/api/v1/public/announcements")
@Tag(name = "Publiek: aankondigingen")
class PublicNewsController {

    static final int MAX_MOSQUES = 100;

    private final AnnouncementService announcementService;
    private final PublicAnnouncementResponses responses;

    PublicNewsController(AnnouncementService announcementService, PublicAnnouncementResponses responses) {
        this.announcementService = announcementService;
        this.responses = responses;
    }

    @GetMapping
    @Operation(summary = "Actuele aankondigingen van alle actieve moskeeën, nieuwste eerst",
            description = "Met mosque (herhaalbaar, slug) alleen die moskeeën, bijv. de gevolgde.")
    PageResponse<AnnouncementResponse> list(@Parameter(description = "Slug van een moskee; mag vaker voorkomen")
                                            @RequestParam(name = "mosque", required = false) List<String> mosques,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        List<String> slugs = mosques == null ? null : mosques.stream().filter(s -> s != null && !s.isBlank())
                .distinct().limit(MAX_MOSQUES).toList();
        var pageable = PageRequests.of(page, size, Sort.unsorted());
        return responses.page(announcementService.publicListAcross(slugs, pageable));
    }

    @GetMapping("/{announcementId}")
    @Operation(summary = "Eén aankondiging van een actieve moskee")
    AnnouncementResponse get(@PathVariable UUID announcementId) {
        return responses.one(announcementService.publicGet(announcementId));
    }
}
