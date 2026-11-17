package com.confereantes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.confereantes.model.RiskSignal;

public interface RiskSignalRepository extends JpaRepository<RiskSignal, Long> {

    List<RiskSignal> findByRiskAnalysisId(Long riskAnalysisId);
}
