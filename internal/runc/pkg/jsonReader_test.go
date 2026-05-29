package pkg

import (
	"os"
	"path/filepath"
	"testing"
)

func TestSaveAndLoadJSON(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "test.json")

	type TestData struct {
		Name  string `json:"name"`
		Value int    `json:"value"`
	}

	data := TestData{Name: "test", Value: 42}
	err := SaveJSON(data, path)
	if err != nil {
		t.Fatalf("SaveJSON failed: %v", err)
	}

	if !FileExists(path) {
		t.Fatal("FileExists returned false after SaveJSON")
	}

	var loaded TestData
	err = LoadJSON(path, &loaded)
	if err != nil {
		t.Fatalf("LoadJSON failed: %v", err)
	}
	if loaded.Name != "test" || loaded.Value != 42 {
		t.Errorf("LoadJSON got %+v, want {test 42}", loaded)
	}
}

func TestLoadJSON_NotExist(t *testing.T) {
	var data interface{}
	err := LoadJSON("/nonexistent/path.json", &data)
	if err == nil {
		t.Error("expected error for nonexistent file")
	}
}

func TestFileExists_True(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "exists.txt")
	os.WriteFile(path, []byte("hello"), 0644)

	if !FileExists(path) {
		t.Error("FileExists should return true for existing file")
	}
}

func TestFileExists_False(t *testing.T) {
	if FileExists("/nonexistent_file_path") {
		t.Error("FileExists should return false for nonexistent file")
	}
}

func TestLoadRawFile(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "data.json")
	content := `{"key": "value"}`
	os.WriteFile(path, []byte(content), 0644)

	raw, err := LoadRawFile(path)
	if err != nil {
		t.Fatalf("LoadRawFile failed: %v", err)
	}
	if string(raw) != content {
		t.Errorf("LoadRawFile got %s, want %s", string(raw), content)
	}
}

func TestLoadRawFile_NotExist(t *testing.T) {
	_, err := LoadRawFile("/nonexistent.json")
	if err == nil {
		t.Error("expected error for nonexistent file")
	}
}

func TestReadDir(t *testing.T) {
	dir := t.TempDir()
	os.WriteFile(filepath.Join(dir, "a.txt"), []byte{}, 0644)
	os.WriteFile(filepath.Join(dir, "b.txt"), []byte{}, 0644)
	os.MkdirAll(filepath.Join(dir, "sub"), 0755)

	names, err := ReadDir(dir)
	if err != nil {
		t.Fatalf("ReadDir failed: %v", err)
	}
	if len(names) != 3 {
		t.Errorf("expected 3 entries, got %d: %v", len(names), names)
	}
}

func TestReadDir_NotExist(t *testing.T) {
	_, err := ReadDir("/nonexistent_dir")
	if err == nil {
		t.Error("expected error for nonexistent directory")
	}
}

func TestSaveJSON_CreatesDir(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "subdir", "nested", "data.json")

	err := SaveJSON(map[string]string{"hello": "world"}, path)
	if err != nil {
		t.Fatalf("SaveJSON with nested dirs failed: %v", err)
	}
	if !FileExists(path) {
		t.Error("file should exist after SaveJSON with nested dirs")
	}
}
