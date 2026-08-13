function LoadingState({
  message = "Đang tải dữ liệu...",
}) {
  return (
    <div role="status">
      <p>{message}</p>
    </div>
  );
}

export default LoadingState;