package service

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"sync"
	"time"

	sdk "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK"
	sdkModels "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK/models"
	"github.com/redis/go-redis/v9"

	"RunningByeBye/internal/runc/consts"
	"RunningByeBye/internal/runc/models"
	"RunningByeBye/internal/runc/pkg"
)

type MemoryTokenStore struct {
	store sync.Map
}

func NewMemoryTokenStore() *MemoryTokenStore {
	return &MemoryTokenStore{}
}

func (m *MemoryTokenStore) Get(openID string) (string, error) {
	val, ok := m.store.Load(openID)
	if !ok {
		return "", fmt.Errorf("token not found for %s", openID)
	}
	cache, ok := val.(models.TokenCache)
	if !ok {
		return "", fmt.Errorf("invalid token cache for %s", openID)
	}
	saveTime, err := time.Parse("2006-01-02 15:04:05", cache.SaveTime)
	if err != nil {
		return "", err
	}
	if time.Since(saveTime).Seconds() > consts.TOKEN_TTL {
		m.store.Delete(openID)
		return "", fmt.Errorf("token expired for %s", openID)
	}
	return cache.Token, nil
}

func (m *MemoryTokenStore) Set(openID string, token string) error {
	m.store.Store(openID, models.TokenCache{
		Token:    token,
		OpenID:   openID,
		SaveTime: time.Now().Format("2006-01-02 15:04:05"),
	})
	return nil
}

func (m *MemoryTokenStore) Delete(openID string) error {
	m.store.Delete(openID)
	return nil
}

type RedisTokenStore struct {
	client *redis.Client
}

func NewRedisTokenStore(client *redis.Client) *RedisTokenStore {
	return &RedisTokenStore{client: client}
}

func (r *RedisTokenStore) key(openID string) string {
	return consts.KEY_PREFIX_TOKEN + openID
}

func (r *RedisTokenStore) Get(openID string) (string, error) {
	val, err := r.client.Get(context.Background(), r.key(openID)).Result()
	if err != nil {
		return "", err
	}
	return val, nil
}

func (r *RedisTokenStore) Set(openID string, token string) error {
	return r.client.Set(context.Background(), r.key(openID), token, time.Duration(consts.TOKEN_TTL)*time.Second).Err()
}

func (r *RedisTokenStore) Delete(openID string) error {
	return r.client.Del(context.Background(), r.key(openID)).Err()
}

type FileSessionStore struct {
	taskDir string
}

func NewFileSessionStore(taskDir string) *FileSessionStore {
	return &FileSessionStore{taskDir: taskDir}
}

func (f *FileSessionStore) taskPath(openID string) string {
	return fmt.Sprintf("%s/%stask.json", f.taskDir, openID)
}

func (f *FileSessionStore) Save(openID string, task *models.TaskNode) error {
	return pkg.SaveJSON(task, f.taskPath(openID))
}

func (f *FileSessionStore) Load(openID string) (*models.TaskNode, error) {
	var task models.TaskNode
	if err := pkg.LoadJSON(f.taskPath(openID), &task); err != nil {
		return nil, err
	}
	return &task, nil
}

func (f *FileSessionStore) Delete(openID string) error {
	path := f.taskPath(openID)
	if !pkg.FileExists(path) {
		return nil
	}
	return os.Remove(path)
}

func (f *FileSessionStore) List() ([]string, error) {
	entries, err := pkg.ReadDir(f.taskDir)
	if err != nil {
		return nil, err
	}
	openIDs := make([]string, 0)
	for _, entry := range entries {
		name := entry
		if len(name) > 9 && name[len(name)-9:] == "task.json" {
			openIDs = append(openIDs, name[:len(name)-9])
		}
	}
	return openIDs, nil
}

type RedisSessionStore struct {
	client *redis.Client
}

func NewRedisSessionStore(client *redis.Client) *RedisSessionStore {
	return &RedisSessionStore{client: client}
}

func (r *RedisSessionStore) key(openID string) string {
	return consts.KEY_PREFIX_SESSION + openID
}

func (r *RedisSessionStore) taskKey(openID string) string {
	return consts.KEY_PREFIX_TASK + openID
}

func (r *RedisSessionStore) Save(openID string, task *models.TaskNode) error {
	data, err := json.Marshal(task)
	if err != nil {
		return err
	}
	pipe := r.client.Pipeline()
	pipe.Set(context.Background(), r.key(openID), data, 0)
	pipe.SAdd(context.Background(), r.taskKey(openID), openID)
	_, err = pipe.Exec(context.Background())
	return err
}

func (r *RedisSessionStore) Load(openID string) (*models.TaskNode, error) {
	data, err := r.client.Get(context.Background(), r.key(openID)).Result()
	if err != nil {
		return nil, err
	}
	var task models.TaskNode
	if err := json.Unmarshal([]byte(data), &task); err != nil {
		return nil, err
	}
	return &task, nil
}

func (r *RedisSessionStore) Delete(openID string) error {
	pipe := r.client.Pipeline()
	pipe.Del(context.Background(), r.key(openID))
	pipe.SRem(context.Background(), r.taskKey(openID), openID)
	_, err := pipe.Exec(context.Background())
	return err
}

func (r *RedisSessionStore) List() ([]string, error) {
	return r.client.SMembers(context.Background(), consts.KEY_PREFIX_TASK).Result()
}

func (s *Service) getSDK(openID string) (*sdk.SDK, error) {
	s.mu.RLock()
	sdkClient, exists := s.sdkClients[openID]
	s.mu.RUnlock()
	if exists {
		return sdkClient, nil
	}
	return nil, fmt.Errorf("sdk not initialized for user %s", openID)
}

func (s *Service) getOrCreateSDK(openID string) (*sdk.SDK, error) {
	s.mu.RLock()
	sdkClient, exists := s.sdkClients[openID]
	s.mu.RUnlock()
	if exists {
		return sdkClient, nil
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	if sdkClient, exists = s.sdkClients[openID]; exists {
		return sdkClient, nil
	}

	cookiePath := fmt.Sprintf("%s/%s_cookie.json", s.reliance.Config.CacheDir, openID)
	var err error
	sdkClient, err = sdk.NewSDK(sdk.WithCookiePath(cookiePath))
	if err != nil {
		return nil, fmt.Errorf("failed to create SDK for %s: %w", openID, err)
	}
	s.sdkClients[openID] = sdkClient
	s.reliance.Logger.Debug("SDK created for user %s", openID)
	return sdkClient, nil
}

func (s *Service) Login(openID string) (*models.UserNode, error) {
	sdkClient, err := s.getOrCreateSDK(openID)
	if err != nil {
		return nil, err
	}

	tokenInfo, err := sdkClient.GetToken(openID)
	if err != nil {
		s.reliance.Logger.Error("Login GetToken failed for %s: %v", openID, err)
		return nil, fmt.Errorf("login GetToken failed: %w", err)
	}
	token := tokenInfo.Data.Token
	s.reliance.Logger.Debug("Login token obtained for %s", openID)

	s.tokenStore.Set(openID, token)

	userInfo, err := sdkClient.GetUserInfo(token)
	if err != nil {
		s.reliance.Logger.Error("Login GetUserInfo failed for %s: %v", openID, err)
		return nil, fmt.Errorf("login GetUserInfo failed: %w", err)
	}

	userNode := &models.UserNode{
		OpenID: openID,
		Token:  token,
		MetaInfo: models.UserMetaInfo{
			Username:  userInfo.Data.Username,
			UnifyId:   userInfo.Data.UnifyId,
			StudentNo: userInfo.Data.StudentNo,
			Sex:       userInfo.Data.Sex,
			Grade:     userInfo.Data.Grade,
			DeptName:  userInfo.Data.DeptName,
		},
	}

	s.reliance.Logger.Debug("Login success for %s (%s)", userNode.MetaInfo.Username, openID)
	return userNode, nil
}

func (s *Service) EnsureToken(openID string) (string, error) {
	token, err := s.tokenStore.Get(openID)
	if err == nil {
		return token, nil
	}

	s.reliance.Logger.Debug("Token refresh for %s", openID)
	userNode, err := s.Login(openID)
	if err != nil {
		return "", err
	}
	return userNode.Token, nil
}

func (s *Service) GetUserInfo(openID string) (*sdkModels.UserInfo, error) {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return nil, err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return nil, err
	}
	return sdkClient.GetUserInfo(token)
}

func (s *Service) GetNotice(openID string) (*sdkModels.NoticeInfo, error) {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return nil, err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return nil, err
	}
	return sdkClient.GetNotice(token)
}

func (s *Service) GetAllArea(openID string) (*sdkModels.AreaInfo, error) {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return nil, err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return nil, err
	}
	return sdkClient.GetAllArea(token)
}

func (s *Service) GetTerm(openID string) (*sdkModels.TermInfo, error) {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return nil, err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return nil, err
	}
	return sdkClient.GetTerm(token)
}

func (s *Service) GetAllSportsResult(openID string, term string) (*sdkModels.AllSportsResults, error) {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return nil, err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return nil, err
	}
	return sdkClient.GetAllSportsResult(token, term)
}

func (s *Service) StartRunning(openID string, fieldID string) (string, error) {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return "", err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return "", err
	}
	resp, err := sdkClient.StartRunning(token, fieldID)
	if err != nil {
		s.reliance.Logger.Error("StartRunning failed for %s: %v", openID, err)
		return "", err
	}
	s.reliance.Logger.Debug("StartRunning success for %s, recordNo=%s", openID, resp.Data)
	return resp.Data, nil
}

func (s *Service) GetCurrentRunningInfo(openID string, recordNo string) (*sdkModels.CurrentRunningInfo, error) {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return nil, err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return nil, err
	}
	return sdkClient.GetCurrentRunningInfo(token, recordNo)
}

func (s *Service) UpdateRunningPoint(openID string, recordNo string, points []sdkModels.SportPointListNode) error {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return err
	}
	_, err = sdkClient.UpdateRunningPoint(token, recordNo, points)
	if err != nil {
		s.reliance.Logger.Error("UpdateRunningPoint failed for %s: %v", openID, err)
		return err
	}
	return nil
}

func (s *Service) FinishRunning(openID string, recordNo string) (bool, error) {
	token, err := s.EnsureToken(openID)
	if err != nil {
		return false, err
	}
	sdkClient, err := s.getSDK(openID)
	if err != nil {
		return false, err
	}
	resp, err := sdkClient.FinishRunning(token, recordNo)
	if err != nil {
		s.reliance.Logger.Error("FinishRunning failed for %s: %v", openID, err)
		return false, err
	}
	s.reliance.Logger.Debug("FinishRunning success for %s, result=%v", openID, resp.Data)
	return resp.Data, nil
}

func (s *Service) Close() {
	s.mu.Lock()
	defer s.mu.Unlock()
	for openID, sdkClient := range s.sdkClients {
		if err := sdkClient.Close(); err != nil {
			s.reliance.Logger.Warn("Failed to close SDK for %s: %v", openID, err)
		}
	}
	s.sdkClients = make(map[string]*sdk.SDK)
	s.reliance.Logger.Debug("All SDK clients closed")
}
