package nl.mijnummah.media.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.media.application.MediaFavoriteService;

@RestController
@RequestMapping("/api/v1/me/favorite-media")
@Tag(name = "Favoriete video's")
class FavoriteMediaController {

    private final MediaFavoriteService favorites;
    private final PublicMediaResponses responses;

    FavoriteMediaController(MediaFavoriteService favorites, PublicMediaResponses responses) {
        this.favorites = favorites;
        this.responses = responses;
    }

    @GetMapping
    @Operation(summary = "Mijn favoriete video's die nu te bekijken zijn, laatst opgeslagen eerst")
    PageResponse<MediaResponse> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size) {
        return responses.page(favorites.list(actor, PageRequests.of(page, size, Sort.unsorted())));
    }

    @GetMapping("/ids")
    @Operation(summary = "IDs van al mijn favoriete video's")
    List<UUID> ids(@AuthenticationPrincipal AuthenticatedUser actor) {
        return favorites.ids(actor);
    }

    @PutMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Video als favoriet opslaan (idempotent)")
    void add(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mediaId) {
        favorites.add(actor, mediaId);
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Video uit de favorieten halen (idempotent)")
    void remove(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mediaId) {
        favorites.remove(actor, mediaId);
    }
}
