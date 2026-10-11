package nl.mijnummah.event.infrastructure;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.event.domain.Event;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    /** Tenant-scoped lookup: an event of another mosque is never returned. */
    Optional<Event> findByIdAndMosqueId(UUID id, UUID mosqueId);

    boolean existsByIdAndMosqueId(UUID id, UUID mosqueId);

    /**
     * Row lock on the event while registering, so concurrent registrations for the last places are serialized and
     * the capacity can never be exceeded.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") UUID id);
}
