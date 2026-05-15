import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { setAccessToken } from "../lib/auth";

import "../css/pages/LoginPage.css";
const formatKoreanDateTime = (value) => {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(date);
};

const parseBannedMessage = (message) => {
  const reasonMatch = message.match(/사유:\s*(.*?)(?:\s*해제 예정:|$)/);
  const untilMatch = message.match(/해제 예정:\s*(.*)$/);
  return {
    reason: reasonMatch?.[1]?.trim() || "-",
    bannedUntil: formatKoreanDateTime(untilMatch?.[1]?.trim()) || null,
  };
};

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [autoLogin, setAutoLogin] = useState(false);
  const [message, setMessage] = useState(location.state?.message || "");
  const [loginErrorModal, setLoginErrorModal] = useState(null);
  const [loading, setLoading] = useState(false);

  const goHome = () => navigate("/", { replace: true });

  const login = async () => {
    if (!loginId.trim() || !password) {
      setMessage("아이디와 비밀번호를 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      const response = await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ loginId: loginId.trim(), password }),
      });
      setAccessToken(response.accessToken);
      if (autoLogin) {
        localStorage.setItem("auto_login_enabled", "true");
      } else {
        localStorage.removeItem("auto_login_enabled");
      }
      goHome();
    } catch (error) {
      if (error.code === "A006") {
        const bannedInfo = parseBannedMessage(error.message);
        setLoginErrorModal({
          title: "로그인이 제한된 계정입니다",
          reason: bannedInfo.reason,
          bannedUntil: bannedInfo.bannedUntil,
        });
        setMessage("");
      } else {
        setMessage(`로그인 실패: ${error.message}`);
      }
    } finally {
      setLoading(false);
    }
  };

  const socialLogin = async (provider) => {
    setLoading(true);
    try {
      const state = `${provider}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
      localStorage.setItem(`social_state_${provider}`, state);
      const response = await api(`/api/auth/social/${provider}/authorize-url?state=${encodeURIComponent(state)}`);
      if (!response?.authorizeUrl) {
        throw new Error("소셜 로그인 URL을 가져오지 못했습니다.");
      }
      window.location.href = response.authorizeUrl;
    } catch (error) {
      setMessage(`소셜 로그인 준비 실패: ${error.message}`);
      setLoading(false);
    }
  };

  return (
    <main className="container login-container">
      <section className="login-card">
        <label htmlFor="login-id">아이디</label>
        <input
          id="login-id"
          value={loginId}
          onChange={(e) => setLoginId(e.target.value)}
          placeholder="아이디를 입력하세요"
          disabled={loading}
          className="login-input"
        />

        <label htmlFor="login-password">비밀번호</label>
        <input
          id="login-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호를 입력하세요"
          disabled={loading}
          className="login-input"
        />

        <div className="login-subrow">
          <label className="auto-login-label">
            <input
              type="checkbox"
              checked={autoLogin}
              onChange={(e) => setAutoLogin(e.target.checked)}
              disabled={loading}
            />
            자동로그인
          </label>
          <Link to="/find-account" className="find-link">아이디/비밀번호 찾기</Link>
        </div>

        <button className="login-primary-btn" onClick={login} disabled={loading}>로그인</button>

        <div className="login-divider">
          <span />
          <em>또는</em>
          <span />
        </div>

        <button
          type="button"
          className="social-btn naver"
          onClick={() => socialLogin("naver")}
          disabled={loading}
        >
          <strong>N</strong>
          네이버 로그인
        </button>
        <button
          type="button"
          className="social-btn kakao"
          onClick={() => socialLogin("kakao")}
          disabled={loading}
        >
          <strong>●</strong>
          카카오 로그인
        </button>

        <p className="signup-row">
          아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>

        {loading || message ? <p className="login-status">{loading ? "요청 처리 중..." : message}</p> : null}
      </section>
      {loginErrorModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>{loginErrorModal.title}</h2>
            <p>정지된 계정입니다.</p>
            <p>사유: {loginErrorModal.reason}</p>
            {loginErrorModal.bannedUntil ? <p>해제 예정: {loginErrorModal.bannedUntil}</p> : null}
            <div className="actions">
              <button type="button" onClick={() => setLoginErrorModal(null)}>확인</button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

export default LoginPage;
