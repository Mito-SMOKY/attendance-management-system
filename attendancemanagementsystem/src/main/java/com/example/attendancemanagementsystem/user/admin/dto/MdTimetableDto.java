package com.example.attendancemanagementsystem.user.admin.dto;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.example.attendancemanagementsystem.user.admin.dto.MdTimetableDto.Cell;

public class MdTimetableDto {

    //指定期間フィールド
    private LocalDate startDate;
    private LocalDate endDate;

    // 検索・特定用フィールド
    private Integer courseId;
    private Integer targetGrade; 
    private String className; 
    private Integer departmentId;
    private String majorName;
    private String courseName;

    // スケジュールデータ
    private Map<Integer, Map<String, Cell>> scheduleMap = new HashMap<Integer, Map<String, Cell>>() {
        @Override
        public Map<String, Cell> get(Object key) {
            if (!super.containsKey(key) && key instanceof Integer) {
                super.put((Integer) key, new HashMap<>());
            }
            return super.get(key);
        }
    };

    public MdTimetableDto() {}

    // --- 内部クラス ---
    public static class Cell {
        private Integer subjectId;
        private Integer classroomId;
        private Integer userId; 
        
        // Getter/Setter
        public Integer getSubjectId() { 
            return subjectId; 
        }

        public void setSubjectId(Integer s) { 
            this.subjectId = s; 
        }

        public Integer getClassroomId() { 
            return classroomId; 
        }

        public void setClassroomId(Integer c) { 
            this.classroomId = c; 
        }

        public Integer getUserId() { 
            return userId; 
        }

        public void setUserId(Integer u) { 
            this.userId = u; 
        }
    }

    // --- Getter/Setter ---
    public LocalDate getStartDate() { 
        return startDate; 
    }

    public void setStartDate(LocalDate s) { 
        this.startDate = s; 
    }

    public LocalDate getEndDate() { 
        return endDate; 
    }

    public void setEndDate(LocalDate e) { 
        this.endDate = e; 
    }
    
    public Integer getCourseId() { 
        return courseId; 
    }

    public void setCourseId(Integer c) { 
        this.courseId = c; 
    }

    public Integer getTargetGrade() { 
        return targetGrade; 
    }

    public void setTargetGrade(Integer g) { 
        this.targetGrade = g; 
    }

    public String getClassName() { 
        return className; 
    }

    public void setClassName(String c) { 
        this.className = c; 
    }
    
    public Integer getDepartmentId() { 
        return departmentId; 
    }

    public void setDepartmentId(Integer d) { 
        this.departmentId = d; 
    }

    public String getMajorName() { 
        return majorName; 
    }

    public void setMajorName(String majorName) { 
        this.majorName = majorName; 
    }

    public String getCourseName() { 
        return courseName; 
    }

    public void setCourseName(String courseName) { 
        this.courseName = courseName; 
    }
    
    public Map<Integer, Map<String, Cell>> getScheduleMap() { 
        return scheduleMap; 
    }

    public void setScheduleMap(Map<Integer, Map<String, Cell>> m) { 
        this.scheduleMap = m; 
    }
}