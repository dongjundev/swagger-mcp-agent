# 구조 가이드

서비스들의 OpenAPI(Swagger) 스펙을 LLM이 직접 조회할 수 있게 해 주는 MCP 서버 프로젝트다.
토스의 [사내 MCP 서버 구현 글](https://toss.tech/article/internal-mcp-server)을 참고해 만들었다.

기동과 검증 절차는 [TEST_GUIDE.md](TEST_GUIDE.md)에 있다.

## 1. 개요

API 스펙을 사람이 링크로 주고받는 대신, LLM이 MCP tool로 최신 스펙을 찾아 읽는다.
"ms-order의 주문 취소 API를 호출하는 코드를 만들어 줘"라고 하면 LLM이 서비스 목록, API 목록, API 상세, 스키마를 차례로 조회해 답한다.

스펙 원본은 각 서비스가 갖고 있다. 이 프로젝트는 그것을 가져와서 LLM이 읽기 좋은 크기로 잘라 주는 역할만 한다.

## 2. 전체 구조

```
[MCP 클라이언트]         LLM 도구, MCP Inspector 등
      │  MCP (Streamable HTTP, POST /mcp)
      ▼
[swagger-mcp :8081]      Tool 4개 + Prompt 1개
      │  REST (GET /api/services/...)
      ▼
[swagger-center :8080]   스펙 수집 · 캐시 · 가공
      │  GET /v3/api-docs (조회 시점에 가져옴)
      ▼
[ms-user :8082] [ms-order :8083] [ms-product :8084]
```

| 모듈 | 포트 | 역할 |
|------|------|------|
| `swagger-mcp` | 8081 | MCP 서버. LLM에게 Tool과 Prompt를 제공하고, 실제 조회는 swagger-center에 위임한다 |
| `swagger-center` | 8080 | 각 서비스의 스펙을 가져와 캐시하고, 목록·상세·스키마 단위로 가공해 REST로 제공한다 |
| `ms-user`, `ms-order`, `ms-product` | 8082~8084 | 테스트용 샘플 서비스. springdoc으로 `/v3/api-docs`를 노출한다 |

Gradle 멀티 모듈이며 공통 설정(Spring Boot, springdoc, Java 21)은 루트 `build.gradle`에 있다.

swagger-mcp는 MCP 프로토콜만 담당하는 얇은 계층이고, 스펙을 다루는 로직은 모두 swagger-center에 있다.
그래서 swagger-center의 REST API는 MCP 없이 curl만으로도 확인할 수 있다.

## 3. 조회 흐름

스펙 전체를 한 번에 주면 LLM 컨텍스트를 넘기기 쉬워서, 네 단계로 좁혀 들어가게 되어 있다.

| 단계 | Tool | 입력 | 출력 |
|------|------|------|------|
| 1 | `listServices` | 없음 | 서비스별 `serviceName`, `title`, `description`, `version`, `servers`, `apiCount`, `fetchedAt` |
| 2 | `getApiList` | `serviceName`, `keyword`(선택), `page`, `size` | API별 `operationId`, `httpMethod`, `path`, `summary`, `tags`, `deprecated`와 페이징 정보 |
| 3 | `getApiDetail` | `serviceName`, `operationId` | `description`, `parameters`, `requestBody`, `responses`, `security`, `securitySchemes` |
| 4 | `getComponentSchema` | `serviceName`, `schemaName` | `name`, `schema` |

예시:

```
"ms-order의 주문 취소 API를 호출하는 코드를 만들어 줘"

1. listServices                           → ms-order 가 있음
2. getApiList(ms-order, keyword="취소")    → cancelOrder
3. getApiDetail(ms-order, cancelOrder)    → PATCH /api/orders/{id}/cancel, 응답은 $ref OrderDto
4. getComponentSchema(ms-order, "#/components/schemas/OrderDto")   → 필드 정의
```

- 3단계는 `$ref`를 풀지 않고 경로만 돌려준다. 필요한 스키마만 4단계로 따로 조회하게 해서 응답 크기를 줄인다.
- `getComponentSchema`의 `schemaName`은 이름(`OrderDto`)과 `$ref` 경로(`#/components/schemas/OrderDto`)를 모두 받는다.
- `keyword`는 공백으로 나눈 단어가 `operationId`, `path`, `summary`, `tags` 어딘가에 모두 들어 있는 API만 남긴다(대소문자 무시).

Tool 외에 두 가지가 더 있다.

- **Prompt `search-apis(serviceName, apiDesc)`**: 사용자가 직접 고르는 템플릿. 위 순서대로 tool을 호출하라는 메시지를 만들어 준다. LLM이 tool을 스스로 호출하지 않을 때 쓴다.
- **instructions**: MCP 연결 시 서버가 클라이언트에 알려 주는 사용 안내. swagger-mcp의 `application.yaml`에 있다.

## 4. swagger-center

### 패키지

```
swagger-center/src/main/java/com/example/swagger_center
├── controller
│   ├── SwaggerCenterController    REST API
│   └── GlobalExceptionHandler     예외를 HTTP 상태로 변환
├── service
│   └── SwaggerCenterService       입력 검증, keyword 필터, 페이징
├── parser
│   └── OpenApiParser              OpenAPI 모델을 응답용 구조로 변환
├── store
│   ├── SpecStore                  스펙 공급원 인터페이스
│   ├── RemoteSpecStore            서비스에서 스펙을 가져오고 캐시
│   └── SpecFetchException         스펙을 가져오지 못했을 때
├── config
│   ├── SwaggerCenterProperties    swagger-center.* 설정
│   └── RestClientConfig           스펙 조회용 RestClient (타임아웃)
├── domain                         ServiceInfo, ApiSummary, ApiDetail, ParameterInfo, ComponentSchema
└── dto
    └── PagedResponse
```

### REST API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/services` | 서비스 목록 |
| GET | `/api/services/{serviceName}/apis` | API 목록. `keyword`, `page`(기본 0), `size`(기본 20, 1~100) |
| GET | `/api/services/{serviceName}/apis/{operationId}` | API 상세 |
| GET | `/api/services/{serviceName}/schemas/{schemaName}` | 컴포넌트 스키마 |

### 스펙 수집

swagger-center는 스펙을 등록받아 저장하지 않는다. `swagger-center.services`에 적힌 주소에서 조회 시점에 가져온다(`RemoteSpecStore`).

```
요청 → 캐시에 있고 TTL 이내인가?
          ├ 예     → 캐시된 스펙 사용
          └ 아니오 → 서비스의 api-docs 호출 → 파싱 → 캐시에 저장
```

- 캐시는 서비스별로 메모리에 두며 `swagger-center.cache-ttl`(기본 1분) 동안 유효하다.
- 서비스의 API가 바뀌면 캐시가 만료된 뒤 다음 조회부터 반영된다. 등록 작업은 없다.
- 스펙을 가져올 수 없는 서비스는 서비스 목록에서 빠진다(로그에 `Skipping service` 경고). 그 서비스를 직접 조회하면 502를 돌려준다.
- 스펙 조회에는 연결 2초, 읽기 5초 타임아웃이 걸려 있다.

### 스펙 가공

`OpenApiParser`가 swagger-parser로 읽은 OpenAPI 모델을 응답용 구조로 바꾼다.

- 스키마, requestBody, responses는 swagger-core의 매퍼로 변환한다. 스펙 버전에 따라 `Json.mapper()`(3.0) 또는 `Json31.mapper()`(3.1)를 쓴다. 일반 `ObjectMapper`를 쓰면 `exampleSetFlag` 같은 내부 필드가 섞여 나오고 3.1 스펙의 `type`이 빠진다.
- 컴포넌트 스키마는 통째로 돌려준다. `enum`, `allOf`, `oneOf`, `items`가 그대로 보존된다.
- `operationId`가 없는 API는 메서드와 경로로 ID를 만들어 쓴다(예: `DELETE /v1/payments/{paymentId}` → `delete_v1_payments__paymentId`).
- `security`는 operation에 선언이 있으면 그것을, 없으면 스펙 전역 선언을 쓴다. 참조된 scheme의 정의는 `securitySchemes`에 함께 담는다.

### 오류 응답

모든 오류는 `{"error": "메시지"}` 형태다.

| 상황 | 상태 |
|------|------|
| 없는 서비스, 없는 operationId, 없는 스키마 | 400 |
| `page`가 음수이거나 `size`가 1~100 밖 | 400 |
| 서비스에서 스펙을 가져오거나 파싱하지 못함 | 502 |

## 5. swagger-mcp

### 패키지

```
swagger-mcp/src/main/java/com/example/swagger_mcp
├── tool
│   └── SwaggerTools           @Tool 메서드 4개
├── config
│   ├── McpServerConfig        Tool 등록, search-apis Prompt 정의
│   └── RestClientConfig       swagger-center 호출용 RestClient
├── client
│   └── SwaggerCenterClient    swagger-center REST 호출
└── dto                        swagger-center 응답을 받는 record
```

### 구성

- Spring AI의 `spring-ai-starter-mcp-server-webmvc`를 쓴다. 엔드포인트는 `POST /mcp`다.
- `protocol: STATELESS`: 요청 사이에 세션을 유지하지 않는다. 서버 재배포 때 세션이 끊기는 문제가 없어 토스 글에서도 이 방식을 택했다.
- `type: ASYNC`: Tool과 Prompt를 비동기 방식으로 등록한다. Prompt 빈의 타입(`AsyncPromptSpecification`)이 이 설정에 맞춰져 있어서, `SYNC`로 바꿀 때는 Prompt 정의도 `SyncPromptSpecification`으로 바꿔야 한다.
- Tool은 `SwaggerTools`의 `@Tool` 메서드를 `MethodToolCallbackProvider`로 등록한다. LLM이 보는 설명문은 `@Tool`과 `@ToolParam`의 `description`이다.
- `dto`의 record는 swagger-center의 `domain`과 같은 모양을 따로 정의한 것이다. 한쪽에 필드를 추가하면 다른 쪽도 고쳐야 한다.

### 오류 전달

swagger-center가 오류를 돌려주면 tool 결과가 `isError: true`로 표시되고, 본문에 swagger-center의 상태와 메시지가 그대로 들어간다.

```
400 Bad Request: "{"error":"Service not found: nope"}"
```

## 6. 설정

`swagger-center/src/main/resources/application.yaml`

| 키 | 기본값 | 설명 |
|----|--------|------|
| `swagger-center.services` | 샘플 서비스 3개 | 서비스 이름 → api-docs 주소. 서비스 목록도 이 순서로 나온다 |
| `swagger-center.cache-ttl` | `1m` | 가져온 스펙을 캐시하는 시간. `0s`이면 매번 가져온다 |

`swagger-mcp/src/main/resources/application.yaml`

| 키 | 값 | 설명 |
|----|----|------|
| `swagger-center.base-url` | `http://localhost:8080` | swagger-center 주소 |
| `spring.ai.mcp.server.protocol` | `STATELESS` | MCP 전송 방식 |
| `spring.ai.mcp.server.type` | `ASYNC` | Tool·Prompt 등록 방식 |
| `spring.ai.mcp.server.instructions` | 안내문 | 클라이언트에 전달되는 사용 안내 |

## 7. 서비스 추가하기

1. 대상 서비스가 OpenAPI 3.x 스펙을 HTTP로 노출해야 한다. Spring 서비스라면 springdoc 의존성을 추가하면 `/v3/api-docs`가 생긴다.
2. `swagger-center.services`에 `이름: api-docs 주소`를 추가한다.
3. swagger-center를 재시작한다.

검색 품질은 스펙에 달려 있다. `keyword`는 `summary`, `tags`, `operationId`, `path`를 대상으로 하므로 `@Operation(summary = ..., operationId = ...)`과 `@Tag`를 채워 두는 것이 좋다.

서로 다른 패키지에 같은 이름의 DTO가 있으면 그 서비스에 `springdoc.use-fqn=true`를 설정한다. 패키지명이 붙은 스키마 이름도 `getComponentSchema`로 조회된다.

## 8. 토스 구현과의 차이

4단계 조회, `$ref`를 경로로만 돌려주는 방식, STATELESS + ASYNC 구성, Prompt 제공은 토스 글과 같다. 다른 점은 다음과 같다.

| 항목 | 토스 글 | 이 프로젝트 |
|------|---------|-------------|
| 스펙을 가져오는 주체 | MCP 서버가 각 서비스를 직접 호출 | swagger-center가 호출하고 가공까지 담당 |
| 서비스 목록 | SwaggerCenter가 관리 | swagger-center의 설정 파일 |
| API 목록 좁히기 | apiGroup 단위로 조회 | `keyword` 필터와 숫자 페이징 |
| API 상세 조회 키 | serviceName, apiGroup, URL, HTTP 메서드 | serviceName, operationId |
| 컴포넌트 조회 | `$ref` 경로 목록을 한 번에 | 한 번에 하나 |

## 9. 알려진 제약

- 스펙 변경은 최대 `cache-ttl`만큼 늦게 반영된다. 내려간 서비스도 캐시가 만료될 때까지는 캐시된 스펙으로 응답한다.
- 서비스를 추가하거나 빼려면 설정을 고치고 swagger-center를 재시작해야 한다.
- 캐시가 비어 있을 때 `listServices`는 모든 서비스에 순차로 요청한다. 실패는 캐시하지 않으므로 응답 없는 서비스가 있으면 호출할 때마다 타임아웃만큼 기다린다.
- 인증이 없다. swagger-center와 swagger-mcp 모두 접근 제어 없이 열려 있다.
- OpenAPI 3.x만 지원한다. Swagger 2.0 스펙은 파싱에 실패한다.
- `getComponentSchema`는 `#/components/schemas/`만 조회한다. `requestBodies`나 `responses`를 가리키는 `$ref`는 따라갈 수 없다.
- MCP 응답에서 `fetchedAt`은 epoch 초 숫자로 나간다(swagger-center REST에서는 ISO-8601).
- `getApiList`의 `page`, `size`는 tool 스키마상 필수다(생략해도 서버는 기본값으로 처리한다).

## 10. 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 3.5.12 |
| Spring AI (MCP Server WebMVC) | 1.1.3 |
| springdoc-openapi | 2.8.4 (OpenAPI 3.1 출력) |
| swagger-parser | 2.1.25 |
| Gradle | 9.4.0 (wrapper) |
