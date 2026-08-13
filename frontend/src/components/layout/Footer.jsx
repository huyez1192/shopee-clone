import styles from "./Footer.module.css";

const customerServiceLinks = [
  "Trung Tâm Trợ Giúp",
  "Shopee Blog",
  "Hướng Dẫn Mua Hàng",
  "Hướng Dẫn Bán Hàng",
  "Thanh Toán",
  "Vận Chuyển",
  "Trả Hàng & Hoàn Tiền",
  "Chăm Sóc Khách Hàng",
];

const aboutLinks = [
  "Giới Thiệu Về Shopee",
  "Tuyển Dụng",
  "Điều Khoản",
  "Chính Sách Bảo Mật",
  "Kênh Người Bán",
  "Liên Hệ Với Truyền Thông",
];

const paymentMethods = [
  "VISA",
  "Mastercard",
  "JCB",
  "COD",
  "ShopeePay",
];

const shippingUnits = [
  "SPX",
  "GHN",
  "GHTK",
  "Viettel Post",
  "Vietnam Post",
];

function FooterLinkList({ title, items }) {
  return (
    <section className={styles.column}>
      <h3>{title}</h3>

      <ul>
        {items.map((item) => (
          <li key={item}>
            <a href="#footer">
              {item}
            </a>
          </li>
        ))}
      </ul>
    </section>
  );
}

function Footer() {
  return (
    <footer
      id="footer"
      className={styles.footer}
    >
      <div className={styles.topBorder} />

      <div className={styles.container}>
        <div className={styles.mainContent}>
          <FooterLinkList
            title="Chăm sóc khách hàng"
            items={customerServiceLinks}
          />

          <FooterLinkList
            title="Về Shopee"
            items={aboutLinks}
          />

          <section className={styles.column}>
            <h3>Thanh toán</h3>

            <div className={styles.badgeGrid}>
              {paymentMethods.map((method) => (
                <span
                  key={method}
                  className={styles.badge}
                >
                  {method}
                </span>
              ))}
            </div>

            <h3 className={styles.shippingTitle}>
              Đơn vị vận chuyển
            </h3>

            <div className={styles.badgeGrid}>
              {shippingUnits.map((unit) => (
                <span
                  key={unit}
                  className={styles.badge}
                >
                  {unit}
                </span>
              ))}
            </div>
          </section>

          <section className={styles.column}>
            <h3>Theo dõi chúng tôi</h3>

            <ul>
              <li>
                <a href="#facebook">
                  Facebook
                </a>
              </li>

              <li>
                <a href="#instagram">
                  Instagram
                </a>
              </li>

              <li>
                <a href="#linkedin">
                  LinkedIn
                </a>
              </li>
            </ul>
          </section>

          <section className={styles.column}>
            <h3>Tải ứng dụng Shopee</h3>

            <div className={styles.downloadArea}>
              <div className={styles.qrCode}>
                <span>QR</span>
              </div>

              <div className={styles.downloadButtons}>
                <button type="button">
                  App Store
                </button>

                <button type="button">
                  Google Play
                </button>

                <button type="button">
                  AppGallery
                </button>
              </div>
            </div>
          </section>
        </div>

        <div className={styles.countrySection}>
          <p>
            © 2026 Shopee. Tất cả các quyền
            được bảo lưu.
          </p>

          <p>
            Quốc gia & Khu vực: Việt Nam |
            Argentina | Singapore | Indonesia | Thái Lan |
            Malaysia | Philippines | Brazil | México | Đài Loan
          </p>
        </div>
      </div>

      <div className={styles.bottomSection}>
        <nav className={styles.policyNavigation}>
          <a href="#privacy">
            Chính sách bảo mật
          </a>

          <a href="#operation">
            Quy chế hoạt động
          </a>

          <a href="#shipping">
            Chính sách vận chuyển
          </a>

          <a href="#return">
            Chính sách trả hàng và hoàn tiền
          </a>
        </nav>

        <div className={styles.certificates}>
          <span>ĐÃ ĐĂNG KÝ</span>
          <span>ĐÃ THÔNG BÁO</span>
        </div>

        <div className={styles.companyInfo}>
          <p>Công ty TNHH Shopee</p>

          <p>
            Địa chỉ: Tầng 4-5-6, Tòa nhà Capital Place, số 29 đường Liễu Giai, Phường Ngọc Hà, Thành phố Hà Nội, Việt Nam
          </p>

        </div>
      </div>
    </footer>
  );
}

export default Footer;