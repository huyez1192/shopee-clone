package com.example.productcrudapi.repository;

import com.example.productcrudapi.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase (String name);

    List<Product> findByCategoryIgnoreCase (String category);

    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("""
            SELECT p FROM Product p
            WHERE (:name IS NULL OR :name = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:category IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category))
            AND (:minPrice IS NULL OR p.price >= :minPrice)
            AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    List<Product> searchAndFilterProducts(
            @Param("name") String name,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
