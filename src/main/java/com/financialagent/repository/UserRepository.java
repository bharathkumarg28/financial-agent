package com.financialagent.repository;

import com.financialagent.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find by username
    Optional<User> findByUsername(String username);

    // Find by email
    Optional<User> findByEmail(String email);

    // Find by AngelOne client ID
    Optional<User> findByAngelOneClientId(String angelOneClientId);

    // Check if username exists
    boolean existsByUsername(String username);

    // Check if email exists
    boolean existsByEmail(String email);

    // Check if AngelOne client ID exists
    boolean existsByAngelOneClientId(String angelOneClientId);

    // Find users by role
    List<User> findByRole(User.UserRole role);

    // Find users created after a specific date
    List<User> findByCreatedAtAfter(LocalDateTime date);

    // Find users with failed login attempts
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts > 0 AND u.lockedUntil > :now")
    List<User> findLockedUsers(@Param("now") LocalDateTime now);

    // Find users who haven't logged in for a specific period
    @Query("SELECT u FROM User u WHERE u.lastLogin < :date OR u.lastLogin IS NULL")
    List<User> findInactiveUsers(@Param("date") LocalDateTime date);

    // Update last login and increment login count
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin, u.loginCount = u.loginCount + 1, " +
            "u.failedLoginAttempts = 0, u.lockedUntil = NULL WHERE u.id = :userId")
    int updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);

    // Increment failed login attempts
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1, " +
            "u.lockedUntil = CASE WHEN u.failedLoginAttempts >= 4 THEN :lockUntil ELSE NULL END " +
            "WHERE u.id = :userId")
    int incrementFailedLoginAttempts(@Param("userId") Long userId, @Param("lockUntil") LocalDateTime lockUntil);

    // Reset failed login attempts
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = NULL WHERE u.id = :userId")
    int resetFailedLoginAttempts(@Param("userId") Long userId);

    // Find users by partial username or email (for search)
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    // Count users by role
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") User.UserRole role);

    // Find recently registered users
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since ORDER BY u.createdAt DESC")
    List<User> findRecentlyRegistered(@Param("since") LocalDateTime since);

    // Find users with AngelOne credentials (for validation)
    @Query("SELECT u FROM User u WHERE u.angelOneApiKey IS NOT NULL AND " +
            "u.angelOneClientId IS NOT NULL AND u.angelOnePassword IS NOT NULL AND " +
            "u.angelOneTotpSecret IS NOT NULL")
    List<User> findUsersWithCompleteAngelOneCredentials();

    // Update AngelOne credentials
    @Modifying
    @Query("UPDATE User u SET u.angelOneApiKey = :apiKey, u.angelOneClientId = :clientId, " +
            "u.angelOnePassword = :password, u.angelOneTotpSecret = :totpSecret WHERE u.id = :userId")
    int updateAngelOneCredentials(@Param("userId") Long userId, @Param("apiKey") String apiKey,
                                  @Param("clientId") String clientId, @Param("password") String password,
                                  @Param("totpSecret") String totpSecret);

    // Find users for notifications
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.notificationsEnabled = true")
    List<User> findUsersForNotifications();

    // Get user statistics
    @Query("SELECT " +
            "COUNT(u) as totalUsers, " +
            "COUNT(CASE WHEN u.createdAt >= :since THEN 1 END) as newUsers, " +
            "COUNT(CASE WHEN u.lastLogin >= :since THEN 1 END) as activeUsers, " +
            "COUNT(CASE WHEN u.lockedUntil > :now THEN 1 END) as lockedUsers " +
            "FROM User u")
    Object[] getUserStatistics(@Param("since") LocalDateTime since, @Param("now") LocalDateTime now);
}
