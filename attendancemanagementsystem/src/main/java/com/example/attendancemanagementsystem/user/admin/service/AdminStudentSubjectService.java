package com.example.attendancemanagementsystem.user.admin.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity; 
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository; 
import com.example.attendancemanagementsystem.user.admin.dto.StudentSubjectDetailDto;
import com.example.attendancemanagementsystem.user.admin.dto.StudentSubjectDetailDto.SubjectDailyRecord;

@Service
public class AdminStudentSubjectService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UsersRepository usersRepository;

    // 生徒の科目詳細情報を取得
    @Transactional(readOnly = true)
    public StudentSubjectDetailDto getSubjectDetail(Integer studentId, Integer subjectId) {
        StudentSubjectDetailDto dto = new StudentSubjectDetailDto();

        // 生徒情報のセット
        StudentEntity student = studentRepository.findById(studentId).orElse(null);
        if (student != null) {
            dto.setStudentId(studentId);
            if (student.getUsers() != null) {
                dto.setStudentName(student.getUsers().getName());
            }
        }
        dto.setSubjectId(subjectId);

        // 出席データの取得
        List<AttendanceEntity> list = attendanceRepository.findByStudentAndSubject(studentId, subjectId);

        if (list.isEmpty()) {
            return dto;
        }

        // 教科情報のセット 
        AttendanceEntity first = list.get(0);
        if (first.getSession() != null && first.getSession().getSubject() != null) {
            dto.setSubjectName(first.getSession().getSubject().getSubjectName());
            
            // 教室名
            if (first.getSession().getClassroom() != null) {
                dto.setClassroomName(first.getSession().getClassroom().getClassroomName());
            } else {
                dto.setClassroomName("-");
            }

            // 教員名の取得
            Integer teacherId = first.getSession().getUserId();
            if (teacherId != null) {
                UsersEntity teacher = usersRepository.findById(teacherId).orElse(null);
                if (teacher != null) {
                    dto.setTeacherName(teacher.getName());
                } else {
                    dto.setTeacherName("不明");
                }
            } else {
                dto.setTeacherName("-");
            }
        }

        // 集計と明細行の作成
        Map<String, SubjectDailyRecord> recordMap = new LinkedHashMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd(E)", Locale.JAPANESE);

        // 出席データのループ処理
        for (AttendanceEntity att : list) {

            // ステータス名の取得
            String statusName = (att.getStatus() != null) ? att.getStatus().getStatusName() : "";

            // 出席カウント集計 
            switch (statusName) {
                case "出席":
                    dto.setAttendanceCount(dto.getAttendanceCount() + 1);
                    break;
                case "欠席":
                    dto.setAbsenceCount(dto.getAbsenceCount() + 1);
                    break;
                case "遅刻":
                    dto.setLateCount(dto.getLateCount() + 1);
                    break;
                case "早退":
                    dto.setEarlyLeaveCount(dto.getEarlyLeaveCount() + 1);
                    break;
                case "出席停止":
                    dto.setSuspensionCount(dto.getSuspensionCount() + 1);
                    break;
                case "公欠":
                case "公欠候補": 
                    dto.setPublicAbsenceCount(dto.getPublicAbsenceCount() + 1);
                    break;
            }

            // 明細行作成 
            if (att.getSession() != null) {
                String dateStr = att.getSession().getSessionDate().format(dtf);
                
                // 同じ日付の行がなければ作成、あれば取得
                SubjectDailyRecord record = recordMap.computeIfAbsent(dateStr, k -> {
                    SubjectDailyRecord r = new SubjectDailyRecord();
                    r.setDateStr(k);
                    return r;
                });

                // 時限に応じてマークをセット
                if (att.getSession().getTimeSlot() != null) {
                    Integer slotId = att.getSession().getTimeSlot().getSlotId();

                    // 公欠候補などに対応した変換メソッドを使う
                    String mark = convertToMark(statusName);

                    // 時限に応じてセット
                    if (slotId != null) {
                        switch (slotId) {
                            case 1: record.setPeriod1(mark); break;
                            case 2: record.setPeriod2(mark); break;
                            case 3: record.setPeriod3(mark); break;
                            case 4: record.setPeriod4(mark); break;
                        }
                    }
                }
            }
        }

        // 明細行をDTOにセット
        dto.setDailyRecords(new ArrayList<>(recordMap.values()));

        // 最終計算 
        int total = list.size();
        dto.setTotalClasses(total);

        // 完了数計算に公欠と出席停止を含める
        int effectiveAttendance = dto.getAttendanceCount() 
                                + dto.getLateCount() 
                                + dto.getEarlyLeaveCount() 
                                + dto.getPublicAbsenceCount()
                                + dto.getSuspensionCount(); 

        // 出席率計算
        dto.setCompletedCount(effectiveAttendance + " / " + total);
        double rate = (total > 0) ? (double) effectiveAttendance / total * 100 : 0.0;
        dto.setAttendanceRate(String.format("%.1f%%", rate));
        return dto;
    }

    // 全ステータスに対応したマーク変換
    private String convertToMark(String status) {
        if ("出席".equals(status)) return "○";
        if ("欠席".equals(status)) return "×";
        if ("遅刻".equals(status)) return "△";
        if ("早退".equals(status)) return "早";
        if ("公欠".equals(status)) return "公";
        if ("公欠候補".equals(status)) return "候"; 
        if ("出席停止".equals(status)) return "停";
        return "-";
    }
}