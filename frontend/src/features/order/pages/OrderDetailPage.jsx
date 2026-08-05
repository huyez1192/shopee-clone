import { useParams } from "react-router-dom";

function OrderDetailPage() {
  const { orderId } = useParams();

  return (
    <main>
      <h1>Chi tiết đơn hàng</h1>
      <p>Order ID: {orderId}</p>
    </main>
  );
}

export default OrderDetailPage;