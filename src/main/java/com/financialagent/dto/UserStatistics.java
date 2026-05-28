package com.financialagent.dto;

import lombok.Data;

/**
 * Data Transfer Object for user statistics.
 */
@Data
public class UserStatistics {
    private Long totalUsers;
    private Long newUsers;
    private Long activeUsers;
    private Long lockedUsers;

    public UserStatistics(Long totalUsers, Long newUsers, Long activeUsers, Long lockedUsers) {
        this.totalUsers = totalUsers;
        this.newUsers = newUsers;
        this.activeUsers = activeUsers;
        this.lockedUsers = lockedUsers;
    }
}
