import { useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";

import fallbackProductImage from "../../../assets/images/product-placeholder.png";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { uploadImage } from "../api/uploadApi";
import { isValidImageUrl, validateImageFile } from "../utils/imageValidation";

import styles from "./ImageSourceInput.module.css";

const CLOUDINARY_DISABLED_MESSAGE = "Chức năng tải ảnh hiện chưa được bật. Bạn vẫn có thể nhập URL ảnh.";

function ImageSourceInput({
  value,
  onChange,
  purpose,
  label = "Ảnh",
  disabled = false,
  onUploadingChange,
}) {
  const [mode, setMode] = useState("url");
  const [fileError, setFileError] = useState("");
  const [failedPreviewUrl, setFailedPreviewUrl] = useState("");
  const [lastFile, setLastFile] = useState(null);
  const fileInputRef = useRef(null);

  const uploadMutation = useMutation({
    mutationFn: async (file) => {
      const response = await uploadImage(file, purpose);
      if (!response?.secureUrl) throw new Error("Backend không trả về URL ảnh hợp lệ.");
      return response;
    },
    onSuccess: (response) => {
      setFileError("");
      setFailedPreviewUrl("");
      onChange(response.secureUrl);
    },
  });

  useEffect(() => {
    onUploadingChange?.(uploadMutation.isPending);
  }, [onUploadingChange, uploadMutation.isPending]);

  const trimmedValue = value.trim();
  const urlError = mode === "url" && trimmedValue && !isValidImageUrl(trimmedValue)
    ? "URL ảnh phải bắt đầu bằng http:// hoặc https://."
    : "";
  const uploadError = uploadMutation.error
    ? uploadMutation.error.response?.status === 503
      ? CLOUDINARY_DISABLED_MESSAGE
      : getApiErrorMessage(uploadMutation.error, "Không thể tải ảnh lên.")
    : "";
  const previewError = Boolean(trimmedValue) && failedPreviewUrl === trimmedValue;
  const canPreview = Boolean(trimmedValue) && isValidImageUrl(trimmedValue) && !previewError;

  function switchMode(nextMode) {
    if (nextMode === mode) return;
    setMode(nextMode);
    setFileError("");
    setLastFile(null);
    setFailedPreviewUrl("");
    uploadMutation.reset();
    onChange("");
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  function startUpload(file) {
    const validationMessage = validateImageFile(file);
    if (validationMessage) {
      setFileError(validationMessage);
      setLastFile(null);
      return;
    }

    setFileError("");
    setLastFile(file);
    setFailedPreviewUrl("");
    uploadMutation.reset();
    onChange("");
    uploadMutation.mutate(file);
  }

  function handleFileChange(event) {
    const [file] = event.target.files;
    if (file) startUpload(file);
    event.target.value = "";
  }

  function clearImage() {
    onChange("");
    setFileError("");
    setLastFile(null);
    setFailedPreviewUrl("");
    uploadMutation.reset();
  }

  const controlsDisabled = disabled || uploadMutation.isPending;

  return (
    <div className={styles.wrapper}>
      <div className={styles.modeSwitch} role="group" aria-label={`Cách cung cấp ${label.toLowerCase()}`}>
        <button type="button" className={mode === "url" ? styles.activeMode : styles.modeButton} onClick={() => switchMode("url")} disabled={controlsDisabled}>
          Dùng URL
        </button>
        <button type="button" className={mode === "upload" ? styles.activeMode : styles.modeButton} onClick={() => switchMode("upload")} disabled={controlsDisabled}>
          Tải ảnh lên
        </button>
      </div>

      <div className={styles.layout}>
        <div className={styles.preview}>
          {canPreview ? (
            <img src={trimmedValue} alt={`Xem trước ${label.toLowerCase()}`} onError={() => setFailedPreviewUrl(trimmedValue)} />
          ) : (
            <div className={styles.placeholder}>
              <img src={fallbackProductImage} alt="" aria-hidden="true" />
              <span>{previewError ? "Không thể tải ảnh xem trước." : "Chưa có ảnh"}</span>
            </div>
          )}
        </div>

        <div className={styles.controls}>
          {mode === "url" ? (
            <label className={styles.urlField}>
              <span>URL ảnh</span>
              <input
                type="text"
                inputMode="url"
                value={value}
                placeholder="https://example.com/image.jpg"
                onChange={(event) => onChange(event.target.value)}
                disabled={disabled}
                aria-invalid={Boolean(urlError)}
              />
            </label>
          ) : (
            <div className={styles.uploadControls}>
              <input
                ref={fileInputRef}
                className={styles.hiddenInput}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleFileChange}
                disabled={controlsDisabled}
                aria-label={`Chọn ${label.toLowerCase()} từ máy`}
              />
              <button type="button" className={styles.chooseButton} onClick={() => fileInputRef.current?.click()} disabled={controlsDisabled}>
                {trimmedValue ? "Thay ảnh" : "Chọn tệp"}
              </button>
              <small>JPEG, PNG hoặc WEBP. Tối đa 5 MB.</small>
            </div>
          )}

          {uploadMutation.isPending && <p className={styles.status} role="status">Đang tải ảnh...</p>}
          {(urlError || fileError || uploadError) && (
            <div className={styles.error} role="alert">
              <span>{urlError || fileError || uploadError}</span>
              {uploadError && lastFile && (
                <button type="button" onClick={() => startUpload(lastFile)} disabled={controlsDisabled}>Thử lại</button>
              )}
            </div>
          )}

          {trimmedValue && (
            <button type="button" className={styles.removeButton} onClick={clearImage} disabled={controlsDisabled}>Xóa ảnh</button>
          )}
        </div>
      </div>
    </div>
  );
}

export default ImageSourceInput;
