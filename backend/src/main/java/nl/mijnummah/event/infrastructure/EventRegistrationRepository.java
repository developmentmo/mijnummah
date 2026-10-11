package nl.mijnummah.event.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.event.domain.EventRegistration;
import nl.mijnummah.event.domain.RegistrationStatus;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    Optional<EventRegistration> findByEventIdAndUserId(UUID eventId, UUID userId);

    long countByEventIdAndStatus(UUID eventId, RegistrationStatus status);

    boolean existsByEventId(UUID eventId);

    List<EventRegistration> findByEventIdAndMosqueIdOrderByCreatedAt(UUID eventId, UUID mosqueId);

    Page<EventRegistration> findByUserIdAndStatus(UUID userId, RegistrationStatus status, Pageable pageable);

    /** Confirmed registrations per event, for list views. */
    @Query("""
            select r.eventId as eventId, count(r) as confirmed from EventRegistration r
            where r.eventId in :eventIds and r.status = nl.mijnummah.event.domain.RegistrationStatus.CONFIRMED
            group by r.eventId""")
    List<ConfirmedCount> countConfirmed(@Param("eventIds") Collection<UUID> eventIds);

    @Query("""
            select r.eventId from EventRegistration r
            where r.userId = :userId and r.eventId in :eventIds
              and r.status = nl.mijnummah.event.domain.RegistrationStatus.CONFIRMED""")
    List<UUID> confirmedEventIdsOf(@Param("userId") UUID userId, @Param("eventIds") Collection<UUID> eventIds);

    interface ConfirmedCount {
        UUID getEventId();

        long getConfirmed();
    }
}
