import { useParams } from "react-router-dom";

function ProductDetailPage() {
  const { productId } = useParams();

  return (
    <main>
      <h1>Chi tiết sản phẩm</h1>
      <p>Product ID: {productId}</p>
    </main>
  );
}

export default ProductDetailPage;