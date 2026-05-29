package consts

const (
	SDK_STATUS_OK = "10200"

	FIELD_FENG_HUA  = "T1001"
	FIELD_TAI_JI    = "T1005"
	FIELD_NING_JING = "T1014"

	SPORTS_FIELD_FENG_HUA  = "风华运动场"
	SPORTS_FIELD_TAI_JI    = "太极运动场"
	SPORTS_FIELD_NING_JING = "宁静苑"

	DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 15; PJF110 Build/UKQ1.231108.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.103 Mobile Safari/537.36 XWEB/1300409 MMWEBSDK/20240404 MMWEBID/1532 MicroMessenger/8.0.49.2600(0x28003133) WeChat/arm64 Weixin NetType/5G Language/zh_CN ABI/arm64 MiniProgramEnv/android"
	DEFAULT_PHONE_TYPE  = "PJF110_Android 15"
	DEFAULT_COOKIE_PATH = "./CookieJar.json"

	TOKEN_TTL = 3600

	DEFAULT_BATCH_SIZE       = 8
	DEFAULT_TICKER_INTERVAL  = 20
	DEFAULT_PACE_MIN         = 5.0
	DEFAULT_PACE_MAX         = 7.0
	DEFAULT_INTERVAL_MIN     = 0.8
	DEFAULT_INTERVAL_MAX_JIT = 0.4
	DEFAULT_JITTER           = 0.00001

	SESSION_STORE_FILE  = "file"
	SESSION_STORE_REDIS = "redis"
	TOKEN_STORE_MEMORY  = "memory"
	TOKEN_STORE_REDIS   = "redis"

	SESSION_TASK_DIR  = "tasks"
	SESSION_CACHE_DIR = "cache"
	POINTS_DIR        = "points"

	KEY_PREFIX_TOKEN   = "runc:token:"
	KEY_PREFIX_SESSION = "runc:session:"
	KEY_PREFIX_TASK    = "runc:task:"
)
