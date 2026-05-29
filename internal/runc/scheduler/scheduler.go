package scheduler

import (
	"sync"
	"time"

	"RunningByeBye/internal/runc/consts"
	"RunningByeBye/internal/runc/models"
	"RunningByeBye/internal/runc/pkg"
)

type Scheduler struct {
	reliance SchedulerReliance
	tasks    map[string]*models.TaskNode
	mu       sync.Mutex
	eventCh  chan models.ProgressEvent
	stopCh   chan struct{}
	stopOnce sync.Once
}

func NewScheduler(reliance SchedulerReliance) *Scheduler {
	return &Scheduler{
		reliance: reliance,
		tasks:    make(map[string]*models.TaskNode),
		eventCh:  make(chan models.ProgressEvent, 100),
		stopCh:   make(chan struct{}),
	}
}

func (sch *Scheduler) EventChannel() <-chan models.ProgressEvent {
	return sch.eventCh
}

func (sch *Scheduler) AddTask(task *models.TaskNode) error {
	sch.mu.Lock()
	defer sch.mu.Unlock()

	if existing, ok := sch.tasks[task.TargetUser.OpenID]; ok {
		if existing.State == models.TaskStateRunning || existing.State == models.TaskStatePaused {
			sch.reliance.Logger.Warn("User %s already has an active task", task.TargetUser.OpenID)
			return nil
		}
	}

	task.State = models.TaskStatePending
	sch.tasks[task.TargetUser.OpenID] = task
	sch.reliance.Logger.Info("Task added for user %s, field=%s", task.TargetUser.OpenID, task.TaskSettings.TargetFieldID)

	go sch.runTask(task)
	return nil
}

func (sch *Scheduler) RemoveTask(openID string) {
	sch.mu.Lock()
	task, ok := sch.tasks[openID]
	if !ok {
		sch.mu.Unlock()
		return
	}
	recordNo := task.RecordNo
	submitted := task.SubmittedCount
	totalPoints := len(task.LocationPoints)
	mileage := task.Mileage
	task.State = models.TaskStateCompleted
	delete(sch.tasks, openID)
	sch.mu.Unlock()

	if recordNo != "" {
		sch.reliance.Service.FinishRunning(openID, recordNo)
	}
	sch.reliance.Service.GetSessionStore().Delete(openID)
	sch.eventCh <- models.ProgressEvent{
		OpenID:         openID,
		RecordNo:       recordNo,
		SubmittedCount: submitted,
		TotalPoints:    totalPoints,
		Mileage:        mileage,
		State:          "completed",
	}
	sch.reliance.Logger.Info("Task removed for user %s", openID)
}

func (sch *Scheduler) PauseTask(openID string) {
	sch.mu.Lock()
	defer sch.mu.Unlock()

	if task, ok := sch.tasks[openID]; ok {
		task.IsPaused = true
		task.State = models.TaskStatePaused
		sch.reliance.Service.GetSessionStore().Save(openID, task)
		sch.reliance.Logger.Info("Task paused for user %s", openID)
	}
}

func (sch *Scheduler) ResumeTask(openID string) {
	sch.mu.Lock()
	defer sch.mu.Unlock()

	if task, ok := sch.tasks[openID]; ok {
		task.IsPaused = false
		task.State = models.TaskStateRunning
		sch.reliance.Logger.Info("Task resumed for user %s", openID)
	}
}

func (sch *Scheduler) GetTask(openID string) *models.TaskNode {
	sch.mu.Lock()
	defer sch.mu.Unlock()
	return sch.tasks[openID]
}

func (sch *Scheduler) RecoverTasks() error {
	openIDs, err := sch.reliance.Service.GetSessionStore().List()
	if err != nil {
		return err
	}

	for _, openID := range openIDs {
		task, err := sch.reliance.Service.GetSessionStore().Load(openID)
		if err != nil {
			sch.reliance.Logger.Warn("Failed to load task for %s: %v", openID, err)
			continue
		}

		_, err = sch.reliance.Service.EnsureToken(openID)
		if err != nil {
			sch.reliance.Logger.Warn("Token refresh failed for %s during recovery, skipping: %v", openID, err)
			continue
		}

		task.State = models.TaskStateRunning
		task.IsPaused = false

		sch.mu.Lock()
		sch.tasks[openID] = task
		sch.mu.Unlock()

		sch.reliance.Logger.Info("Task recovered for user %s, progress=%d/%d", openID, task.SubmittedCount, len(task.LocationPoints))

		go sch.runTask(task)
	}

	return nil
}

func (sch *Scheduler) runTask(task *models.TaskNode) {
	openID := task.TargetUser.OpenID
	sch.reliance.Logger.Info("Task runner started for user %s", openID)

	if task.RecordNo == "" {
		recordNo, err := sch.reliance.Service.StartRunning(openID, task.TaskSettings.TargetFieldID)
		if err != nil {
			sch.reliance.Logger.Error("StartRunning failed for %s: %v", openID, err)
			sch.failTask(openID, err)
			return
		}
		task.RecordNo = recordNo
		sch.reliance.Logger.Info("Running started for %s, recordNo=%s", openID, recordNo)
	}

	task.State = models.TaskStateRunning
	task.StartTime = time.Now()

	batchSize := consts.DEFAULT_BATCH_SIZE
	tickerInterval := time.Duration(consts.DEFAULT_TICKER_INTERVAL) * time.Second
	ticker := time.NewTicker(tickerInterval)
	defer ticker.Stop()

	for {
		select {
		case <-sch.stopCh:
			sch.reliance.Logger.Info("Task runner stopping for %s", openID)
			sch.reliance.Service.GetSessionStore().Save(openID, task)
			return
		case <-ticker.C:
			sch.mu.Lock()
			currentTask, exists := sch.tasks[openID]
			sch.mu.Unlock()

			if !exists || currentTask.State == models.TaskStateCompleted || currentTask.State == models.TaskStateFailed {
				return
			}

			if currentTask.IsPaused {
				continue
			}

			totalPoints := len(currentTask.LocationPoints)
			submitted := currentTask.SubmittedCount

			if submitted >= totalPoints {
				sch.finishTask(openID, currentTask)
				return
			}

			end := submitted + batchSize
			if end > totalPoints {
				end = totalPoints
			}

			batch := pkg.ToSportPointList(currentTask.LocationPoints[submitted:end], currentTask.TaskSettings.TargetFieldID)
			if err := sch.reliance.Service.UpdateRunningPoint(openID, currentTask.RecordNo, batch); err != nil {
				sch.reliance.Logger.Error("UpdateRunningPoint failed for %s: %v", openID, err)
				sch.failTask(openID, err)
				return
			}

			currentTask.SubmittedCount = end

			mileage := pkg.CalcMileage(currentTask.LocationPoints[:end])
			currentTask.Mileage = mileage

			sch.reliance.Service.GetSessionStore().Save(openID, currentTask)

			sch.eventCh <- models.ProgressEvent{
				OpenID:         openID,
				RecordNo:       currentTask.RecordNo,
				SubmittedCount: end,
				TotalPoints:    totalPoints,
				Mileage:        mileage,
				State:          "running",
			}

			sch.reliance.Logger.Debug("Task progress for %s: %d/%d points, mileage=%.3f", openID, end, totalPoints, mileage)

			if end >= totalPoints {
				sch.finishTask(openID, currentTask)
				return
			}
		}
	}
}

func (sch *Scheduler) finishTask(openID string, task *models.TaskNode) {
	result, err := sch.reliance.Service.FinishRunning(openID, task.RecordNo)
	if err != nil || !result {
		sch.reliance.Logger.Error("FinishRunning failed for %s: %v", openID, err)
		task.State = models.TaskStateFailed
		sch.eventCh <- models.ProgressEvent{
			OpenID:         openID,
			RecordNo:       task.RecordNo,
			SubmittedCount: task.SubmittedCount,
			TotalPoints:    len(task.LocationPoints),
			Mileage:        task.Mileage,
			State:          "failed",
		}
	} else {
		task.State = models.TaskStateCompleted
		task.Mileage = pkg.CalcMileage(task.LocationPoints)
		sch.eventCh <- models.ProgressEvent{
			OpenID:         openID,
			RecordNo:       task.RecordNo,
			SubmittedCount: task.SubmittedCount,
			TotalPoints:    len(task.LocationPoints),
			Mileage:        task.Mileage,
			State:          "completed",
		}
	}

	sch.reliance.Service.GetSessionStore().Delete(openID)

	sch.mu.Lock()
	delete(sch.tasks, openID)
	sch.mu.Unlock()

	sch.reliance.Logger.Info("Task finished for %s, state=%d", openID, task.State)
}

func (sch *Scheduler) failTask(openID string, err error) {
	sch.mu.Lock()
	if task, ok := sch.tasks[openID]; ok {
		task.State = models.TaskStateFailed
	}
	sch.mu.Unlock()

	sch.eventCh <- models.ProgressEvent{
		OpenID: openID,
		State:  "failed",
	}

	sch.reliance.Logger.Error("Task failed for %s: %v", openID, err)
}

func (sch *Scheduler) Stop() {
	sch.stopOnce.Do(func() {
		close(sch.stopCh)
		sch.reliance.Logger.Info("Scheduler stopped")
	})
}
