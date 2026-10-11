package nl.mijnummah.identity.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.identity.domain.PasswordResetCode;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

    /** The newest code of the user that was neither used nor invalidated (row lock: attempts are counted exactly). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetCode> findFirstByUserIdAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
            UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PasswordResetCode c set c.invalidatedAt = :now
            where c.userId = :userId and c.usedAt is null and c.invalidatedAt is null""")
    int invalidateOpenCodes(@Param("userId") UUID userId, @Param("now") Instant now);
}
