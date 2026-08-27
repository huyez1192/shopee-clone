import { Outlet } from "react-router-dom";

import Footer from "../components/layout/Footer";
import Header from "../components/layout/Header";
import ScrollToTop from "../components/common/ScrollToTop";

import styles from "./MainLayout.module.css";

function MainLayout() {
  return (
    <div className={styles.app}>

      <ScrollToTop />

      <Header />

      <div className={styles.content}>
        <Outlet />
      </div>

      <Footer />
    </div>
  );
}

export default MainLayout;