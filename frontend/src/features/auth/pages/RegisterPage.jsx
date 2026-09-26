import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import useAuth from "../hooks/useAuth";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";

import styles from "./RegisterPage.module.css";

function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setErrorMessage("");

    if (form.password !== form.confirmPassword) {
      setErrorMessage("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsSubmitting(true);

    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        password: form.password,
        confirmPassword: form.confirmPassword,
      });

      navigate("/", { replace: true });
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "Không thể đăng ký tài khoản."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className={styles.card}>
      <h1>Đăng ký</h1>

      <form onSubmit={handleSubmit} className={styles.form}>
        <input
          name="fullName"
          aria-label="Họ và tên"
          required
          value={form.fullName}
          placeholder="Họ và tên"
          onChange={handleChange}
        />

        <input
          type="email"
          name="email"
          aria-label="Email"
          required
          value={form.email}
          placeholder="Email"
          onChange={handleChange}
        />

        <input
          name="phone"
          type="tel"
          aria-label="Số điện thoại"
          required
          value={form.phone}
          placeholder="Số điện thoại"
          onChange={handleChange}
        />

        <input
          type="password"
          name="password"
          aria-label="Mật khẩu"
          autoComplete="new-password"
          minLength={6}
          required
          value={form.password}
          placeholder="Mật khẩu"
          onChange={handleChange}
        />

        <input
          type="password"
          name="confirmPassword"
          aria-label="Nhập lại mật khẩu"
          autoComplete="new-password"
          minLength={6}
          required
          value={form.confirmPassword}
          placeholder="Nhập lại mật khẩu"
          onChange={handleChange}
        />

        {errorMessage && <p className={styles.errorMessage} role="alert">{errorMessage}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "ĐANG ĐĂNG KÝ..." : "ĐĂNG KÝ"}
        </button>

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

        <div className={styles.login}>
          Bạn đã có tài khoản?

          <Link to="/login">
            Đăng nhập
          </Link>
        </div>
      </form>
    </section>
  );
}

export default RegisterPage;
