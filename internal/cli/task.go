package cli

import (
	"fmt"

	"RunningByeBye/internal/runc/models"

	"github.com/spf13/cobra"
)

func registerTaskCmd() {
	taskCmd := &cobra.Command{
		Use:   "task <openid>",
		Short: "查询任务详情（内存 + 磁盘）",
		Long: `查询指定用户的跑步任务详情。

优先从内存中查找活跃任务，如果不存在则从磁盘持久化文件中加载。
即使进程已退出，只要 task 文件还在就能查看到上次的进度。`,
		Args: cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			openID := args[0]
			PrintTitle("📋 任务详情")
			PrintDivider()

			PrintInfo("正在查询 %s ...", openID)

			task, err := r.Handler.LoadTask(openID)
			if err != nil {
				PrintWarn("未找到任务: %v", err)
				PrintDim("可尝试用 recover 命令扫描所有任务")
				PrintDivider()
				return nil
			}

			printFullTaskInfo(task)
			return nil
		},
	}

	rootCmd.AddCommand(taskCmd)
}

func printFullTaskInfo(task *models.TaskNode) {
	stateNames := map[models.TaskState]string{
		models.TaskStatePending:   "等待中",
		models.TaskStateRunning:   "运行中",
		models.TaskStatePaused:    "已暂停",
		models.TaskStateCompleted: "已完成",
		models.TaskStateFailed:    "已失败",
	}

	stateName, ok := stateNames[task.State]
	if !ok {
		stateName = fmt.Sprintf("未知(%d)", task.State)
	}

	percent := 0.0
	if len(task.LocationPoints) > 0 {
		percent = float64(task.SubmittedCount) / float64(len(task.LocationPoints)) * 100
	}

	PrintDivider()
	PrintField("OpenID", task.TargetUser.OpenID)
	PrintField("姓名", task.TargetUser.MetaInfo.Username)
	PrintField("学号", task.TargetUser.MetaInfo.StudentNo)
	PrintField("状态", stateName)

	if task.IsPaused {
		PrintField("暂停", "是")
	}

	PrintField("场地", task.TaskSettings.TargetFieldID)
	PrintField("配速", fmt.Sprintf("%.1f min/km", task.TaskSettings.TargetPace))
	PrintField("上报间隔", fmt.Sprintf("%.1f 秒", task.TaskSettings.TargetInterval))
	PrintField("进度", fmt.Sprintf("%d / %d (%.1f%%)", task.SubmittedCount, len(task.LocationPoints), percent))
	PrintField("里程", fmt.Sprintf("%.3f km", task.Mileage))
	PrintField("记录编号", task.RecordNo)

	if !task.StartTime.IsZero() {
		PrintField("开始时间", task.StartTime.Format("2006-01-02 15:04:05"))
	}

	points := task.LocationPoints
	if len(points) > 0 {
		PrintField("点位总数", fmt.Sprintf("%d 个", len(points)))
		PrintField("坐标范围",
			fmt.Sprintf("(%.4f~%.4f, %.4f~%.4f)",
				minCoord(points, "lon"), maxCoord(points, "lon"),
				minCoord(points, "lat"), maxCoord(points, "lat")))
	}

	PrintDivider()

	if task.State == models.TaskStateRunning && !task.IsPaused {
		PrintDim("提示: 使用 run pause 暂停 | run stop 停止 | progress 查看进度")
	} else if task.State == models.TaskStatePaused || task.IsPaused {
		PrintDim("提示: 使用 resume 恢复跑步")
	} else if task.State == models.TaskStateFailed {
		PrintDim("提示: 任务已失败，可删除 task 文件或重新 start")
	}
}

func minCoord(points []models.LocationPointNode, field string) float64 {
	if len(points) == 0 {
		return 0
	}
	v := points[0].Longitude
	if field == "lat" {
		v = points[0].Latitude
	}
	for _, p := range points {
		c := p.Longitude
		if field == "lat" {
			c = p.Latitude
		}
		if c < v {
			v = c
		}
	}
	return v
}

func maxCoord(points []models.LocationPointNode, field string) float64 {
	if len(points) == 0 {
		return 0
	}
	v := points[0].Longitude
	if field == "lat" {
		v = points[0].Latitude
	}
	for _, p := range points {
		c := p.Longitude
		if field == "lat" {
			c = p.Latitude
		}
		if c > v {
			v = c
		}
	}
	return v
}
