package com.helpdesk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.entity.User;
import com.helpdesk.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    long countByRoleAndEnabledTrue(Role role);

    List<User> findByRoleAndEnabledTrueOrderByFullNameAsc(Role role);

    List<User> findByDepartmentIdOrderByFullNameAsc(Long departmentId);

    List<User> findAllByOrderByIdAsc();
}
