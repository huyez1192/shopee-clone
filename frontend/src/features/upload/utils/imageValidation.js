export const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

export const ALLOWED_IMAGE_TYPES = [
  "image/jpeg",
  "image/png",
  "image/webp",
];

export function isValidImageUrl(value) {
  const trimmedValue = value.trim();
  if (!trimmedValue) return true;

  try {
    const url = new URL(trimmedValue);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

export function validateImageFile(file) {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return "Chỉ hỗ trợ ảnh JPEG, PNG hoặc WEBP.";
  }

  if (file.size > MAX_IMAGE_SIZE) {
    return "Tệp ảnh không được vượt quá 5 MB.";
  }

  return "";
}
