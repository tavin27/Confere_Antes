package com.confereantes.model;

import java.time.LocalDateTime;

import com.confereantes.model.enums.ContactChannel;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_channel", nullable = false, length = 20)
    private ContactChannel contactChannel;

    @Column(name = "contact_value", length = 150)
    private String contactValue;

    @Column(name = "overall_risk_score")
    private Integer overallRiskScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Alert() {

    }

    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ContactChannel getContactChannel() {
        return contactChannel;
    }

    public void setContactChannel(ContactChannel contactChannel) {
        this.contactChannel = contactChannel;
    }

    public String getContactValue() {
        return contactValue;
    }

    public void setContactValue(String contactValue) {
        this.contactValue = contactValue;
    }

    public Integer getOverallRiskScore() {
        return overallRiskScore;
    }

    public void setOverallRiskScore(Integer overallRiskScore) {
        this.overallRiskScore = overallRiskScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}