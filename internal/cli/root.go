package cli

import (
	"fmt"
	"os"
	"path/filepath"

	"RunningByeBye/internal/runc"
	"RunningByeBye/internal/runc/pkg"

	"github.com/spf13/cobra"
)

var (
	pointsDir string
	taskDir   string
	cacheDir  string
	logLevel  string
	r         *runc.Runc
)

var rootCmd = &cobra.Command{
	Use:   "commandline",
	Short: "CQUPT 校园跑命令行工具",
	Long: `基于 runc 运行时模块的校园跑命令行工具。

支持登录、跑步（启动/暂停/恢复/停止）、进度查询、
锻炼记录查询和崩溃恢复等功能。`,
	PersistentPreRunE: func(cmd *cobra.Command, args []string) error {
		if cmd.Name() == "help" || cmd.Name() == "completion" {
			return nil
		}
		return initRunc()
	},
	PersistentPostRunE: func(cmd *cobra.Command, args []string) error {
		if r != nil {
			r.Close()
		}
		return nil
	},
}

func Execute() {
	if err := rootCmd.Execute(); err != nil {
		os.Exit(1)
	}
}

func init() {
	rootCmd.PersistentFlags().StringVar(&pointsDir, "points", "./data/points", "轨迹点文件目录")
	rootCmd.PersistentFlags().StringVar(&taskDir, "tasks", "./data/tasks", "任务持久化目录")
	rootCmd.PersistentFlags().StringVar(&cacheDir, "cache", "./data/cache", "缓存目录")
	rootCmd.PersistentFlags().StringVar(&logLevel, "log", "info", "日志级别: debug/info/warn/error")

	registerLoginCmd()
	registerRunCmd()
	registerTaskCmd()
	registerRecordsCmd()
	registerRecoverCmd()
}

func initRunc() error {
	var level pkg.LogLevel
	switch logLevel {
	case "debug":
		level = pkg.LogLevelDebug
	case "info":
		level = pkg.LogLevelInfo
	case "warn":
		level = pkg.LogLevelWarn
	case "error":
		level = pkg.LogLevelError
	default:
		level = pkg.LogLevelInfo
	}

	for _, dir := range []string{pointsDir, taskDir, cacheDir} {
		if err := os.MkdirAll(dir, 0755); err != nil {
			return fmt.Errorf("创建目录失败 %s: %w", dir, err)
		}
	}

	absPoints, _ := filepath.Abs(pointsDir)

	var err error
	r, err = runc.New(
		runc.WithPointsDir(absPoints),
		runc.WithTaskDir(taskDir),
		runc.WithCacheDir(cacheDir),
		runc.WithLogLevel(level),
	)
	if err != nil {
		return fmt.Errorf("初始化运行时失败: %w", err)
	}

	return nil
}
