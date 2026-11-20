# AGENTS.md — IT Helpdesk System 開發指令

## 1. 任務與文件

本專案是 Java 全端工程師求職作品，採初階工程師友善版。開始之前完整閱讀 SPEC.md 與 DESIGN.md。SPEC.md 定義需求與業務規則；DESIGN.md 定義架構及實作基準；本文件定義開發方式。若發現衝突或無法相容，先說明原因並取得使用者確認，不擅自改規格。

首次任務只檢查本機 Java/Maven、資料夾內容及規格，提出逐階段計畫、依賴版本選擇與風險，等待使用者確認再開始寫程式。

## 2. 必須遵守

1. Java 21、Spring Boot 3.5.14、Spring MVC、相容 Maven 3.9.x、Thymeleaf（SSR）、SQLite、Spring Data JPA/Hibernate、Spring Security；不得自行改成其他框架、資料庫或 SPA。
2. 使用 MVC 與 Controller → Service → Repository 三層架構，業務邏輯集中 Service。
3. 採建構子注入、private final 依賴；不使用欄位注入。
4. 不使用 Lombok；明確撰寫建構子、Getter、Setter，JPA Entity 提供必要無參數建構子。
5. Java 類別與方法命名清楚；重要業務邏輯使用繁體中文註解，避免逐行重複語法。
6. 角色、狀態、優先級使用 Enum；表單使用 DTO 與 Jakarta Validation。
7. 使用 Spring Security、Session、BCrypt、CSRF、伺服器端角色及資源權限；不以畫面隱藏代替驗證。
8. 交易與狀態更新依 SPEC.md；禁止破壞歷史案件。
9. 所有前端資源本機化；執行時不得依賴網路。
10. 不加入規格以外的進階功能；不建立不必要的抽象或微服務。
11. 不修改專案外檔案、不註冊外部服務、不上傳資料、不使用真實帳密。Git 忽略 SQLite 執行資料、target、秘密與本機組態。
12. 不因測試失敗而刪除測試、關閉安全功能或宣稱未執行的測試成功。

依賴版本必須以 spring-boot-starter-parent 3.5.14 統一管理：受管理的 starter（含 web、thymeleaf）不指定 version；未受管理者才選擇並固定相容版本，Hibernate community dialect 與 core 同版。Phase 1 檢查 effective-pom、dependency:tree 並記錄 Maven Wrapper 版本。不得擅自覆寫或更新指定版本。

## 3. 分階段開發

每階段先解釋目的及預計修改檔案，實作後執行測試，報告變更、測試結果、未解決問題及下一步；等待使用者確認再進下一階段。

| Phase | 開發內容 | 驗收閘門 |
|---|---|---|
| 1 | Maven、Spring Boot、Wrapper、基本 Thymeleaf 頁面 | 應用啟動且首頁可顯示 |
| 2 | SQLite 組態、schema.sql、Entity、Repository、Enum | 建表、CRUD、外鍵與重啟保留資料測試 |
| 3 | Security、登入登出、密碼、Session、角色授權 | 三角色登入及越權測試 |
| 4 | 帳號、部門、分類管理 | 新增修改啟停及驗證測試 |
| 5 | 報修申請、列表、詳情、篩選與分頁 | 本人資料隔離及非法表單測試 |
| 6 | 分派、開始、維修、退回、結案及取消 | 完整狀態流程、交易 rollback、重複提交測試 |
| 7 | Dashboard、共用 fragments、商務風、本機靜態資源 | 統計符合資料、角色選單與頁面操作 |
| 8 | 全系統驗收、demo、README、打包 | SPEC AC-01～AC-11 驗收，離線 jar 測試 |

## 4. 測試與交付

- 優先使用 mvnw.cmd（Windows）或 ./mvnw（Linux/macOS），每階段執行 Maven test；最後執行 clean verify。
- 使用獨立 SQLite 暫存檔案進行整合測試，測試角色越權、非法狀態、資料一致性與持久化；使用 MockMvc 測試 GET/POST、CSRF、授權及錯誤處理。
- 初次下載依賴可以連網；完成打包後確認斷網能執行 java -jar，並透過 localhost 操作。
- 不把展示資料庫當成測試資料庫，不清空既有資料。
- README 須包含技術架構、目錄、資料庫 ERD、啟動/打包方式、展示帳號、操作演示及測試指令。
- 最終交付實際可執行的專案及測試結果；說明任何無法驗證的項目。

## 5. 教學與溝通

以繁體中文與使用者溝通。每完成階段，用初階工程師能理解的方式說明類別職責、資料流以及本次用到的 Spring/Java 技術，讓使用者能自行修改程式並在面試中解釋。遇到錯誤先說明原因，再提出修正；重大規格變更必須先詢問。
