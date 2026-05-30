package cli

import (
	"fmt"

	"github.com/fatih/color"
)

var (
	StyleTitle   = color.New(color.FgHiCyan, color.Bold)
	StyleSuccess = color.New(color.FgHiGreen)
	StyleError   = color.New(color.FgHiRed, color.Bold)
	StyleWarn    = color.New(color.FgHiYellow)
	StyleInfo    = color.New(color.FgHiBlue)
	StyleDim     = color.New(color.FgWhite, color.Faint)
	StyleLabel   = color.New(color.FgCyan, color.Bold)
	StyleValue   = color.New(color.FgHiWhite)
	StyleAccent  = color.New(color.FgHiMagenta)
)

func PrintTitle(format string, v ...interface{}) {
	StyleTitle.Println(fmt.Sprintf(format, v...))
}

func PrintSuccess(format string, v ...interface{}) {
	StyleSuccess.Println("✓ " + fmt.Sprintf(format, v...))
}

func PrintError(format string, v ...interface{}) {
	StyleError.Println("✗ " + fmt.Sprintf(format, v...))
}

func PrintWarn(format string, v ...interface{}) {
	StyleWarn.Println("⚠ " + fmt.Sprintf(format, v...))
}

func PrintInfo(format string, v ...interface{}) {
	StyleInfo.Println("▸ " + fmt.Sprintf(format, v...))
}

func PrintField(label, value string) {
	StyleLabel.Printf("  %-16s ", label)
	StyleValue.Println(value)
}

func PrintDim(format string, v ...interface{}) {
	StyleDim.Println(fmt.Sprintf(format, v...))
}

func PrintKV(key string, value interface{}) {
	StyleLabel.Printf("  %s: ", key)
	StyleAccent.Println(value)
}

func PrintDivider() {
	StyleDim.Println("────────────────────────────────────────────")
}
