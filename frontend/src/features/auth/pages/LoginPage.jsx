import { useState } from "react";
import {Link, useLocation, useNavigate,} from "react-router-dom";

import useAuth from "../hooks/useAuth";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";

function LoginPage() {
  const { login } = useAuth();

  const navigate = useNavigate();
  const location = useLocation();

  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const redirectPath =
    location.state?.redirectPath || "/";

  function handleChange(event) {
    const { name, value } = event.target;

    setFormData((currentFormData) => ({
      ...currentFormData,
      [name]: value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    setErrorMessage("");
    setIsSubmitting(true);

    try {
      await login({
        email: formData.email.trim(),
        password: formData.password,
      });

      navigate(redirectPath, {
        replace: true,
      });
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          "Email hoặc mật khẩu không chính xác.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main>
      <h1>Đăng nhập</h1>

      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">Email</label>

          <input
            id="email"
            name="email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            autoComplete="email"
            required
          />
        </div>

        <div>
          <label htmlFor="password">Mật khẩu</label>

          <input
            id="password"
            name="password"
            type="password"
            value={formData.password}
            onChange={handleChange}
            autoComplete="current-password"
            required
          />
        </div>

        {errorMessage && (
          <p role="alert">{errorMessage}</p>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
        >
          {isSubmitting
            ? "Đang đăng nhập..."
            : "Đăng nhập"}
        </button>
      </form>

      <p>
        Chưa có tài khoản?{" "}
        <Link
          to="/register"
          state={{ redirectPath }}
        >
          Đăng ký
        </Link>
      </p>
    </main>
  );
}

export default LoginPage;