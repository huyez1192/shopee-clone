import { useState } from "react";
import { useQuery } from "@tanstack/react-query";

import EmptyState from "../../../components/common/EmptyState";
import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";
import Pagination from "../../../components/common/Pagination";
import fallbackProductImage from "../../../assets/images/product-placeholder.png";
import { queryKeys } from "../../../query/queryKeys";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { getProductReviews, getProductReviewSummary } from "../api/reviewApi";
import StarRating from "./StarRating";
import styles from "./ProductReviews.module.css";

const PAGE_SIZE = 6;

function formatDate(value) {
  return value ? new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(value)) : "";
}

function ProductReviews({ productId }) {
  const [rating, setRating] = useState(null);
  const [page, setPage] = useState(1);
  const params = { page: page - 1, size: PAGE_SIZE, ...(rating ? { rating } : {}) };
  const summaryQuery = useQuery({ queryKey: queryKeys.reviews.summary(productId), queryFn: () => getProductReviewSummary(productId), enabled: Boolean(productId) });
  const reviewsQuery = useQuery({ queryKey: queryKeys.reviews.product(productId, params), queryFn: () => getProductReviews(productId, params), enabled: Boolean(productId) });
  const summary = summaryQuery.data;
  const reviews = reviewsQuery.data?.content || [];
  const counts = { 5: summary?.fiveStarCount || 0, 4: summary?.fourStarCount || 0, 3: summary?.threeStarCount || 0, 2: summary?.twoStarCount || 0, 1: summary?.oneStarCount || 0 };
  const error = summaryQuery.error || reviewsQuery.error;

  function chooseRating(value) { setRating(value); setPage(1); }

  return (
    <section className={styles.section}>
      <h2>ĐÁNH GIÁ SẢN PHẨM</h2>
      {summaryQuery.isLoading ? <LoadingState message="Đang tải tổng quan đánh giá..." /> : summaryQuery.error ? (
        <ErrorState message={getApiErrorMessage(summaryQuery.error, "Không thể tải tổng quan đánh giá.")} onRetry={() => summaryQuery.refetch()} />
      ) : (
        <div className={styles.summary}>
          <div className={styles.score}><div><strong>{Number(summary?.averageRating || 0).toFixed(1)}</strong> trên 5</div><StarRating value={Math.round(summary?.averageRating || 0)} /></div>
          <div className={styles.filters}>
            <button className={rating == null ? styles.activeFilter : ""} onClick={() => chooseRating(null)}>Tất Cả</button>
            {[5, 4, 3, 2, 1].map((star) => <button key={star} className={rating === star ? styles.activeFilter : ""} onClick={() => chooseRating(star)}>{star} Sao ({counts[star]})</button>)}
          </div>
        </div>
      )}
      {reviewsQuery.isLoading ? <LoadingState message="Đang tải đánh giá..." /> : error ? (
        <ErrorState message={getApiErrorMessage(error, "Không thể tải đánh giá sản phẩm.")} onRetry={() => reviewsQuery.refetch()} />
      ) : reviews.length === 0 ? (
        <EmptyState title={rating ? `Chưa có đánh giá ${rating} sao` : "Chưa có đánh giá"} description="Hãy quay lại sau khi người mua chia sẻ trải nghiệm." />
      ) : (
        <>
          <div>{reviews.map((review) => <article key={review.id} className={styles.review}>
            <img src={review.buyerAvatarUrl || fallbackProductImage} alt="" onError={(event) => { event.currentTarget.src = fallbackProductImage; }} />
            <div><div className={styles.buyer}>{review.buyerDisplayName}</div><StarRating value={review.rating} /><div className={styles.date}>{formatDate(review.createdAt)}</div>
              <p>{review.comment || "Người mua không để lại bình luận."}</p>
              {review.sellerReply && <div className={styles.reply}><strong>Phản hồi của Người Bán</strong><p>{review.sellerReply}</p></div>}
            </div>
          </article>)}</div>
          <Pagination currentPage={page} totalPages={reviewsQuery.data?.totalPages || 0} onPageChange={setPage} />
        </>
      )}
    </section>
  );
}

export default ProductReviews;
