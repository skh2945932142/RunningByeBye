package runc

import (
	"github.com/redis/go-redis/v9"

	rds "RunningByeBye/inferstructure/redis"
	"RunningByeBye/internal/runc/consts"
	"RunningByeBye/internal/runc/handler"
	"RunningByeBye/internal/runc/models"
	"RunningByeBye/internal/runc/pkg"
	"RunningByeBye/internal/runc/scheduler"
	"RunningByeBye/internal/runc/service"
)

type Option func(info *BasicInfo)

type BasicInfo struct {
	RedisAddr     string
	RedisPassword string
	RedisDB       int
	TokenStore    string
	SessionStore  string
	PointsDir     string
	TaskDir       string
	CacheDir      string
	UserAgent     string
	PhoneType     string
	LogLevel      pkg.LogLevel
	LogFile       string
}

func WithRedis(addr string, password string, db int) Option {
	return func(info *BasicInfo) {
		info.RedisAddr = addr
		info.RedisPassword = password
		info.RedisDB = db
	}
}

func WithTokenStore(storeType string) Option {
	return func(info *BasicInfo) {
		info.TokenStore = storeType
	}
}

func WithSessionStore(storeType string) Option {
	return func(info *BasicInfo) {
		info.SessionStore = storeType
	}
}

func WithPointsDir(dir string) Option {
	return func(info *BasicInfo) {
		info.PointsDir = dir
	}
}

func WithTaskDir(dir string) Option {
	return func(info *BasicInfo) {
		info.TaskDir = dir
	}
}

func WithCacheDir(dir string) Option {
	return func(info *BasicInfo) {
		info.CacheDir = dir
	}
}

func WithUserAgent(ua string) Option {
	return func(info *BasicInfo) {
		info.UserAgent = ua
	}
}

func WithPhoneType(pt string) Option {
	return func(info *BasicInfo) {
		info.PhoneType = pt
	}
}

func WithLogLevel(level pkg.LogLevel) Option {
	return func(info *BasicInfo) {
		info.LogLevel = level
	}
}

func WithLogFile(filePath string) Option {
	return func(info *BasicInfo) {
		info.LogFile = filePath
	}
}

type Runc struct {
	Handler   *handler.Handler
	Scheduler *scheduler.Scheduler
	Service   *service.Service
	Logger    *pkg.Logger
	redisCli  *redis.Client
}

func New(opts ...Option) (*Runc, error) {
	basicInfo := &BasicInfo{
		RedisAddr:    "localhost:6379",
		RedisPassword: "",
		RedisDB:      0,
		TokenStore:   consts.TOKEN_STORE_MEMORY,
		SessionStore: consts.SESSION_STORE_FILE,
		PointsDir:    consts.POINTS_DIR,
		TaskDir:      consts.SESSION_TASK_DIR,
		CacheDir:     consts.SESSION_CACHE_DIR,
		UserAgent:    consts.DEFAULT_USER_AGENT,
		PhoneType:    consts.DEFAULT_PHONE_TYPE,
		LogLevel:     pkg.LogLevelInfo,
		LogFile:      "",
	}

	for _, opt := range opts {
		opt(basicInfo)
	}

	logger := pkg.NewLogger(basicInfo.LogLevel)
	if basicInfo.LogFile != "" {
		logger = pkg.NewLoggerWithFile(basicInfo.LogLevel, basicInfo.LogFile)
	}
	logger.SetModular("runc")

	var redisClient *redis.Client
	if basicInfo.TokenStore == consts.TOKEN_STORE_REDIS || basicInfo.SessionStore == consts.SESSION_STORE_REDIS {
		var err error
		redisClient, err = rds.NewRedisClient(
			rds.WithAddr(basicInfo.RedisAddr),
			rds.WithPassword(basicInfo.RedisPassword),
			rds.WithDB(basicInfo.RedisDB),
		)
		if err != nil {
			logger.Warn("Redis connection failed, falling back to memory/file: %v", err)
			redisClient = nil
		}
	}

	config := models.RuncConfig{
		TokenStoreType:   basicInfo.TokenStore,
		SessionStoreType: basicInfo.SessionStore,
		PointsDir:        basicInfo.PointsDir,
		TaskDir:          basicInfo.TaskDir,
		CacheDir:         basicInfo.CacheDir,
		UserAgent:        basicInfo.UserAgent,
		PhoneType:        basicInfo.PhoneType,
		CookiePath:       consts.DEFAULT_COOKIE_PATH,
	}

	svcReliance := service.ServiceReliance{
		RedisClient: redisClient,
		Logger:      logger,
		Config:      config,
	}
	svc := service.NewService(svcReliance)

	schReliance := scheduler.SchedulerReliance{
		Service: svc,
		Logger:  logger,
	}
	sch := scheduler.NewScheduler(schReliance)

	hReliance := handler.HandlerReliance{
		Service:   svc,
		Scheduler: sch,
		Logger:    logger,
		PointsDir: basicInfo.PointsDir,
	}
	h := handler.NewHandler(hReliance)

	logger.Info("Runc module initialized")

	return &Runc{
		Handler:   h,
		Scheduler: sch,
		Service:   svc,
		Logger:    logger,
		redisCli:  redisClient,
	}, nil
}

func (r *Runc) Close() {
	r.Logger.Info("Runc module shutting down")
	r.Scheduler.Stop()
	r.Service.Close()
	r.Logger.Close()
}
