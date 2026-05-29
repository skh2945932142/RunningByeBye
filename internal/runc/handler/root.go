package handler

import (
	"RunningByeBye/internal/runc/pkg"
	"RunningByeBye/internal/runc/scheduler"
	"RunningByeBye/internal/runc/service"
)

type HandlerReliance struct {
	Service   *service.Service
	Scheduler *scheduler.Scheduler
	Logger    *pkg.Logger
	PointsDir string
}
