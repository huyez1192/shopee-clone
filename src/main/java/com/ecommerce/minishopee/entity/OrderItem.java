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
@Table(name = "order_items", indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_product_id", columnList = "product_id"),
                @Index(name = "idx_order_items_variant_id", columnList = "product_variant_id")
        }
)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;


    // Vẫn giữ liên kết để phục vụ truy vấn, review và quản lý sau này.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    /*
     * Snapshot thông tin sản phẩm.
     *
     * Seller sửa tên/ảnh/SKU sau này cũng không làm thay đổi đơn hàng cũ.
     */
    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "product_image_url_snapshot")
    private String productImageUrlSnapshot;

    @Column(name = "sku_code_snapshot")
    private String skuCodeSnapshot;

    @Column(name = "variant_name_1_snapshot")
    private String variantName1Snapshot;

    @Column(name = "variant_value_1_snapshot")
    private String variantValue1Snapshot;

    @Column(name = "variant_name_2_snapshot")
    private String variantName2Snapshot;

    @Column(name = "variant_value_2_snapshot")
    private String variantValue2Snapshot;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}