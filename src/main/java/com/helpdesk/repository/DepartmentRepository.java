package com.helpdesk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByOrderByNameAsc();

    List<Department> findByEnabledTrueOrderByNameAsc();

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByName(String name);
}
