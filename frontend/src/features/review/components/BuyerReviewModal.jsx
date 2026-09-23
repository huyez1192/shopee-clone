import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import fallbackProductImage from "../../../assets/images/product-placeholder.png";
import { queryKeys } from "../../../query/queryKeys";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { createReview } from "../api/reviewApi";
import StarRating from "./StarRating";
import styles from "./BuyerReviewModal.module.css";

function BuyerReviewModal({ item, orderId, onClose }) {
  const queryClient = useQueryClient();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const mutation = useMutation({ mutationFn: createReview, onSuccess: async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.orders.detail(orderId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.reviews.productRoot(item.productId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.reviews.summary(item.productId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.products.detail(item.productId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.products.all }),
    ]);
    onClose();
  }});
  useEffect(() => { const handler = (event) => event.key === "Escape" && !mutation.isPending && onClose(); document.addEventListener("keydown", handler); return () => document.removeEventListener("keydown", handler); }, [mutation.isPending, onClose]);
  function submit(event) { event.preventDefault(); mutation.mutate({ orderItemId: item.id, rating, comment: comment.trim() || null }); }
  return createPortal(<div className={styles.backdrop} onMouseDown={(event) => event.target === event.currentTarget && !mutation.isPending && onClose()}>
    <section className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="review-title">
      <h2 id="review-title">Đánh Giá Sản Phẩm</h2>
      <div className={styles.product}><img src={item.productImageUrl || fallbackProductImage} alt="" /><div><strong>{item.productName}</strong><span>Chất lượng sản phẩm</span></div></div>
      <StarRating value={rating} interactive onChange={setRating} label="Chất lượng sản phẩm" />
      <form onSubmit={submit}><textarea value={comment} maxLength={2000} onChange={(event) => setComment(event.target.value)} placeholder="Hãy chia sẻ những điều bạn thích về sản phẩm này với những người mua khác nhé." autoFocus /><div className={styles.count}>{comment.length}/2000</div>
        {mutation.error && <p className={styles.error}>{getApiErrorMessage(mutation.error, "Không thể gửi đánh giá.")}</p>}
        <div className={styles.actions}><button type="button" onClick={onClose} disabled={mutation.isPending}>TRỞ LẠI</button><button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Đang gửi..." : "Hoàn Thành"}</button></div>
      </form>
    </section>
  </div>, document.body);
}
export default BuyerReviewModal;
