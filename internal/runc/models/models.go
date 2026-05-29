package models

import (
	"time"

	sdk "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK"
)

type TaskState int

const (
	TaskStatePending TaskState = iota
	TaskStateRunning
	TaskStatePaused
	TaskStateCompleted
	TaskStateFailed
)

type UserMetaInfo struct {
	Username  string `json:"username"`
	UnifyId   string `json:"unify_id"`
	StudentNo string `json:"student_no"`
	Sex       string `json:"sex"`
	Grade     string `json:"grade"`
	DeptName  string `json:"dept_name"`
}

type UserNode struct {
	OpenID   string       `json:"open_id"`
	Token    string       `json:"token"`
	MetaInfo UserMetaInfo `json:"meta_info"`
}

type SDKClients struct {
	SDK *sdk.SDK
}

type LocationPointNode struct {
	Longitude float64 `json:"longitude"`
	Latitude  float64 `json:"latitude"`
	FieldID   string  `json:"field_id"`
	Timestamp string  `json:"timestamp"`
}

type TaskSettingNode struct {
	TargetPace     float64 `json:"target_pace"`
	TargetInterval float64 `json:"target_interval"`
	TargetFieldID  string  `json:"target_field_id"`
}

type TaskNode struct {
	TargetUser     UserNode            `json:"target_user"`
	TaskSettings   TaskSettingNode     `json:"task_settings"`
	LocationPoints []LocationPointNode `json:"location_points"`
	RecordNo       string              `json:"record_no"`
	SubmittedCount int                 `json:"submitted_count"`
	StartTime      time.Time           `json:"start_time"`
	IsPaused       bool                `json:"is_paused"`
	Mileage        float64             `json:"mileage"`
	State          TaskState           `json:"state"`
}

type TokenCache struct {
	Token    string `json:"token"`
	OpenID   string `json:"open_id"`
	SaveTime string `json:"save_time"`
}

type TokenStore interface {
	Get(openID string) (string, error)
	Set(openID string, token string) error
	Delete(openID string) error
}

type SessionStore interface {
	Save(openID string, task *TaskNode) error
	Load(openID string) (*TaskNode, error)
	Delete(openID string) error
	List() ([]string, error)
}

type RuncConfig struct {
	TokenStoreType   string `json:"token_store_type"`
	SessionStoreType string `json:"session_store_type"`
	PointsDir        string `json:"points_dir"`
	TaskDir          string `json:"task_dir"`
	CacheDir         string `json:"cache_dir"`
	UserAgent        string `json:"user_agent"`
	PhoneType        string `json:"phone_type"`
	CookiePath       string `json:"cookie_path"`
}

type ProgressEvent struct {
	OpenID         string  `json:"open_id"`
	RecordNo       string  `json:"record_no"`
	SubmittedCount int     `json:"submitted_count"`
	TotalPoints    int     `json:"total_points"`
	Mileage        float64 `json:"mileage"`
	State          string  `json:"state"`
}
