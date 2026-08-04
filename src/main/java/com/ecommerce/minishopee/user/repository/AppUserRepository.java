package com.ecommerce.minishopee.user.repository;

import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.enums.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    Boolean existsByEmail(String email);

    /*
     * Admin tìm kiếm và lọc User.
     *
     * keyword tìm theo:
     * - fullName
     * - email
     * - phone
     */
    @Query("""
            SELECT u
            FROM AppUser u
            WHERE (
                LOWER(u.fullName) LIKE CONCAT('%', :keyword, '%')
                OR LOWER(u.email) LIKE CONCAT('%', :keyword, '%')
                OR u.phone LIKE CONCAT('%', :keyword, '%')
            )
              AND (
                    :role IS NULL
                    OR u.role = :role
                  )
              AND (
                    :status IS NULL
                    OR u.status = :status
                  )
            ORDER BY u.createdAt DESC
            """)
    List<AppUser> searchUsersForAdmin(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("status") UserStatus status
    );

    // Khóa record khi Admin khóa hoặc mở khóa User.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM AppUser u
            WHERE u.id = :userId
            """)
    Optional<AppUser> findByIdForUpdate(
            @Param("userId") Long userId
    );
}
