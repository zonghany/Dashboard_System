package com.dashboard.domain;

import com.dashboard.domain.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);

    Optional<Invitation> findByProjectIdAndEmailAndStatus(Long projectId, String email, InvitationStatus status);
}