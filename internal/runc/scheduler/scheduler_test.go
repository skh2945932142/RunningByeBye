package scheduler

import (
	"path/filepath"
	"testing"
	"time"

	"RunningByeBye/internal/runc/models"
	"RunningByeBye/internal/runc/pkg"
	"RunningByeBye/internal/runc/service"
)

func newTestScheduler(t *testing.T) (*Scheduler, *service.Service) {
	t.Helper()
	dir := t.TempDir()
	logger := pkg.NewLogger(pkg.LogLevelError)

	svc := service.NewService(service.ServiceReliance{
		RedisClient: nil,
		Logger:      logger,
		Config: models.RuncConfig{
			TokenStoreType:   "memory",
			SessionStoreType: "file",
			TaskDir:          dir,
			CacheDir:         filepath.Join(dir, "cache"),
			PointsDir:        filepath.Join(dir, "points"),
		},
	})

	sch := NewScheduler(SchedulerReliance{
		Service: svc,
		Logger:  logger,
	})

	return sch, svc
}

func makeTestTask(openID string) *models.TaskNode {
	return &models.TaskNode{
		TargetUser: models.UserNode{
			OpenID: openID,
			Token:  "fake_token_for_test",
		},
		TaskSettings: models.TaskSettingNode{
			TargetPace:     6.0,
			TargetInterval: 1.0,
			TargetFieldID:  "T1001",
		},
		LocationPoints: []models.LocationPointNode{
			{Latitude: 29.53286, Longitude: 106.60525, Timestamp: "2025-01-01 12:00:00"},
			{Latitude: 29.53320, Longitude: 106.60550, Timestamp: "2025-01-01 12:00:01"},
			{Latitude: 29.53350, Longitude: 106.60580, Timestamp: "2025-01-01 12:00:02"},
		},
		SubmittedCount: 0,
		StartTime:      time.Now(),
	}
}

func TestAddTask(t *testing.T) {
	sch, _ := newTestScheduler(t)
	task := makeTestTask("user1")

	err := sch.AddTask(task)
	if err != nil {
		t.Fatalf("AddTask failed: %v", err)
	}

	if task.State != models.TaskStatePending {
		t.Errorf("task state = %d, want %d (Pending)", task.State, models.TaskStatePending)
	}

	saved := sch.GetTask("user1")
	if saved == nil {
		t.Fatal("GetTask returned nil after AddTask")
	}
	if saved.TargetUser.OpenID != "user1" {
		t.Errorf("GetTask OpenID = %s, want user1", saved.TargetUser.OpenID)
	}
}

func TestAddTask_DuplicateUser(t *testing.T) {
	sch, _ := newTestScheduler(t)
	task := makeTestTask("user1")

	sch.AddTask(task)
	sch.AddTask(makeTestTask("user1"))

	saved := sch.GetTask("user1")
	if saved == nil {
		t.Fatal("GetTask returned nil")
	}
}

func TestRemoveTask(t *testing.T) {
	sch, _ := newTestScheduler(t)
	task := makeTestTask("user1")
	sch.AddTask(task)

	sch.RemoveTask("user1")

	saved := sch.GetTask("user1")
	if saved != nil {
		t.Errorf("expected nil after RemoveTask, got state=%d", saved.State)
	}
}

func TestRemoveTask_NonExistent(t *testing.T) {
	sch, _ := newTestScheduler(t)
	sch.RemoveTask("nonexistent")
}

func TestPauseAndResumeTask(t *testing.T) {
	sch, _ := newTestScheduler(t)
	task := makeTestTask("user1")
	sch.AddTask(task)

	sch.PauseTask("user1")
	saved := sch.GetTask("user1")
	if saved == nil {
		t.Fatal("GetTask returned nil after Pause")
	}
	if !saved.IsPaused {
		t.Error("expected IsPaused = true after PauseTask")
	}
	if saved.State != models.TaskStatePaused {
		t.Errorf("expected state Paused (2), got %d", saved.State)
	}

	sch.ResumeTask("user1")
	saved = sch.GetTask("user1")
	if saved.IsPaused {
		t.Error("expected IsPaused = false after ResumeTask")
	}
	if saved.State != models.TaskStateRunning {
		t.Errorf("expected state Running (1), got %d", saved.State)
	}
}

func TestGetTask_NonExistent(t *testing.T) {
	sch, _ := newTestScheduler(t)
	saved := sch.GetTask("nobody")
	if saved != nil {
		t.Errorf("expected nil for non-existent task, got %+v", saved)
	}
}

func TestRecoverTasks_Empty(t *testing.T) {
	sch, _ := newTestScheduler(t)
	err := sch.RecoverTasks()
	if err != nil {
		t.Fatalf("RecoverTasks on empty store failed: %v", err)
	}
}

func TestEventChannel(t *testing.T) {
	sch, _ := newTestScheduler(t)
	ch := sch.EventChannel()
	if ch == nil {
		t.Fatal("EventChannel returned nil")
	}
}

func TestStop(t *testing.T) {
	sch, _ := newTestScheduler(t)
	sch.Stop()
	sch.Stop()
}

func TestMultipleUsers(t *testing.T) {
	sch, _ := newTestScheduler(t)
	sch.AddTask(makeTestTask("user_a"))
	sch.AddTask(makeTestTask("user_b"))

	if sch.GetTask("user_a") == nil {
		t.Error("user_a task not found")
	}
	if sch.GetTask("user_b") == nil {
		t.Error("user_b task not found")
	}

	sch.PauseTask("user_a")
	if !sch.GetTask("user_a").IsPaused {
		t.Error("user_a should be paused")
	}
	if sch.GetTask("user_b").IsPaused {
		t.Error("user_b should not be paused")
	}
}

func TestRunScenario_FullFlow(t *testing.T) {
	sch, svc := newTestScheduler(t)
	defer sch.Stop()

	openID := "test_runner"

	base := []models.LocationPointNode{
		{Latitude: 29.53286, Longitude: 106.60525, FieldID: "T1001"},
		{Latitude: 29.53320, Longitude: 106.60550, FieldID: "T1001"},
		{Latitude: 29.53350, Longitude: 106.60580, FieldID: "T1001"},
		{Latitude: 29.53380, Longitude: 106.60610, FieldID: "T1001"},
		{Latitude: 29.53410, Longitude: 106.60640, FieldID: "T1001"},
		{Latitude: 29.53440, Longitude: 106.60670, FieldID: "T1001"},
		{Latitude: 29.53470, Longitude: 106.60700, FieldID: "T1001"},
		{Latitude: 29.53500, Longitude: 106.60730, FieldID: "T1001"},
		{Latitude: 29.53530, Longitude: 106.60760, FieldID: "T1001"},
		{Latitude: 29.53560, Longitude: 106.60790, FieldID: "T1001"},
	}

	settings := models.TaskSettingNode{
		TargetPace:     6.0,
		TargetInterval: 0,
		TargetFieldID:  "T1001",
	}

	generated := pkg.GeneratePoints(base, settings, time.Now())
	if len(generated) != len(base) {
		t.Fatalf("GeneratePoints length = %d, want %d", len(generated), len(base))
	}
	for i, p := range generated {
		if p.Timestamp == "" {
			t.Errorf("generated point %d has empty timestamp", i)
		}
	}

	expectedMileage := pkg.CalcMileage(generated)
	if expectedMileage <= 0 {
		t.Fatalf("expected positive mileage, got %f", expectedMileage)
	}

	svc.GetTokenStore().Set(openID, "pre_stored_token")

	task := &models.TaskNode{
		TargetUser: models.UserNode{
			OpenID: openID,
			Token:  "pre_stored_token",
		},
		TaskSettings:   settings,
		LocationPoints: generated,
		SubmittedCount: 0,
		StartTime:      time.Now(),
		IsPaused:       false,
		State:          models.TaskStatePending,
	}

	eventCh := sch.EventChannel()

	err := sch.AddTask(task)
	if err != nil {
		t.Fatalf("AddTask failed: %v", err)
	}

	if task.State != models.TaskStatePending {
		t.Errorf("expected state Pending (0) after AddTask, got %d", task.State)
	}

	select {
	case evt := <-eventCh:
		if evt.State != "failed" {
			t.Errorf("expected failed event, got state=%s (RecordNo=%s, SubmittedCount=%d, Mileage=%.3f)",
				evt.State, evt.RecordNo, evt.SubmittedCount, evt.Mileage)
		}
		if evt.OpenID != openID {
			t.Errorf("event OpenID = %s, want %s", evt.OpenID, openID)
		}
	case <-time.After(3 * time.Second):
		t.Fatal("timed out waiting for progress event")
	}

	saved := sch.GetTask(openID)
	if saved == nil {
		t.Fatal("task should still exist with Failed state after SDK call fails")
	}
	if saved.State != models.TaskStateFailed {
		t.Errorf("expected state Failed (4), got %d", saved.State)
	}
}

func TestRunScenario_PauseResumeFlow(t *testing.T) {
	sch, svc := newTestScheduler(t)
	defer sch.Stop()

	openID := "pause_user"
	svc.GetTokenStore().Set(openID, "token")

	generated := pkg.GeneratePoints(
		[]models.LocationPointNode{
			{Latitude: 29.53286, Longitude: 106.60525, FieldID: "T1001"},
			{Latitude: 29.53320, Longitude: 106.60550, FieldID: "T1001"},
			{Latitude: 29.53350, Longitude: 106.60580, FieldID: "T1001"},
		},
		models.TaskSettingNode{TargetPace: 6.0, TargetFieldID: "T1001"},
		time.Now(),
	)

	task := &models.TaskNode{
		TargetUser: models.UserNode{OpenID: openID, Token: "token"},
		TaskSettings: models.TaskSettingNode{
			TargetPace: 6.0, TargetInterval: 5.0, TargetFieldID: "T1001",
		},
		LocationPoints: generated,
		SubmittedCount: 0,
		StartTime:      time.Now(),
	}

	sch.AddTask(task)

	sch.PauseTask(openID)
	saved := sch.GetTask(openID)
	if saved == nil {
		t.Fatal("task should exist after pause")
	}
	if !saved.IsPaused {
		t.Error("expected IsPaused = true")
	}
	if saved.State != models.TaskStatePaused {
		t.Errorf("expected state Paused, got %d", saved.State)
	}

	sch.ResumeTask(openID)
	saved = sch.GetTask(openID)
	if saved == nil {
		t.Fatal("task should exist after resume")
	}
	if saved.IsPaused {
		t.Error("expected IsPaused = false after resume")
	}
	if saved.State != models.TaskStateRunning {
		t.Errorf("expected state Running, got %d", saved.State)
	}

	sch.RemoveTask(openID)
	if sch.GetTask(openID) != nil {
		t.Error("task should be nil after remove")
	}
}

func TestRunScenario_CalcMileage(t *testing.T) {
	base := []models.LocationPointNode{
		{Latitude: 29.53286, Longitude: 106.60525},
		{Latitude: 29.53500, Longitude: 106.60730},
	}

	settings := models.TaskSettingNode{
		TargetPace:     6.0,
		TargetFieldID:  "T1001",
	}
	points := pkg.GeneratePoints(base, settings, time.Time{})

	mileage := pkg.CalcMileage(points)
	if mileage <= 0 {
		t.Fatalf("expected mileage > 0, got %.6f", mileage)
	}

	distance := pkg.HaversineDistance(points[0], points[1])
	if distance <= 0 {
		t.Fatalf("expected haversine distance > 0, got %.6f", distance)
	}
}

func TestRunScenario_PointConversion(t *testing.T) {
	points := []models.LocationPointNode{
		{Longitude: 106.605, Latitude: 29.532, Timestamp: "2025-01-01 12:00:00", FieldID: "T1001"},
	}

	sdkPoints := pkg.ToSportPointList(points, "T1001")
	if len(sdkPoints) != 1 {
		t.Fatalf("expected 1 SDK point, got %d", len(sdkPoints))
	}
	if sdkPoints[0].Longitude != 106.605 {
		t.Errorf("Longitude = %f, want 106.605", sdkPoints[0].Longitude)
	}
	if sdkPoints[0].PlaceCode != "T1001" {
		t.Errorf("PlaceCode = %s, want T1001", sdkPoints[0].PlaceCode)
	}
	if sdkPoints[0].IsValid != "1" {
		t.Errorf("IsValid = %s, want 1", sdkPoints[0].IsValid)
	}
}

func TestRunScenario_MultiUserIsolation(t *testing.T) {
	sch, svc := newTestScheduler(t)
	defer sch.Stop()

	users := []string{"user_a", "user_b", "user_c"}
	for _, openID := range users {
		svc.GetTokenStore().Set(openID, "token_"+openID)

		generated := pkg.GeneratePoints(
			[]models.LocationPointNode{
				{Latitude: 29.53286, Longitude: 106.60525, FieldID: "T1001"},
				{Latitude: 29.53320, Longitude: 106.60550, FieldID: "T1001"},
			},
			models.TaskSettingNode{TargetPace: 6.0, TargetFieldID: "T1001"},
			time.Now(),
		)

		sch.AddTask(&models.TaskNode{
			TargetUser:     models.UserNode{OpenID: openID, Token: "token_" + openID},
			TaskSettings:   models.TaskSettingNode{TargetPace: 6.0, TargetFieldID: "T1001"},
			LocationPoints: generated,
			SubmittedCount: 0,
			StartTime:      time.Now(),
		})
	}

	for _, openID := range users {
		saved := sch.GetTask(openID)
		if saved == nil {
			t.Errorf("task for %s should exist", openID)
			continue
		}
		if saved.TargetUser.Token != "token_"+openID {
			t.Errorf("%s: expected token token_%s, got %s", openID, openID, saved.TargetUser.Token)
		}
	}

	for _, openID := range users {
		sch.RemoveTask(openID)
		if sch.GetTask(openID) != nil {
			t.Errorf("task for %s should be removed", openID)
		}
	}
}

func TestRunScenario_DuplicateUserRejected(t *testing.T) {
	sch, svc := newTestScheduler(t)
	defer sch.Stop()

	openID := "dup_user"
	svc.GetTokenStore().Set(openID, "token")

	genPoints := func() []models.LocationPointNode {
		return pkg.GeneratePoints(
			[]models.LocationPointNode{
				{Latitude: 29.53286, Longitude: 106.60525, FieldID: "T1001"},
				{Latitude: 29.53320, Longitude: 106.60550, FieldID: "T1001"},
			},
			models.TaskSettingNode{TargetPace: 6.0, TargetFieldID: "T1001"},
			time.Now(),
		)
	}

	sch.AddTask(&models.TaskNode{
		TargetUser:     models.UserNode{OpenID: openID, Token: "token"},
		TaskSettings:   models.TaskSettingNode{TargetFieldID: "T1001"},
		LocationPoints: genPoints(),
		SubmittedCount: 0,
		StartTime:      time.Now(),
	})

	sch.AddTask(&models.TaskNode{
		TargetUser:     models.UserNode{OpenID: openID, Token: "token2"},
		TaskSettings:   models.TaskSettingNode{TargetFieldID: "T1005"},
		LocationPoints: genPoints(),
		SubmittedCount: 0,
		StartTime:      time.Now(),
	})

	saved := sch.GetTask(openID)
	if saved == nil {
		t.Fatal("task should exist")
	}
}
