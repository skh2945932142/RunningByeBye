package pkg

import (
	"encoding/json"
	"testing"

	"RunningByeBye/internal/runc/consts"
	"RunningByeBye/internal/runc/models"
)

func TestFieldCodeToName_FengHua(t *testing.T) {
	result := FieldCodeToName(consts.FIELD_FENG_HUA)
	if result != consts.SPORTS_FIELD_FENG_HUA {
		t.Errorf("FieldCodeToName(T1001) = %s, want %s", result, consts.SPORTS_FIELD_FENG_HUA)
	}
}

func TestFieldCodeToName_TaiJi(t *testing.T) {
	result := FieldCodeToName(consts.FIELD_TAI_JI)
	if result != consts.SPORTS_FIELD_TAI_JI {
		t.Errorf("FieldCodeToName(T1005) = %s, want %s", result, consts.SPORTS_FIELD_TAI_JI)
	}
}

func TestFieldCodeToName_NingJing(t *testing.T) {
	result := FieldCodeToName(consts.FIELD_NING_JING)
	if result != consts.SPORTS_FIELD_NING_JING {
		t.Errorf("FieldCodeToName(T1014) = %s, want %s", result, consts.SPORTS_FIELD_NING_JING)
	}
}

func TestFieldCodeToName_Unknown(t *testing.T) {
	result := FieldCodeToName("UNKNOWN")
	if result != "UNKNOWN" {
		t.Errorf("FieldCodeToName(UNKNOWN) = %s, want UNKNOWN", result)
	}
}

func TestFieldNameToCode_FengHua(t *testing.T) {
	result := FieldNameToCode(consts.SPORTS_FIELD_FENG_HUA)
	if result != consts.FIELD_FENG_HUA {
		t.Errorf("FieldNameToCode(风华运动场) = %s, want %s", result, consts.FIELD_FENG_HUA)
	}
}

func TestFieldNameToCode_TaiJi(t *testing.T) {
	result := FieldNameToCode(consts.SPORTS_FIELD_TAI_JI)
	if result != consts.FIELD_TAI_JI {
		t.Errorf("FieldNameToCode(太极运动场) = %s, want %s", result, consts.FIELD_TAI_JI)
	}
}

func TestFieldNameToCode_NingJing(t *testing.T) {
	result := FieldNameToCode(consts.SPORTS_FIELD_NING_JING)
	if result != consts.FIELD_NING_JING {
		t.Errorf("FieldNameToCode(宁静苑) = %s, want %s", result, consts.FIELD_NING_JING)
	}
}

func TestFieldNameToCode_Unknown(t *testing.T) {
	result := FieldNameToCode("不存在的场地")
	if result != "" {
		t.Errorf("FieldNameToCode(unknown) = %s, want empty", result)
	}
}

func TestToSportPointList(t *testing.T) {
	points := []models.LocationPointNode{
		{Longitude: 106.605, Latitude: 29.532, Timestamp: "2025-01-01 12:00:00", FieldID: "T1001"},
		{Longitude: 106.606, Latitude: 29.533, Timestamp: "2025-01-01 12:00:01", FieldID: "T1001"},
	}
	result := ToSportPointList(points, "T1001")
	if len(result) != 2 {
		t.Fatalf("ToSportPointList length = %d, want 2", len(result))
	}
	if result[0].Longitude != 106.605 {
		t.Errorf("expected longitude 106.605, got %f", result[0].Longitude)
	}
	if result[0].PlaceCode != "T1001" {
		t.Errorf("expected PlaceCode T1001, got %s", result[0].PlaceCode)
	}
	if result[0].PlaceName != "风华运动场" {
		t.Errorf("expected PlaceName 风华运动场, got %s", result[0].PlaceName)
	}
	if result[0].IsValid != "1" {
		t.Errorf("expected IsValid 1, got %s", result[0].IsValid)
	}
}

func TestReadPointsFromRaw(t *testing.T) {
	raw := json.RawMessage(`{
		"msg": "请求成功",
		"code": "10200",
		"data": [
			{"longitude": "106.608", "latitude": "29.533", "collectTime": "2025-03-02 19:36:48", "isValid": "1"},
			{"longitude": "106.609", "latitude": "29.534", "collectTime": "2025-03-02 19:36:49", "isValid": "1"}
		]
	}`)
	points, err := ReadPointsFromRaw(raw)
	if err != nil {
		t.Fatalf("ReadPointsFromRaw failed: %v", err)
	}
	if len(points) != 2 {
		t.Fatalf("expected 2 points, got %d", len(points))
	}
}

func TestReadPointsFromRaw_InvalidJSON(t *testing.T) {
	_, err := ReadPointsFromRaw(json.RawMessage(`invalid`))
	if err == nil {
		t.Error("expected error for invalid JSON")
	}
}

func TestPickRandomPointsFile_Match(t *testing.T) {
	result := PickRandomPointsFile("/base", "T1001", []string{"T1001_1.json", "T1001_2.json", "other.json"})
	if result == "" {
		t.Error("expected a matched file, got empty")
	}
}

func TestPickRandomPointsFile_NoMatch(t *testing.T) {
	result := PickRandomPointsFile("/base", "T9999", []string{"T1001_1.json", "T1001_2.json"})
	if result != "" {
		t.Errorf("expected empty for no match, got %s", result)
	}
}

func TestPickRandomPointsFile_EmptyList(t *testing.T) {
	result := PickRandomPointsFile("/base", "T1001", []string{})
	if result != "" {
		t.Errorf("expected empty for empty list, got %s", result)
	}
}

func TestExtractTokenInfo_Valid(t *testing.T) {
	data := json.RawMessage(`{
		"msg": "成功",
		"code": "10200",
		"data": {"isBind": "1", "publicKey": "", "token": "test_token_value"}
	}`)
	token := ExtractTokenInfo(data)
	if token != "test_token_value" {
		t.Errorf("expected test_token_value, got %s", token)
	}
}

func TestExtractTokenInfo_Invalid(t *testing.T) {
	token := ExtractTokenInfo(json.RawMessage(`invalid`))
	if token != "" {
		t.Errorf("expected empty for invalid, got %s", token)
	}
}

func TestExtractRecordNo_Valid(t *testing.T) {
	no := ExtractRecordNo(json.RawMessage(`"RECORD123"`))
	if no != "RECORD123" {
		t.Errorf("expected RECORD123, got %s", no)
	}
}

func TestExtractRecordNo_Invalid(t *testing.T) {
	no := ExtractRecordNo(json.RawMessage(`invalid`))
	if no != "" {
		t.Errorf("expected empty for invalid, got %s", no)
	}
}

func TestExtractUserMeta_Valid(t *testing.T) {
	data := json.RawMessage(`{
		"msg": "成功",
		"code": "10200",
		"data": {
			"username": "张三",
			"unifyId": "U123",
			"studentNo": "2024001",
			"sex": "男",
			"grade": "2024",
			"deptName": "计算机学院"
		}
	}`)
	meta := ExtractUserMeta(data)
	if meta.Username != "张三" {
		t.Errorf("expected 张三, got %s", meta.Username)
	}
	if meta.StudentNo != "2024001" {
		t.Errorf("expected 2024001, got %s", meta.StudentNo)
	}
	if meta.DeptName != "计算机学院" {
		t.Errorf("expected 计算机学院, got %s", meta.DeptName)
	}
}

func TestExtractUserMeta_Invalid(t *testing.T) {
	meta := ExtractUserMeta(json.RawMessage(`invalid`))
	if meta.Username != "" {
		t.Errorf("expected empty for invalid, got %s", meta.Username)
	}
}
