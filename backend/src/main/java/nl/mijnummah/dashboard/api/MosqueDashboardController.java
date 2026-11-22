package nl.mijnummah.dashboard.api;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.dashboard.application.MosqueStatistics;
import nl.mijnummah.dashboard.application.MosqueStatistics.Figures;
import nl.mijnummah.identity.AuthenticatedUser;

/** Administrators and editors of the mosque. Upcoming events come from the events endpoint. */
@RestController
@RequestMapping("/api/v1/admin/mosques/{mosqueId}/dashboard")
@Tag(name = "Beheer: dashboard")
class MosqueDashboardController {

    private final MosqueStatistics statistics;

    MosqueDashboardController(MosqueStatistics statistics) {
        this.statistics = statistics;
    }

    @GetMapping
    @Operation(summary = "Aantallen voor het dashboard van de moskee")
    Figures dashboard(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId) {
        return statistics.figures(actor, mosqueId);
    }
}
