# E-Book 借阅系统

一个精简的电子书借阅系统后端，支持书籍浏览、详情查询、借阅、归还与「当前借阅中」清单查询，并提供 Swagger 在线接口文档。

## 技术栈

| 项目 | 选择 | 说明 |
|---|---|---|
| 语言 / 运行时 | Java 21 | 使用 record、switch 表达式等现代语法 |
| 框架 | Spring Boot 3.5（`web` + `validation`） | 未引入 DB / Security 等额外基础设施 |
| 接口文档 | springdoc-openapi 2.8（OpenAPI 3 / Swagger UI） | 启动后即可访问 `/swagger-ui.html` |
| 存储 | 内存（`ConcurrentHashMap`） | 通过 Service 接口抽象，可无痛替换为 RDB |
| 测试 | JUnit 5 + Mockito + MockMvc | 领域单元测试、服务测试、API 集成测试 |

## 快速开始

```bash
mvn spring-boot:run          # 启动（dev profile 会自动载入种子数据）
mvn test                     # 执行全部测试
```

启动后访问 Swagger 文档：

| 入口 | 地址 |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

在 UI 中可直接对全部接口进行在线调试（Try it out）。

也可以直接用 curl 调用：

```bash
# 浏览书籍（支持关键字与分页）
curl http://localhost:8080/api/books?keyword=design&page=0&size=10

# 书籍详情
curl http://localhost:8080/api/books/1

# 借阅书籍
curl -X POST http://localhost:8080/api/books/1/borrow \
     -H 'Content-Type: application/json' -d '{"memberId": 1}'

# 归还书籍（loanId 取自借阅响应）
curl -X POST http://localhost:8080/api/loans/1/return

# 查看当前借阅中的书籍
curl http://localhost:8080/api/members/1/loans
```

## 应用架构

采用单向依赖的分层架构（Controller → Service → 领域实体，存储通过接口抽象）：

```
com.kw
├── Application              ← Spring Boot 启动类
└── ebook
    ├── controller           ← HTTP 边界：BookController、LoanController
    ├── service              ← 应用层：用例编排（*ServiceHandler）与存储接口
    │   └── impl             ← 存储实现：内存版 Book/Loan/Member Service
    ├── entity               ← 领域实体（Book/Member/Loan，含业务规则）
    │   ├── req              ← 请求 DTO（BorrowBookReq）
    │   └── vo               ← 响应视图（BookDetailVo、LoanVo、PageVo 等）
    ├── enums                ← 领域枚举（LoanStatus）
    ├── exception            ← DomainException、ErrorCode、全局异常处理
    └── config               ← 横切配置：Clock Bean、OpenAPI 文档、种子数据
```

```
HTTP → controller → service（编排）→ entity（业务规则）← impl（内存存储实现）
```

### 关键取舍（Trade-offs）

1. **内存存储 vs 数据库**
   需求未要求持久化，引入 JPA/H2 会增加与目标无关的复杂度。
   但 `BookService` / `LoanService` / `MemberService` 接口定义在 service 层、
   内存实现放在 `service.impl`（依赖倒置），替换为 Spring Data JPA 时编排逻辑零改动。

2. **富领域模型 vs 事务脚本**
   业务规则由实体自身守卫，避免「规则散落在 Service」：
   - `Book.borrowOneCopy()`：库存不足一律拒绝（`synchronized` 防超借）；
   - `Member.ensureAllowsMoreLoans()`：同时借阅上限 5 本；
   - `Loan.markReturned()`：状态机 ACTIVE→RETURNED 单向且不可重复归还。

3. **RPC 式动作 vs RESTful 资源建模**
   借阅建模为 `POST /api/books/{id}/borrow`（发生在书籍资源上的动作）、
   归还建模为 `POST /api/loans/{id}/return`（借阅单的状态转换），
   而非硬造 `PUT /loans` 等不自然的动词资源；以 201 + `Location` 表达借阅单创建。

4. **错误语义映射 HTTP 状态码**
   领域业务异常采用**单一 `DomainException` 类型、以 `ErrorCode` 区分**（不定义子异常类），
   API 层只需一个 handler，由 code 决定 HTTP 语义：
   - 数据不存在 → `404`（BOOK/MEMBER/LOAN_NOT_FOUND）
   - 状态冲突（无库存、重复借阅、超上限、重复归还）→ `409 Conflict`
   - 输入校验失败 → `400`，并返回字段明细
   统一以 RFC 7807 `Problem Detail` 输出，并附稳定的 `code` 字段供程序判断，
   未预期异常一律 500 且不外泄内部细节（仅记录日志）。

5. **并发正确性的边界**
   库存扣减由 `Book` 同步方法保证正确（不会超借、不会变负）；
   但「检查重复借阅 → 创建借阅单」非原子操作，极端并发下可能产生重复借阅。
   在内存实现与本题范围内可接受；升级 DB 时应以
   `(memberId, bookId)` + 未归还条件的唯一索引彻底防堵（已在 `LoanServiceHandler` 注释标明）。

6. **服务层返回 DTO（LoanVo）**
   为避免 Controller 为了组装 `bookTitle` 而直接触碰存储接口，
   由 `LoanServiceHandler` 输出含显示信息（书名）的读模型。严格分层可再拆 Mapper/Output Port，
   对本规模属过度设计，此为务实取舍。

7. **文档方案：springdoc-openapi 而非 springfox**
   springfox 不兼容 Spring Boot 3，选用 springdoc-openapi（`@Tag` / `@Operation` 注解），
   文档由代码注解 + 方法签名自动生成的 OpenAPI 3 规范，无额外维护成本。

8. **假设说明**（需求未明确，自行合理设定）
   - 会员（Member）已存在，无需会员注册流程；
   - 借阅上限：单一会员同时最多借 5 本（可随会员调整）；
   - 同一会员不可同时借阅两本相同书籍（借新前先还旧）；
   - 无到期日 / 逾期罚金 / 预约队列（明确超出合理范围）。

## API 一览

| 方法 | 路径 | 说明 | 成功响应 |
|---|---|---|---|
| GET | `/api/books?keyword=&page=&size=` | 浏览书籍（分页+搜索） | 200 `PageVo<BookListItem>` |
| GET | `/api/books/{id}` | 书籍详情 | 200 `BookDetailVo` |
| POST | `/api/books/{id}/borrow`，body `{"memberId":1}` | 借阅 | 201 + `Location: /api/loans/{id}` |
| POST | `/api/loans/{id}/return` | 归还 | 200 `LoanVo` |
| GET | `/api/loans/{id}` | 单笔借阅 | 200 `LoanVo` |
| GET | `/api/members/{id}/loans?status=ACTIVE` | 借阅记录（默认当前借阅中） | 200 `LoanVo[]` |

错误响应统一格式（以重复归还为例，409）：

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "借阅单已归还，不可重复归还: id=1",
  "code": "LOAN_ALREADY_RETURNED"
}
```

## 领域模型

```
Book (id, isbn, title, author, description, totalCopies, availableCopies)
  规则：borrowOneCopy() 需有库存；returnOneCopy() 不超过总数
Member (id, name, maxActiveLoans=5)
  规则：ensureAllowsMoreLoans(当前借阅数)
Loan (id, bookId, memberId, borrowedAt, status, returnedAt)
  规则：ACTIVE → RETURNED 单向转换，重复归还拒绝
```

借阅前置检查顺序（成本低且语义明确者先）：
`会员存在 → 书籍存在 → 未重复借阅 → 未达上限 → 扣库存 → 创建借阅单`。

## 测试策略

| 层 | 测试 | 内容 |
|---|---|---|
| entity | `BookTest` / `MemberTest` / `LoanTest` | 纯业务规则，无需 Spring 容器 |
| service | `LoanServiceHandlerTest`（Mockito） | 用例编排与全部拒绝场景；注入固定 `Clock` 验证时间 |
| controller | `EbookApiIntegrationTest`（MockMvc） | 端到端：借→查→还全流程、404/409/400 错误契约 |

## 已知限制（刻意不处理）

- 数据重启即消失（内存实现）；无认证／授权（没有「谁在操作」的身份概念）；
- 无借阅期限／逾期／续借／预约；无管理端（新增书籍／会员）。
  以上如需扩展，架构均已预留位置。
