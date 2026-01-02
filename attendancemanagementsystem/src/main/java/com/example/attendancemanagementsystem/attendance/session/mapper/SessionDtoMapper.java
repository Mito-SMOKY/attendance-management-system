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

    public SessionDto toDto(SessionEntity session, EnrollmentsEntity enrollment, EntryLogEntity log, AttendanceEntity attendance) {
        
        SessionDto dto = new SessionDto();
        
        // 生徒情報のセット
        if (enrollment.getStudent() != null && enrollment.getStudent().getUsers() != null) {
            dto.setUserId(enrollment.getStudent().getUserId());
            dto.setStudentName(enrollment.getStudent().getUsers().getName());
        } else {
            dto.setUserId(null);
            dto.setStudentName("Unknown");
        }

        // 学年・クラス情報のセット
        String deptName = (enrollment.getDepartment() != null) ? enrollment.getDepartment().getClassName() : "";
        dto.setGradeClass(enrollment.getGrade() + "年" + deptName + "組"); // 例: "2年A組"

        // 授業情報のセット
        dto.setActualClassroomId(session.getActualClassroomId());
        dto.setSessionStatus(session.getSessionStatus());

        // 入室時間のセット（ログがあれば時間を、なければハイフンを表示）
        if (log != null) {
            dto.setEntryTime(log.getEntryTime().format(TIME_FMT));
        } else {
            dto.setEntryTime("--:--");
        }

        // 5出席ステータスのセット
        if (attendance != null) {
            dto.setStatusId(attendance.getStatus().getStatusId());
        } else {
            // ログがあれば出席(1)、なければ欠席(2)として仮表示
            dto.setStatusId(log != null ? 1 : 2);
        }

        return dto;
    }
}