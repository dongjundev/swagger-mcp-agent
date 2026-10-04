# E2E 테스트 절차 (MCP Inspector)

구현 완료된 swagger-center, swagger-mcp, 테스트 MS 3개(user/order/product)를 MCP Inspector로 전체 연동 테스트한다.

## 아키텍처

```
[MCP Inspector]
    ↕ Streamable-HTTP (POST /mcp)
[swagger-mcp :8081]  ← Tool 4개, Prompt 1개
    ↕ REST
[swagger-center :8080]  ← 서비스 주소록 + 캐시
    ↓ 조회 시 각 MS의 /v3/api-docs 를 가져옴
[ms-user :8082] [ms-order :8083] [ms-product :8084]
```

---

## Step 1: 전체 서비스 기동

터미널 4개에서 순서대로 실행:

```bash
# 터미널 1 - swagger-center
cd swagger-center && ../gradlew bootRun

# 터미널 2 - ms-user
cd ms-user && ../gradlew bootRun

# 터미널 3 - ms-order
cd ms-order && ../gradlew bootRun

# 터미널 4 - ms-product
cd ms-product && ../gradlew bootRun
```

기동 확인:
```bash
curl -s http://localhost:8082/v3/api-docs | jq .info.title
curl -s http://localhost:8083/v3/api-docs | jq .info.title
curl -s http://localhost:8084/v3/api-docs | jq .info.title
```

---

## Step 2: swagger-center가 MS의 스펙을 가져오는지 확인

등록 작업은 없다. swagger-center가 `swagger-center/src/main/resources/application.yaml`의
`swagger-center.services`에 적힌 주소에서 조회 시점에 스펙을 가져온다.

```bash
curl -s http://localhost:8080/api/services | jq '.[].serviceName'
# → ms-user, ms-order, ms-product 3개 출력되어야 함
```

가져온 스펙은 `swagger-center.cache-ttl`(기본 1분) 동안 캐시된다.
- MS의 API가 바뀌면 캐시가 만료된 뒤 다음 조회부터 반영된다.
- MS를 중지하면 캐시가 만료된 뒤 목록에서 빠지고, 다시 기동하면 자동으로 돌아온다.

---

## Step 3: swagger-center REST API 수동 검증

```bash
# API 목록 조회
curl -s "http://localhost:8080/api/services/ms-user/apis" | jq .

# API 상세 조회
curl -s "http://localhost:8080/api/services/ms-user/apis/createUser" | jq .

# 컴포넌트 스키마 조회
curl -s "http://localhost:8080/api/services/ms-order/schemas/OrderDto" | jq .
```

---

## Step 4: swagger-mcp 기동

```bash
# 터미널 5
cd swagger-mcp && ../gradlew bootRun
```

기동 확인 - MCP 엔드포인트 응답 확인:
```bash
curl -s http://localhost:8081/mcp -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | jq .
# → serverInfo, capabilities 포함된 JSON 응답
```

---

## Step 5: MCP Inspector로 Tool 테스트

```bash
npx @modelcontextprotocol/inspector
```

Inspector UI에서:
1. **Connection**: Transport Type → `Streamable HTTP`, URL → `http://localhost:8081/mcp`
2. **Connect** 클릭

### 5-1. listServices 테스트
- Tools 탭 → `listServices` 선택 → Run
- 기대 결과: ms-user, ms-order, ms-product 3개 서비스 목록

### 5-2. getApiList 테스트
- `getApiList` 선택
- 파라미터: `serviceName` = `ms-user`, `page` = `0`, `size` = `20`
- 기대 결과: listUsers, getUser, createUser, deleteUser 4개 API
- `keyword` = `삭제` 를 추가하면 deleteUser 1개만 반환

### 5-3. getApiDetail 테스트
- `getApiDetail` 선택
- 파라미터: `serviceName` = `ms-order`, `operationId` = `createOrder`
- 기대 결과: parameters, requestBody(userId, items), responses 상세 정보

### 5-4. getComponentSchema 테스트
- `getComponentSchema` 선택
- 파라미터: `serviceName` = `ms-product`, `schemaName` = `ProductDto`
- 기대 결과: id, name, category, price, stock 필드 스키마

### 5-5. search-apis 프롬프트 테스트
- Prompts 탭 → `search-apis` 선택
- 파라미터: `serviceName` = `ms-order`, `apiDesc` = `주문 취소`
- 기대 결과: getApiList → getApiDetail → getComponentSchema 순서로 조회하라는 user 메시지

---

## Step 6: 크로스 서비스 시나리오 테스트

Inspector에서 순차적으로:
1. `listServices` → 서비스 목록 확인
2. `getApiList(ms-order)` → 주문 API 중 `cancelOrder` 확인
3. `getApiDetail(ms-order, cancelOrder)` → PATCH 메서드, path parameter 확인
4. `getComponentSchema(ms-order, OrderItemDto)` → 중첩 스키마 확인

---

## 체크리스트

| # | 항목 | 상태 |
|---|------|------|
| 1 | swagger-center 기동 (:8080) | ☐ |
| 2 | ms-user/order/product 기동 (:8082-8084) | ☐ |
| 3 | 등록 없이 3개 MS 스펙 조회됨 | ☐ |
| 4 | swagger-center REST API 응답 정상 | ☐ |
| 5 | swagger-mcp 기동 (:8081) | ☐ |
| 6 | MCP Inspector 연결 성공 | ☐ |
| 7 | listServices → 3개 서비스 반환 | ☐ |
| 8 | getApiList → API 목록 반환 | ☐ |
| 9 | getApiDetail → 상세 정보 반환 | ☐ |
| 10 | getComponentSchema → 스키마 반환 | ☐ |
| 11 | search-apis 프롬프트 → 메시지 반환 | ☐ |

## 트러블슈팅

- **Connection refused**: 해당 포트의 서비스가 기동되었는지 확인
- **Service not found**: `swagger-center.services` 설정에 해당 서비스가 있는지 확인
- **Failed to fetch spec (502)**: 해당 MS가 기동되어 있는지, 설정된 api-docs 주소가 맞는지 확인
- **MCP Inspector 연결 실패**: Transport Type이 `Streamable HTTP`인지, URL이 `http://localhost:8081/mcp`인지 확인
- **서비스 목록에 MS가 안 보임**: 스펙을 가져올 수 없는 MS는 목록에서 제외됨. swagger-center 로그의 `Skipping service` 경고 확인
