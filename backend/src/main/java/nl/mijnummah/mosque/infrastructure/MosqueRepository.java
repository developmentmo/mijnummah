package nl.mijnummah.mosque.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.mosque.MosqueStatus;
import nl.mijnummah.mosque.domain.Mosque;

public interface MosqueRepository extends JpaRepository<Mosque, UUID>, JpaSpecificationExecutor<Mosque> {

    Optional<Mosque> findBySlug(String slug);

    List<Mosque> findBySlugIn(Collection<String> slugs);

    boolean existsBySlug(String slug);

    @Query("select m.id from Mosque m where m.status = :status")
    List<UUID> findIdsByStatus(@Param("status") MosqueStatus status);

    /** Places with at least one mosque in this status, alphabetical; mosques without a city are left out. */
    @Query("""
            select distinct m.city from Mosque m
            where m.status = :status and m.city is not null and m.city <> ''
            order by m.city""")
    List<String> findCitiesByStatus(@Param("status") MosqueStatus status);
}
