package nl.mijnummah.media.api;

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

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.notification.PushNotificationView;
import nl.mijnummah.notification.PushText;
import nl.mijnummah.media.application.MediaAdminService;
import nl.mijnummah.media.domain.MediaStatus;

@RestController
@RequestMapping("/api/v1/admin/mosques/{mosqueId}/media")
@Tag(name = "Beheer: lezingen en media")
class AdminMediaController {

    private final MediaAdminService mediaService;

    AdminMediaController(MediaAdminService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    @Operation(summary = "Alle media van de moskee (ook concepten), laatst gewijzigd eerst")
    PageResponse<MediaResponse.Admin> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                           @PathVariable UUID mosqueId,
                                           @RequestParam(required = false) MediaStatus status,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id")));
        return PageResponse.of(mediaService.list(actor, mosqueId, status, q, pageable), MediaResponse.Admin::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Media-item toevoegen (als concept)")
    MediaResponse.Admin create(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                               @Valid @RequestBody MediaRequest request) {
        return MediaResponse.Admin.from(mediaService.create(actor, mosqueId, request.toInput()));
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Media-item ophalen")
    MediaResponse.Admin get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                            @PathVariable UUID mediaId) {
        return MediaResponse.Admin.from(mediaService.get(actor, mosqueId, mediaId));
    }

    @PutMapping("/{mediaId}")
    @Operation(summary = "Media-item wijzigen")
    MediaResponse.Admin update(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                               @PathVariable UUID mediaId, @Valid @RequestBody MediaRequest request) {
        return MediaResponse.Admin.from(mediaService.update(actor, mosqueId, mediaId, request.toInput()));
    }

    @PostMapping("/{mediaId}/publish")
    @Operation(summary = "Publiceren")
    MediaResponse.Admin publish(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                @PathVariable UUID mediaId) {
        return MediaResponse.Admin.from(mediaService.publish(actor, mosqueId, mediaId));
    }

    @PostMapping("/{mediaId}/unpublish")
    @Operation(summary = "Terug naar concept")
    MediaResponse.Admin unpublish(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                  @PathVariable UUID mediaId) {
        return MediaResponse.Admin.from(mediaService.unpublish(actor, mosqueId, mediaId));
    }

    @PostMapping("/{mediaId}/push")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Pushmelding versturen naar volgers (alleen ADMIN, één keer per video)",
            description = "Los van publiceren. Titel en tekst zijn optioneel; standaard samengesteld uit de video.")
    PushNotificationView push(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                              @PathVariable UUID mediaId, @Valid @RequestBody(required = false) PushText text) {
        return mediaService.sendPush(actor, mosqueId, mediaId, text == null ? PushText.DEFAULT : text);
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Media-item verwijderen (de video bij de provider blijft bestaan)")
    void delete(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                @PathVariable UUID mediaId) {
        mediaService.delete(actor, mosqueId, mediaId);
    }
}
