package main

import (
	"flag"
	"fmt"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"

	"RunningByeBye/internal/runc"
	"RunningByeBye/internal/runc/pkg"
)

func main() {
	var (
		openID   string
		field    string
		pace     float64
		interval float64
	)

	flag.StringVar(&openID, "openid", "", "微信 OpenID（必填）")
	flag.StringVar(&openID, "o", "", "微信 OpenID（简写）")
	flag.StringVar(&field, "field", "风华运动场", "跑步场地：风华运动场 / 太极运动场 / 宁静苑")
	flag.StringVar(&field, "f", "风华运动场", "跑步场地（简写）")
	flag.Float64Var(&pace, "pace", 0, "配速 min/km，0=随机 5.0~7.0")
	flag.Float64Var(&pace, "p", 0, "配速（简写）")
	flag.Float64Var(&interval, "interval", 0, "点位间隔秒数，0=自动计算")
	flag.Float64Var(&interval, "i", 0, "点位间隔（简写）")
	flag.Parse()

	if openID == "" {
		openID = os.Getenv("OPEN_ID")
	}
	if openID == "" {
		fmt.Println("错误：请提供 OpenID")
		fmt.Println("用法: go run main.go -openid=your_wechat_openid [-field=风华运动场] [-pace=6.0]")
		fmt.Println("  或: set OPEN_ID=your_openid && go run main.go")
		flag.PrintDefaults()
		os.Exit(1)
	}

	baseDir := "./data"
	pointsDir := filepath.Join(baseDir, "points")
	taskDir := filepath.Join(baseDir, "tasks")
	cacheDir := filepath.Join(baseDir, "cache")

	for _, dir := range []string{pointsDir, taskDir, cacheDir} {
		if err := os.MkdirAll(dir, 0755); err != nil {
			fmt.Fprintf(os.Stderr, "创建目录失败 %s: %v\n", dir, err)
			os.Exit(1)
		}
	}

	pointsDir, _ = filepath.Abs(pointsDir)
	fmt.Printf("初始化运行时...\n")
	fmt.Printf("  场地:   %s\n", field)
	fmt.Printf("  配速:   ")
	if pace > 0 {
		fmt.Printf("%.1f min/km\n", pace)
	} else {
		fmt.Println("随机 (5.0~7.0 min/km)")
	}
	fmt.Printf("  点位:   %s\n", pointsDir)

	r, err := runc.New(
		runc.WithPointsDir(pointsDir),
		runc.WithTaskDir(taskDir),
		runc.WithCacheDir(cacheDir),
		runc.WithLogLevel(pkg.LogLevelInfo),
	)
	if err != nil {
		fmt.Fprintf(os.Stderr, "初始化失败: %v\n", err)
		os.Exit(1)
	}
	defer r.Close()

	r.Handler.RecoverTasks()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		for evt := range r.Handler.ProgressChannel() {
			percent := 0.0
			if evt.TotalPoints > 0 {
				percent = float64(evt.SubmittedCount) / float64(evt.TotalPoints) * 100
			}
			switch evt.State {
			case "running":
				fmt.Printf("\r[跑步中] %s: %d/%d (%.1f%%) 里程=%.3fkm     ",
					evt.OpenID, evt.SubmittedCount, evt.TotalPoints, percent, evt.Mileage)
			case "completed":
				fmt.Printf("\n[完成] %s: 里程 %.3fkm ✓\n", evt.OpenID, evt.Mileage)
			case "failed":
				fmt.Printf("\n[失败] %s: 跑步失败\n", evt.OpenID)
			}
		}
	}()

	fmt.Printf("\n登录中 %s ...\n", openID)
	loginResult := <-r.Handler.Login(openID)
	if loginResult.Err != nil {
		fmt.Fprintf(os.Stderr, "登录失败: %v\n", loginResult.Err)
		os.Exit(1)
	}
	user := loginResult.User
	fmt.Printf("欢迎 %s (%s %s)\n", user.MetaInfo.Username, user.MetaInfo.StudentNo, user.MetaInfo.DeptName)

	fmt.Printf("\n开始跑步: %s @ %s\n", field, user.MetaInfo.Username)
	startResult := <-r.Handler.StartRun(openID, field, pace, interval)
	if startResult.Err != nil {
		fmt.Fprintf(os.Stderr, "启动跑步失败: %v\n", startResult.Err)
		os.Exit(1)
	}
	fmt.Println("跑步已启动，每 20 秒上报一批点位...")
	fmt.Println("按 Ctrl+C 暂停")

	<-sigCh
	fmt.Println("\n\n暂停中...")
	r.Handler.PauseRun(openID)
	fmt.Println("跑步已暂停。再次按 Ctrl+C 结束跑步并退出。")

	<-sigCh
	fmt.Println("正在结束跑步并退出...")
	r.Handler.StopRun(openID)
	fmt.Println("跑步已结束。")
}
