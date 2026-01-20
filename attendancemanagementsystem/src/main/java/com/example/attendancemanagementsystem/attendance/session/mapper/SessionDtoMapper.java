package com.example.attendancemanagementsystem.attendance.session.mapper;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.example.attendancemanagementsystem.attendance.session.dto.SessionDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;

@Component
public class SessionDtoMapper {

    // 時刻フォーマット定義
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    
    // 出席一覧表示用
    public SessionDto toDto(SessionEntity session, EnrollmentsEntity enrollment, EntryLogEntity log, AttendanceEntity attendance) {
        SessionDto dto = new SessionDto();
        
        // session自体がnullの場合は空を返して防ぐ
        if (session == null) {
            return dto; 
        }
        dto.setSessionId(session.getSessionId());

        // 科目名
        if (session.getSubject() != null) {
            dto.setSubjectName(session.getSubject().getSubjectName());
        } else {
            dto.setSubjectName("未設定");
        }

        // 生徒情報
        if (enrollment != null && enrollment.getStudent() != null && enrollment.getStudent().getUsers() != null) {
            dto.setUserId(enrollment.getStudent().getUserId());
            dto.setStudentName(enrollment.getStudent().getUser().getName());
        } else {
            dto.setUserId(null);
            dto.setStudentName("Unknown");
        }

        // 学年・クラス
        String gradeStr = (enrollment != null && enrollment.getGrade() != null) ? enrollment.getGrade().toString() : "?";
        String deptName = (enrollment != null && enrollment.getDepartment() != null) ? enrollment.getDepartment().getClassName() : "";
        dto.setGradeClass(gradeStr + "年" + deptName); 

        // 教室情報
        mapClassroomInfo(session, dto);
        
        dto.setSessionFlag(session.getSessionFlag());
        
        // 開始時間
        if (session.getStartTime() != null) {
            dto.setStartTime(session.getStartTime().format(TIME_FMT));
        }

        // 入室時間
        if (log != null && log.getEntryTime() != null) {
            dto.setEntryTime(log.getEntryTime().format(TIME_FMT));
        } else {
            dto.setEntryTime("--:--");
        }

        // 出席ステータス
        if (attendance != null && attendance.getStatus() != null) {
            dto.setStatusId(attendance.getStatus().getStatusId());
        } else {
            dto.setStatusId(null); 
        }
        
        return dto;
    }

    // セッション情報表示用
    public SessionDto toDto(SessionEntity session) {
        if (session == null) return new SessionDto();

        SessionDto dto = new SessionDto();
        dto.setSessionId(session.getSessionId());

        // 科目名
        if (session.getSubject() != null) {
            dto.setSubjectName(session.getSubject().getSubjectName());
        } else {
            dto.setSubjectName("未設定");
        }
        
        // 学年表示
        if (session.getTargetGrade() != null) {
            dto.setGradeClass(session.getTargetGrade() + "年");
        }

        
        // クラス表示
        if (session.getDepartment() != null) {
        dto.setClassName(session.getDepartment().getClassName());
        }

        // 教室情報
        mapClassroomInfo(session, dto);
        
        dto.setSessionFlag(session.getSessionFlag());
        
        // 終了時間
        if (session.getStartTime() != null) {
            dto.setStartTime(session.getStartTime().format(TIME_FMT));
        }
        
        return dto;
    }

    // 教室情報をDTOにマッピング
    private void mapClassroomInfo(SessionEntity session, SessionDto dto) {
        
        // 教室名とID
        if (session.getClassroom() != null) {
            dto.setActualClassroomId(session.getClassroom().getClassroomId());
            dto.setClassroomName(session.getClassroom().getClassroomName());
        }
    }
}