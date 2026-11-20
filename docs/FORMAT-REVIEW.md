# 程式碼格式整理與驗證

日期：2026-11-17（Asia/Taipei）。本次只整理格式、import 與說明註解，不變更功能、業務規則、資料庫、操作介面、權限或套件版本。

## 修改範圍

- 整理 67 份 Java 檔案（含正式程式、測試及 Java 驗收工具）：4 空格縮排、方法空行、成員／建構子／方法分段、長行、運算子空格與一致的大括號。
- 展開萬用字元 import，移除未使用項目；統一為 Java 標準函式庫、Jakarta、第三方、專案類別、static import 分組。
- 在角色查詢範圍、LIKE 萬用字元跳脫、台北日期區間、停用部門關聯、密碼空白處理及統計範圍補上繁體中文註解。
- 整理 22 份 HTML／Thymeleaf：標籤層次、縮排、屬性與長行。表格操作控制項保留必要的行內空白邊界，避免新增可見間距。
- 整理自有 `app.css` 與兩份 JavaScript 驗收腳本。CSS 宣告分行、分號與空白一致；規則順序及屬性值保持不變。
- 整理 `pom.xml` 的 XML 排版，未加入或改動依賴、版本與 plugin。
- 新增 `.editorconfig`，統一 4 空格、UTF-8、換行與檔尾；第三方 vendor 排除縮排規則。

未格式化 Bootstrap CSS／JS／LICENSE；未修改 SPEC.md、DESIGN.md、AGENTS.md、schema.sql、application properties、既有資料庫或 MVC 分層。

## 等價與介面檢查

| 檢查 | 實際結果 |
|---|---|
| 三份規格、schema、既有資料與 Bootstrap 雜湊 | 9 個受保護檔案全部一致 |
| Java 編譯指令、方法簽章、常數 | 相同編譯選項產生 70 個 class，忽略除錯行號後比對一致 |
| JavaScript 語法樹 | 兩份自有腳本一致 |
| CSS 語法結構 | 選擇器、規則順序、宣告與值一致 |
| Thymeleaf 結構 | 22 份 template 的元素、屬性、指令與文字一致；保留 pre／textarea 內容 |
| pom.xml | XML 組態、依賴及版本一致 |
| 瀏覽器畫面 | 三角色共 21 個頁面，文字、控制項、樣式及位置比對差異 0 |

瀏覽器使用獨立的 `.tools/format-review/ui-comparison.db` 與 18081 埠，先後啟動整理前後的 JAR；未操作既有展示／正式資料庫，也未停止使用者原本執行中的服務。
原始摘要：[format-equivalence.json](verification/format-equivalence.json)、[format-ui.json](verification/format-ui.json)。

## Maven 完整測試

最終結果：**BUILD SUCCESS，36 tests，0 failures，0 errors，0 skipped**。

| 測試類別 | 測試數 |
|---|---:|
| BootstrapTest | 1 |
| DashboardTest | 3 |
| DemoInitializationTest | 1 |
| HomePageTest | 2 |
| ManagementTest | 5 |
| PersistenceTest | 4 |
| SecurityTest | 7 |
| TicketQueryTest | 5 |
| WorkflowTest | 8 |

涵蓋登入／登出、CSRF、角色授權與 Session 撤銷、管理表單、案件所有權、日期分頁、完整狀態流程、重複提交、交易 rollback、樂觀鎖與持久化。原有測試未刪除或停用。

本次首先執行一般 `clean verify`，但現有 `target/it-helpdesk-system-1.0.0.jar` 被執行中的程式鎖定，Windows 無法刪除。為保留該執行個體，使用 `.tools/format-review/build-pom.xml` 隔離輸出路徑；此暫存 POM 只指定原專案的 source／test／resources 絕對路徑與獨立 build 目錄，依賴與 plugin 原封不動。正式 pom.xml 僅有排版修改。

實際完成的命令：

```powershell
.\mvnw.cmd -f .tools/format-review/build-pom.xml -B clean verify help:effective-pom '-Doutput=build/effective-pom.xml' dependency:tree '-DoutputFile=build/dependency-tree.txt'
```

原始 JUnit 報告與解析結果複製至 `target/format-test-reports/`。測試使用獨立暫存 SQLite，未修改既有資料。

## 新打包檔案

- JAR：`target/it-helpdesk-system-1.0.0-formatted.jar`
- 原始碼：`target/it-helpdesk-system-formatted-source.zip`
- SHA-256：`target/SHA256SUMS-formatted.txt`

原本執行中的 JAR 保留不覆寫。停止原本的應用程式後，可用相同啟動參數啟動新 JAR；資料檔路徑不變即可沿用既有資料。勿讓兩個應用同時操作同一個資料庫。

若要另開一個獨立展示實例，可指定不同埠與新的展示資料檔：

```powershell
java -jar target/it-helpdesk-system-1.0.0-formatted.jar --spring.profiles.active=demo --helpdesk.database=data/format-demo.db --server.port=8081
```

之後在沒有程序占用建置檔案時，仍可依 README 使用正式 `pom.xml` 執行 `mvnw.cmd clean verify`。
