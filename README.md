# Jmarket

> 실시간 중고거래, 경매, 채팅, 마일리지 시스템을 통합한 개인 풀스택 프로젝트

<p align="center">
  <strong>Jmarket</strong><br />
  중고거래 흐름에 실시간 경매, 사용자 간 채팅, 마일리지 결제/정산, 관리자 운영 기능을 더한 거래 플랫폼입니다.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Project-Personal_Portfolio-111827?style=for-the-badge" alt="Personal Portfolio" />
  <img src="https://img.shields.io/badge/Status-In_Progress-2563EB?style=for-the-badge" alt="In Progress" />
  <img src="https://img.shields.io/badge/Deploy-AWS-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white" alt="AWS" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Realtime-Chat_&_Auction-16A34A?style=flat-square" alt="Realtime" />
  <img src="https://img.shields.io/badge/Payment-Mileage_System-7C3AED?style=flat-square" alt="Mileage" />
  <img src="https://img.shields.io/badge/Admin-Operation_Dashboard-0F172A?style=flat-square" alt="Admin" />
</p>

---

## Overview

Jmarket는 단순 CRUD 기반 중고거래 서비스가 아니라, 실제 거래 서비스에서 발생하는 흐름을 고려해 만든 프로젝트입니다.

사용자는 상품을 등록하고, 거래를 요청하고, 경매에 입찰하고, 마일리지를 충전/출금하며, 실시간 채팅으로 거래를 이어갈 수 있습니다.

관리자는 회원, 상품, 경매, 신고, 출금, 감사 로그를 운영 화면에서 관리할 수 있습니다. 운영 화면은 일반 사용자 화면과 달리 검색, 필터, 테이블, 상태 배지, 빠른 액션 중심으로 구성했습니다.

---

## Tech Stack

### Frontend

<p>
  <img src="https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=111111" alt="React" />
  <img src="https://img.shields.io/badge/Vite-Frontend-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite" />
  <img src="https://img.shields.io/badge/React_Router-Routing-CA4245?style=for-the-badge&logo=reactrouter&logoColor=white" alt="React Router" />
  <img src="https://img.shields.io/badge/STOMP-WebSocket-111827?style=for-the-badge" alt="STOMP" />
</p>

### Backend

<p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/JPA-Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white" alt="JPA" />
</p>

### Database / Infra

<p>
  <img src="https://img.shields.io/badge/MySQL-8.x-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Redis-Cache_&_Realtime-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/Elasticsearch-Search-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" alt="Elasticsearch" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker Compose" />
</p>

### Tooling

<p>
  <img src="https://img.shields.io/badge/Gradle-Build-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle" />
  <img src="https://img.shields.io/badge/JUnit-Test-25A162?style=for-the-badge" alt="JUnit" />
  <img src="https://img.shields.io/badge/ESLint-Frontend_Lint-4B32C3?style=for-the-badge&logo=eslint&logoColor=white" alt="ESLint" />
</p>

---

## Architecture

```text
Client Browser
     |
     v
React + Vite Frontend
     |
     | HTTP API / WebSocket(STOMP)
     v
Spring Boot API Server
     |
     |-- MySQL
     |   |-- users / products / trades / auctions
     |   |-- mileage / reports / support / audit logs
     |
     |-- Redis
     |   |-- cache
     |   |-- auction bid snapshot
     |
     |-- Elasticsearch
     |   |-- product search index
     |
     `-- External Payment API
         `-- KakaoPay mileage charge flow
```

---

## Main Features

| 구분 | 주요 기능 |
| --- | --- |
| 사용자 기능 | 회원가입, 로그인, 이메일 인증, 비밀번호 찾기/변경, 상품 등록/수정/삭제, 상품 검색/필터, 사용자 프로필 조회 |
| 거래 기능 | 구매 신청, 거래 수락/취소/완료, 마일리지 예약, 거래 채팅, 거래 취소 시 채팅 읽기 전용 전환 |
| 경매 기능 | 경매 등록, 시작 대기/진행/마감 상태 처리, 실시간 입찰, 즉시구매, 낙찰 처리, 마일리지 예약/반환 |
| 채팅/알림 | WebSocket 기반 실시간 채팅, 채팅방 목록, 알림, 신고/거래/경매 관련 이동 링크 |
| 결제/마일리지 | KakaoPay 충전, 마일리지 보유/예약/출금, 관리자 출금 승인/반려, 마일리지 조정 |
| 신고/상담 | 신고 등록, 신고 상세 페이지, 관리자 신고 처리, 1:1 상담 등록/답변/상태 변경 |
| 관리자 기능 | 관리자 권한 검증, 대시보드 통계, 회원 관리, 상품/경매 운영, 기능 제재, 카테고리, 감사 로그 |

---

## Feature Highlights

### User

- 상품 목록, 상세, 등록, 수정 화면 분리
- 상품/경매 이미지 Drag & Drop 업로드
- 썸네일 미리보기, 대표 이미지 선택, 서버 이미지 검증
- 사용자 프로필에서 등록 상품 이미지 클릭 시 상세 이동
- 상품 상세 이미지 확대 보기 및 클릭 닫기

### Admin

- 관리자 권한 사용자만 서버에서 접근 허용
- 회원 검색/필터, 상태 배지, 빠른 액션
- 상품 운영과 경매 운영 분리
- 경매 숨김 처리/복구 필터
- 신고 상태별 필터: 대기, 처리완료, 반려
- 출금 승인/반려 및 결과 모달/토스트 통일

### Realtime

- STOMP WebSocket 기반 채팅
- 채팅방 목록에 상품명/경매 상품명 표시
- 거래 완료 또는 취소 시 채팅 전송 비활성화
- Redis를 활용한 경매 입찰 스냅샷 관리

### Payment / Mileage

- KakaoPay 결제 준비/승인/취소/실패 흐름
- 거래 구매 신청 시 마일리지 부족 모달
- 경매 입찰 시 최고 입찰자 변경에 따른 예약/해제 처리
- 관리자 마일리지 지급/차감 기능

---

## Core Implementation Points

### 1. 경매 입찰과 마일리지 예약 처리

**문제 상황**

경매 입찰은 단순히 최고가만 변경하면 되는 기능이 아닙니다. 최고 입찰자가 바뀌면 이전 입찰자의 예약 마일리지를 해제하고, 신규 최고 입찰자의 마일리지를 예약해야 합니다.

**해결 방법**

- 현재 최고 입찰 금액과 최고 입찰자 정보를 Redis 스냅샷으로 관리
- 입찰 성공 시 신규 입찰자의 마일리지 예약
- 이전 최고 입찰자가 존재하면 예약 마일리지 해제
- 즉시구매 조건 충족 시 경매 마감 및 정산 처리

**고려한 부분**

- 중복 입찰, 본인 상품 입찰, 시작 전/종료 후 입찰 방지
- 관리자 강제 마감 시 예약 마일리지 반환
- DB 데이터와 Redis 스냅샷의 일관성 유지

---

### 2. 관리자 강제 삭제 안정성 보완

**문제 상황**

상품을 강제로 삭제할 때 조회 기록, 좋아요, 문의, 이미지 등 연관 데이터 때문에 외래키 제약 오류가 발생할 수 있습니다.

**해결 방법**

- 삭제 전 연관 데이터 삭제 순서 정리
- 거래/경매 이력이 존재하는 상품은 데이터 정합성을 위해 하드 삭제 제한
- 경매는 실제 삭제 대신 `hidden` 플래그로 숨김 처리
- 숨김 경매는 관리자 필터에서 확인하고 복구 가능하게 구성

**고려한 부분**

- 사용자의 거래/입찰 이력 보존
- 감사 로그를 통한 관리자 액션 추적
- 운영 중 실수로 인한 데이터 손실 방지

---

### 3. 권한 기반 운영 화면 구성

**문제 상황**

관리자 기능은 프론트에서 버튼만 숨기는 것으로는 충분하지 않습니다. URL 직접 접근이나 API 직접 호출을 막기 위해 서버 권한 검증이 필요합니다.

**해결 방법**

- Spring Security에서 관리자 API 접근 권한 검증
- 프론트에서는 권한별 메뉴/버튼 숨김 처리
- 관리자 화면은 일반 사용자 UI와 분리해 운영툴 밀도로 재구성

**고려한 부분**

- 관리자 권한이 없는 사용자의 `/admin` 접근 차단
- 역할별 메뉴 노출 제어
- 회원, 상품, 경매, 신고, 출금 기능의 빠른 운영 흐름

---

### 4. 이미지 업로드 UX 개선

**문제 상황**

기본 파일 선택 방식은 여러 이미지 등록, 대표 이미지 선택, 미리보기 흐름이 불편했습니다.

**해결 방법**

- Drag & Drop 업로드 추가
- 단일/다중 이미지 미리보기 UI 분리
- 대표 이미지 선택 표시
- 삭제 버튼과 추가 업로드 영역 제공
- 서버에서 파일 크기, 확장자, Content-Type 검증

**고려한 부분**

- 상품 등록과 상품 수정 UI 일관성
- 잘못된 이미지 업로드 방지
- 사용자에게 업로드 상태를 과하게 노출하지 않도록 메시지 정리

---

## API / Auth Flow

```text
Login Request
     |
     v
Spring Security + AuthService
     |
     |-- Access Token 발급
     |-- Refresh Token HttpOnly Cookie 저장
     v
Frontend API Client
     |
     |-- Access Token으로 API 호출
     |-- 만료 시 Refresh Token으로 재발급
     v
Role 기반 권한 처리
     |
     |-- USER: 일반 기능
     |-- ADMIN / SUPER_ADMIN: 관리자 API 접근
```

| 항목 | 설명 |
| --- | --- |
| 인증 방식 | JWT Access Token + Refresh Token Cookie |
| 보안 처리 | Spring Security 기반 API 보호 |
| 권한 처리 | Role 기반 관리자 API 접근 제어 |
| 프론트 처리 | 권한별 메뉴/버튼 숨김, 인증 필요 라우트 보호 |
| 이메일 인증 | 회원가입 및 비밀번호 찾기 흐름에 인증 코드 적용 |

---

## Screens / Pages

| 화면 | 설명 |
| --- | --- |
| 메인 | 실시간 검색어 순위, 급상승 물품 |
| 상품 | 상품 목록, 상세, 등록, 수정 |
| 경매 | 경매 목록, 상세, 입찰, 즉시구매 |
| 거래 | 구매 요청, 수락, 취소, 완료 |
| 채팅 | 채팅방 목록, 실시간 채팅, 새 창 채팅 |
| 마이페이지 | 내 정보, 거래 내역, 신고 내역, 채팅 내역 |
| 마일리지 | 충전, 출금 요청, 결제/출금/사용 내역 |
| 신고 | 신고 등록, 내 신고 목록, 신고 상세 |
| 상담 | 1:1 상담 등록, 상담 목록, 관리자 답변 |
| 관리자 | 통계, 회원, 기능제재, 카테고리, 상품/경매, 출금, 감사 로그 |

---

## Project Structure

```text
Jmarket
├─ backend
│  ├─ src/main/java/com/jmarket
│  │  ├─ auth          # 회원, 인증, 이메일 인증, 소셜 로그인
│  │  ├─ product       # 상품, 이미지, 문의, 찜, 조회
│  │  ├─ trade         # 일반 거래 요청/수락/취소/완료
│  │  ├─ auction       # 경매 등록, 입찰, 마감, Redis 입찰 스냅샷
│  │  ├─ chat          # 채팅방, 메시지, WebSocket
│  │  ├─ mileage       # 마일리지 충전/예약/출금/정산
│  │  ├─ report        # 신고 등록/상세/관리자 처리
│  │  ├─ support       # 1:1 상담 등록/답변/상태 관리
│  │  ├─ admin         # 관리자 대시보드, 회원/상품/경매 운영
│  │  └─ config        # Security, WebSocket, Cache, 환경 설정
│  └─ build.gradle
│
├─ front
│  ├─ src
│  │  ├─ pages         # 라우트 단위 화면
│  │  ├─ components    # 공통 컴포넌트
│  │  ├─ css           # 공통/페이지별 스타일
│  │  └─ lib           # API, 인증, 권한, 상태 유틸
│  └─ package.json
│
├─ infra
│  └─ docker-compose.yml
│
├─ portfolio           # 포트폴리오 정리 자료
├─ .env.example
└─ README.md
```

---

## Live Demo / Local Run

### Live Demo

| 구분 | 주소 |
| --- | --- |
| 배포 사이트 | [http://3.34.44.26](http://3.34.44.26) |

> 개인 학습 및 포트폴리오 목적의 배포 환경입니다. 테스트 데이터는 변경될 수 있습니다.

### Local Run

로컬 실행은 인프라 실행 후 백엔드와 프론트를 각각 실행합니다.

```bash
# infra
cd infra
docker compose up -d

# backend
cd ../backend
./gradlew bootRun

# frontend
cd ../front
npm install
npm run dev
```

| 구분 | 기본 주소 |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Backend | `http://localhost:8081` |

---

## Deployment Notes

- 프론트 변경: 정적 빌드 파일 재배포
- 백엔드 변경: 서버 재빌드 및 재시작
- DB 변경: 운영 DB 스키마 확인

---

## Roadmap

- [ ] 관리자 대시보드 통계 시각화 고도화
- [ ] 경매 동시성 테스트 강화
- [ ] Elasticsearch 검색 정확도 개선
- [ ] 실시간 알림 범위 확장
- [ ] 감사 로그 검색/필터 고도화
- [ ] 디자인 시스템 토큰 분리
- [ ] 주요 API 통합 테스트 확대
- [ ] 배포 파이프라인 자동화

---

## Purpose

이 프로젝트는 개인 학습 및 포트폴리오 목적의 풀스택 프로젝트입니다.

실제 서비스 흐름에서 발생할 수 있는 권한 처리, 상태 전이, 실시간 데이터, 예약/정산, 운영자 기능, 데이터 정합성 문제를 직접 구현하며 개선하고 있습니다.

---

<p align="center">
  <strong>Jmarket</strong><br />
  개인 학습 및 포트폴리오 목적 프로젝트 · 꾸준히 개선 중
</p>
