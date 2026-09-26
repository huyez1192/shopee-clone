import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import useAuth from "../hooks/useAuth";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";

import styles from "./LoginPage.module.css";

function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const redirectPath = location.state?.redirectPath || "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    setErrorMessage("");
    setIsSubmitting(true);

    try {
      await login({
        email: email.trim(),
        password: password,
      });

      navigate(redirectPath, { replace: true });
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "Email hoặc mật khẩu không chính xác."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className={styles.card}>
      <h1>Đăng nhập</h1>

      <form className={styles.form} onSubmit={handleSubmit}>
        <input
          type="email"
          aria-label="Email"
          required
          value={email}
          placeholder="Email"
          autoComplete="email"
          onChange={(event) => setEmail(event.target.value)}
        />

        <div className={styles.passwordField}>
          <input
            type={showPassword ? "text" : "password"}
            aria-label="Mật khẩu"
            required
            value={password}
            placeholder="Mật khẩu"
            autoComplete="current-password"
            onChange={(event) => setPassword(event.target.value)}
          />

          <button type="button" onClick={() => setShowPassword((current) => !current)}>
            {showPassword ? "Ẩn" : "Hiện"}
          </button>
        </div>

        {errorMessage && <p className={styles.errorMessage}>{errorMessage}</p>}

        <button type="submit" className={styles.loginButton} disabled={isSubmitting}>
          {isSubmitting ? "ĐANG ĐĂNG NHẬP..." : "ĐĂNG NHẬP"}
        </button>

        <div className={styles.links}>
          <button type="button" disabled title="Tính năng chưa được hỗ trợ">
            Quên mật khẩu
          </button>
        </div>

        <div className={styles.divider}>
          <span />
          <p>HOẶC</p>
          <span />
        </div>

        <div className={styles.social}>
          <button type="button" disabled title="Tính năng chưa được hỗ trợ">
            <strong>f</strong>
            Facebook
          </button>

          <button type="button" disabled title="Tính năng chưa được hỗ trợ">
            <strong>G</strong>
            Google
          </button>
        </div>

        <p className={styles.terms}>
          Bằng việc đăng nhập, bạn đồng ý với Shopee về{" "}
          <span>Điều khoản dịch vụ</span>{" "}
          &{" "}
          <span>Chính sách bảo mật</span>
        </p>

        <div className={styles.register}>
          Bạn mới biết đến Shopee?

          <Link to="/register">
            Đăng ký
          </Link>
        </div>
      </form>
    </section>
  );
}

export default LoginPage;
