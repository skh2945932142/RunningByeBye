package scheduler

import (
	"RunningByeBye/internal/runc/models"
	"sync"
)

type SchedulerDependence struct {
}

type Scheduler struct {
	Tasks map[string]models.TaskNode
	mu    sync.Mutex
}
