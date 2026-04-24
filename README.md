# 외환 주문 시스템

실시간 환율 기반 외환 주문 시스템입니다. 한국수출입은행 Open API에서 환율을 수집하��, 매수/매도 주문을 처리합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.0
- Spring Data JPA
- H2 Database (In-Memory)
- SpringDoc OpenAPI (Swagger)
- Gradle

## 실행 방법

### 환경 변수 설정

한국수출입은행 API 키가 필요합니다. [한국수출입은행 Open API](https://www.koreaexim.go.kr/ir/HPHKIR020M01?apino=2&viewtype=C)에서 발급받을 수 있습니다.

```bash
export KOREAEXIM_AUTH_KEY=your-api-key
```

### 빌드 및 실행

```bash
./gradlew build
./gradlew bootRun
```

### 접속 정보

| 항목 | URL |
|------|-----|
| API 서버 | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

H2 Console 접속 시 JDBC URL: `jdbc:h2:mem:forexdb`

## API 명세

### 환율 조회

#### 전체 통화 최신 환율 조회

```
GET /exchange-rate/latest
```

**Response**
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": [
    {
      "currency": "USD",
      "tradeStanRate": 1345.50,
      "buyRate": 1412.78,
      "sellRate": 1278.23,
      "dateTime": "2026-04-25T11:00:00"
    }
  ]
}
```

#### 특정 통화 최신 환율 조회

```
GET /exchange-rate/latest/{currency}
```

- `currency`: USD, JPY, CNY, EUR

### 주문

#### 외화 주문

```
POST /order
Content-Type: application/json

{
  "orderType": "BUY",
  "currency": "USD",
  "forexAmount": 100.00
}
```

- `orderType`: BUY (매수, KRW → 외화) / SELL (매도, 외화 → KRW)
- `currency`: USD, JPY, CNY, EUR
- `forexAmount`: 외화 기준 금액 (양수)

**Response**
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "id": 1,
    "fromAmount": 141278,
    "fromCurrency": "KRW",
    "toAmount": 100.00,
    "toCurrency": "USD",
    "tradeRate": 1412.78,
    "dateTime": "2026-04-25T14:30:00"
  }
}
```

#### 주문 내역 조회

```
GET /order/list
```

## 대상 통화

| 통화 | 설명 |
|------|------|
| USD | 미국 달러 |
| JPY | 일본 엔 (100엔 단위) |
| CNY | 중국 위안 |
| EUR | 유로 |

## 비즈니스 규칙

- **매매기준율**: 한국수출입은행 API에서 1분 주기로 수집
- **매수 환율**: 매매기준율 × 1.05 (소수점 둘째 자리, HALF_UP)
- **매도 환율**: 매매기준율 × 0.95 (소수점 둘째 자리, HALF_UP)
- **원화 환산**: 소수점 버림 (FLOOR)
- **JPY**: 100엔 단위로 환산

## 테스트

```bash
./gradlew test
```
