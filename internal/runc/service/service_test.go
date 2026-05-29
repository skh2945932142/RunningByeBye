package service

import (
	"path/filepath"
	"testing"
	"time"

	"RunningByeBye/internal/runc/models"
	"RunningByeBye/internal/runc/pkg"
)

func TestMemoryTokenStore_SetAndGet(t *testing.T) {
	store := NewMemoryTokenStore()
	err := store.Set("test_openid", "test_token")
	if err != nil {
		t.Fatalf("Set failed: %v", err)
	}
	token, err := store.Get("test_openid")
	if err != nil {
		t.Fatalf("Get failed: %v", err)
	}
	if token != "test_token" {
		t.Errorf("Get = %s, want test_token", token)
	}
}

func TestMemoryTokenStore_Get_NotExist(t *testing.T) {
	store := NewMemoryTokenStore()
	_, err := store.Get("nonexistent")
	if err == nil {
		t.Error("expected error for nonexistent token")
	}
}

func TestMemoryTokenStore_Delete(t *testing.T) {
	store := NewMemoryTokenStore()
	store.Set("test_openid", "test_token")
	err := store.Delete("test_openid")
	if err != nil {
		t.Fatalf("Delete failed: %v", err)
	}
	_, err = store.Get("test_openid")
	if err == nil {
		t.Error("expected error after delete")
	}
}

func TestMemoryTokenStore_Expired(t *testing.T) {
	store := &MemoryTokenStore{}
	cache := models.TokenCache{
		Token:    "expired_token",
		OpenID:   "test_openid",
		SaveTime: "2000-01-01 00:00:00",
	}
	store.store.Store("test_openid", cache)
	_, err := store.Get("test_openid")
	if err == nil {
		t.Error("expected error for expired token")
	}
}

func TestFileSessionStore_SaveLoadDelete(t *testing.T) {
	dir := t.TempDir()
	store := NewFileSessionStore(dir)

	task := &models.TaskNode{
		TargetUser: models.UserNode{
			OpenID: "test_user",
			Token:  "test_token",
		},
		RecordNo:       "REC001",
		SubmittedCount: 5,
		StartTime:      time.Now(),
	}

	err := store.Save("test_user", task)
	if err != nil {
		t.Fatalf("Save failed: %v", err)
	}

	loaded, err := store.Load("test_user")
	if err != nil {
		t.Fatalf("Load failed: %v", err)
	}
	if loaded.RecordNo != "REC001" {
		t.Errorf("Load RecordNo = %s, want REC001", loaded.RecordNo)
	}
	if loaded.SubmittedCount != 5 {
		t.Errorf("Load SubmittedCount = %d, want 5", loaded.SubmittedCount)
	}

	err = store.Delete("test_user")
	if err != nil {
		t.Fatalf("Delete failed: %v", err)
	}
}

func TestFileSessionStore_Load_NotExist(t *testing.T) {
	store := NewFileSessionStore(t.TempDir())
	_, err := store.Load("nonexistent_user")
	if err == nil {
		t.Error("expected error for nonexistent task")
	}
}

func TestFileSessionStore_List(t *testing.T) {
	dir := t.TempDir()
	store := NewFileSessionStore(dir)

	store.Save("user_a", &models.TaskNode{
		TargetUser: models.UserNode{OpenID: "user_a"},
	})
	store.Save("user_b", &models.TaskNode{
		TargetUser: models.UserNode{OpenID: "user_b"},
	})

	ids, err := store.List()
	if err != nil {
		t.Fatalf("List failed: %v", err)
	}
	if len(ids) != 2 {
		t.Errorf("List returned %d items, want 2: %v", len(ids), ids)
	}
}

func TestFileSessionStore_ListEmpty(t *testing.T) {
	dir := t.TempDir()
	store := NewFileSessionStore(dir)

	ids, err := store.List()
	if err != nil {
		t.Fatalf("List on empty dir failed: %v", err)
	}
	if len(ids) != 0 {
		t.Errorf("expected empty list, got %v", ids)
	}
}

func TestService_NewWithDefaults(t *testing.T) {
	dir := t.TempDir()
	logger := pkg.NewLogger(pkg.LogLevelError)

	svc := NewService(ServiceReliance{
		RedisClient: nil,
		Logger:      logger,
		Config: models.RuncConfig{
			TokenStoreType:   "memory",
			SessionStoreType: "file",
			TaskDir:          dir,
			CacheDir:         filepath.Join(dir, "cache"),
			UserAgent:        "test-agent",
			PhoneType:        "test-phone",
		},
	})

	ts := svc.GetTokenStore()
	if ts == nil {
		t.Fatal("TokenStore should not be nil")
	}
	_, ok := ts.(*MemoryTokenStore)
	if !ok {
		t.Error("expected MemoryTokenStore")
	}

	ss := svc.GetSessionStore()
	if ss == nil {
		t.Fatal("SessionStore should not be nil")
	}
	_, ok = ss.(*FileSessionStore)
	if !ok {
		t.Error("expected FileSessionStore")
	}
}
