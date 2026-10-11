package nl.mijnummah.announcement.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.announcement.application.AnnouncementService;
import nl.mijnummah.announcement.domain.AnnouncementStatus;
import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.identity.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/admin/mosques/{mosqueId}/announcements")
@Tag(name = "Beheer: aankondigingen")
class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    AdminAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    @Operation(summary = "Alle aankondigingen van de moskee (ook concepten), laatst gewijzigd eerst")
    PageResponse<AnnouncementResponse.Admin> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                                  @PathVariable UUID mosqueId,
                                                  @RequestParam(required = false) AnnouncementStatus status,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id")));
        return PageResponse.of(announcementService.list(actor, mosqueId, status, pageable),
                AnnouncementResponse.Admin::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Aankondiging aanmaken (als concept)")
    AnnouncementResponse.Admin create(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                      @Valid @RequestBody AnnouncementRequest request) {
        return AnnouncementResponse.Admin.from(announcementService.create(actor, mosqueId, request.toDetails()));
    }

    @GetMapping("/{announcementId}")
    @Operation(summary = "Aankondiging ophalen")
    AnnouncementResponse.Admin get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                   @PathVariable UUID announcementId) {
        return AnnouncementResponse.Admin.from(announcementService.get(actor, mosqueId, announcementId));
    }

    @PutMapping("/{announcementId}")
    @Operation(summary = "Aankondiging wijzigen")
    AnnouncementResponse.Admin update(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                      @PathVariable UUID announcementId,
                                      @Valid @RequestBody AnnouncementRequest request) {
        return AnnouncementResponse.Admin.from(announcementService.update(actor, mosqueId, announcementId,
                request.toDetails()));
    }

    @PostMapping("/{announcementId}/publish")
    @Operation(summary = "Publiceren in de app (verstuurt geen pushmelding)")
    AnnouncementResponse.Admin publish(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                       @PathVariable UUID announcementId) {
        return AnnouncementResponse.Admin.from(announcementService.publish(actor, mosqueId, announcementId));
    }

    @PostMapping("/{announcementId}/unpublish")
    @Operation(summary = "Terug naar concept")
    AnnouncementResponse.Admin unpublish(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                         @PathVariable UUID announcementId) {
        return AnnouncementResponse.Admin.from(announcementService.unpublish(actor, mosqueId, announcementId));
    }

    @DeleteMapping("/{announcementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Aankondiging verwijderen")
    void delete(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                @PathVariable UUID announcementId) {
        announcementService.delete(actor, mosqueId, announcementId);
    }
}
