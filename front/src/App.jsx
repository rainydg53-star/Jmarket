import { useEffect, useRef, useState } from "react";
import { Link, Navigate, NavLink, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import "./App.css";
import Footer from "./components/Footer";
import LoginPage from "./pages/LoginPage";
import SignUpPage from "./pages/SignUpPage";
import MePage from "./pages/MePage";
import HomePage from "./pages/HomePage";
import ProductsPage from "./pages/ProductsPage";
import ProductCreatePage from "./pages/ProductCreatePage";
import ProductDetailPage from "./pages/ProductDetailPage";
import ProductEditPage from "./pages/ProductEditPage";
import TradesPage from "./pages/TradesPage";
import AuctionsPage from "./pages/AuctionsPage";
import AuctionProductsPage from "./pages/AuctionProductsPage";
import AuctionDetailPage from "./pages/AuctionDetailPage";
import ChatRoomsPage from "./pages/ChatRoomsPage";
import ChatRoomPage from "./pages/ChatRoomPage";
import MileagePage from "./pages/MileagePage";
import SupportPage from "./pages/SupportPage";
import ReportsPage from "./pages/ReportsPage";
import ReportDetailPage from "./pages/ReportDetailPage";
import NotificationsPage from "./pages/NotificationsPage";
import SocialCallbackPage from "./pages/SocialCallbackPage";
import FindAccountPage from "./pages/FindAccountPage";
import AdminPage from "./pages/Admin/AdminPage";
import UserProfilePage from "./pages/UserProfilePage";
import { getAccessToken, isAuthenticated, logout as logoutSession } from "./lib/auth";
import { api, refreshAccessToken } from "./lib/api";
import { openChatWindow } from "./lib/chatWindow";
import { API_BASE_URL } from "./lib/config";
import { resolveNotificationLink } from "./lib/notificationLinks";
import { canCreateAuction, canCreateProduct, isAdmin } from "./lib/permissions";

const MAX_TOAST_COUNT = 5;
const TOAST_AUTO_CLOSE_MS = 6000;

const TOAST_TYPE_LABELS = {
  AUCTION_OUTBID: "상위입찰",
  AUCTION_OUTBID_LOST: "입찰밀림",
  AUCTION_WON: "낙찰",
  AUCTION_SOLD: "낙찰확정",
  TRADE_REQUESTED: "구매신청",
  TRADE_COMPLETED: "거래완료",
  TRADE_REVIEW_REQUESTED: "후기요청",
  REVIEW_RECEIVED: "리뷰",
  REPORT_RESOLVED: "신고처리",
  USER_RESTRICTED: "이용제한",
  MILEAGE_WITHDRAWAL_REQUESTED: "출금요청",
  CHAT_MESSAGE: "채팅",
};

function RequireAuth({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function RequireAdmin({ children }) {
  const [status, setStatus] = useState("checking");

  useEffect(() => {
    let active = true;

    (async () => {
      try {
        await api("/api/admin/dashboard");
        if (active) {
          setStatus("allowed");
        }
      } catch (error) {
        if (active) {
          setStatus(error.status === 401 ? "unauthorized" : "denied");
        }
      }
    })();

    return () => {
      active = false;
    };
  }, []);

  if (status === "checking") {
    return (
      <main className="container">
        <section className="card admin-access-card">
          <h1>관리자 권한 확인 중</h1>
          <p className="meta">서버 권한을 확인하고 있습니다.</p>
        </section>
      </main>
    );
  }

  if (status === "unauthorized") {
    return <Navigate to="/login" replace />;
  }

  if (status !== "allowed") {
    return (
      <main className="container">
        <section className="card admin-access-card">
          <h1>접근할 수 없습니다</h1>
          <p className="meta">관리자 권한이 있는 계정만 관리자 페이지에 접근할 수 있습니다.</p>
          <Link className="primary-link-button" to="/">홈으로 이동</Link>
        </section>
      </main>
    );
  }

  return children;
}

function RequireCapability({ children, canAccess, message }) {
  const [status, setStatus] = useState("checking");

  useEffect(() => {
    let active = true;

    (async () => {
      try {
        const me = await api("/api/auth/me");
        if (active) {
          setStatus(canAccess(me) ? "allowed" : "denied");
        }
      } catch (error) {
        if (active) {
          setStatus(error.status === 401 ? "unauthorized" : "denied");
        }
      }
    })();

    return () => {
      active = false;
    };
  }, [canAccess]);

  if (status === "checking") {
    return (
      <main className="container">
        <section className="card admin-access-card">
          <h1>권한 확인 중</h1>
          <p className="meta">사용 가능한 기능인지 확인하고 있습니다.</p>
        </section>
      </main>
    );
  }

  if (status === "unauthorized") {
    return <Navigate to="/login" replace />;
  }

  if (status !== "allowed") {
    return (
      <main className="container">
        <section className="card admin-access-card">
          <h1>접근할 수 없습니다</h1>
          <p className="meta">{message}</p>
          <Link className="primary-link-button" to="/">홈으로 이동</Link>
        </section>
      </main>
    );
  }

  return children;
}

function NavDropdown({ label, items, active }) {
  const location = useLocation();
  const currentUrl = `${location.pathname}${location.search}`;

  const isItemActive = (to) => {
    if (to.includes("?")) {
      return currentUrl === to;
    }
    return location.pathname === to;
  };

  return (
    <div className={`nav-dropdown${active ? " active" : ""}`}>
      <Link to={items[0].to} className="nav-link nav-dropdown-trigger">
        {label}
      </Link>
      <div className="nav-dropdown-menu">
        {items.map((item) => (
          <Link
            key={item.to}
            to={item.to}
            className={`nav-dropdown-item${isItemActive(item.to) ? " active" : ""}`}
          >
            {item.label}
          </Link>
        ))}
      </div>
    </div>
  );
}

function TopNav() {
  const navigate = useNavigate();
  const location = useLocation();
  const authed = isAuthenticated();
  const [welcomeName, setWelcomeName] = useState("");
  const [me, setMe] = useState(null);

  useEffect(() => {
    let active = true;

    if (!authed) {
      return () => {
        active = false;
      };
    }

    (async () => {
      try {
        const me = await api("/api/auth/me");
        if (!active) {
          return;
        }

        const displayName = (me?.name ?? "").trim() || (me?.nickname ?? "").trim();
        setWelcomeName(displayName);
        setMe(me);
      } catch {
        if (active) {
          setWelcomeName("");
          setMe(null);
        }
      }
    })();

    return () => {
      active = false;
    };
  }, [authed]);

  const logout = async () => {
    await logoutSession();
    setWelcomeName("");
    setMe(null);
    navigate("/login", { replace: true });
  };

  const path = location.pathname;
  const myMenuItems = [
    { to: "/me", label: "내 정보" },
    { to: "/notifications", label: "알림" },
    { to: "/mileage", label: "마일리지" },
    { to: "/support?mode=create", label: "상담하기" },
    { to: "/support?mode=list", label: "내 상담 목록" },
    { to: "/reports?mode=create", label: "신고하기" },
    { to: "/reports?mode=list", label: "내 신고 목록" },
    ...(isAdmin(me) ? [{ to: "/admin", label: "관리자" }] : []),
  ];
  const productMenuItems = [
    { to: "/products", label: "상품 목록" },
    ...(canCreateProduct(me) ? [{ to: "/products/new", label: "상품 등록" }] : []),
  ];
  const auctionMenuItems = [
    { to: "/auctions/products", label: "경매 목록" },
    ...(canCreateAuction(me) ? [{ to: "/auctions/manage", label: "경매 등록" }] : []),
  ];

  return (
    <header className="top-nav">
      <div className="top-nav-inner">
        <Link to="/" className="brand-link">Jmarket</Link>
        <nav className="top-nav-links" aria-label="주요 메뉴">
          {!authed ? (
            <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
              홈
            </NavLink>
          ) : (
            <>
              <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
                홈
              </NavLink>
              <NavDropdown
                label="상품"
                active={path === "/products" || path === "/products/new" || path.startsWith("/products/")}
                items={productMenuItems}
              />
              <NavDropdown
                label="경매"
                active={path === "/auctions" || path.startsWith("/auctions/")}
                items={auctionMenuItems}
              />
              <NavLink to="/trades" className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
                거래
              </NavLink>
              <button
                type="button"
                className={`nav-link nav-button${path.startsWith("/chat/rooms") ? " active" : ""}`}
                onClick={() => openChatWindow()}
              >
                채팅
              </button>
              <NavDropdown
                label="마이"
                active={path === "/me" || path === "/notifications" || path === "/mileage" || path === "/support" || path === "/reports" || path === "/admin"}
                items={myMenuItems}
              />
            </>
          )}
        </nav>
        <div className="top-nav-right">
          {authed ? (
            <>
              <span className="nav-status">{welcomeName || "사용자"}님 환영합니다</span>
              {location.pathname !== "/login" ? (
                <button className="nav-logout" onClick={logout}>로그아웃</button>
              ) : null}
            </>
          ) : (
            <>
              <NavLink to="/login" className={({ isActive }) => `nav-link nav-auth-link${isActive ? " active" : ""}`}>
                로그인
              </NavLink>
              <NavLink to="/signup" className={({ isActive }) => `nav-link nav-auth-link${isActive ? " active" : ""}`}>
                회원가입
              </NavLink>
            </>
          )}
        </div>
      </div>
    </header>
  );
}

function NotificationToasts() {
  const navigate = useNavigate();
  const authed = isAuthenticated();
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef(new Map());

  useEffect(() => () => {
    timersRef.current.forEach((timerId) => clearTimeout(timerId));
    timersRef.current.clear();
  }, []);

  useEffect(() => {
    if (!authed) {
      return undefined;
    }

    let reconnectTimer = null;
    let eventSource = null;
    let closed = false;

    const removeToast = (id) => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
      const timerId = timersRef.current.get(id);
      if (timerId) {
        clearTimeout(timerId);
        timersRef.current.delete(id);
      }
    };

    const pushToast = (payload) => {
      const id = payload.notificationId ?? `${Date.now()}-${Math.random()}`;
      const toast = {
        id,
        notificationId: payload.notificationId ?? null,
        type: payload.type ?? "CHAT_MESSAGE",
        title: payload.title ?? "알림",
        message: payload.message ?? "",
        link: payload.link ?? null,
      };

      setToasts((prev) => [...prev, toast].slice(-MAX_TOAST_COUNT));
      const timerId = setTimeout(() => removeToast(id), TOAST_AUTO_CLOSE_MS);
      timersRef.current.set(id, timerId);
    };

    const connect = () => {
      let token = getAccessToken();
      if (!token) {
        return;
      }

      const streamUrl = `${API_BASE_URL}/api/notifications/stream?accessToken=${encodeURIComponent(token)}`;
      eventSource = new EventSource(streamUrl);

      eventSource.addEventListener("notification", (event) => {
        try {
          const payload = JSON.parse(event.data);
          pushToast(payload);
        } catch (error) {
          console.error("Failed to parse notification payload", error);
        }
      });

      eventSource.onerror = () => {
        if (eventSource) {
          eventSource.close();
        }
        if (!closed) {
          reconnectTimer = setTimeout(async () => {
            try {
              token = await refreshAccessToken();
            } catch {
              return;
            }
            if (token && !closed) {
              connect();
            }
          }, 2000);
        }
      };
    };

    connect();

    return () => {
      closed = true;
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
      }
      if (eventSource) {
        eventSource.close();
      }
    };
  }, [authed]);

  if (!authed || toasts.length === 0) {
    return null;
  }

  return (
    <div className="toast-stack">
      {toasts.map((toast) => (
        <button
          key={toast.id}
          type="button"
          className={`toast-item toast-${toast.type.toLowerCase()}`}
          onClick={async () => {
            if (toast.notificationId) {
              try {
                await api(`/api/notifications/${toast.notificationId}/read`, { method: "PATCH" });
              } catch (error) {
                console.error("Failed to mark notification as read", error);
              }
            }
            const link = resolveNotificationLink(toast);
            if (link) {
              if (toast.type === "CHAT_MESSAGE") {
                const roomId = link.match(/\/chat\/rooms\/(\d+)/)?.[1] ?? null;
                openChatWindow(roomId);
                return;
              }
              navigate(link);
            }
          }}
        >
          <div className="toast-title">{toast.title}</div>
          <div className="meta">{TOAST_TYPE_LABELS[toast.type] ?? toast.type ?? "알림"}</div>
          <div className="toast-message">{toast.message}</div>
        </button>
      ))}
    </div>
  );
}

function App() {
  const location = useLocation();
  const isChatPopup = location.pathname.startsWith("/chat/rooms")
    && new URLSearchParams(location.search).get("popup") === "1";

  return (
    <>
      {isChatPopup ? null : <TopNav />}
      <NotificationToasts />
      <Routes>
        <Route
          path="/"
          element={<HomePage />}
        />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/find-account" element={<FindAccountPage />} />
        <Route path="/oauth/callback/:provider" element={<SocialCallbackPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route
          path="/products"
          element={(
            <RequireAuth>
              <ProductsPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/products/new"
          element={(
            <RequireAuth>
              <RequireCapability canAccess={canCreateProduct} message="현재 계정은 상품 등록 기능을 사용할 수 없습니다.">
                <ProductCreatePage />
              </RequireCapability>
            </RequireAuth>
          )}
        />
        <Route
          path="/products/:productId/edit"
          element={(
            <RequireAuth>
              <ProductEditPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/products/:productId"
          element={(
            <RequireAuth>
              <ProductDetailPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/trades"
          element={(
            <RequireAuth>
              <TradesPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/auctions"
          element={<Navigate to="/auctions/products" replace />}
        />
        <Route
          path="/auctions/products"
          element={(
            <RequireAuth>
              <AuctionProductsPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/auctions/manage"
          element={(
            <RequireAuth>
              <RequireCapability canAccess={canCreateAuction} message="현재 계정은 경매 등록 기능을 사용할 수 없습니다.">
                <AuctionsPage />
              </RequireCapability>
            </RequireAuth>
          )}
        />
        <Route
          path="/auctions/:auctionId"
          element={(
            <RequireAuth>
              <AuctionDetailPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/chat/rooms"
          element={(
            <RequireAuth>
              <ChatRoomsPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/chat/rooms/:roomId"
          element={(
            <RequireAuth>
              <ChatRoomPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/mileage"
          element={(
            <RequireAuth>
              <MileagePage />
            </RequireAuth>
          )}
        />
        <Route
          path="/support"
          element={(
            <RequireAuth>
              <SupportPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/reports"
          element={(
            <RequireAuth>
              <ReportsPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/reports/:reportId"
          element={(
            <RequireAuth>
              <ReportDetailPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/notifications"
          element={(
            <RequireAuth>
              <NotificationsPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/me"
          element={(
            <RequireAuth>
              <MePage />
            </RequireAuth>
          )}
        />
        <Route
          path="/users/:userId"
          element={(
            <RequireAuth>
              <UserProfilePage />
            </RequireAuth>
          )}
        />
        <Route
          path="/admin"
          element={(
            <RequireAuth>
              <RequireAdmin>
                <AdminPage />
              </RequireAdmin>
            </RequireAuth>
          )}
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      {isChatPopup ? null : <Footer />}
    </>
  );
}

export default App;
