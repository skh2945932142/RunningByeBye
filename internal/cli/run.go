package cli

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"

	"github.com/spf13/cobra"
)

var (
	runField    string
	runPace     float64
	runInterval float64
)

func registerRunCmd() {
	runCmd := &cobra.Command{
		Use:   "run",
		Short: "跑步控制（start / pause / resume / stop）",
		Long:  `校园跑步任务的完整生命周期控制。`,
	}

	startCmd := &cobra.Command{
		Use:   "start <openid>",
		Short: "启动跑步",
		Long: `启动一次校园跑任务，自动上报点位直到完成或用户中断。

运行期间实时显示进度。按 Ctrl+C 暂停跑步并退出命令行，
之后可用 resume 恢复。`,
		Args: cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			openID := args[0]
			PrintTitle("🏃 开始跑步")
			PrintDivider()

			sigCh := make(chan os.Signal, 1)
			signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)

			done := make(chan struct{})

			go func() {
				for evt := range r.Handler.ProgressChannel() {
					percent := 0.0
					if evt.TotalPoints > 0 {
						percent = float64(evt.SubmittedCount) / float64(evt.TotalPoints) * 100
					}
					switch evt.State {
					case "running":
						fmt.Printf("\r%s %s 进度: %d/%d (%.1f%%) 里程: %.3f km%s",
							green("🏃"), cyan(evt.OpenID[:8]+"..."),
							evt.SubmittedCount, evt.TotalPoints, percent, evt.Mileage, reset())
					case "completed":
						fmt.Printf("\n%s", "")
						PrintSuccess("跑步完成! 里程: %.3f km", evt.Mileage)
						close(done)
						return
					case "failed":
						fmt.Printf("\n%s", "")
						PrintError("跑步失败")
						close(done)
						return
					}
				}
			}()

			PrintInfo("正在登录 %s ...", openID)
			loginResult := <-r.Handler.Login(openID)
			if loginResult.Err != nil {
				PrintError("登录失败: %v", loginResult.Err)
				return loginResult.Err
			}
			user := loginResult.User
			PrintSuccess("登录成功 %s (%s)", user.MetaInfo.Username, user.MetaInfo.StudentNo)

			fieldDesc := runField
			if code := mapFieldToCode(fieldDesc); code != "" {
				fieldDesc = fieldDesc + " (" + code + ")"
			}
			PrintField("场地", fieldDesc)
			if runPace > 0 {
				PrintField("配速", fmt.Sprintf("%.1f min/km", runPace))
			} else {
				PrintField("配速", "随机 (5.0~7.0 min/km)")
			}
			PrintDivider()

			PrintInfo("正在启动跑步 ...")
			startResult := <-r.Handler.StartRun(openID, runField, runPace, runInterval)
			if startResult.Err != nil {
				PrintError("启动失败: %v", startResult.Err)
				return startResult.Err
			}
			PrintSuccess("跑步已启动")
			PrintDim("每 20 秒上报一批点位，按 Ctrl+C 暂停")
			PrintDivider()

			select {
			case <-sigCh:
				fmt.Println()
				PrintInfo("收到中断信号，正在暂停跑步 ...")
				r.Handler.PauseRun(openID)
				PrintSuccess("跑步已暂停")
				PrintDim("使用 resume 命令恢复跑步")
				PrintDivider()
			case <-done:
				PrintDivider()
			}

			return nil
		},
	}

	startCmd.Flags().StringVarP(&runField, "field", "f", "风华运动场", "跑步场地: 风华运动场/太极运动场/宁静苑")
	startCmd.Flags().Float64VarP(&runPace, "pace", "p", 0, "配速 min/km, 0=随机")
	startCmd.Flags().Float64VarP(&runInterval, "interval", "i", 0, "点位间隔秒数, 0=自动")

	resumeCmd := &cobra.Command{
		Use:   "resume <openid>",
		Short: "恢复跑步",
		Long: `恢复已暂停的跑步任务，从上次进度继续上报点位。

支持跨进程恢复：即使上次进程已退出，只要任务文件还在 disk 上，
resume 会自动从磁盘加载任务并恢复上报。

恢复后终端会实时显示进度，按 Ctrl+C 可再次暂停。`,
		Args: cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			openID := args[0]
			PrintTitle("▶ 恢复跑步")
			PrintDivider()

			task := r.Handler.GetProgress(openID)
			if task == nil {
				PrintInfo("任务不在内存中，正在从磁盘恢复 ...")
				if err := r.Handler.RecoverTasks(); err != nil {
					PrintWarn("恢复扫描时出现错误: %v", err)
				}
				task = r.Handler.GetProgress(openID)
			}

			if task == nil {
				PrintWarn("没有找到已暂停的任务: %s", openID)
				PrintDim("可尝试用 recover 命令扫描所有任务")
				PrintDivider()
				return nil
			}
			if !task.IsPaused {
				PrintWarn("任务当前不是暂停状态 (state=%d)", task.State)
				PrintDivider()
				return nil
			}

			PrintField("进度", fmt.Sprintf("%d/%d", task.SubmittedCount, len(task.LocationPoints)))
			PrintField("里程", fmt.Sprintf("%.3f km", task.Mileage))

			sigCh := make(chan os.Signal, 1)
			signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
			done := make(chan struct{})

			go func() {
				for evt := range r.Handler.ProgressChannel() {
					percent := 0.0
					if evt.TotalPoints > 0 {
						percent = float64(evt.SubmittedCount) / float64(evt.TotalPoints) * 100
					}
					switch evt.State {
					case "running":
						fmt.Printf("\r%s %s 进度: %d/%d (%.1f%%) 里程: %.3f km%s",
							green("🏃"), cyan(evt.OpenID[:8]+"..."),
							evt.SubmittedCount, evt.TotalPoints, percent, evt.Mileage, reset())
					case "completed":
						fmt.Printf("\n%s", "")
						PrintSuccess("跑步完成! 里程: %.3f km", evt.Mileage)
						close(done)
						return
					case "failed":
						fmt.Printf("\n%s", "")
						PrintError("跑步失败")
						close(done)
						return
					}
				}
			}()

			r.Handler.ResumeRun(openID)
			PrintSuccess("跑步已恢复")
			PrintDim("按 Ctrl+C 暂停")
			PrintDivider()

			select {
			case <-sigCh:
				fmt.Println()
				PrintInfo("收到中断信号，正在暂停跑步 ...")
				r.Handler.PauseRun(openID)
				PrintSuccess("跑步已暂停")
				PrintDim("使用 resume 命令恢复跑步")
				PrintDivider()
			case <-done:
				PrintDivider()
			}

			return nil
		},
	}

	stopCmd := &cobra.Command{
		Use:   "stop <openid>",
		Short: "停止跑步",
		Long: `停止跑步任务并通知服务器结束本次跑步记录。

支持停止正在运行和已暂停的任务，即使进程已退出。`,
		Args: cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			openID := args[0]
			PrintTitle("⏹ 停止跑步")
			PrintDivider()

			task := r.Handler.GetProgress(openID)
			if task != nil {
				r.Handler.StopRun(openID)
				PrintSuccess("跑步已停止")
				PrintField("已上报", fmt.Sprintf("%d 个点位", task.SubmittedCount))
				PrintField("里程", fmt.Sprintf("%.3f km", task.Mileage))
				PrintDivider()
				return nil
			}

			PrintInfo("任务不在内存中，正在从磁盘加载 ...")
			task, err := r.Handler.LoadTask(openID)
			if err != nil {
				PrintWarn("未找到任务: %s (%v)", openID, err)
				PrintDivider()
				return nil
			}
			PrintSuccess("已从磁盘加载任务")

			if task.RecordNo != "" {
				r.Service.EnsureToken(openID)
				r.Service.FinishRunning(openID, task.RecordNo)
			}

			r.Service.GetSessionStore().Delete(openID)
			PrintSuccess("跑步已停止")
			PrintField("已上报", fmt.Sprintf("%d 个点位", task.SubmittedCount))
			PrintField("里程", fmt.Sprintf("%.3f km", task.Mileage))
			PrintDivider()

			return nil
		},
	}

	runCmd.AddCommand(startCmd)
	runCmd.AddCommand(resumeCmd)
	runCmd.AddCommand(stopCmd)

	rootCmd.AddCommand(runCmd)
}

func mapFieldToCode(name string) string {
	switch name {
	case "风华运动场":
		return "T1001"
	case "太极运动场":
		return "T1005"
	case "宁静苑":
		return "T1014"
	default:
		return ""
	}
}

var colorReset = "\033[0m"

func green(s string) string { return "\033[32m" + s + colorReset }
func red(s string) string   { return "\033[31m" + s + colorReset }
func cyan(s string) string  { return "\033[36m" + s + colorReset }
func reset() string         { return colorReset }
