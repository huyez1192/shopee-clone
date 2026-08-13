//Nhiệm vụ:
//    Đọc token
//    Đọc user
//    Lưu thông tin đăng nhập
//    Xóa thông tin đăng nhập



const ACCESS_TOKEN_KEY = "accessToken";
const CURRENT_USER_KEY = "currentUser";

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getStoredUser() {
  const storedUser = localStorage.getItem(CURRENT_USER_KEY);

  if (!storedUser) {
    return null;
  }

  try {
    return JSON.parse(storedUser);
  } catch (error) {
    console.error("Dữ liệu user trong localStorage không hợp lệ.", error);

    localStorage.removeItem(CURRENT_USER_KEY);

    return null;
  }
}

export function saveAuthStorage({ token, user }) {
  if (!token) {
    throw new Error("Không thể lưu thông tin đăng nhập vì token bị thiếu.");
  }

  localStorage.setItem(ACCESS_TOKEN_KEY, token);
  localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
}

export function clearAuthStorage() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(CURRENT_USER_KEY);
}