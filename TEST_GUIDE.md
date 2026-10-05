# E2E 테스트 절차 (MCP Inspector)

swagger-center, swagger-mcp, 테스트 MS 3개(user/order/product)를 MCP Inspector로 전체 연동 테스트한다.
전체 구조는 [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md)에 정리되어 있다.

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

## 자동 테스트

서버를 띄우지 않고 실행된다. E2E 전에 먼저 통과하는지 확인한다.

```bash
./gradlew test
```

| 테스트 | 검증 내용 |
|--------|-----------|
| `OpenApiParserTest` | 스키마 변환(enum, allOf/oneOf, 3.1 스펙의 type), tags·security 등 메타데이터 추출 |
| `SwaggerCenterServiceTest` | page/size 검증, keyword 필터, 서비스 정보 |
| `RemoteSpecStoreTest` | 스펙 가져오기, 캐시 TTL, 가져올 수 없는 서비스 처리 |
| `SwaggerToolsTest` | `$ref` 경로 형태의 schemaName 처리 |
| `McpEndpointTest` | `/mcp` 엔드포인트로 search-apis 프롬프트 조회 |

---

## Step 1: 전체 서비스 기동

터미널 4개에서 실행한다. swagger-center가 조회 시점에 스펙을 가져오므로 기동 순서는 상관없다.

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
# → 각각 "OpenAPI definition"
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

# keyword로 좁히기 (공백으로 나눈 단어가 모두 포함된 API만 반환)
curl -s -G "http://localhost:8080/api/services/ms-user/apis" --data-urlencode "keyword=삭제" | jq .
# → deleteUser 1개

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
# → serverInfo, capabilities, instructions 포함된 JSON 응답
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
- 기대 결과: ms-user, ms-order, ms-product 3개 서비스. 각 항목에 title, servers, apiCount, fetchedAt 포함

### 5-2. getApiList 테스트
- `getApiList` 선택
- 파라미터: `serviceName` = `ms-user`, `page` = `0`, `size` = `20`
- 기대 결과: listUsers, createUser, getUser, deleteUser 4개 API. 각 항목에 tags 포함
- `keyword` = `삭제` 를 추가하면 deleteUser 1개만 반환

### 5-3. getApiDetail 테스트
- `getApiDetail` 선택
- 파라미터: `serviceName` = `ms-order`, `operationId` = `createOrder`
- 기대 결과: POST /api/orders. requestBody는 `#/components/schemas/CreateOrderRequest`, 응답은 `#/components/schemas/OrderDto`를 `$ref`로 참조
- `$ref`는 풀리지 않고 경로만 나온다. 필드 정의는 5-4처럼 getComponentSchema로 조회한다

### 5-4. getComponentSchema 테스트
- `getComponentSchema` 선택
- 파라미터: `serviceName` = `ms-product`, `schemaName` = `ProductDto`
- 기대 결과: id, name, category, price, stock 필드 스키마
- `schemaName`에 `#/components/schemas/ProductDto`를 넣어도 같은 결과

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

## Step 7: 스펙 자동 반영 확인

캐시 만료를 오래 기다리지 않도록 swagger-center를 짧은 TTL로 다시 기동한다.

```bash
# 터미널 1 - 기존 swagger-center를 중지한 뒤
cd swagger-center && ../gradlew bootRun --args='--swagger-center.cache-ttl=5s'
```

Inspector에서 순차적으로:
1. ms-order 터미널에서 Ctrl+C로 중지
2. 5초 뒤 `listServices` → ms-user, ms-product 2개만 반환
3. `getApiList(ms-order)` → `Failed to fetch spec of ms-order ...` 오류 (502)
4. ms-order 재기동 → `listServices`가 다시 3개 반환 (등록 작업 없음)

---

## 체크리스트

| # | 항목 | 상태 |
|---|------|------|
| 1 | `./gradlew test` 통과 | ☐ |
| 2 | swagger-center 기동 (:8080) | ☐ |
| 3 | ms-user/order/product 기동 (:8082-8084) | ☐ |
| 4 | 등록 없이 3개 MS 스펙 조회됨 | ☐ |
| 5 | swagger-center REST API 응답 정상 | ☐ |
| 6 | swagger-mcp 기동 (:8081) | ☐ |
| 7 | MCP Inspector 연결 성공 | ☐ |
| 8 | listServices → 3개 서비스 반환 | ☐ |
| 9 | getApiList → API 목록 반환, keyword로 좁혀짐 | ☐ |
| 10 | getApiDetail → 상세 정보 반환 | ☐ |
| 11 | getComponentSchema → 스키마 반환 | ☐ |
| 12 | search-apis 프롬프트 → 메시지 반환 | ☐ |
| 13 | MS 중지·재기동이 서비스 목록에 자동 반영 | ☐ |

## 트러블슈팅

- **Connection refused**: 해당 포트의 서비스가 기동되었는지 확인
- **Service not found**: `swagger-center.services` 설정에 해당 서비스가 있는지 확인
- **Failed to fetch spec (502)**: 해당 MS가 기동되어 있는지, 설정된 api-docs 주소가 맞는지 확인
- **size must be between 1 and 100 / page must be 0 or greater**: `getApiList`의 `size`는 1~100, `page`는 0 이상이어야 함
- **MCP Inspector 연결 실패**: Transport Type이 `Streamable HTTP`인지, URL이 `http://localhost:8081/mcp`인지 확인
- **서비스 목록에 MS가 안 보임**: 스펙을 가져올 수 없는 MS는 목록에서 제외됨. swagger-center 로그의 `Skipping service` 경고 확인
- **MS의 API를 바꿨는데 반영이 안 됨**: 캐시 TTL(기본 1분)이 지난 뒤 다시 조회
