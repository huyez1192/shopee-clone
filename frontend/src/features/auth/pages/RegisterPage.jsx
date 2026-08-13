import { useState } from "react";
import {Link,useLocation,useNavigate,} from "react-router-dom";

import useAuth from "../hooks/useAuth";

import {getApiErrorMessage,getApiFieldErrors,} from "../../../utils/getApiErrorMessage";

const INITIAL_FORM_DATA = {
  fullName: "",
  email: "",
  phone: "",
  password: "",
  confirmPassword: "",
};

function RegisterPage() {
  const { register } = useAuth();

  const navigate = useNavigate();
  const location = useLocation();

  const redirectPath =
    location.state?.redirectPath || "/";

  const [formData, setFormData] =
    useState(INITIAL_FORM_DATA);

  const [fieldErrors, setFieldErrors] =
    useState({});

  const [errorMessage, setErrorMessage] =
    useState("");

  const [isSubmitting, setIsSubmitting] =
    useState(false);

  function handleChange(event) {
    const { name, value } = event.target;

    setFormData((currentFormData) => ({
      ...currentFormData,
      [name]: value,
    }));

    setFieldErrors((currentErrors) => ({
      ...currentErrors,
      [name]: "",
    }));

    setErrorMessage("");
  }

  async function handleSubmit(event) {
    event.preventDefault();

    setErrorMessage("");
    setFieldErrors({});

    if (formData.password !== formData.confirmPassword) {
      setFieldErrors({
        confirmPassword:
          "Mật khẩu xác nhận không khớp.",
      });

      return;
    }

    setIsSubmitting(true);

    try {
      await register({
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        password: formData.password,
        confirmPassword: formData.confirmPassword,
      });

      navigate(redirectPath, {
        replace: true,
      });
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          "Không thể đăng ký tài khoản.",
        ),
      );

      setFieldErrors(
        getApiFieldErrors(error),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main>
      <h1>Đăng ký</h1>

      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="fullName">
            Họ và tên
          </label>

          <input
            id="fullName"
            name="fullName"
            type="text"
            value={formData.fullName}
            onChange={handleChange}
            autoComplete="name"
            required
          />

          {fieldErrors.fullName && (
            <p role="alert">
              {fieldErrors.fullName}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="email">
            Email
          </label>

          <input
            id="email"
            name="email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            autoComplete="email"
            required
          />

          {fieldErrors.email && (
            <p role="alert">
              {fieldErrors.email}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="phone">
            Số điện thoại
          </label>

          <input
            id="phone"
            name="phone"
            type="tel"
            value={formData.phone}
            onChange={handleChange}
            autoComplete="tel"
            required
          />

          {fieldErrors.phone && (
            <p role="alert">
              {fieldErrors.phone}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="password">
            Mật khẩu
          </label>

          <input
            id="password"
            name="password"
            type="password"
            value={formData.password}
            onChange={handleChange}
            autoComplete="new-password"
            minLength={6}
            required
          />

          {fieldErrors.password && (
            <p role="alert">
              {fieldErrors.password}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="confirmPassword">
            Nhập lại mật khẩu
          </label>

          <input
            id="confirmPassword"
            name="confirmPassword"
            type="password"
            value={formData.confirmPassword}
            onChange={handleChange}
            autoComplete="new-password"
            minLength={6}
            required
          />

          {fieldErrors.confirmPassword && (
            <p role="alert">
              {fieldErrors.confirmPassword}
            </p>
          )}
        </div>

        {errorMessage && (
          <p role="alert">
            {errorMessage}
          </p>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
        >
          {isSubmitting
            ? "Đang đăng ký..."
            : "Đăng ký"}
        </button>
      </form>

      <p>
        Đã có tài khoản?{" "}
        <Link
          to="/login"
          state={{ redirectPath }}
        >
          Đăng nhập
        </Link>
      </p>
    </main>
  );
}

export default RegisterPage;