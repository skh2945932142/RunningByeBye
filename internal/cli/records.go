package cli

import (
	"fmt"
	"time"

	"github.com/spf13/cobra"
)

func registerRecordsCmd() {
	recordsCmd := &cobra.Command{
		Use:   "records <openid> [term]",
		Short: "查询锻炼记录",
		Long: `查询指定学期的校园跑锻炼记录。

学期编号说明:
  20251 = 2025-2026 第一学期
  20252 = 2025-2026 第二学期
  不传 term 则默认为当前学期 20252`,
		Args: cobra.RangeArgs(1, 2),
		RunE: func(cmd *cobra.Command, args []string) error {
			openID := args[0]
			term := "20252"
			if len(args) > 1 {
				term = args[1]
			}

			PrintTitle("📋 锻炼记录")
			PrintDivider()
			PrintInfo("正在查询学期 %s 的记录 ...", term)

			result := <-r.Handler.GetRecords(openID, term)
			if result.Err != nil {
				PrintError("查询失败: %v", result.Err)
				return result.Err
			}

			records := result.Results.Data
			if len(records) == 0 {
				PrintWarn("学期 %s 暂无锻炼记录", term)
				PrintDivider()
				return nil
			}

			validCount := 0
			for _, record := range records {
				if record.IsValid == "1" {
					validCount++
				}
			}

			PrintSuccess("共 %d 条记录（有效 %d 条）", len(records), validCount)
			PrintDivider()

			for i, record := range records {
				validMark := " "
				if record.IsValid == "1" {
					validMark = "✓"
				} else {
					validMark = "✗"
				}

				startTime, _ := time.Parse("2006-01-02 15:04:05", record.SportsStartTime)
				durationMin := record.Duration / 60
				durationSec := int(record.Duration) % 60

				PrintDim("  [%d] %s", i+1, validMark)
				PrintField("   场地", record.PlaceName)
				PrintField("   日期", startTime.Format("01-02 15:04"))
				PrintField("   距离", fmt.Sprintf("%.2f km", record.Distance))
				PrintField("   时长", fmt.Sprintf("%.0f 分 %d 秒", durationMin, durationSec))
				if i < len(records)-1 {
					fmt.Println()
				}
			}
			PrintDivider()

			return nil
		},
	}

	rootCmd.AddCommand(recordsCmd)
}
