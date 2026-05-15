import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

import "../css/pages/FindAccountPage.css";
function FindAccountPage() {
  const [tab, setTab] = useState("id");
  const [name, setName] = useState("");
  const [foundIds, setFoundIds] = useState([]);
  const [email, setEmail] = useState("");
  const [emailCode, setEmailCode] = useState("");
  const [emailCodeSent, setEmailCodeSent] = useState(false);
  const [emailVerificationToken, setEmailVerificationToken] = useState("");
  const [verifiedEmail, setVerifiedEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const resetPasswordState = () => {
    setEmailCode("");
    setEmailCodeSent(false);
    setEmailVerificationToken("");
    setVerifiedEmail("");
    setNewPassword("");
    setNewPasswordConfirm("");
  };

  const onChangeEmail = (value) => {
    setEmail(value);
    resetPasswordState();
  };

  const findId = async () => {
    if (!name.trim()) {
      setMessage("이름을 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      const response = await api("/api/auth/find-id", {
        method: "POST",
        body: JSON.stringify({ name: name.trim() }),
      });
      setFoundIds(response.loginIds ?? []);
      setMessage("아이디 조회가 완료되었습니다.");
    } catch (error) {
      setFoundIds([]);
      setMessage(`아이디 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const sendPasswordResetCode = async () => {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setMessage("이메일 형식을 확인해주세요.");
      return;
    }

    setLoading(true);
    try {
      const response = await api("/api/auth/password/verify-email", {
        method: "POST",
        body: JSON.stringify({ email: email.trim() }),
      });
      setEmailCodeSent(true);
      setMessage(response.devCode
        ? `인증 코드를 발송했습니다. 개발용 코드: ${response.devCode}`
        : "인증 코드를 이메일로 발송했습니다.");
    } catch (error) {
      setEmailCodeSent(false);
      setMessage(`인증 코드 발송 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const confirmPasswordResetCode = async () => {
    if (!emailCodeSent) {
      setMessage("먼저 인증 코드를 발송해주세요.");
      return;
    }
    if (!/^[0-9]{6}$/.test(emailCode.trim())) {
      setMessage("인증 코드는 숫자 6자리로 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      const response = await api("/api/auth/password/confirm-email", {
        method: "POST",
        body: JSON.stringify({ email: email.trim(), code: emailCode.trim() }),
      });
      setEmailVerificationToken(response.emailVerificationToken);
      setVerifiedEmail(email.trim());
      setMessage("이메일 인증이 완료되었습니다.");
    } catch (error) {
      setEmailVerificationToken("");
      setVerifiedEmail("");
      setMessage(`이메일 인증 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const resetPassword = async () => {
    if (!emailVerificationToken || verifiedEmail !== email.trim()) {
      setMessage("먼저 이메일 인증을 완료해주세요.");
      return;
    }
    if (!newPassword || !newPasswordConfirm) {
      setMessage("새 비밀번호를 입력해주세요.");
      return;
    }
    if (newPassword.length < 8) {
      setMessage("새 비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (newPassword !== newPasswordConfirm) {
      setMessage("새 비밀번호 확인이 일치하지 않습니다.");
      return;
    }

    setLoading(true);
    try {
      await api("/api/auth/password/reset", {
        method: "POST",
        body: JSON.stringify({
          email: email.trim(),
          newPassword,
          newPasswordConfirm,
          emailVerificationToken,
        }),
      });
      setMessage("비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.");
      resetPasswordState();
      setEmail("");
    } catch (error) {
      setMessage(`비밀번호 변경 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const renderPasswordReset = () => (
    <>
      <label htmlFor="find-email">이메일</label>
      <div className="signup-email-row find-email-row">
        <input
          id="find-email"
          className="login-input"
          value={email}
          onChange={(e) => onChangeEmail(e.target.value)}
          placeholder="가입한 이메일을 입력하세요"
          disabled={loading || Boolean(emailVerificationToken)}
        />
        <button
          type="button"
          className="login-primary-btn"
          onClick={sendPasswordResetCode}
          disabled={loading || !email.trim() || Boolean(emailVerificationToken)}
        >
          인증 코드 발송
        </button>
      </div>

      {emailCodeSent ? (
        <div className="signup-email-row find-email-row">
          <input
            className="login-input"
            value={emailCode}
            onChange={(e) => setEmailCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
            placeholder="인증 코드 6자리"
            disabled={loading || Boolean(emailVerificationToken)}
          />
          <button
            type="button"
            className="login-primary-btn"
            onClick={confirmPasswordResetCode}
            disabled={loading || Boolean(emailVerificationToken)}
          >
            인증 확인
          </button>
        </div>
      ) : null}

      {emailVerificationToken ? (
        <>
          <p className="meta">이메일 인증 완료</p>
          <label htmlFor="new-password">새 비밀번호</label>
          <input
            id="new-password"
            type="password"
            className="login-input"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="새 비밀번호를 입력하세요"
            disabled={loading}
          />
          <label htmlFor="new-password-confirm">새 비밀번호 확인</label>
          <input
            id="new-password-confirm"
            type="password"
            className="login-input"
            value={newPasswordConfirm}
            onChange={(e) => setNewPasswordConfirm(e.target.value)}
            placeholder="새 비밀번호를 다시 입력하세요"
            disabled={loading}
          />
          <button type="button" className="login-primary-btn" onClick={resetPassword} disabled={loading}>
            비밀번호 변경
          </button>
        </>
      ) : null}
    </>
  );

  return (
    <main className="container login-container">
      <section className="login-card">
        <h2 className="find-title">아이디/비밀번호 찾기</h2>
        <div className="find-tabs">
          <button type="button" className={`find-tab${tab === "id" ? " active" : ""}`} onClick={() => setTab("id")} disabled={loading}>
            아이디 찾기
          </button>
          <button type="button" className={`find-tab${tab === "password" ? " active" : ""}`} onClick={() => setTab("password")} disabled={loading}>
            비밀번호 찾기
          </button>
        </div>

        {tab === "id" ? (
          <>
            <label htmlFor="find-name">이름</label>
            <input
              id="find-name"
              className="login-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="이름을 입력하세요"
              disabled={loading}
            />
            <button type="button" className="login-primary-btn" onClick={findId} disabled={loading}>아이디 찾기</button>
            {foundIds.length > 0 ? (
              <div className="found-box">
                {foundIds.map((id) => (
                  <p key={id}>찾은 아이디: {id}</p>
                ))}
              </div>
            ) : null}
          </>
        ) : renderPasswordReset()}

        <p className="login-status">{loading ? "요청 처리 중..." : (message || "원하는 항목을 선택해 진행해주세요.")}</p>
        <p className="signup-row"><Link to="/login">로그인으로 돌아가기</Link></p>
      </section>
    </main>
  );
}

export default FindAccountPage;
