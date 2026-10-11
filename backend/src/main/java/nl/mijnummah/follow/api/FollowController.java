package nl.mijnummah.follow.api;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.follow.application.FollowService;
import nl.mijnummah.follow.application.FollowView;
import nl.mijnummah.follow.NotificationTopic;
import nl.mijnummah.identity.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/me/follows")
@Tag(name = "Volgen en meldingsvoorkeuren")
class FollowController {

    private final FollowService followService;

    FollowController(FollowService followService) {
        this.followService = followService;
    }

    @GetMapping
    @Operation(summary = "Moskeeën die ik volg")
    List<FollowView> mine(@AuthenticationPrincipal AuthenticatedUser actor) {
        return followService.mine(actor);
    }

    @PutMapping("/{mosqueId}")
    @Operation(summary = "Moskee volgen (idempotent)")
    FollowView follow(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId) {
        return followService.follow(actor, mosqueId);
    }

    @DeleteMapping("/{mosqueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Moskee ontvolgen (idempotent, verwijdert ook de meldingsvoorkeuren)")
    void unfollow(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId) {
        followService.unfollow(actor, mosqueId);
    }

    @GetMapping("/{mosqueId}")
    @Operation(summary = "Eén gevolgde moskee met meldingsvoorkeuren")
    FollowView get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId) {
        return followService.get(actor, mosqueId);
    }

    @PutMapping("/{mosqueId}/notifications")
    @Operation(summary = "Meldingsvoorkeuren wijzigen; weggelaten onderwerpen blijven ongewijzigd",
            description = "Bepaalt welke pushmeldingen van deze moskee je ontvangt (standaard alles aan).")
    FollowView updatePreferences(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                 @Valid @RequestBody PreferencesRequest request) {
        return followService.updatePreferences(actor, mosqueId, request.changes());
    }

    record PreferencesRequest(Boolean announcements, Boolean events, Boolean media) {

        Map<NotificationTopic, Boolean> changes() {
            Map<NotificationTopic, Boolean> changes = new EnumMap<>(NotificationTopic.class);
            if (announcements != null) {
                changes.put(NotificationTopic.ANNOUNCEMENTS, announcements);
            }
            if (events != null) {
                changes.put(NotificationTopic.EVENTS, events);
            }
            if (media != null) {
                changes.put(NotificationTopic.MEDIA, media);
            }
            return changes;
        }
    }
}
