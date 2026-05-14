import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { setAccessToken } from "../lib/auth";

function SocialCallbackPage() {
  const navigate = useNavigate();
  const { provider } = useParams();
  const [searchParams] = useSearchParams();
  const [message, setMessage] = useState("소셜 로그인 처리 중...");
  const [loading, setLoading] = useState(true);

  const normalizedProvider = useMemo(() => String(provider || "").toLowerCase(), [provider]);

  useEffect(() => {
    const code = searchParams.get("code");
    const state = searchParams.get("state");
    const expectedState = localStorage.getItem(`social_state_${normalizedProvider}`);

    if (!code) {
      setMessage("인가 코드가 없어 소셜 로그인을 진행할 수 없습니다.");
      setLoading(false);
      return;
    }

    if (expectedState && state && expectedState !== state) {
      setMessage("state 검증에 실패했습니다. 다시 로그인해주세요.");
      setLoading(false);
      return;
    }

    (async () => {
      try {
        const response = await api(`/api/auth/social/${normalizedProvider}/callback`, {
          method: "POST",
          body: JSON.stringify({ code, state }),
        });
        setAccessToken(response.accessToken);
        localStorage.removeItem(`social_state_${normalizedProvider}`);
        navigate("/products", { replace: true });
      } catch (error) {
        setMessage(`소셜 로그인 실패: ${error.message}`);
      } finally {
        setLoading(false);
      }
    })();
  }, [navigate, normalizedProvider, searchParams]);

  return (
    <main className="container">
      <h1>소셜 로그인</h1>
      <div className="card">
        <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
          {loading ? "요청 처리 중..." : message}
        </p>
        {!loading ? (
          <div className="actions">
            <Link to="/login">
              <button type="button">로그인으로 돌아가기</button>
            </Link>
          </div>
        ) : null}
      </div>
    </main>
  );
}

export default SocialCallbackPage;
