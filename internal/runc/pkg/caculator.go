package pkg

import (
	"math"
	"math/rand"
	"time"

	"RunningByeBye/internal/runc/models"
)

func HaversineDistance(p1, p2 models.LocationPointNode) float64 {
	const R = 6371.0
	dLat := (p2.Latitude - p1.Latitude) * (math.Pi / 180)
	dLon := (p2.Longitude - p1.Longitude) * (math.Pi / 180)
	a := math.Sin(dLat/2)*math.Sin(dLat/2) +
		math.Cos(p1.Latitude*(math.Pi/180))*math.Cos(p2.Latitude*(math.Pi/180))*
			math.Sin(dLon/2)*math.Sin(dLon/2)
	c := 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))
	return R * c
}

func GeneratePoints(
	base []models.LocationPointNode,
	settings models.TaskSettingNode,
	startTime time.Time,
) []models.LocationPointNode {
	generated := make([]models.LocationPointNode, 0, len(base))

	for _, bp := range base {
		lat := bp.Latitude
		lon := bp.Longitude
		lat += (rand.Float64() - 0.5) * 0.00001
		lon += (rand.Float64() - 0.5) * 0.00001
		generated = append(generated, models.LocationPointNode{
			Longitude: lon,
			Latitude:  lat,
			FieldID:   bp.FieldID,
		})
	}

	totalDistance := 0.0
	for i := 1; i < len(generated); i++ {
		totalDistance += HaversineDistance(generated[i-1], generated[i])
	}

	targetPace := settings.TargetPace
	if targetPace <= 0 {
		targetPace = 5.0 + rand.Float64()*2.0
	}
	totalDuration := totalDistance * targetPace * 60

	interval := settings.TargetInterval
	if interval <= 0 {
		interval = totalDuration / float64(len(generated)-1)
		if interval < 0.8 {
			interval = 0.8 + rand.Float64()*0.4
		}
	}

	st := startTime
	if st.IsZero() {
		st = time.Now()
	}

	for i := range generated {
		generated[i].Timestamp = st.Add(time.Duration(float64(i)*interval) * time.Second).Format("2006-01-02 15:04:05")
	}

	return generated
}

func FormatFloat(f float64, precision int) float64 {
	pow := math.Pow(10, float64(precision))
	return math.Round(f*pow) / pow
}

func CalcMileage(points []models.LocationPointNode) float64 {
	if len(points) < 2 {
		return 0
	}
	total := 0.0
	for i := 1; i < len(points); i++ {
		total += HaversineDistance(points[i-1], points[i])
	}
	return FormatFloat(total, 3)
}
