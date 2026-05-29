package pkg

import (
	"encoding/json"
	"os"
	"path/filepath"
)

func SaveJSON(data interface{}, filePath string) error {
	dir := filepath.Dir(filePath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return err
	}
	jsonData, err := json.MarshalIndent(data, "", "    ")
	if err != nil {
		return err
	}
	return os.WriteFile(filePath, jsonData, 0644)
}

func LoadJSON(filePath string, data interface{}) error {
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		return err
	}
	raw, err := os.ReadFile(filePath)
	if err != nil {
		return err
	}
	return json.Unmarshal(raw, data)
}

func LoadRawFile(filePath string) (json.RawMessage, error) {
	raw, err := os.ReadFile(filePath)
	if err != nil {
		return nil, err
	}
	return json.RawMessage(raw), nil
}

func FileExists(filePath string) bool {
	_, err := os.Stat(filePath)
	return err == nil
}

func ReadDir(dirPath string) ([]string, error) {
	entries, err := os.ReadDir(dirPath)
	if err != nil {
		return nil, err
	}
	names := make([]string, 0, len(entries))
	for _, entry := range entries {
		names = append(names, entry.Name())
	}
	return names, nil
}
