package com.confereantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.confereantes.model.CallTranscription;

public interface CallTranscriptionRepository extends JpaRepository<CallTranscription, Long> {

    Optional<CallTranscription> findByCallRecordingId(Long callRecordingId);
}
