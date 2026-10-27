package com.confereantes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.confereantes.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {

}
