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

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // 既存メソッド: 生徒1人分の行データを作成
    public SessionDto toDto(SessionEntity session, EnrollmentsEntity enrollment, EntryLogEntity log, AttendanceEntity attendance) {
        
        SessionDto dto = new SessionDto();
        dto.setSessionId(session.getSessionId());

        // 科目名
        if (session.getSubject() != null) {
            dto.setSubjectName(session.getSubject().getSubjectName());
        } else {
            dto.setSubjectName("未設定");
        }

        // 生徒情報
        if (enrollment.getStudent() != null && enrollment.getStudent().getUsers() != null) {
            dto.setUserId(enrollment.getStudent().getUserId());
            dto.setStudentName(enrollment.getStudent().getUsers().getName());
        } else {
            dto.setUserId(null);
            dto.setStudentName("Unknown");
        }

        // 学年・クラス
        String deptName = (enrollment.getDepartment() != null) ? enrollment.getDepartment().getClassName() : "";
        dto.setGradeClass(enrollment.getGrade() + "年" + deptName); 

        // 教室情報
        mapClassroomInfo(session, dto);
        
        dto.setSessionFlag(session.getSessionFlag());
        
        // 開始時間
        if (session.getStartTime() != null) {
            dto.setStartTime(session.getStartTime().format(TIME_FMT));
        }

        // 入室時間ログ
        if (log != null) {
            dto.setEntryTime(log.getEntryTime().format(TIME_FMT));
        } else {
            dto.setEntryTime("--:--");
        }

        // 出席ステータス
        if (attendance != null && attendance.getStatus() != null) {
            // ステータスID (1:出席, 2:欠席など) をセット
            dto.setStatusId(attendance.getStatus().getStatusId());
        } else {
            dto.setStatusId(null); 
        }
        
        dto.setSessionFlag(session.getSessionFlag());

        return dto;
    }

    // ★追加: セッションヘッダー表示用（生徒情報なしで変換）
    public SessionDto toDto(SessionEntity session) {
        SessionDto dto = new SessionDto();
        dto.setSessionId(session.getSessionId());

        if (session.getSubject() != null) {
            dto.setSubjectName(session.getSubject().getSubjectName());
        } else {
            dto.setSubjectName("未設定");
        }
        
        // 簡易的な学年表示
        if (session.getTargetGrade() != null) {
            dto.setGradeClass(session.getTargetGrade() + "年");
        }

        mapClassroomInfo(session, dto);
        
        dto.setSessionFlag(session.getSessionFlag());
        
        if (session.getStartTime() != null) {
            dto.setStartTime(session.getStartTime().format(TIME_FMT));
        }
        
        return dto;
    }

    // 共通処理切り出し
    private void mapClassroomInfo(SessionEntity session, SessionDto dto) {
        if (session.getClassroom() != null) {
            dto.setActualClassroomId(session.getClassroom().getClassroomId());
            dto.setClassroomName(session.getClassroom().getClassroomName());
        }
    }
}