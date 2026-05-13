package com.knowledge.assistant.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "integrations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "integration_type"})
})
public class Integration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false)
    private IntegrationType integrationType;

    @Column(name = "access_token")
    private String accessToken;

    // Slack-specific
    @Column(name = "slack_team_id")
    private String slackTeamId;

    @Column(name = "slack_channel_id")
    private String slackChannelId;

    @Column(name = "slack_channel_name")
    private String slackChannelName;

    // GitHub-specific
    @Column(name = "github_repo_owner")
    private String githubRepoOwner;

    @Column(name = "github_repo_name")
    private String githubRepoName;

    @Column(name = "github_default_branch")
    private String githubDefaultBranch;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integration() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public IntegrationType getIntegrationType() { return integrationType; }
    public void setIntegrationType(IntegrationType integrationType) { this.integrationType = integrationType; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getSlackTeamId() { return slackTeamId; }
    public void setSlackTeamId(String slackTeamId) { this.slackTeamId = slackTeamId; }

    public String getSlackChannelId() { return slackChannelId; }
    public void setSlackChannelId(String slackChannelId) { this.slackChannelId = slackChannelId; }

    public String getSlackChannelName() { return slackChannelName; }
    public void setSlackChannelName(String slackChannelName) { this.slackChannelName = slackChannelName; }

    public String getGithubRepoOwner() { return githubRepoOwner; }
    public void setGithubRepoOwner(String githubRepoOwner) { this.githubRepoOwner = githubRepoOwner; }

    public String getGithubRepoName() { return githubRepoName; }
    public void setGithubRepoName(String githubRepoName) { this.githubRepoName = githubRepoName; }

    public String getGithubDefaultBranch() { return githubDefaultBranch; }
    public void setGithubDefaultBranch(String githubDefaultBranch) { this.githubDefaultBranch = githubDefaultBranch; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
