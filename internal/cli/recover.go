package cli

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"

	"github.com/spf13/cobra"
)

func registerRecoverCmd() {
	recoverCmd := &cobra.Command{
		Use:   "recover",
		Short: "恢复未完成的跑步任务",
		Long: `扫描持久化存储中的未完成跑步任务，自动恢复执行。

对于每个未完成的任务：
  - 验证 Token 是否有效
  - Token 有效：从上次进度继续上报点位
  - Token 过期：跳过该用户

恢复后终端会实时显示所有任务的进度。`,
		Args: cobra.NoArgs,
		RunE: func(cmd *cobra.Command, args []string) error {
			PrintTitle("🔄 恢复任务")
			PrintDivider()

			PrintInfo("正在扫描持久化任务 ...")

			ids, _ := r.Service.GetSessionStore().List()
			err := r.Handler.RecoverTasks()
			if err != nil {
				PrintWarn("扫描过程中出现错误: %v", err)
			}

			for _, id := range ids {
				r.Handler.ResumeRun(id)
			}

			PrintSuccess("已恢复 %d 个跑步任务", len(ids))
			PrintDivider()

			sigCh := make(chan os.Signal, 1)
			signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
			done := make(chan struct{})
			pausedIDs := ids

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
						PrintSuccess("[%s] 跑步完成! 里程: %.3f km", evt.OpenID[:8]+"...", evt.Mileage)
					case "failed":
						fmt.Printf("\n%s", "")
						PrintError("[%s] 跑步失败", evt.OpenID[:8]+"...")
					}
				}
				close(done)
			}()

			select {
			case <-sigCh:
				fmt.Println()
				PrintInfo("收到中断信号，正在暂停所有任务 ...")
				for _, id := range pausedIDs {
					r.Handler.PauseRun(id)
				}
				PrintSuccess("已暂停 %d 个跑步任务", len(pausedIDs))
				PrintDim("使用 resume <openid> 恢复单个任务")
			case <-done:
			}

			PrintDivider()
			return nil
		},
	}

	rootCmd.AddCommand(recoverCmd)
}
