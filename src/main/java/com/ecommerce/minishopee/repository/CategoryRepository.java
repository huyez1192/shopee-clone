package com.ecommerce.minishopee.repository;

import com.ecommerce.minishopee.entity.Category;
import com.ecommerce.minishopee.entity.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    Optional<Category> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    List<Category> findAllByStatus(CategoryStatus status);

    Optional<Category> findByIdAndStatus(Long id, CategoryStatus status);

    boolean existsByParent_IdAndStatusNot(Long parentId, CategoryStatus status);
}
