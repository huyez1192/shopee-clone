import { useEffect, useState } from "react";
import axiosClient from "./api/axiosClient";

function App() {
  const [products, setProducts] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadProducts = async () => {
      try {
        const response = await axiosClient.get("/products");
        setProducts(response.data);
      } catch (requestError) {
        console.error(requestError);
        setError("Không thể tải danh sách sản phẩm.");
      }
    };

    loadProducts();
  }, []);

  return (
    <main>
      <h1>Mini Shopee</h1>

      {error && <p>{error}</p>}

      <pre>{JSON.stringify(products, null, 2)}</pre>
    </main>
  );
}

export default App;