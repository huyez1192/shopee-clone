package com.ecommerce.minishopee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /*
     * Ví dụ:
     * variantName1 = "Màu sắc"
     * variantValue1 = "Đỏ"
     */
    private String variantName1;

    private String variantValue1;

    /*
     * Ví dụ:
     * variantName2 = "Size"
     * variantValue2 = "M"
     *
     * Có thể null nếu sản phẩm chỉ có 1 nhóm biến thể.
     */
    private String variantName2;

    private String variantValue2;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    // Ví dụ: IP15-BLACK-256
    @Column(unique = true)
    private String skuCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductVariantStatus status = ProductVariantStatus.ACTIVE;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ProductVariantStatus.ACTIVE;
        }

        if (this.stockQuantity != null && this.stockQuantity == 0) {
            this.status = ProductVariantStatus.OUT_OF_STOCK;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();

        if (this.status == ProductVariantStatus.ACTIVE
                && this.stockQuantity != null
                && this.stockQuantity == 0) {
            this.status = ProductVariantStatus.OUT_OF_STOCK;
        }
    }
}