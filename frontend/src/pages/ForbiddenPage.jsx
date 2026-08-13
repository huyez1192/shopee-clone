import { Link } from "react-router-dom";

function ForbiddenPage() {
  return (
    <main>
      <h1>403</h1>

      <p>
        Bạn không có quyền truy cập trang này.
      </p>

      <Link to="/">
        Quay lại trang chủ
      </Link>
    </main>
  );
}

export default ForbiddenPage;