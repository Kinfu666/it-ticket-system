# 最終測試結果

日期：2026-11-16（Asia/Taipei）。JDK 21.0.11、Spring Boot 3.5.14、Maven 3.9.16、Wrapper 3.3.4。

## Maven

實際執行：

```powershell
.\mvnw.cmd -B clean verify help:effective-pom '-Doutput=target/effective-pom.xml' dependency:tree '-DoutputFile=target/dependency-tree.txt'
```

結果：**BUILD SUCCESS，36 tests，0 failures，0 errors，0 skipped**。

| 測試類別 | 數量 | 驗證重點 |
|---|---:|---|
| BootstrapTest | 1 | 首次管理員憑證要求、初始化不重複 |
| DashboardTest | 3 | 實際統計、角色範圍、選單、空資料、本機資源 |
| DemoInitializationTest | 1 | 四部門、五分類、三角色、六狀態；帳號改名後不重複初始化 |
| HomePageTest | 2 | MockMvc 渲染、隨機埠實際 HTTP 服務 |
| ManagementTest | 5 | 管理頁面、CRUD／啟停、唯一性、最後管理員及工程師保護、越權 |
| PersistenceTest | 4 | 五表 CRUD、FK RESTRICT／CHECK／唯一、時間轉換、SQLite 關閉重開 |
| SecurityTest | 7 | 三角色登入、停用／錯誤密碼、Session 失效、CSRF、角色、密碼修改 |
| TicketQueryTest | 5 | 申請人偽造、資料隔離、日期區間、分頁、XSS 跳脫、歷史紀錄 |
| WorkflowTest | 8 | 完整狀態流程、兩次歷史、重新分派、重複提交、rollback、樂觀鎖 |
| **合計** | **36** | 全部成功 |

JUnit 原始 XML／文字報告在 `target/surefire-reports/`。測試檔案位於 `target/test-databases/`，未使用正式／展示資料庫。

`effective-pom` 與 dependency tree 確認 Hibernate Core／community dialect 都為 **6.6.49.Final**、SQLite JDBC **3.49.1.0**；Spring Security **6.5.10**，受 Parent 管理，沒有手動覆寫。

## 打包及離線程序驗收

產物：`target/it-helpdesk-system-1.0.0.jar`。

SHA-256：`926a655c3034d349c4f573c36e8d20537bea9ea40ec21f1cb4d03a60648fc274`。

以 Java policy 阻擋非 localhost socket，探針確認封鎖有效；實際 jar 在隔離條件啟動，Chrome 僅允許本機請求。完整操作與重啟結果為 passed=true。
這是程序隔離驗收，未變更整機網路或宣稱實際停用網卡。

最終獨立資料庫：`.tools/acceptance/run-d073c17928d445bb8413eabc0abf7511.db`。
重啟前後：**14 件案件、6 個帳號、5 個部門、5 個分類**；驗收案件 ID 13 已結案、兩筆維修紀錄保留。
瀏覽器 pageErrors：0；externalRequests：0。原始 JSON 在 [verification/](verification/)。

## 已處理的失敗

首次建置與各階段遇到的錯誤均修正後重新驗證：PowerShell Maven 參數解析、測試缺少 import、SQLite trigger 例外映射、測試用網路政策反射權限、下拉選單的瀏覽器 locator，以及 Oracle Java 轉接程序的 PID。
最後一次重啟腳本已確認使用真正 JDK 程序，舊程序退出後埠釋放，再啟動新 PID；未採用先前連回舊服務的結果。

未刪除失敗測試、未關閉 Security／CSRF／Validation、未把未執行項目標示成功。
