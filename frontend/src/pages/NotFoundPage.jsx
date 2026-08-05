import { Link } from "react-router-dom";

function NotFoundPage() {
  return (
    <main>
      <h1>404</h1>
      <p>Không tìm thấy trang.</p>
      <Link to="/">Quay lại trang chủ</Link>
    </main>
  );
}

export default NotFoundPage;