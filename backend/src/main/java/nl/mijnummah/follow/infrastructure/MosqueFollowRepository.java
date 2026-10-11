package nl.mijnummah.follow.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.follow.domain.MosqueFollow;

public interface MosqueFollowRepository extends JpaRepository<MosqueFollow, MosqueFollow.Key> {

    List<MosqueFollow> findByUserIdOrderByCreatedAt(UUID userId);
}
