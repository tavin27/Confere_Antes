package com.confereantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.confereantes.model.CallRecording;

public interface CallRecordingRepository extends JpaRepository<CallRecording, Long> {

    Optional<CallRecording> findByIdAndAlert_User_Id(Long id, Long userId);
}
