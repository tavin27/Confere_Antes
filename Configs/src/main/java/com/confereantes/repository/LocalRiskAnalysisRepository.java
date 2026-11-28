
package com.confereantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.confereantes.model.LocalRiskAnalysis;

public interface LocalRiskAnalysisRepository
        extends JpaRepository<LocalRiskAnalysis, Long> {

    Optional<LocalRiskAnalysis> findByCallRecordingId(Long callRecordingId);
}
