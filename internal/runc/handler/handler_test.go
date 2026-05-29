package handler

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
	"time"

	"RunningByeBye/internal/runc/models"
	"RunningByeBye/internal/runc/pkg"
	"RunningByeBye/internal/runc/scheduler"
	"RunningByeBye/internal/runc/service"
)

func newTestHandler(t *testing.T) (*Handler, *service.Service, string) {
	t.Helper()
	dir := t.TempDir()
	pointsDir := filepath.Join(dir, "points")
	taskDir := filepath.Join(dir, "tasks")
	cacheDir := filepath.Join(dir, "cache")
	logger := pkg.NewLogger(pkg.LogLevelError)

	os.MkdirAll(pointsDir, 0755)
	os.MkdirAll(taskDir, 0755)
	os.MkdirAll(cacheDir, 0755)

	createPointsFile(t, pointsDir, "T1001", "T1001_风华运动场_1.json", []pointData{
		{Longitude: "106.60808268229167", Latitude: "29.533206922743055"},
		{Longitude: "106.60900000000000", Latitude: "29.534000000000000"},
		{Longitude: "106.61000000000000", Latitude: "29.535000000000000"},
		{Longitude: "106.61100000000000", Latitude: "29.536000000000000"},
		{Longitude: "106.61200000000000", Latitude: "29.537000000000000"},
		{Longitude: "106.61300000000000", Latitude: "29.538000000000000"},
	})

	svc := service.NewService(service.ServiceReliance{
		RedisClient: nil,
		Logger:      logger,
		Config: models.RuncConfig{
			TokenStoreType:   "memory",
			SessionStoreType: "file",
			TaskDir:          taskDir,
			CacheDir:         cacheDir,
			PointsDir:        pointsDir,
		},
	})

	sch := scheduler.NewScheduler(scheduler.SchedulerReliance{
		Service: svc,
		Logger:  logger,
	})

	h := NewHandler(HandlerReliance{
		Service:   svc,
		Scheduler: sch,
		Logger:    logger,
		PointsDir: pointsDir,
	})

	return h, svc, taskDir
}

type pointData struct {
	Longitude string `json:"longitude"`
	Latitude  string `json:"latitude"`
}

func createPointsFile(t *testing.T, baseDir, fieldCode, fileName string, points []pointData) {
	t.Helper()
	fieldDir := filepath.Join(baseDir, fieldCode)
	err := os.MkdirAll(fieldDir, 0755)
	if err != nil {
		t.Fatalf("failed to create points dir %s: %v", fieldDir, err)
	}

	type rawData struct {
		Data []pointData `json:"data"`
	}

	data := rawData{Data: points}
	raw, err := json.MarshalIndent(data, "", "  ")
	if err != nil {
		t.Fatalf("failed to marshal points: %v", err)
	}

	path := filepath.Join(fieldDir, fileName)
	err = os.WriteFile(path, raw, 0644)
	if err != nil {
		t.Fatalf("failed to write points file %s: %v", path, err)
	}
}

func TestHandler_LoadBasePoints(t *testing.T) {
	h, _, _ := newTestHandler(t)

	points, err := loadBasePoints(h.reliance.PointsDir, "T1001")
	if err != nil {
		t.Fatalf("loadBasePoints failed: %v", err)
	}
	if len(points) == 0 {
		t.Fatal("loadBasePoints returned empty slice")
	}
	if points[0].Longitude == 0 || points[0].Latitude == 0 {
		t.Errorf("invalid point data: %+v", points[0])
	}
}

func TestHandler_LoadBasePoints_NoFile(t *testing.T) {
	h, _, _ := newTestHandler(t)

	_, err := loadBasePoints(h.reliance.PointsDir, "T9999")
	if err == nil {
		t.Error("expected error for non-existent field code")
	}
}

func TestHandler_LoginChannel(t *testing.T) {
	h, svc, _ := newTestHandler(t)
	openID := "test_login_user"
	svc.GetTokenStore().Set(openID, "existing_token")

	ch := h.Login(openID)
	if ch == nil {
		t.Fatal("Login returned nil channel")
	}
}

func TestHandler_GetUserInfoChannel(t *testing.T) {
	h, svc, _ := newTestHandler(t)
	openID := "test_info_user"
	svc.GetTokenStore().Set(openID, "token")

	ch := h.GetUserInfo(openID)
	if ch == nil {
		t.Fatal("GetUserInfo returned nil channel")
	}
}

func TestHandler_GetRecordsChannel(t *testing.T) {
	h, svc, _ := newTestHandler(t)
	openID := "test_records_user"
	svc.GetTokenStore().Set(openID, "token")

	ch := h.GetRecords(openID, "20252")
	if ch == nil {
		t.Fatal("GetRecords returned nil channel")
	}
}

func TestHandler_StartRunFlow(t *testing.T) {
	h, svc, _ := newTestHandler(t)
	openID := "runner1"

	svc.GetTokenStore().Set(openID, "pre_stored_token")

	startCh := h.StartRun(openID, "T1001", 6.0, 0)
	result := <-startCh
	if result.Err != nil {
		t.Logf("StartRun returned expected error (no real SDK): %v", result.Err)
	} else {
		t.Log("StartRun succeeded (unexpected without real SDK)")
	}
}

func TestHandler_StartRunWithName(t *testing.T) {
	h, svc, _ := newTestHandler(t)
	openID := ""

	svc.GetTokenStore().Set(openID, "")

	startCh := h.StartRun(openID, "风华运动场", 6, 1)
	result := <-startCh
	if result.Err != nil {
		t.Logf("StartRun with Chinese name returned expected error: %v", result.Err)
	}
}

func TestHandler_PauseResumeStop(t *testing.T) {
	h, svc, _ := newTestHandler(t)
	openID := "flow_user"

	svc.GetTokenStore().Set(openID, "token")

	h.StartRun(openID, "T1001", 0, 0)

	time.Sleep(50 * time.Millisecond)

	h.PauseRun(openID)
	task := h.GetProgress(openID)
	if task != nil && !task.IsPaused {
		t.Error("expected task to be paused after PauseRun")
	}

	h.ResumeRun(openID)
	task = h.GetProgress(openID)
	if task != nil && task.IsPaused {
		t.Error("expected task to be resumed after ResumeRun")
	}

	h.StopRun(openID)
	task = h.GetProgress(openID)
	if task != nil && task.State != models.TaskStateCompleted {
		t.Logf("task state after StopRun: %d", task.State)
	}
}

func TestHandler_GetProgress_NoTask(t *testing.T) {
	h, _, _ := newTestHandler(t)
	task := h.GetProgress("nonexistent_user")
	if task != nil {
		t.Errorf("expected nil for user without task, got %+v", task)
	}
}

func TestHandler_ProgressChannel(t *testing.T) {
	h, _, _ := newTestHandler(t)
	ch := h.ProgressChannel()
	if ch == nil {
		t.Fatal("ProgressChannel returned nil")
	}
}

func TestHandler_RecoverTasks_Empty(t *testing.T) {
	h, _, _ := newTestHandler(t)
	err := h.RecoverTasks()
	if err != nil {
		t.Fatalf("RecoverTasks on empty store failed: %v", err)
	}
}

func TestHandler_FullRunEventFlow(t *testing.T) {
	h, svc, _ := newTestHandler(t)
	openID := "event_user"

	svc.GetTokenStore().Set(openID, "test_token")

	progressCh := h.ProgressChannel()

	startResult := <-h.StartRun(openID, "T1001", 6.0, 0)
	if startResult.Err != nil {
		t.Logf("StartRun returned (expected without real SDK): %v", startResult.Err)
	} else {
		t.Log("StartRun pipeline succeeded (SDK call will fail in background)")
	}

	task := h.GetProgress(openID)
	if task != nil {
		t.Logf("Task exists after StartRun: state=%d, points=%d", task.State, len(task.LocationPoints))
		if len(task.LocationPoints) != 6 {
			t.Errorf("expected 6 points, got %d", len(task.LocationPoints))
		}
		h.PauseRun(openID)
		h.ResumeRun(openID)
		h.StopRun(openID)
	} else {
		t.Log("Task was cleaned up immediately (expected if SDK Login failed)")
	}

	select {
	case evt := <-progressCh:
		t.Logf("Received progress event: state=%s, openID=%s", evt.State, evt.OpenID)
	case <-time.After(500 * time.Millisecond):
		t.Log("No immediate event (SDK call pending or task already stopped)")
	}
}

func TestHandler_SessionPersistence(t *testing.T) {
	_, svc, taskDir := newTestHandler(t)
	openID := "persist_user"

	svc.GetTokenStore().Set(openID, "token")

	points := pkg.GeneratePoints(
		[]models.LocationPointNode{
			{Latitude: 29.53286, Longitude: 106.60525, FieldID: "T1001"},
			{Latitude: 29.53320, Longitude: 106.60550, FieldID: "T1001"},
		},
		models.TaskSettingNode{TargetPace: 6.0, TargetFieldID: "T1001", TargetInterval: 1.0},
		time.Now(),
	)

	task := &models.TaskNode{
		TargetUser:     models.UserNode{OpenID: openID, Token: "token"},
		TaskSettings:   models.TaskSettingNode{TargetFieldID: "T1001"},
		LocationPoints: points,
		SubmittedCount: 0,
		StartTime:      time.Now(),
	}
	err := svc.GetSessionStore().Save(openID, task)
	if err != nil {
		t.Fatalf("SessionStore.Save failed: %v", err)
	}

	taskFilePath := filepath.Join(taskDir, openID+"task.json")
	if !pkg.FileExists(taskFilePath) {
		t.Fatalf("task file not found at %s", taskFilePath)
	}

	loadedTask, err := svc.GetSessionStore().Load(openID)
	if err != nil {
		t.Fatalf("SessionStore.Load failed: %v", err)
	}
	if loadedTask.TargetUser.OpenID != openID {
		t.Errorf("loaded OpenID = %s, want %s", loadedTask.TargetUser.OpenID, openID)
	}

	loadedMileage := pkg.CalcMileage(loadedTask.LocationPoints)
	if loadedMileage <= 0 {
		t.Errorf("expected mileage > 0, got %f", loadedMileage)
	}
}
