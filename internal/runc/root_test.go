package runc

import (
	"path/filepath"
	"testing"

	"RunningByeBye/internal/runc/pkg"
)

func TestNew_DefaultOptions(t *testing.T) {
	dir := t.TempDir()
	r, err := New(
		WithPointsDir(filepath.Join(dir, "points")),
		WithTaskDir(filepath.Join(dir, "tasks")),
		WithCacheDir(filepath.Join(dir, "cache")),
	)
	if err != nil {
		t.Fatalf("New with defaults failed: %v", err)
	}
	if r.Handler == nil {
		t.Error("Handler is nil")
	}
	if r.Scheduler == nil {
		t.Error("Scheduler is nil")
	}
	if r.Service == nil {
		t.Error("Service is nil")
	}
	if r.Logger == nil {
		t.Error("Logger is nil")
	}
	r.Close()
}

func TestNew_WithCustomOptions(t *testing.T) {
	dir := t.TempDir()
	r, err := New(
		WithTokenStore("memory"),
		WithSessionStore("file"),
		WithPointsDir(filepath.Join(dir, "points")),
		WithTaskDir(filepath.Join(dir, "tasks")),
		WithCacheDir(filepath.Join(dir, "cache")),
		WithLogLevel(pkg.LogLevelDebug),
		WithLogFile(filepath.Join(dir, "test.log")),
	)
	if err != nil {
		t.Fatalf("New with custom options failed: %v", err)
	}
	if r.Handler == nil {
		t.Error("Handler is nil")
	}
	r.Close()
}

func TestNew_WithRedisFallback(t *testing.T) {
	dir := t.TempDir()
	// Redis not running, should fallback gracefully
	r, err := New(
		WithRedis("127.0.0.1:16379", "", 0),
		WithTokenStore("redis"),
		WithSessionStore("redis"),
		WithPointsDir(filepath.Join(dir, "points")),
		WithTaskDir(filepath.Join(dir, "tasks")),
		WithCacheDir(filepath.Join(dir, "cache")),
	)
	if err != nil {
		t.Fatalf("New with unavailable Redis should not fail: %v", err)
	}
	if r.Handler == nil {
		t.Error("Handler is nil after Redis fallback")
	}
	r.Close()
}

func TestNew_WithUserAgent(t *testing.T) {
	dir := t.TempDir()
	r, err := New(
		WithUserAgent("test-user-agent"),
		WithPhoneType("test-phone"),
		WithPointsDir(filepath.Join(dir, "points")),
		WithTaskDir(filepath.Join(dir, "tasks")),
		WithCacheDir(filepath.Join(dir, "cache")),
	)
	if err != nil {
		t.Fatalf("New with UA options failed: %v", err)
	}
	if r.Logger == nil {
		t.Error("Logger is nil")
	}
	r.Close()
}

func TestClose(t *testing.T) {
	dir := t.TempDir()
	r, err := New(
		WithPointsDir(filepath.Join(dir, "points")),
		WithTaskDir(filepath.Join(dir, "tasks")),
		WithCacheDir(filepath.Join(dir, "cache")),
	)
	if err != nil {
		t.Fatalf("New failed: %v", err)
	}

	// Close should not panic
	r.Close()
}

func TestDoubleClose(t *testing.T) {
	dir := t.TempDir()
	r, err := New(
		WithPointsDir(filepath.Join(dir, "points")),
		WithTaskDir(filepath.Join(dir, "tasks")),
		WithCacheDir(filepath.Join(dir, "cache")),
	)
	if err != nil {
		t.Fatalf("New failed: %v", err)
	}

	r.Close()
	r.Close()
}
