package models

import "time"

type UserMetaInfo struct {
	Username  string
	UnifyId   string
	StudentNo string
	Sex       string
	Grade     string
	DeptName  string
}

type UserNode struct {
	OpenID   string
	Token    string
	MetaInfo UserMetaInfo
}

type LocationPointNode struct {
	Index     int64
	Longitude float64
	Latitude  float64
	FieldID   string
	Timestamp string
}

type TaskSettingNode struct {
	TargetPace     float64
	TargetInterval float64
	TargetFieldID  string
}

type TaskNode struct {
	TargetUser     UserNode
	TaskSettings   TaskSettingNode
	LocationPoints []LocationPointNode
	StartTime      time.Time
}
