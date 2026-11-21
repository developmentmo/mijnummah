package nl.mijnummah.dashboard.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.dashboard.application.PlatformStatistics;
import nl.mijnummah.dashboard.application.PlatformStatistics.MosqueCounts;
import nl.mijnummah.dashboard.application.PlatformStatistics.Totals;
import nl.mijnummah.identity.AuthenticatedUser;

/** Platform administrators only. Recent activity comes from the audit log ({@code GET /admin/audit-log}). */
@RestController
@RequestMapping("/api/v1/admin/platform")
@Tag(name = "Platformbeheer: dashboard")
class PlatformDashboardController {

    private final PlatformStatistics statistics;

    PlatformDashboardController(PlatformStatistics statistics) {
        this.statistics = statistics;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Totalen van het platform")
    Totals dashboard(@AuthenticationPrincipal AuthenticatedUser actor) {
        return statistics.totals(actor);
    }

    @GetMapping("/mosques")
    @Operation(summary = "Alle moskeeën met beheerders, volgers en content")
    List<MosqueCounts> mosques(@AuthenticationPrincipal AuthenticatedUser actor) {
        return statistics.perMosque(actor);
    }
}
