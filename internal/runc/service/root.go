package service

import (
	"time"

	CQUPT_Sports_SDK "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK"
)

type TokenJar interface {
	GetToken(openid string) (string, time.Time, error)
	SetToken(openid string, token string) error
	GetAllTokens() (map[string]string, error)
	RemoveToken(openid string) error
}

type Service struct {
	tokenJar TokenJar
	sportSDK *CQUPT_Sports_SDK.SDK
}
