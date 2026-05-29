package pkg

import (
	"encoding/json"
	"math/rand"
	"strings"

	"RunningByeBye/internal/runc/consts"
	"RunningByeBye/internal/runc/models"

	sdkModels "github.com/Auto-CQUPT-Plan/CQUPT-Sports-SDK/models"
)

func ExtractTokenInfo(data json.RawMessage) string {
	var info sdkModels.TokenInfo
	if err := json.Unmarshal(data, &info); err != nil {
		return ""
	}
	return info.Data.Token
}

func ExtractUserMeta(data json.RawMessage) models.UserMetaInfo {
	var info sdkModels.UserInfo
	if err := json.Unmarshal(data, &info); err != nil {
		return models.UserMetaInfo{}
	}
	return models.UserMetaInfo{
		Username:  info.Data.Username,
		UnifyId:   info.Data.UnifyId,
		StudentNo: info.Data.StudentNo,
		Sex:       info.Data.Sex,
		Grade:     info.Data.Grade,
		DeptName:  info.Data.DeptName,
	}
}

func ExtractRecordNo(data json.RawMessage) string {
	var no string
	if err := json.Unmarshal(data, &no); err != nil {
		return ""
	}
	return no
}

func FieldCodeToName(code string) string {
	switch code {
	case consts.FIELD_FENG_HUA:
		return consts.SPORTS_FIELD_FENG_HUA
	case consts.FIELD_TAI_JI:
		return consts.SPORTS_FIELD_TAI_JI
	case consts.FIELD_NING_JING:
		return consts.SPORTS_FIELD_NING_JING
	default:
		return code
	}
}

func FieldNameToCode(name string) string {
	switch name {
	case consts.SPORTS_FIELD_FENG_HUA:
		return consts.FIELD_FENG_HUA
	case consts.SPORTS_FIELD_TAI_JI:
		return consts.FIELD_TAI_JI
	case consts.SPORTS_FIELD_NING_JING:
		return consts.FIELD_NING_JING
	default:
		return ""
	}
}

func ReadPointsFromRaw(data json.RawMessage) ([]models.LocationPointNode, error) {
	type rawPoint struct {
		Longitude string `json:"longitude"`
		Latitude  string `json:"latitude"`
	}
	type rawPayload struct {
		Data []rawPoint `json:"data"`
	}
	var payload rawPayload
	if err := json.Unmarshal(data, &payload); err != nil {
		return nil, err
	}
	points := make([]models.LocationPointNode, len(payload.Data))
	for i, p := range payload.Data {
		var lon, lat float64
		json.Unmarshal([]byte(p.Longitude), &lon)
		json.Unmarshal([]byte(p.Latitude), &lat)
		points[i] = models.LocationPointNode{
			Longitude: lon,
			Latitude:  lat,
		}
	}
	for i, j := 0, len(points)-1; i < j; i, j = i+1, j-1 {
		points[i], points[j] = points[j], points[i]
	}
	return points, nil
}

func ReadPointsFiles(baseDir, fieldID string) ([][]models.LocationPointNode, error) {
	return [][]models.LocationPointNode{}, nil
}

func PickRandomPointsFile(baseDir, fieldID string, allFiles []string) string {
	matched := make([]string, 0)
	for _, f := range allFiles {
		if strings.Contains(f, fieldID) {
			matched = append(matched, f)
		}
	}
	if len(matched) == 0 {
		return ""
	}
	return matched[rand.Intn(len(matched))]
}

func ToSportPointList(points []models.LocationPointNode, fieldCode string) []sdkModels.SportPointListNode {
	fieldName := FieldCodeToName(fieldCode)
	result := make([]sdkModels.SportPointListNode, len(points))
	for i, p := range points {
		result[i] = sdkModels.SportPointListNode{
			Longitude:   p.Longitude,
			Latitude:    p.Latitude,
			PlaceName:   fieldName,
			PlaceCode:   fieldCode,
			CollectTime: p.Timestamp,
			IsValid:     "1",
		}
	}
	return result
}
