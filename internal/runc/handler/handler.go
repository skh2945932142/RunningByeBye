package handler

import (
	"fmt"
	"time"

	"RunningByeBye/internal/runc/models"
	"RunningByeBye/internal/runc/pkg"

	sdkModels "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK/models"
)

type Handler struct {
	reliance HandlerReliance
}

func NewHandler(reliance HandlerReliance) *Handler {
	return &Handler{reliance: reliance}
}

type LoginResult struct {
	User *models.UserNode
	Err  error
}

func (h *Handler) Login(openID string) <-chan LoginResult {
	ch := make(chan LoginResult, 1)
	go func() {
		user, err := h.reliance.Service.Login(openID)
		ch <- LoginResult{User: user, Err: err}
	}()
	return ch
}

type UserInfoResult struct {
	Info *sdkModels.UserInfo
	Err  error
}

func (h *Handler) GetUserInfo(openID string) <-chan UserInfoResult {
	ch := make(chan UserInfoResult, 1)
	go func() {
		info, err := h.reliance.Service.GetUserInfo(openID)
		ch <- UserInfoResult{Info: info, Err: err}
	}()
	return ch
}

type RecordsResult struct {
	Results *sdkModels.AllSportsResults
	Err     error
}

func (h *Handler) GetRecords(openID string, term string) <-chan RecordsResult {
	ch := make(chan RecordsResult, 1)
	go func() {
		results, err := h.reliance.Service.GetAllSportsResult(openID, term)
		ch <- RecordsResult{Results: results, Err: err}
	}()
	return ch
}

type StartRunResult struct {
	RecordNo string
	Err      error
}

func (h *Handler) StartRun(openID string, fieldID string, pace float64, interval float64) <-chan StartRunResult {
	ch := make(chan StartRunResult, 1)
	go func() {
		user, err := h.reliance.Service.Login(openID)
		if err != nil {
			ch <- StartRunResult{Err: err}
			return
		}

		fieldCode := pkg.FieldNameToCode(fieldID)
		if fieldCode == "" {
			fieldCode = fieldID
		}

		basePoints, err := loadBasePoints(h.reliance.PointsDir, fieldCode)
		if err != nil {
			ch <- StartRunResult{Err: err}
			return
		}

		settings := models.TaskSettingNode{
			TargetPace:     pace,
			TargetInterval: interval,
			TargetFieldID:  fieldCode,
		}

		generated := pkg.GeneratePoints(basePoints, settings, time.Time{})

		task := &models.TaskNode{
			TargetUser:     *user,
			TaskSettings:   settings,
			LocationPoints: generated,
		}

		if err := h.reliance.Scheduler.AddTask(task); err != nil {
			ch <- StartRunResult{Err: err}
			return
		}

		ch <- StartRunResult{}
	}()
	return ch
}

func (h *Handler) PauseRun(openID string) {
	h.reliance.Scheduler.PauseTask(openID)
}

func (h *Handler) ResumeRun(openID string) {
	h.reliance.Scheduler.ResumeTask(openID)
}

func (h *Handler) StopRun(openID string) {
	h.reliance.Scheduler.RemoveTask(openID)
}

func (h *Handler) GetProgress(openID string) *models.TaskNode {
	return h.reliance.Scheduler.GetTask(openID)
}

func (h *Handler) ProgressChannel() <-chan models.ProgressEvent {
	return h.reliance.Scheduler.EventChannel()
}

func (h *Handler) RecoverTasks() error {
	return h.reliance.Scheduler.RecoverTasks()
}

func loadBasePoints(pointsDir string, fieldCode string) ([]models.LocationPointNode, error) {
	fieldDir := pointsDir + "/" + fieldCode
	files, err := pkg.ReadDir(fieldDir)
	if err != nil {
		return nil, err
	}
	if len(files) == 0 {
		return nil, fmt.Errorf("no points files found for field %s", fieldCode)
	}

	chosen := pkg.PickRandomPointsFile(fieldDir, fieldCode, files)
	if chosen == "" {
		return nil, fmt.Errorf("no matching points file for field %s", fieldCode)
	}

	filePath := fieldDir + "/" + chosen
	rawBytes, err := pkg.LoadRawFile(filePath)
	if err != nil {
		return nil, err
	}

	return pkg.ReadPointsFromRaw(rawBytes)
}
