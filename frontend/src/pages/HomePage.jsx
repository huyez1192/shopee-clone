import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";

import ProductCard from "../features/product/components/ProductCard";
import { getProducts } from "../features/product/api/productApi";
import { getCategories } from "../features/category/api/categoryApi";
import { queryKeys } from "../query/queryKeys";

import fallbackProductImage from "../assets/images/product-placeholder.png";
import styles from "./HomePage.module.css";

import bannerMain1 from "../assets/images/home/banner01.jpg";
import bannerMain2 from "../assets/images/home/banner02.jpg";
import bannerMain3 from "../assets/images/home/banner03.jpg";
import bannerMain4 from "../assets/images/home/banner04.jpg";
import bannerMain5 from "../assets/images/home/banner05.jpg";
import bannerRightTop from "../assets/images/home/banner_right_01.jpg";
import bannerRightBottom from "../assets/images/home/banner_right_02.jpg";

import mallBannerImage from "../assets/images/home/mall_banner.jpg";

import loyalCustomerImage from "../assets/images/shortcuts/khach_hang_than_thiet.jpg";
import dealImage from "../assets/images/shortcuts/deal.jpg";
import shopeeProcessingImage from "../assets/images/shortcuts/shopee_xu_ly.jpg";
import shopeeHomeImage from "../assets/images/shortcuts/shopee_home.jpg";
import discountCodeImage from "../assets/images/shortcuts/ma_giam_gia.jpg";
import phoneTopUpAndBillImage from "../assets/images/shortcuts/nap_dt.jpg";

import fashionImage from "../assets/images/categories/men-fashion.jpg";
import phoneImage from "../assets/images/categories/phone.jpg";
import electronicsImage from "../assets/images/categories/electronics.jpg";
import laptopImage from "../assets/images/categories/laptop.jpg";
import cameraImage from "../assets/images/categories/camera.jpg";
import watchImage from "../assets/images/categories/watch.jpg";
import shoesImage from "../assets/images/categories/shoes.jpg";
import homeImage from "../assets/images/categories/home.jpg";
import sportImage from "../assets/images/categories/sport.jpg";
import motorbikeImage from "../assets/images/categories/motorbike.jpg";
import womenFashionImage from "../assets/images/categories/women-fashion.jpg";
import babyImage from "../assets/images/categories/baby.jpg";
import kitchenImage from "../assets/images/categories/kitchen.jpg";
import beautyImage from "../assets/images/categories/beauty.jpg";
import healthImage from "../assets/images/categories/health.jpg";
import heelsImage from "../assets/images/categories/heels.jpg";
import bagImage from "../assets/images/categories/bag.jpg";
import jewelryImage from "../assets/images/categories/jewelry.jpg";
import foodImage from "../assets/images/categories/food.jpg";
import booksImage from "../assets/images/categories/books.jpg";

const MAIN_BANNERS = [bannerMain1, bannerMain2, bannerMain3, bannerMain4, bannerMain5];

const SHORTCUTS = [
  { image: loyalCustomerImage, title: "Siêu Hội", subtitle: "Thành Viên" },
  { image: dealImage, title: "Deal Từ 1.000D" },
  { image: shopeeProcessingImage, title: "Shopee Xử Lý" },
  { image: shopeeHomeImage, title: "Shopee Home" },
  { image: discountCodeImage, title: "Mã Giảm Giá" },
  { image: phoneTopUpAndBillImage, title: "Nạp Điện Thoại", subtitle: "& Hóa Đơn" },
];

const CATEGORY_IMAGES = [
  fashionImage,
  phoneImage,
  electronicsImage,
  laptopImage,
  cameraImage,
  watchImage,
  shoesImage,
  homeImage,
  sportImage,
  motorbikeImage,
  womenFashionImage,
  babyImage,
  kitchenImage,
  beautyImage,
  healthImage,
  heelsImage,
  bagImage,
  jewelryImage,
  foodImage,
  booksImage,
];

function formatPrice(value) {
  return new Intl.NumberFormat("vi-VN").format(Number(value) || 0);
}

function HomePage() {
  const [bannerIndex, setBannerIndex] = useState(0);
  const [enableTransition, setEnableTransition] = useState(true);
  const [visibleCount, setVisibleCount] = useState(24);

  const timerRef = useRef(null);
  const navigate = useNavigate();
  const carouselBanners = [...MAIN_BANNERS, MAIN_BANNERS[0]];

  const {
    data: products = [],
    isLoading: isProductsLoading,
    error: productError,
    refetch: refetchProducts,
  } = useQuery({
    queryKey: queryKeys.products.all,
    queryFn: getProducts,
    select: (response) => Array.isArray(response) ? response : response?.content || [],
  });

  const {
    data: categories = [],
    isLoading: isCategoriesLoading,
    error: categoryError,
    refetch: refetchCategories,
  } = useQuery({
    queryKey: queryKeys.categories.all,
    queryFn: getCategories,
    select: (response) => Array.isArray(response) ? response : response?.content || [],
  });

  const isLoading = isProductsLoading || isCategoriesLoading;
  const queryError = productError || categoryError;
  const errorMessage = queryError ? queryError?.response?.data?.message || "Không thể tải dữ liệu trang chủ." : "";

  useEffect(() => {
    timerRef.current = setInterval(() => {
      setBannerIndex((currentIndex) => currentIndex + 1);
    }, 3000);

    return () => clearInterval(timerRef.current);
  }, []);

  const flashSaleProducts = useMemo(() => products.slice(0, 6), [products]);
  const mallProducts = useMemo(() => products.slice(6, 14), [products]);
  const topProducts = useMemo(() => products.slice(0, 6), [products]);
  const suggestedProducts = useMemo(() => products.slice(0, visibleCount), [products, visibleCount]);

  function loadHome() {
    refetchProducts();
    refetchCategories();
  }

  function handleBannerTransitionEnd() {
    if (bannerIndex === MAIN_BANNERS.length) {
      setEnableTransition(false);
      setBannerIndex(0);

      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          setEnableTransition(true);
        });
      });
    }
  }

  return (
    <main className={styles.page}>
      {/* ================= BANNER ================= */}

      <section className={styles.hero}>
        <div className={styles.heroGrid}>
          {/* ================= BANNER TRÁI ================= */}

          <div className={styles.mainBanner}>
            <div
              className={styles.bannerTrack}
              style={{
                transform: `translateX(-${bannerIndex * 100}%)`,
                transition: enableTransition ? "transform 0.6s ease" : "none",
              }}
              onTransitionEnd={handleBannerTransitionEnd}
            >
              {carouselBanners.map((banner, index) => (
                <Link key={index} to="/products" className={styles.bannerSlide}>
                  <img src={banner} alt={`Banner ${index + 1}`} />
                </Link>
              ))}
            </div>

            {/* Dấu chấm dưới banner */}
            <div className={styles.bannerDots}>
              {MAIN_BANNERS.map((_, index) => (
                <button
                  key={index}
                  type="button"
                  className={index === bannerIndex % MAIN_BANNERS.length ? styles.activeDot : styles.dot}
                  onClick={() => {
                    setEnableTransition(true);
                    setBannerIndex(index);
                  }}
                />
              ))}
            </div>
          </div>

          {/* ================= 2 BANNER PHẢI ================= */}

          <div className={styles.sideBanners}>
            <Link to="/products" className={styles.sideBannerImage}>
              <img src={bannerRightTop} alt="Banner khuyến mãi" />
            </Link>

            <Link to="/products" className={styles.sideBannerImage}>
              <img src={bannerRightBottom} alt="Banner ưu đãi" />
            </Link>
          </div>
        </div>

        {/* ================= SHORTCUT ================= */}

        <div className={styles.shortcuts}>
          {SHORTCUTS.map((item) => (
            <Link key={`${item.title}-${item.subtitle || ""}`} to="/products" className={styles.shortcut}>
              <div className={styles.shortcutIcon}>
                <img src={item.image} alt={item.title} />
              </div>

              <span>
                {item.title}

                {item.subtitle && (
                  <>
                    <br />
                    {item.subtitle}
                  </>
                )}
              </span>
            </Link>
          ))}
        </div>
      </section>

      <div className={styles.homeContainer}>
        {/* ================= CATEGORY ================= */}

        <section className={styles.section}>
          <div className={styles.sectionTitle}>DANH MỤC</div>

          <div className={styles.categoryGrid}>
            {categories.slice(0, 20).map((category, index) => (
              <Link
                key={category.id}
                to={`/products?categoryId=${category.id}&page=1`}
                className={styles.categoryItem}
              >
                <div className={styles.categoryImage}>
                  <img
                    src={CATEGORY_IMAGES[index % CATEGORY_IMAGES.length]}
                    alt={category.name}
                  />
                </div>

                <span>{category.name}</span>
              </Link>
            ))}
          </div>
        </section>

        {/* ================= FLASH SALE ================= */}

        <section className={`${styles.section} ${styles.flashSection}`}>
          <div className={styles.sectionHeader}>
            <h2 className={styles.flashTitle}>⚡ FLASH SALE</h2>

            <Link to="/products">Xem tất cả ›</Link>
          </div>

          <div className={styles.flashGrid}>
            {flashSaleProducts.map((product, index) => (
              <Link
                key={product.id}
                to={`/products/${product.id}`}
                className={styles.flashCard}
              >
                <div className={styles.flashImageWrapper}>
                  <img
                    src={product.imageUrl || fallbackProductImage}
                    alt={product.name}
                    onError={(event) => {
                      event.currentTarget.src = fallbackProductImage;
                    }}
                  />

                  <span className={styles.discount}>
                    -{[25, 35, 45, 50, 40, 30][index % 6]}%
                  </span>
                </div>

                <strong>₫ {formatPrice(product.minPrice)}</strong>

                <div className={styles.flashProgress}>
                  <span>ĐANG BÁN CHẠY</span>
                </div>
              </Link>
            ))}
          </div>
        </section>

        {/* ================= MALL ================= */}

        <section className={`${styles.section} ${styles.mallSection}`}>
          <div className={styles.mallHeader}>
            <div>
              <strong>SHOPEE MALL</strong>

              <span>✓ Trả Hàng Miễn Phí 15 Ngày</span>

              <span>✓ Hàng Chính Hãng 100%</span>

              <span>🚚 Miễn Phí Vận Chuyển</span>
            </div>

            <Link to="/products">Xem tất cả ›</Link>
          </div>

          <div className={styles.mallBody}>
            <Link to="/products" className={styles.mallBanner}>
              <img src={mallBannerImage} alt="Shopee Mall Banner" />
            </Link>

            <div className={styles.mallGrid}>
              {mallProducts.map((product, index) => (
                <Link
                  key={product.id}
                  to={`/products/${product.id}`}
                  className={styles.mallItem}
                >
                  <img
                    src={product.imageUrl || fallbackProductImage}
                    alt={product.name}
                    onError={(event) => {
                      event.currentTarget.src = fallbackProductImage;
                    }}
                  />

                  <strong>
                    {["Ưu đãi đến 20%", "Mua là có quà", "Siêu sale", "Deal siêu hot"][index % 4]}
                  </strong>
                </Link>
              ))}
            </div>
          </div>
        </section>

        {/* ================= TOP SEARCH ================= */}

        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <h2 className={styles.topSearchTitle}>TÌM KIẾM HÀNG ĐẦU</h2>

            <Link to="/products">Xem tất cả ›</Link>
          </div>

          <div className={styles.topSearchGrid}>
            {topProducts.map((product, index) => (
              <Link
                key={product.id}
                to={`/products/${product.id}`}
                className={styles.topSearchItem}
              >
                <div className={styles.topImage}>
                  <img
                    src={product.imageUrl || fallbackProductImage}
                    alt={product.name}
                    onError={(event) => {
                      event.currentTarget.src = fallbackProductImage;
                    }}
                  />

                  <span>TOP</span>

                  <div>
                    Bán {index + 3}k+ / tháng
                  </div>
                </div>

                <strong>{product.name}</strong>
              </Link>
            ))}
          </div>
        </section>

        {/* ================= SUGGESTION ================= */}

        <section className={styles.suggestionSection}>
          <button
            type="button"
            className={styles.suggestionTitle}
            onClick={() => navigate("/products")}
          >
            GỢI Ý HÔM NAY
          </button>

          {isLoading ? (
            <div className={styles.state}>Đang tải sản phẩm...</div>
          ) : errorMessage ? (
            <div className={styles.state}>
              <p>{errorMessage}</p>

              <button type="button" onClick={loadHome}>
                Thử lại
              </button>
            </div>
          ) : (
            <>
              <div className={styles.productGrid}>
                {suggestedProducts.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              {visibleCount < products.length && (
                <div className={styles.moreWrapper}>
                  <button
                    type="button"
                    onClick={() => setVisibleCount((current) => current + 12)}
                  >
                    Xem thêm
                  </button>
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </main>
  );
}

export default HomePage;