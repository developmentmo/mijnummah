package nl.mijnummah.announcement.api;

import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.announcement.application.AnnouncementService;
import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;

@RestController
@RequestMapping("/api/v1/public/mosques/{slug}/announcements")
@Tag(name = "Publiek: aankondigingen")
class PublicAnnouncementController {

    private final AnnouncementService announcementService;
    private final PublicAnnouncementResponses responses;

    PublicAnnouncementController(AnnouncementService announcementService, PublicAnnouncementResponses responses) {
        this.announcementService = announcementService;
        this.responses = responses;
    }

    @GetMapping
    @Operation(summary = "Actuele aankondigingen: uitgelicht eerst, dan nieuwste eerst; verlopen worden niet getoond")
    PageResponse<AnnouncementResponse> list(@PathVariable String slug,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.unsorted());
        return responses.page(announcementService.publicList(slug, pageable));
    }

    @GetMapping("/{announcementId}")
    @Operation(summary = "Eén aankondiging")
    AnnouncementResponse get(@PathVariable String slug, @PathVariable UUID announcementId) {
        return responses.one(announcementService.publicGet(slug, announcementId));
    }
}
