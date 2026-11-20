# 驗收對照

驗收日期：2026-11-16（Asia/Taipei）。最終 `clean verify` 成功：36 tests、0 failures、0 errors、0 skipped。
實際 jar、Chrome 操作與真正停止／重啟程序的驗收均通過。詳細數字見 [TEST-RESULTS.md](TEST-RESULTS.md)。

| 案例 | 自動測試／驗證（全部通過） |
|---|---|
| AC-01 | SecurityTest 三角色登入、登出、DashboardTest 角色選單 |
| AC-02 | WorkflowTest 完整申請、分派、開始、完成、確認結案 |
| AC-03 | WorkflowTest 退回、再分派與兩次維修紀錄 |
| AC-04 | WorkflowTest 僅本人 PENDING 可取消 |
| AC-05 | SecurityTest / ManagementTest / TicketQueryTest / WorkflowTest 越權 GET/POST、CSRF |
| AC-06 | 表單驗證、非法狀態、非法工程師、密碼位元組與資料庫約束測試 |
| AC-07 | TicketQueryTest 關鍵字、分類、狀態、日期邊界與分頁 |
| AC-08 | SecurityTest 停用帳號登入與既有 Session 撤銷 |
| AC-09 | PersistenceTest 關閉重連；DemoInitializationTest 再次初始化；最終 jar 重啟 |
| AC-10 | 打包 jar 在非 loopback socket 被拒絕的程序中啟動，瀏覽器僅允許 localhost 請求 |
| AC-11 | Maven clean verify、README 可重現步驟 |

測試只使用 target/test-databases/；打包驗收使用 .tools/ 內獨立資料庫，不清空或修改 data/ 展示資料庫。

## 實際 jar 與瀏覽器結果

- 使用最終 jar，以 demo profile 在 127.0.0.1:18080 啟動，資料庫為獨立 `.tools/acceptance/run-*.db`。
- 網路探針確認非 loopback socket 被拒絕。Java 21 的測試用 policy 允許應用所需的反射、檔案及本機 HTTP；不關閉 CSRF 或表單驗證。
- Chrome 依序完成三角色登入、申請、分派、開始維修、第一次完成、退回、第二次完成、驗收結案、取消、越權 403、查詢、部門與帳號新增／停用、登出。
- 桌面 1440px 與行動 390px 頁面截圖已檢視。瀏覽器 JavaScript 錯誤 0，頁面外部資源請求 0。
- 第一程序實際停止後，再啟動不同 PID 的 jar；14 件案件、6 個帳號、5 個部門、5 個分類均保留；驗收案件仍已結案，兩筆維修紀錄保留，demo 未重複建立。
- 此驗證採程序網路隔離，未實際拔除網路線或停用電腦網卡。驗收腳本結束後已停止其建立的伺服器。

原始結果：[browser-result.json](verification/browser-result.json)、[restart-result.json](verification/restart-result.json)。

可重現命令（先完成 README 的工具安裝與建置）：

```powershell
powershell -NoProfile -File scripts/verify-offline.ps1
```

腳本使用實際 JDK 執行檔，避開 Oracle javapath 轉接程序 PID；檢查埠空閒與啟動日誌，重啟驗收後停止測試服務。若 18080 已被其他服務使用，會拒絕開始。
