import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="app-footer">
      <div className="app-footer-inner">
        <div className="app-footer-company" aria-label="회사 정보">
          <strong className="app-footer-brand">Jmarket</strong>
          <p>대표자: 윤진성 <span className="app-footer-separator">/</span> 사업자번호: 000-00-00000</p>
          <p>통신판매업신고번호: 제 0000-인천부평-0000호</p>
          <p>인천광역시 부평 더조은아카데미 702호</p>
          <p className="app-footer-copy">copyright (c) 2026 Jmarket All right reserved.</p>
        </div>

        <Link className="app-footer-support" to="/support?mode=create" aria-label="고객센터로 이동">
          <span>고객센터</span>
          <strong>0000-0000</strong>
          <small>상담 문의 바로가기</small>
        </Link>
      </div>
    </footer>
  );
}
