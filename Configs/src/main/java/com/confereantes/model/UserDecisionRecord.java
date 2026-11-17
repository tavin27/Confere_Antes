package com.confereantes.model;

import java.time.LocalDateTime;

import com.confereantes.model.enums.UserDecision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_decisions")
public class UserDecisionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "call_recording_id", nullable = false, unique = true)
    private CallRecording callRecording;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserDecision decision;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public CallRecording getCallRecording() { return callRecording; }
    public void setCallRecording(CallRecording callRecording) { this.callRecording = callRecording; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public UserDecision getDecision() { return decision; }
    public void setDecision(UserDecision decision) { this.decision = decision; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
