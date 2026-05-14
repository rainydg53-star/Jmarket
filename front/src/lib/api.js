import { clearAccessToken, getAccessToken, setAccessToken } from "./auth";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

let refreshPromise = null;

export async function refreshAccessToken() {
  refreshPromise ??= fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: "POST",
    credentials: "include",
  })
    .then(async (response) => {
      const data = await response.json().catch(() => null);
      if (!response.ok || !data?.accessToken) {
        throw new Error(data?.message || "Token refresh failed");
      }
      setAccessToken(data.accessToken);
      return data.accessToken;
    })
    .catch((error) => {
      clearAccessToken();
      throw error;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

export async function api(path, options = {}) {
  return request(path, options, true);
}

async function request(path, options = {}, allowRefresh) {
  const token = getAccessToken();
  const isFormData = options.body instanceof FormData;
  const headers = {
    ...(isFormData ? {} : { "Content-Type": "application/json" }),
    ...(options.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: "include",
  });

  const data = await response.json().catch(() => null);
  if (response.status === 401 && allowRefresh && path !== "/api/auth/refresh" && path !== "/api/auth/logout") {
    await refreshAccessToken();
    return request(path, options, false);
  }
  if (!response.ok) {
    const error = new Error(data?.message || "API request failed");
    error.status = response.status;
    error.code = data?.code;
    throw error;
  }
  return data ?? {};
}
