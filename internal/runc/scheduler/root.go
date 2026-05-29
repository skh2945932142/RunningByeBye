package scheduler

import (
	"RunningByeBye/internal/runc/pkg"
	"RunningByeBye/internal/runc/service"
)

type SchedulerReliance struct {
	Service *service.Service
	Logger  *pkg.Logger
}
