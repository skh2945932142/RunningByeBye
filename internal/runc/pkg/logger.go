package pkg

import (
	"fmt"
	"io"
	"log"
	"os"
	"sync"
	"time"
)

type LogLevel int

const (
	LogLevelDebug LogLevel = iota
	LogLevelInfo
	LogLevelWarn
	LogLevelError
)

func (l LogLevel) String() string {
	switch l {
	case LogLevelDebug:
		return "DEBUG"
	case LogLevelInfo:
		return "INFO"
	case LogLevelWarn:
		return "WARN"
	case LogLevelError:
		return "ERROR"
	default:
		return "UNKNOWN"
	}
}

type Logger struct {
	mu       sync.Mutex
	level    LogLevel
	modular  string
	stdout   *log.Logger
	fileout  *log.Logger
	filePath string
}

func NewLogger(level LogLevel) *Logger {
	return &Logger{
		level:  level,
		stdout: log.New(os.Stdout, "", 0),
	}
}

func NewLoggerWithFile(level LogLevel, filePath string) *Logger {
	l := &Logger{
		level:    level,
		stdout:   log.New(os.Stdout, "", 0),
		filePath: filePath,
	}
	if filePath != "" {
		f, err := os.OpenFile(filePath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
		if err == nil {
			l.fileout = log.New(f, "", 0)
		}
	}
	return l
}

func (l *Logger) SetModular(m string) {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.modular = m
}

func (l *Logger) SetLevel(level LogLevel) {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.level = level
}

func (l *Logger) format(level LogLevel, format string, v ...interface{}) string {
	msg := fmt.Sprintf(format, v...)
	timestamp := time.Now().Format("2006-01-02 15:04:05.000")
	var modular string
	if l.modular != "" {
		modular = fmt.Sprintf(" [%s]", l.modular)
	}
	return fmt.Sprintf("[%s] [%s]%s %s", timestamp, level.String(), modular, msg)
}

func (l *Logger) output(level LogLevel, format string, v ...interface{}) {
	if level < l.level {
		return
	}
	line := l.format(level, format, v...)
	if l.stdout != nil {
		l.stdout.Println(line)
	}
	if l.fileout != nil {
		l.fileout.Println(line)
	}
}

func (l *Logger) Debug(format string, v ...interface{}) {
	l.output(LogLevelDebug, format, v...)
}

func (l *Logger) Info(format string, v ...interface{}) {
	l.output(LogLevelInfo, format, v...)
}

func (l *Logger) Warn(format string, v ...interface{}) {
	l.output(LogLevelWarn, format, v...)
}

func (l *Logger) Error(format string, v ...interface{}) {
	l.output(LogLevelError, format, v...)
}

func (l *Logger) Close() {
	if l.fileout != nil {
		if w, ok := l.fileout.Writer().(io.Closer); ok {
			w.Close()
		}
	}
}
