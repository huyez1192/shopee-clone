import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import EmptyState from "../../../../components/common/EmptyState";
import ErrorState from "../../../../components/common/ErrorState";
import LoadingState from "../../../../components/common/LoadingState";
import Pagination from "../../../../components/common/Pagination";
import fallbackProductImage from "../../../../assets/images/product-placeholder.png";
import { queryKeys } from "../../../../query/queryKeys";
import { getApiErrorMessage } from "../../../../utils/getApiErrorMessage";
import { getSellerReviews, replyToReview } from "../../../review/api/reviewApi";
import StarRating from "../../../review/components/StarRating";
import styles from "./SellerReviewPage.module.css";

const PAGE_SIZE = 10;
function SellerReviewPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [rating, setRating] = useState("");
  const [replyStatus, setReplyStatus] = useState("");
  const [drafts, setDrafts] = useState({});
  const params = { page: page - 1, size: PAGE_SIZE, ...(rating ? { rating } : {}), ...(replyStatus ? { replied: replyStatus === "replied" } : {}) };
  const query = useQuery({ queryKey: queryKeys.seller.reviews.list(params), queryFn: () => getSellerReviews(params) });
  const mutation = useMutation({ mutationFn: replyToReview, onSuccess: async (review) => { setDrafts((current) => ({ ...current, [review.id]: "" })); await Promise.all([queryClient.invalidateQueries({ queryKey: queryKeys.seller.reviews.all }), queryClient.invalidateQueries({ queryKey: queryKeys.reviews.productRoot(review.productId) })]); } });
  const reviews = query.data?.content || [];
  function changeFilter(setter, value) { setter(value); setPage(1); }
  return <section className={styles.page}>
    <header><h1>Quản Lý Đánh Giá</h1><p>Theo dõi và phản hồi đánh giá từ người mua</p></header>
    <div className={styles.filters}><label>Mức đánh giá<select value={rating} onChange={(event) => changeFilter(setRating, event.target.value)}><option value="">Tất cả</option>{[5,4,3,2,1].map((star) => <option key={star} value={star}>{star} sao</option>)}</select></label><label>Phản hồi<select value={replyStatus} onChange={(event) => changeFilter(setReplyStatus, event.target.value)}><option value="">Tất cả</option><option value="unreplied">Chưa phản hồi</option><option value="replied">Đã phản hồi</option></select></label></div>
    {query.isLoading ? <LoadingState message="Đang tải đánh giá..." /> : query.error ? <ErrorState message={getApiErrorMessage(query.error, "Không thể tải đánh giá.")} onRetry={() => query.refetch()} /> : reviews.length === 0 ? <EmptyState title="Không có đánh giá phù hợp" description="Thử thay đổi bộ lọc để xem các đánh giá khác." /> : <><div className={styles.list}>{reviews.map((review) => <article key={review.id} className={styles.card}>
      <div className={styles.product}><img src={review.productImageUrl || fallbackProductImage} alt="" /><div><strong>{review.productName}</strong><span>Đơn hàng #{review.orderItemId}</span></div></div>
      <div className={styles.review}><div className={styles.meta}><strong>{review.buyerDisplayName}</strong><StarRating value={review.rating} /></div><p>{review.comment || "Người mua không để lại bình luận."}</p>
        {review.sellerReply ? <div className={styles.existingReply}><strong>Phản hồi của shop</strong><p>{review.sellerReply}</p></div> : <div className={styles.replyBox}><textarea maxLength={2000} value={drafts[review.id] || ""} onChange={(event) => setDrafts((current) => ({ ...current, [review.id]: event.target.value }))} placeholder="Nhập phản hồi cho người mua..." /><button type="button" disabled={!drafts[review.id]?.trim() || (mutation.isPending && mutation.variables?.reviewId === review.id)} onClick={() => mutation.mutate({ reviewId: review.id, reply: drafts[review.id].trim() })}>Phản hồi</button>{mutation.error && mutation.variables?.reviewId === review.id && <span className={styles.error}>{getApiErrorMessage(mutation.error, "Không thể gửi phản hồi.")}</span>}</div>}
      </div></article>)}</div><Pagination currentPage={page} totalPages={query.data?.totalPages || 0} onPageChange={setPage} /></>}
  </section>;
}
export default SellerReviewPage;
