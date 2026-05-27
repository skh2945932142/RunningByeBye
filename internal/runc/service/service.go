package service

import (
	"RunningByeBye/internal/runc/consts"
	"fmt"
)

func (r *Service) GetTokenByOpenID(openid string) (string, error) {
	token, ttl, err := r.tokenJar.GetToken(openid)
	if err != nil {
		data, err := r.sportSDK.GetToken(openid)
		if err != nil {
			return "", err
		}

		if data.Code != consts.SDK_STATUS_OK {
			return "", fmt.Errorf("sdk status error with code: %v, msg: %v", data.Code, data.Msg)
		}

		err = r.tokenJar.SetToken(openid, data.Data.Token)
		if err != nil {
			return "", err
		}

		return data.Data.Token, nil
	}

	return token, nil
}

func (r *Service)