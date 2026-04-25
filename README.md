# 외환 주문 시스템

실시간 환율 기반 외환 주문 시스템입니다. 한국수출입은행 Open API에서 환율을 수집하고, 매수/매도 주문을 처리합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.0
- Spring Data JPA
- H2 Database (In-Memory)
- SpringDoc OpenAPI (Swagger)
- Gradle

## 실행 방법

### 방법 1: Mock 데이터로 바로 실행 (API 키 불필요)

별도 설정 없이 바로 실행할 수 있습니다. 현실적인 시세 범위 내에서 환율 데이터가 자동 생성됩니다.

```bash
./gradlew bootRun
```

### 방법 2: 한국수출입은행 실제 API 연동

실제 환율 데이터를 사용하려면 API 키를 설정합니다.
[한국수출입은행 Open API](https://www.koreaexim.go.kr/ir/HPHKIR020M01?apino=2&viewtype=C)에서 무료로 발급받을 수 있습니다.

```bash
export KOREAEXIM_AUTH_KEY=your-api-key
./gradlew bootRun
```

> API 키가 설정되면 자동으로 실제 API를 사용합니다. 키가 비어있거나 없으면 Mock 클라이언트로 동작합니다.

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
  "returnObject": {
    "exchangeRateList": [
      {
        "currency": "USD",
        "tradeStanRate": 1345.50,
        "buyRate": 1412.78,
        "sellRate": 1278.23,
        "dateTime": "2026-04-25T11:00:00"
      }
    ]
  }
}
```

#### 특정 통화 최신 환율 조회

```
GET /exchange-rate/latest/{currency}
```

- `currency`: USD, JPY, CNY, EUR

### 주문

#### 외화 매수 (KRW -> 외화)

```
POST /order
Content-Type: application/json

{
  "forexAmount": 200,
  "fromCurrency": "KRW",
  "toCurrency": "USD"
}
```

**Response** (적용 환율: buyRate)
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "fromAmount": 296086,
    "fromCurrency": "KRW",
    "toAmount": 200.0,
    "toCurrency": "USD",
    "tradeRate": 1480.43,
    "dateTime": "2026-04-25T14:30:00"
  }
}
```

#### 외화 매도 (외화 -> KRW)

```
POST /order
Content-Type: application/json

{
  "forexAmount": 133,
  "fromCurrency": "USD",
  "toCurrency": "KRW"
}
```

**Response** (적용 환율: sellRate)
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "fromAmount": 133,
    "fromCurrency": "USD",
    "toAmount": 196104,
    "toCurrency": "KRW",
    "tradeRate": 1474.47,
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

- **매매기준율**: 1분 주기로 수집 (실제 API 또는 Mock)
- **매수 환율**: 매매기준율 × 1.05 (소수점 둘째 자리, HALF_UP)
- **매도 환율**: 매매기준율 × 0.95 (소수점 둘째 자리, HALF_UP)
- **원화 환산**: 소수점 버림 (FLOOR)
- **JPY**: 100엔 단위로 환산

## 테스트

```bash
./gradlew test
```

## 빌드

```bash
./gradlew build
```
