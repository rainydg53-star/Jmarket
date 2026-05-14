# 🛒 Jmarket

> 중고 거래 · 실시간 경매 · 마일리지 시스템을 통합한 개인 프로젝트

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-Vite-61DAFB?style=for-the-badge&logo=react)
![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?style=for-the-badge&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=for-the-badge&logo=redis)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-Search-005571?style=for-the-badge&logo=elasticsearch)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker)

---

# 📌 프로젝트 소개

Jmarket은 단순 CRUD 기반 중고거래 서비스가 아니라,

- 거래 상태 흐름
- 실시간 경매 입찰
- 마일리지 예약 및 환불
- 관리자 운영 기능
- 실시간 채팅
- 알림 및 신고 처리

까지 포함한 실제 서비스 흐름 구현을 목표로 제작한 프로젝트입니다.

일반 사용자는 상품 등록, 거래 요청, 경매 입찰, 마일리지 충전/출금, 채팅 기능을 사용할 수 있고,  
관리자는 회원/상품/경매/신고/출금/감사 로그를 운영 화면에서 관리할 수 있습니다.

---

# ✨ 주요 기능

## 👤 사용자 기능

- 회원가입 / 로그인 / 이메일 인증
- 비밀번호 찾기 및 변경
- 상품 등록 / 수정 / 삭제
- 상품 검색 및 필터
- 거래 요청 및 거래 상태 변경
- 실시간 경매 입찰
- 마일리지 충전 / 출금
- 사용자 간 실시간 채팅
- 신고 등록 및 처리 결과 조회
- 알림 조회

---

## 🛠 관리자 기능

- 관리자 권한 기반 접근 제어
- 회원 검색 / 상태 변경 / 제재 관리
- 상품 및 경매 운영 관리
- 신고 처리 및 강제 삭제
- 출금 승인 / 반려
- 감사 로그 조회
- 관리자 대시보드 통계

---

# 🔥 핵심 구현 포인트

## 1️⃣ 경매 입찰 + 마일리지 예약 처리

경매 입찰 시:

- 최고 입찰자 변경
- 예약 마일리지 변경
- 이전 입찰자 예약 해제
- 신규 입찰자 예약 처리

를 하나의 흐름으로 처리했습니다.

또한 관리자가 경매를 강제 취소하는 경우에도 현재 최고 입찰자의 예약 마일리지를 반환하도록 보완했습니다.

---

## 2️⃣ 관리자 강제 삭제 안정성 보완

상품 삭제 시 발생할 수 있는:

- 조회 기록
- 찜
- 문의
- 이미지

등 연관 데이터의 외래키 문제를 고려하여 삭제 순서를 정리했습니다.

거래/경매 이력이 존재하는 상품은 데이터 정합성을 위해 하드 삭제하지 않고 예외 처리하도록 구성했습니다.

---

## 3️⃣ 권한 기반 운영 화면 구성

- 서버에서 `ADMIN` 권한 강제 검증
- 프론트에서 권한별 메뉴 분리
- 관리자 전용 운영 UI 구성

을 통해 사용자 화면과 관리자 화면을 목적에 맞게 분리했습니다.

---

## 4️⃣ 이미지 업로드 UX 개선

공통 이미지 업로드 컴포넌트를 제작하여:

- Drag & Drop 업로드
- 다중 이미지 업로드
- 대표 이미지 설정
- 썸네일 미리보기
- 서버 이미지 검증

기능을 통합했습니다.

---

# 🧱 기술 스택

| 영역 | 기술 |
| --- | --- |
| Frontend | React, Vite, React Router, STOMP WebSocket |
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Database | MySQL |
| Cache / Realtime | Redis, WebSocket, STOMP |
| Search | Elasticsearch |
| Authentication | JWT, Refresh Token Cookie, Email Verification |
| Payment | KakaoPay 연동, 마일리지 충전/예약/출금 |
| Infra | Docker Compose, Gradle |
| Test / Tool | JUnit, Spring Security Test, ESLint |

---

# 🖥 화면 구성

| 화면 | 설명 |
| --- | --- |
| 메인 | 실시간 검색어, 급상승 상품 |
| 상품 | 목록, 상세, 등록, 수정 |
| 경매 | 목록, 상세, 입찰 |
| 거래 | 요청 / 수락 / 취소 / 완료 |
| 채팅 | 채팅방 목록, 실시간 채팅 |
| 마이페이지 | 내 정보, 거래내역, 마일리지 |
| 관리자 | 회원/상품/경매/신고/출금 관리 |

---

# 📂 프로젝트 구조

```text
Jmarket
├─ backend          # Spring Boot API 서버
├─ front            # React + Vite 프론트엔드
├─ infra            # Docker Compose 환경
├─ portfolio        # 포트폴리오 정리 자료
├─ .env.example
└─ README.md

📈 향후 개선 방향
디자인 토큰 분리
관리자 페이지네이션 개선
경매 동시성 테스트 강화
외부 스토리지 연동
실시간 알림 범위 확대
감사 로그 검색 고도화
👨‍💻 개발 목적

실제 서비스 흐름에서 발생할 수 있는:

권한 처리
상태 변경
실시간 데이터 처리
예약 처리
운영 기능
데이터 정합성 문제

