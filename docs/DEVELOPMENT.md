# 開發與驗證紀錄

本文件記錄各階段實際執行結果。以 SPEC.md、DESIGN.md 為業務與架構基準。
使用者於 2026-11-16 授權連續完成八階段，免除逐階段確認；重大規格變更仍須確認。

## 工具版本

- Oracle JDK 21.0.11，Java release 21。
- Spring Boot Parent 3.5.14；受管理依賴不覆寫版本。
- Apache Maven 3.9.16；Maven Wrapper 3.3.4，only-script。
- Wrapper 預設快取改為專案 `.tools/maven-home`；Maven repository 為 `.tools/repository`。
- 初次 Wrapper 生成因 PowerShell 未引用 `-Dmaven=3.9.16` 而失敗，使用引號重新生成成功。
- 初次 PowerShell 下載遇連線限制，經工具權限審查後完成下載。

## Phase 1

建立 Boot 主程式、Controller 與 Thymeleaf 首頁。Controller 回傳 View 名稱，由 Thymeleaf 渲染 HTML；尚未加入資料庫與認證。

驗證命令：`mvnw.cmd test help:effective-pom -Doutput=target/effective-pom.xml dependency:tree -DoutputFile=target/dependency-tree.txt`。
結果：BUILD SUCCESS，2 tests，0 failures/errors/skipped；含實際 HTTP 隨機埠首頁驗證。
effective-pom 確認 Hibernate 6.6.49.Final、SQLite JDBC 3.49.1.0；原始輸出位於 target/ 與 .tools/phase1*.log。

## Phase 2

五表 Entity 與 Repository、schema.sql、固定九位小數的 UTC TEXT converter、Hikari 單連線及每連線外鍵設定。
首次測試編譯缺少 Path import，補齊後重跑成功。
`mvnw.cmd test dependency:tree`：6 tests，0 failures/errors/skipped；CRUD、唯一、CHECK、FK RESTRICT、時間轉換與檔案重新連線保留資料通過。
所有測試檔案使用 target/test-databases/ 獨立 SQLite；不接觸 data/helpdesk.db。

## Phase 3

登入透過 UserDetailsService 讀取帳號，Spring Security 驗證 BCrypt 與 Session；每次請求由 LiveAccountFilter 比對最新角色及啟用狀態。
Service 再檢查目前身分，避免只靠路由或畫面。密碼修改保留首尾空白，檢查 72 UTF-8 位元組上限；成功後 Session 失效。
非 demo 首次管理員透過環境變數初始化；test profile 不執行初始化。
`mvnw.cmd test`：13 tests，0 failures/errors/skipped，含三角色登入、越權、CSRF、Session 撤銷、登出、密碼與中文字元上限。

## Phase 4

帳號、部門及分類管理使用 DTO + Jakarta Validation；Controller 處理 BindingResult，Service 檢查唯一性、最後管理員、未完成案件與停用部門。
展示 DTO 不包含密碼雜湊，編輯表單不回填密碼。停用保留被引用的歷史資料。
`mvnw.cmd test`：18 tests，0 failures/errors/skipped；管理頁、表單、權限與業務限制通過。

## Phase 5

TicketService 用角色範圍 Specification 合併查詢條件；分頁排序為 created_at DESC、id DESC，日期由 Asia/Taipei 換算 UTC 半開區間。
報修 DTO 不含 requester/status，避免任意參數指定身分或跳過初始狀態；展示 DTO 無密碼。
工程師目前分派案件可讀該案歷史；轉派後另以本人紀錄列表唯讀查看自己撰寫紀錄，無該案操作權。
`mvnw.cmd test`：23 tests，0 failures/errors/skipped，含資料隔離、非法表單、參數偽造、XSS 跳脫、查詢、日期邊界與分頁。

## Phase 6

TicketWorkflowService 集中八個狀態轉換，依角色、所有權、原狀態與表單版本驗證。@Version 保護交易間更新，Service @Transactional 包住狀態與維修紀錄。
故障注入測試以 SQLite trigger 強制紀錄插入失敗；SQLite 將錯誤包成 JpaSystemException，補上中文錯誤映射後重跑。
測試不使用外層測試交易，逐次 HTTP 請求實際 commit/rollback；確認狀態與版本 rollback，且無半筆維修紀錄。
`mvnw.cmd test`：31 tests，0 failures/errors/skipped；完整流程、退回兩次歷史、轉派、重複/過期提交、終態、越權與樂觀鎖均通過。

## Phase 7

DashboardService 依角色 Specification 統計總數、六狀態、近期五案；管理員分類統計為實際 JPQL GROUP BY 查詢，含零件數分類。
Thymeleaf 共用 header/sidebar/footer，Bootstrap 5.3.9 編譯 CSS/JS 與 MIT LICENSE 本機化；深藍側欄、白色內容與行動裝置排版。
`mvnw.cmd test`：34 tests，0 failures/errors/skipped；真實資料統計、角色選單、空資料與本機資源通過。

## Phase 8

DemoService 只在 demo profile 啟用，以單一交易建立虛構展示資料。使用不可刪除的 demo 案件判斷已初始化，帳號改名不會造成重複建立。
BootstrapService 提供非 demo 首次管理員初始化，密碼從環境變數取得；一般啟動不建立公開展示帳號。
只讀展示資料採 Java record；可編輯表單採一般 DTO、明確 getter/setter 與 Jakarta Validation。

最終 `clean verify`：36 tests，0 failures/errors/skipped。jar 的 Chrome 操作、程序網路隔離與真正重啟驗收通過，詳見 TEST-RESULTS.md 與 ACCEPTANCE.md。
測試工具曾遇到 Windows PowerShell 編碼／stderr 處理、測試用反射權限、下拉選單定位及 Oracle javapath 轉接 PID 問題；已修正，並以實際 JDK PID 與空閒埠檢查重新驗證。
桌面與行動截圖已檢視，無頁面 JavaScript 錯誤與外部資源請求。所有測試資料與工具留在專案內，不上傳資料。

### 面試與自行修改重點

新增欄位時依序修改 schema.sql、Entity、表單 DTO、Service、template 與對應測試。既有正式資料庫結構如需變更，須另外規劃保留資料的遷移，不把 ddl-auto 改為 create。
新增業務規則放在 Service；Controller 只處理 HTTP／Model／BindingResult，Repository 只處理查詢。
`@Transactional` 確保狀態與維修紀錄同成同敗；`@Version` 防止舊資料覆蓋新資料；表單版本防止舊請求在後續維修週期被重送。
路由角色驗證與 Service 個別資源驗證缺一不可；畫面隱藏按鈕只是操作提示。
