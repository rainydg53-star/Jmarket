import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../lib/api";

import "../css/pages/SignUpPage.css";
function SignUpPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [emailCode, setEmailCode] = useState("");
  const [emailVerificationToken, setEmailVerificationToken] = useState("");
  const [verifiedEmail, setVerifiedEmail] = useState("");
  const [emailCodeSent, setEmailCodeSent] = useState(false);
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [modalMessage, setModalMessage] = useState("");
  const [signupComplete, setSignupComplete] = useState(false);
  const [loading, setLoading] = useState(false);

  const onChangePhone = (value) => {
    const onlyNumbers = value.replace(/\D/g, "");
    setPhoneNumber(onlyNumbers);
  };

  const onChangeEmail = (value) => {
    setEmail(value);
    setEmailVerificationToken("");
    setVerifiedEmail("");
    setEmailCodeSent(false);
    setEmailCode("");
  };

  const sendEmailCode = async () => {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setModalMessage("이메일 형식을 확인해주세요.");
      return;
    }

    setLoading(true);
    try {
      const response = await api("/api/auth/email-verification/send", {
        method: "POST",
        body: JSON.stringify({ email: email.trim() }),
      });
      setEmailCodeSent(true);
      setModalMessage(response.devCode
        ? `인증 코드를 발송했습니다. 개발용 코드: ${response.devCode}`
        : "인증 코드를 이메일로 발송했습니다.");
    } catch (error) {
      setModalMessage(`인증 코드 발송 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const confirmEmailCode = async () => {
    if (!emailCodeSent) {
      setModalMessage("먼저 인증 코드를 발송해주세요.");
      return;
    }
    if (!/^[0-9]{6}$/.test(emailCode.trim())) {
      setModalMessage("인증 코드는 숫자 6자리로 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      const response = await api("/api/auth/email-verification/confirm", {
        method: "POST",
        body: JSON.stringify({ email: email.trim(), code: emailCode.trim() }),
      });
      setEmailVerificationToken(response.emailVerificationToken);
      setVerifiedEmail(email.trim());
      setModalMessage("이메일 인증이 완료되었습니다.");
    } catch (error) {
      setModalMessage(`이메일 인증 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const signUp = async () => {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setModalMessage("이메일 형식을 확인해주세요.");
      return;
    }
    if (!emailVerificationToken || verifiedEmail !== email.trim()) {
      setModalMessage("이메일 인증을 완료해주세요.");
      return;
    }
    if (!name.trim() || !nickname.trim()) {
      setModalMessage("이름과 닉네임을 입력해주세요.");
      return;
    }
    if (!/^\d{10,11}$/.test(phoneNumber)) {
      setModalMessage("전화번호는 숫자만 10~11자리로 입력해주세요.");
      return;
    }
    if (password.length < 8) {
      setModalMessage("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (password !== passwordConfirm) {
      setModalMessage("비밀번호 확인이 일치하지 않습니다.");
      return;
    }

    setLoading(true);
    try {
      await api("/api/auth/signup", {
        method: "POST",
        body: JSON.stringify({
          email: email.trim(),
          name: name.trim(),
          nickname: nickname.trim(),
          phoneNumber,
          password,
          passwordConfirm,
          emailVerificationToken,
        }),
      });
      setSignupComplete(true);
      setModalMessage("\uD68C\uC6D0\uAC00\uC785\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB85C\uADF8\uC778\uD574\uC8FC\uC138\uC694.");
    } catch (error) {
      setModalMessage(`회원가입 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="container">
      <h1>회원가입</h1>
      <div className="card">
        <label>이메일(아이디)</label>
        <div className="signup-email-row signup-email-row--signup">
          <input value={email} onChange={(e) => onChangeEmail(e.target.value)} disabled={loading} />
          <button type="button" onClick={sendEmailCode} disabled={loading || !email.trim()}>
            인증 코드 발송
          </button>
        </div>
        {emailCodeSent ? (
          <div className="signup-email-row signup-email-row--signup">
            <input
              value={emailCode}
              onChange={(e) => setEmailCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
              disabled={loading || Boolean(emailVerificationToken)}
              placeholder="인증 코드 6자리"
            />
            <button type="button" onClick={confirmEmailCode} disabled={loading || Boolean(emailVerificationToken)}>
              인증 확인
            </button>
          </div>
        ) : null}
        {emailVerificationToken ? <p className="meta">이메일 인증 완료</p> : null}

        <label>이름</label>
        <input value={name} onChange={(e) => setName(e.target.value)} disabled={loading} />

        <label>닉네임</label>
        <input value={nickname} onChange={(e) => setNickname(e.target.value)} disabled={loading} />

        <label>전화번호 (숫자만)</label>
        <input value={phoneNumber} onChange={(e) => onChangePhone(e.target.value)} disabled={loading} />

        <label>비밀번호</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} disabled={loading} />

        <label>비밀번호 확인</label>
        <input
          type="password"
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
          disabled={loading}
        />

        <div className="actions">
          <button onClick={signUp} disabled={loading}>회원가입</button>
          <Link to="/login">
            <button type="button" disabled={loading}>로그인으로</button>
          </Link>
        </div>
      </div>

      {modalMessage ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card signup-alert-modal">
            <h2>{signupComplete ? "\uD68C\uC6D0\uAC00\uC785 \uC644\uB8CC" : "\uC785\uB825 \uD655\uC778"}</h2>
            <p>{modalMessage}</p>
            <div className="actions">
              <button
                type="button"
                onClick={() => {
                  if (signupComplete) {
                    navigate("/login", { replace: true });
                    return;
                  }
                  setModalMessage("");
                }}
              >
                {"\uD655\uC778"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

export default SignUpPage;
