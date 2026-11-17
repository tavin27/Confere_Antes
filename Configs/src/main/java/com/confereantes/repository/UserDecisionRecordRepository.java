package com.confereantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.confereantes.model.UserDecisionRecord;

public interface UserDecisionRecordRepository extends JpaRepository<UserDecisionRecord, Long> {

    Optional<UserDecisionRecord> findByCallRecordingId(Long callRecordingId);
}
