package cli

import (
	"github.com/spf13/cobra"
)

func registerLoginCmd() {
	loginCmd := &cobra.Command{
		Use:   "login <openid>",
		Short: "登录并获取用户信息",
		Long:  `使用微信 OpenID 登录校园跑平台，获取 Token 并显示用户详细信息。`,
		Args:  cobra.ExactArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			openID := args[0]
			PrintTitle("🔑 用户登录")
			PrintDivider()

			PrintInfo("正在登录 %s ...", openID)

			result := <-r.Handler.Login(openID)
			if result.Err != nil {
				PrintError("登录失败: %v", result.Err)
				return result.Err
			}

			user := result.User
			PrintDivider()
			PrintSuccess("登录成功")
			PrintField("姓名", user.MetaInfo.Username)
			PrintField("学号", user.MetaInfo.StudentNo)
			PrintField("性别", user.MetaInfo.Sex)
			PrintField("年级", user.MetaInfo.Grade)
			PrintField("学院", user.MetaInfo.DeptName)
			PrintField("OpenID", user.OpenID)
			PrintDim("Token: %s...", user.Token[:16])
			PrintDivider()

			return nil
		},
	}

	rootCmd.AddCommand(loginCmd)
}
