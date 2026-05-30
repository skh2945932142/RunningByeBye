package service

import (
	"sync"

	sdk "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK"
	"github.com/redis/go-redis/v9"

	"RunningByeBye/internal/runc/consts"
	"RunningByeBye/internal/runc/models"
	"RunningByeBye/internal/runc/pkg"
)

type ServiceReliance struct {
	RedisClient *redis.Client
	Logger      *pkg.Logger
	Config      models.RuncConfig
}

type Service struct {
	reliance     ServiceReliance
	sdkClients   map[string]*sdk.SDK
	mu           sync.RWMutex
	tokenStore   models.TokenStore
	sessionStore models.SessionStore
}

func NewService(reliance ServiceReliance) *Service {
	svc := &Service{
		reliance:   reliance,
		sdkClients: make(map[string]*sdk.SDK),
	}

	switch reliance.Config.TokenStoreType {
	case consts.TOKEN_STORE_REDIS:
		if reliance.RedisClient != nil {
			svc.tokenStore = NewRedisTokenStore(reliance.RedisClient)
			svc.reliance.Logger.Debug("TokenStore: redis")
		} else {
			svc.tokenStore = NewMemoryTokenStore()
			svc.reliance.Logger.Warn("TokenStore: redis configured but no client, fallback to memory")
		}
	default:
		svc.tokenStore = NewMemoryTokenStore()
		svc.reliance.Logger.Debug("TokenStore: memory")
	}

	switch reliance.Config.SessionStoreType {
	case consts.SESSION_STORE_REDIS:
		if reliance.RedisClient != nil {
			svc.sessionStore = NewRedisSessionStore(reliance.RedisClient)
			svc.reliance.Logger.Debug("SessionStore: redis")
		} else {
			svc.sessionStore = NewFileSessionStore(reliance.Config.TaskDir)
			svc.reliance.Logger.Warn("SessionStore: redis configured but no client, fallback to file")
		}
	default:
		svc.sessionStore = NewFileSessionStore(reliance.Config.TaskDir)
		svc.reliance.Logger.Debug("SessionStore: file")
	}

	return svc
}

func (s *Service) GetTokenStore() models.TokenStore {
	return s.tokenStore
}

func (s *Service) GetSessionStore() models.SessionStore {
	return s.sessionStore
}

func (s *Service) GetRedisClient() *redis.Client {
	return s.reliance.RedisClient
}
