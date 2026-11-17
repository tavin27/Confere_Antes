package com.confereantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.confereantes.model.RiskAnalysis;

public interface RiskAnalysisRepository extends JpaRepository<RiskAnalysis, Long> {

    Optional<RiskAnalysis> findByCallRecordingId(Long callRecordingId);
}
