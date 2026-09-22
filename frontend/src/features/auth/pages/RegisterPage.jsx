import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import useAuth from "../hooks/useAuth";

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

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (form.password !== form.confirmPassword) {
      alert("Mật khẩu xác nhận không khớp.");
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
      alert(error?.response?.data?.message || error?.message || "Không thể đăng ký tài khoản.");
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
          value={form.fullName}
          placeholder="Họ và tên"
          onChange={handleChange}
        />

        <input
          type="email"
          name="email"
          value={form.email}
          placeholder="Email"
          onChange={handleChange}
        />

        <input
          name="phone"
          value={form.phone}
          placeholder="Số điện thoại"
          onChange={handleChange}
        />

        <input
          type="password"
          name="password"
          value={form.password}
          placeholder="Mật khẩu"
          onChange={handleChange}
        />

        <input
          type="password"
          name="confirmPassword"
          value={form.confirmPassword}
          placeholder="Nhập lại mật khẩu"
          onChange={handleChange}
        />

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "ĐANG ĐĂNG KÝ..." : "ĐĂNG KÝ"}
        </button>

        <div className={styles.divider}>
          <span />
          <p>HOẶC</p>
          <span />
        </div>

        <div className={styles.social}>
          <button type="button">
            <strong>f</strong>
            Facebook
          </button>

          <button type="button">
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