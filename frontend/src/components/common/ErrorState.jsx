function ErrorState({
  message = "Đã xảy ra lỗi.",
  onRetry,
}) {
  return (
    <div role="alert">
      <p>{message}</p>

      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
        >
          Thử lại
        </button>
      )}
    </div>
  );
}

export default ErrorState;