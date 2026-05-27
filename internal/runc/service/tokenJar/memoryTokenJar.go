package tokenJar

import (
	"fmt"
	"sync"
	"time"
)

type MemTokenJar struct {
	Tokens map[string]string
	mu     sync.Mutex
}

func NewMemTokenJar() *MemTokenJar {
	return &MemTokenJar{
		Tokens: make(map[string]string),
		mu:     sync.Mutex{},
	}
}

func (r *MemTokenJar) GetToken(openid string) (string, time.Duration, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	token, ok := r.Tokens[openid]
	if !ok {
		return "", , fmt.Errorf("no token found in this jar")
	}

	return token, nil
}

func (r *MemTokenJar) SetToken(openid string, token string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	r.Tokens[openid] = token

	return nil
}

func (r *MemTokenJar) GetAllTokens() (map[string]string, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	return r.Tokens, nil
}

func (r *MemTokenJar) RemoveToken(openid string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	delete(r.Tokens, openid)

	return nil
}
