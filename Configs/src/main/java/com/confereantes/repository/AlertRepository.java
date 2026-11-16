
package com.confereantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.confereantes.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByIdAndUserId(Long id, Long userId);

}
