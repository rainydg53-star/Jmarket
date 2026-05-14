import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getSupportStatusTone } from "../lib/statusTone";

const STATUS_LABEL = {
  WAITING: "대기",
  ANSWERED: "답변완료",
  CLOSED: "종료",
};

function SupportPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [me, setMe] = useState(null);
  const [categories, setCategories] = useState([]);
  const [majorCategory, setMajorCategory] = useState("");
  const [minorCategory, setMinorCategory] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [inquiries, setInquiries] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [answerContent, setAnswerContent] = useState("");
  const [nextStatus, setNextStatus] = useState("WAITING");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("상담 정보를 불러오는 중...");

  const isAdmin = me?.role === "ADMIN";
  const viewMode = searchParams.get("mode") === "list" ? "list" : "create";
  const showCreate = !isAdmin && viewMode === "create";
  const showList = isAdmin || viewMode === "list";

  const selectedMajor = useMemo(
    () => categories.find((group) => group.majorCategory === majorCategory),
    [categories, majorCategory]
  );

  const minorCategories = selectedMajor?.minorCategories ?? [];

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const formatDateTime = (value) => {
    if (!value) {
      return "-";
    }
    return new Intl.DateTimeFormat("ko-KR", {
      timeZone: "Asia/Seoul",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }).format(new Date(value));
  };

  const loadInitial = useCallback(async () => {
    setLoading(true);
    try {
      const meRes = await api("/api/auth/me");
      const categoryRes = await api("/api/support/categories");
      setMe(meRes);
      setCategories(categoryRes);
      if (categoryRes.length > 0) {
        setMajorCategory(categoryRes[0].majorCategory);
      }
      setMessage("상담 정보 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상담 정보 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  const loadInquiries = useCallback(async (admin) => {
    setLoading(true);
    try {
      const path = admin ? "/api/admin/support/inquiries" : "/api/support/inquiries/me";
      const list = await api(path);
      setInquiries(list);
      if (list.length === 0) {
        setSelectedId(null);
        setDetail(null);
      }
      setMessage("상담 목록 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상담 목록 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  const loadDetail = async (inquiryId, admin) => {
    setLoading(true);
    try {
      const path = admin
        ? `/api/admin/support/inquiries/${inquiryId}`
        : `/api/support/inquiries/${inquiryId}`;
      const response = await api(path);
      setDetail(response);
      setNextStatus(admin ? response.status : "CLOSED");
      setMessage("상담 상세 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상담 상세 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const createInquiry = async () => {
    const trimmedTitle = title.trim();
    const trimmedContent = content.trim();
    if (!majorCategory || !minorCategory) {
      setMessage("대분류와 소분류를 선택해주세요.");
      return;
    }
    if (!trimmedTitle) {
      setMessage("상담 제목을 입력해주세요.");
      return;
    }
    if (!trimmedContent) {
      setMessage("상담 내용을 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      await api("/api/support/inquiries", {
        method: "POST",
        body: JSON.stringify({
          majorCategory,
          minorCategory,
          title: trimmedTitle,
          content: trimmedContent,
        }),
      });
      setTitle("");
      setContent("");
      setMinorCategory("");
      await loadInquiries(false);
      setMessage("상담 등록 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상담 등록 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const answerInquiry = async () => {
    const trimmedAnswer = answerContent.trim();
    if (!selectedId) {
      setMessage("답변할 상담을 선택해주세요.");
      return;
    }
    if (!trimmedAnswer) {
      setMessage("답변 내용을 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      await api(`/api/admin/support/inquiries/${selectedId}/answer`, {
        method: "PATCH",
        body: JSON.stringify({ answerContent: trimmedAnswer }),
      });
      setAnswerContent("");
      await loadDetail(selectedId, true);
      await loadInquiries(true);
      setMessage("상담 답변 등록 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상담 답변 등록 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const updateStatus = async () => {
    if (!selectedId) {
      setMessage("상담을 선택해주세요.");
      return;
    }

    setLoading(true);
    try {
      const path = isAdmin
        ? `/api/admin/support/inquiries/${selectedId}/status`
        : `/api/support/inquiries/${selectedId}/status`;
      await api(path, {
        method: "PATCH",
        body: JSON.stringify({ status: nextStatus }),
      });
      await loadDetail(selectedId, isAdmin);
      await loadInquiries(isAdmin);
      setMessage("상담 상태 변경 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상담 상태 변경 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const run = async () => {
      await loadInitial();
    };
    run();
  }, [loadInitial]);

  useEffect(() => {
    if (!me) {
      return;
    }
    loadInquiries(me.role === "ADMIN");
  }, [loadInquiries, me]);

  useEffect(() => {
    if (!majorCategory) {
      return;
    }
    const group = categories.find((item) => item.majorCategory === majorCategory);
    if (!group) {
      setMinorCategory("");
      return;
    }
    if (!group.minorCategories.includes(minorCategory)) {
      setMinorCategory("");
    }
  }, [majorCategory, categories, minorCategory]);

  return (
    <main className="container">
      <h1>1:1 상담</h1>
      <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
        {loading ? "요청 처리 중..." : message}
      </p>

      {showCreate ? (
        <div className="card">
          <h2>상담 등록</h2>
          <label>대분류</label>
          <div className="radio-group">
            {categories.map((group) => (
              <label key={group.majorCategory} className="radio-item">
                <input
                  type="radio"
                  name="majorCategory"
                  value={group.majorCategory}
                  checked={majorCategory === group.majorCategory}
                  onChange={(e) => setMajorCategory(e.target.value)}
                  disabled={loading}
                />
                <span>{group.majorCategory}</span>
              </label>
            ))}
          </div>

          <label>소분류</label>
          <div className="radio-group">
            {minorCategories.map((minor) => (
              <label key={minor} className="radio-item">
                <input
                  type="radio"
                  name="minorCategory"
                  value={minor}
                  checked={minorCategory === minor}
                  onChange={(e) => setMinorCategory(e.target.value)}
                  disabled={loading}
                />
                <span>{minor}</span>
              </label>
            ))}
          </div>

          <label>제목</label>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={loading}
            maxLength={200}
          />

          <label>내용</label>
          <textarea
            className="textarea"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            disabled={loading}
            maxLength={5000}
          />
          <div className="actions">
            <button onClick={createInquiry} disabled={loading}>등록</button>
          </div>
        </div>
      ) : null}

      {showList ? (
      <div className="card">
        <h2>{isAdmin ? "전체 상담 목록" : "내 상담 목록"}</h2>
        {inquiries.length === 0 ? (
          <p className="empty-box">등록된 상담이 없습니다.</p>
        ) : (
          <ul className="list">
            {inquiries.map((item) => (
              <li key={item.id} className="list-item">
                <button
                  type="button"
                  onClick={() => {
                    setSelectedId(item.id);
                    loadDetail(item.id, isAdmin);
                  }}
                  disabled={loading}
                >
                  {item.title}
                </button>
                <span className="meta">{item.majorCategory} / {item.minorCategory}</span>
                <span className="meta">
                  상태: <span className={`status-badge ${getSupportStatusTone(item.status)}`}>{STATUS_LABEL[item.status] ?? item.status}</span>
                </span>
                <span className="meta">{formatDateTime(item.createdAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
      ) : null}

      {showList && detail ? (
        <div className="card">
          <h2>상담 상세</h2>
          <p><strong>분류:</strong> {detail.majorCategory} / {detail.minorCategory}</p>
          <p><strong>제목:</strong> {detail.title}</p>
          <p><strong>내용:</strong> {detail.content}</p>
          <p>
            <strong>상태:</strong>{" "}
            <span className={`status-badge ${getSupportStatusTone(detail.status)}`}>{STATUS_LABEL[detail.status] ?? detail.status}</span>
          </p>
          <p className="meta">작성: {formatDateTime(detail.createdAt)}</p>
          <p className="meta">답변: {detail.answerContent ?? "-"}</p>

          {isAdmin ? (
            <>
              <label>답변 등록</label>
              <textarea
                className="textarea"
                value={answerContent}
                onChange={(e) => setAnswerContent(e.target.value)}
                disabled={loading}
                maxLength={5000}
              />
              <div className="actions">
                <button onClick={answerInquiry} disabled={loading}>답변 등록</button>
              </div>
            </>
          ) : null}

          <label>상태 변경</label>
          <select
            className="select"
            value={nextStatus}
            onChange={(e) => setNextStatus(e.target.value)}
            disabled={loading || (!isAdmin && detail.status === "CLOSED")}
          >
            {isAdmin ? (
              <>
                <option value="WAITING">대기</option>
                <option value="ANSWERED">답변완료</option>
                <option value="CLOSED">종료</option>
              </>
            ) : (
              <option value="CLOSED">종료</option>
            )}
          </select>
          <div className="actions">
            <button onClick={updateStatus} disabled={loading}>상태 변경</button>
          </div>
        </div>
      ) : null}
    </main>
  );
}

export default SupportPage;
