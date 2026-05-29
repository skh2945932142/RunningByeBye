package pkg

import (
	"math"
	"testing"
	"time"

	"RunningByeBye/internal/runc/models"
)

func TestHaversineDistance_SamePoint(t *testing.T) {
	p := models.LocationPointNode{Latitude: 29.53286, Longitude: 106.60525}
	d := HaversineDistance(p, p)
	if d != 0 {
		t.Errorf("distance between same point should be 0, got %f", d)
	}
}

func TestHaversineDistance_Positive(t *testing.T) {
	p1 := models.LocationPointNode{Latitude: 29.53286, Longitude: 106.60525}
	p2 := models.LocationPointNode{Latitude: 29.53320, Longitude: 106.60550}
	d := HaversineDistance(p1, p2)
	if d <= 0 {
		t.Errorf("distance between different points should be > 0, got %f", d)
	}
}

func TestHaversineDistance_Symmetric(t *testing.T) {
	p1 := models.LocationPointNode{Latitude: 29.53286, Longitude: 106.60525}
	p2 := models.LocationPointNode{Latitude: 29.54000, Longitude: 106.61000}
	d1 := HaversineDistance(p1, p2)
	d2 := HaversineDistance(p2, p1)
	if math.Abs(d1-d2) > 1e-10 {
		t.Errorf("haversine should be symmetric: %f != %f", d1, d2)
	}
}

func TestHaversineDistance_CrossEquator(t *testing.T) {
	p1 := models.LocationPointNode{Latitude: 0, Longitude: 0}
	p2 := models.LocationPointNode{Latitude: 1, Longitude: 0}
	d := HaversineDistance(p1, p2)
	if d <= 0 {
		t.Errorf("distance crossing equator should be > 0, got %f", d)
	}
}

func TestFormatFloat(t *testing.T) {
	tests := []struct {
		input     float64
		precision int
		expected  float64
	}{
		{3.14159, 2, 3.14},
		{3.14159, 3, 3.142},
		{3.14159, 0, 3.0},
		{1.0, 2, 1.0},
		{0.0, 3, 0.0},
		{1.5, 0, 2.0},
		{-1.234, 2, -1.23},
	}
	for _, tc := range tests {
		result := FormatFloat(tc.input, tc.precision)
		if result != tc.expected {
			t.Errorf("FormatFloat(%f, %d) = %f, want %f", tc.input, tc.precision, result, tc.expected)
		}
	}
}

func TestCalcMileage_Empty(t *testing.T) {
	if m := CalcMileage(nil); m != 0 {
		t.Errorf("expected 0 for nil, got %f", m)
	}
}

func TestCalcMileage_SinglePoint(t *testing.T) {
	if m := CalcMileage([]models.LocationPointNode{{}}); m != 0 {
		t.Errorf("expected 0 for single point, got %f", m)
	}
}

func TestCalcMileage_SamePoints(t *testing.T) {
	p := models.LocationPointNode{Latitude: 29.53286, Longitude: 106.60525}
	if m := CalcMileage([]models.LocationPointNode{p, p}); m != 0 {
		t.Errorf("expected 0 for same points, got %f", m)
	}
}

func TestCalcMileage_TwoPoints(t *testing.T) {
	p1 := models.LocationPointNode{Latitude: 29.53286, Longitude: 106.60525}
	p2 := models.LocationPointNode{Latitude: 29.53320, Longitude: 106.60550}
	m := CalcMileage([]models.LocationPointNode{p1, p2})
	expected := FormatFloat(HaversineDistance(p1, p2), 3)
	if m != expected {
		t.Errorf("CalcMileage = %f, want %f", m, expected)
	}
}

func TestCalcMileage_ThreePoints(t *testing.T) {
	p1 := models.LocationPointNode{Latitude: 29.53286, Longitude: 106.60525}
	p2 := models.LocationPointNode{Latitude: 29.53320, Longitude: 106.60550}
	p3 := models.LocationPointNode{Latitude: 29.53350, Longitude: 106.60580}
	m := CalcMileage([]models.LocationPointNode{p1, p2, p3})
	d12 := HaversineDistance(p1, p2)
	d23 := HaversineDistance(p2, p3)
	expected := FormatFloat(d12+d23, 3)
	if m != expected {
		t.Errorf("CalcMileage = %f, want %f", m, expected)
	}
}

func TestGeneratePoints_WithPace(t *testing.T) {
	base := []models.LocationPointNode{
		{Latitude: 29.53286, Longitude: 106.60525},
		{Latitude: 29.53320, Longitude: 106.60550},
		{Latitude: 29.53350, Longitude: 106.60580},
	}
	settings := models.TaskSettingNode{
		TargetPace:     6.0,
		TargetInterval: 0,
		TargetFieldID:  "T1001",
	}
	points := GeneratePoints(base, settings, time.Time{})
	if len(points) != len(base) {
		t.Errorf("GeneratePoints length = %d, want %d", len(points), len(base))
	}
	for i, p := range points {
		if p.Timestamp == "" {
			t.Errorf("point %d has empty timestamp", i)
		}
	}
}

func TestGeneratePoints_WithCustomInterval(t *testing.T) {
	base := []models.LocationPointNode{
		{Latitude: 29.53286, Longitude: 106.60525},
		{Latitude: 29.53320, Longitude: 106.60550},
	}
	settings := models.TaskSettingNode{
		TargetPace:     0,
		TargetInterval: 2.0,
		TargetFieldID:  "T1001",
	}
	points := GeneratePoints(base, settings, time.Time{})
	if len(points) != len(base) {
		t.Errorf("GeneratePoints length = %d, want %d", len(points), len(base))
	}
}
