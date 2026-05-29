# CQUPT-Sports-SDK 使用总结

> **重庆邮电大学智慧体育 SDK** — Go 语言封装，用于调用重邮智慧体育平台接口。

- 仓库地址：[github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK](https://github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK)
- 开源协议：MIT
- 核心功能：Token 获取、用户信息查询、跑步流程（开始/上报点位/结束）、锻炼记录查询、场地/学期信息等

---

## 1. 安装

```bash
go get github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK
```

---

## 2. 快速开始

### 2.1 初始化 SDK

SDK 通过 `Options` 模式进行配置，支持自定义 Cookie 持久化路径：

```go
import sdk "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK"

// 默认 Cookie 路径 ./CookieJar.json
s, err := sdk.NewSDK()

// 自定义 Cookie 路径
s, err := sdk.NewSDK(sdk.WithCookiePath("./my_cookies.json"))

// 使用完毕后关闭（自动保存 Cookie）
defer s.Close()
```

| 配置项 | 类型 | 说明 |
|--------|------|------|
| `WithCookiePath(path string)` | `Options` | 设置 Cookie Jar 持久化文件路径 |

### 2.2 完整使用流程

```go
package main

import (
    "fmt"
    "time"
    sdk "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK"
    "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK/models"
)

func main() {
    // 1. 初始化 SDK
    s, err := sdk.NewSDK(sdk.WithCookiePath("./cookies.json"))
    if err != nil {
        panic(err)
    }
    defer s.Close()

    // 2. 获取 Token（需要微信 OpenID）
    tokenInfo, err := s.GetToken("your_wechat_openid")
    if err != nil {
        panic(err)
    }
    token := tokenInfo.Data.Token
    fmt.Println("Token:", token)

    // 3. （可选）查询用户信息
    userInfo, _ := s.GetUserInfo(token)
    fmt.Printf("用户: %s (%s)\n", userInfo.Data.StudentNo, userInfo.Data.Username)

    // 4. 开始跑步（风华运动场）
    startResp, err := s.StartRunning(token, s.GetFieldID_FengHua())
    if err != nil {
        panic(err)
    }
    runID := startResp.Data
    fmt.Println("跑步记录编号:", runID)

    // 5. 上报位置点（循环上报）
    points := []models.SportPointListNode{
        {
            Longitude:   106.60525,
            Latitude:    29.53286,
            PlaceName:   "风华运动场",
            PlaceCode:   "T1001",
            CollectTime: time.Now().Format("2006-01-02 15:04:05"),
            IsValid:     "1",
        },
    }
    s.UpdateRunningPoint(token, runID, points)

    // 6. 结束跑步
    finishResp, err := s.FinishRunning(token, runID)
    if err != nil {
        panic(err)
    }
    fmt.Println("跑步结束, 成功:", finishResp.Data)
}
```

---

## 3. API 函数详解

### 3.1 GetToken — 获取 Token

根据微信 OpenID 获取用户 Token，是后续所有接口调用的前提。

```go
tokenInfo, err := s.GetToken("your_openid")
```

| 参数 | 类型 | 说明 |
|------|------|------|
| openid | `string` | 微信 OpenID |
| 返回 | 类型 | 说明 |
|------|------|------|
| tokenInfo | `*models.TokenInfo` | 包含 `isBind`(是否已绑定)、`publicKey`、`token` |
| err | `error` | 请求错误 |

### 3.2 GetUserInfo — 获取用户信息

```go
userInfo, err := s.GetUserInfo("your_token")
```

| 参数 | 类型 | 说明 |
|------|------|------|
| token | `string` | 用户 Token |
| 返回 | 类型 | 说明 |
|------|------|------|
| userInfo | `*models.UserInfo` | 学号、姓名、性别、年级、院系等 |
| err | `error` | 请求错误 |

### 3.3 GetNotice — 获取通知

```go
noticeInfo, err := s.GetNotice("your_token")
```

| 返回字段 | 说明 |
|----------|------|
| `noticeName` | 通知标题 |
| `content` | 通知内容 |

### 3.4 GetAllArea — 获取所有跑步场地

```go
areaInfo, err := s.GetAllArea("your_token")
```

返回场地列表，每个场地包含：
- `placeName` / `placeCode` — 场地名称与代码
- `areaPointList` — 区域坐标点列表（含经纬度）

内置场地映射：
| 辅助方法 | 场地代码 | 场地名称 |
|----------|----------|----------|
| `s.GetFieldID_FengHua()` | T1001 | 风华运动场 |
| `s.GetFieldID_TaiJi()` | T1005 | 太极运动场 |
| `s.GetFieldID_NingJing()` | T1014 | 宁静苑 |

### 3.5 GetTerm — 获取学期信息

```go
termInfo, err := s.GetTerm("your_token")
```

返回学期列表，关键字段：
- `yearTerm` — 学期编号（如 `"20251"` = 2025-2026 第一学期，`"20252"` = 第二学期）
- `yearTermDes` — 学期描述
- `startDate` / `endDate` — 起止日期
- `isCurrent` — 是否为当前学期

### 3.6 GetAllSportsResult — 获取所有锻炼记录

```go
results, err := s.GetAllSportsResult("your_token", "20252")
```

| 参数 | 类型 | 说明 |
|------|------|------|
| token | `string` | 用户 Token |
| term | `string` | 学期编号，如 `"20251"`、`"20252"` |
| 返回 | 类型 | 说明 |
|------|------|------|
| results | `*models.AllSportsResults` | 锻炼记录列表 |

每条记录包含：
- `sportsType` — 运动类型
- `isValid` — 是否有效
- `sportsStartTime` / `sportsEndTime` — 开始/结束时间
- `duration` — 时长（秒）
- `distance` — 距离（公里）
- `placeName` — 场地名称

### 3.7 StartRunning — 开始跑步

```go
resp, err := s.StartRunning("your_token", s.GetFieldID_FengHua())
```

| 参数 | 类型 | 说明 |
|------|------|------|
| token | `string` | 用户 Token |
| fieldID | `string` | 场地编号（建议使用辅助方法获取） |
| 返回 | 类型 | 说明 |
|------|------|------|
| resp | `*models.StartRunningResp` | `data` 字段为跑步记录编号（runningEventID） |
| err | `error` | 请求错误 |

### 3.8 GetCurrentRunningInfo — 获取当前跑步详情

```go
info, err := s.GetCurrentRunningInfo("your_token", "running_event_id")
```

| 返回字段 | 说明 |
|----------|------|
| `mileage` | 当前里程（公里） |
| `timeConsuming` | 耗时（秒） |
| `points` | 已上报的轨迹点列表 |
| `expiredCountInForbiddenArea` | 禁区停留次数 |
| `runningCount` | 跑步次数 |

### 3.9 UpdateRunningPoint — 上报跑步位置点

```go
points := []models.SportPointListNode{
    {
        Longitude:     106.60525,
        Latitude:      29.53286,
        PlaceName:     "风华运动场",
        PlaceCode:     "T1001",
        CollectTime:   "2006-01-02 15:04:05",
        IsValid:       "1",
    },
}
resp, err := s.UpdateRunningPoint("your_token", "running_event_id", points)
```

请求参数 `SportPointListNode` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `sportRecordNo` | `string` | 跑步记录编号（可留空） |
| `longitude` | `float64` | 经度 |
| `latitude` | `float64` | 纬度 |
| `placeName` | `string` | 场地名称 |
| `placeCode` | `string` | 场地代码 |
| `collectTime` | `string` | 采集时间，格式 `"2006-01-02 15:04:05"` |
| `isValid` | `string` | 是否有效，`"1"`=有效 |

### 3.10 FinishRunning — 结束跑步

```go
resp, err := s.FinishRunning("your_token", "running_event_id")
```

| 返回 | 类型 | 说明 |
|------|------|------|
| resp | `*models.FinishRunningResp` | `data` 为 `bool`，`true`=成功，`false`=失败 |
| err | `error` | 请求错误 |

### 3.11 Close — 关闭 SDK

```go
err := s.Close()
```

保存 Cookie 到持久化文件，建议用 `defer` 确保执行。

---

## 4. 数据结构总览

所有响应均包含通用字段 `Msg`(消息) 和 `Code`(状态码)，SDK 内置定义：

```go
const SDK_STATUS_OK = "10200"  // 请求成功
```

### 核心数据结构关系图

```
TokenInfo
  └─ Data.Token      ──→ 后续接口的 token 参数

UserInfo
  └─ Data          ──→ StudentNo, Username, Sex, Grade, DeptName

StartRunningResp
  └─ Data          ──→ runningEventID（后续操作以此标识）

UpdateRunningPoint
  ├─ 请求体: []SportPointListNode
  └─ 响应体: RunningLocationPointResp（含当前里程/耗时）

FinishRunningResp
  └─ Data (bool)   ──→ 跑步是否成功结束
```

---

## 5. 单元测试

### 环境变量依赖

| 环境变量 | 说明 |
|----------|------|
| `OPEN_ID` | 微信 OpenID，用于 `TestSDK_GetToken` |
| `TOKEN` | 用户 Token，用于其他测试用例 |

### 运行测试

```bash
# 运行所有测试
go test -v

# 运行单个测试
go test -v -run TestSDK_GetToken
```

测试覆盖了 SDK 所有公开方法，其中：
- `TestSDK_UpdateRunningPoint` 从 `test_location.json` 读取坐标数据进行循环上报
- 测试用例按顺序依赖：`GetToken` → 其他接口

---

## 6. 内部实现要点

### 6.1 网络层

- **HTTP 客户端**：基于 `net/http` 标准库
- **Cookie 持久化**：使用 `persistent-cookiejar` 库，Cookie 自动管理
- **User-Agent**：模拟微信小程序环境（Android + 微信内置浏览器）
- **重定向策略**：`http.ErrUseLastResponse`，不自动跟随重定向

### 6.2 API 端点

| 功能 | 方法 | URL |
|------|------|-----|
| 获取 Token | GET | `/new_wxapp/wxUnifyId/checkBinding` |
| 用户信息 | GET | `/new_wxapp/wxUnifyId/getUser` |
| 通知列表 | GET | `/new_wxapp/notice/getNotice` |
| 场地列表 | GET | `/new_wxapp/area/getAllArea` |
| 学期列表 | GET | `/new_wxapp/yearTerm/list` |
| 锻炼记录 | GET | `/new_wxapp/sportsResult/list` |
| 开始跑步 | POST | `/new_wxapp/sportRecord/sport/start2` |
| 跑步详情 | GET | `/new_wxapp/sportRecord/info/{eventID}` |
| 上报点位 | POST | `/new_wxapp/sportRecord/point/saveListByNo` |
| 结束跑步 | POST | `/new_wxapp/sportRecord/sport/end/{eventID}` |

> 基础地址：`https://sport.cqupt.edu.cn`

### 6.3 请求头

- GET 请求：仅携带 `User-Agent` 和 `token`（如有）
- POST 请求：额外携带 `Content-Type: application/json`

---

## 7. 典型场景示例

### 场景：模拟一次完整跑步（含多点位上报）

```go
func runSport(s *sdk.SDK, token, fieldID, placeName, placeCode string, locations []models.SportPointListNode) error {
    // 1. 开始跑步
    startResp, err := s.StartRunning(token, fieldID)
    if err != nil {
        return fmt.Errorf("start running failed: %w", err)
    }
    runID := startResp.Data

    // 2. 分批上报位置点
    batchSize := 5
    for i := 0; i < len(locations); i += batchSize {
        end := i + batchSize
        if end > len(locations) {
            end = len(locations)
        }
        batch := locations[i:end]

        resp, err := s.UpdateRunningPoint(token, runID, batch)
        if err != nil {
            return fmt.Errorf("update point failed at batch %d: %w", i/batchSize, err)
        }
        fmt.Printf("当前里程: %.3f km, 耗时: %d s\n", resp.Data.Mileage, resp.Data.TimeConsuming)
        time.Sleep(2 * time.Second) // 间隔上报
    }

    // 3. 结束跑步
    finishResp, err := s.FinishRunning(token, runID)
    if err != nil {
        return fmt.Errorf("finish running failed: %w", err)
    }
    fmt.Println("完成跑步, 成功:", finishResp.Data)
    return nil
}
```

---

## 8. 注意事项

1. **Token 获取**：`GetToken` 需要微信小程序的 `OpenID`，这是整个 SDK 使用的起点
2. **Cookie 管理**：SDK 会自动持久化 Cookie，建议在程序退出前调用 `Close()`
3. **场地编号**：使用 `GetFieldID_*()` 辅助方法而非硬编码字符串
4. **时间格式**：`collectTime` 必须为 `"2006-01-02 15:04:05"` 格式
5. **上报频率**：建议每次上报间隔 2-5 秒，避免被服务端限流
6. **点位坐标**：需在场地允许的范围内，可在 `GetAllArea` 返回的 `areaPointList` 中获取参考坐标
