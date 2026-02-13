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
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity; 
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
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

    @Autowired
    private EnrollmentsRepository enrollmentsRepository;

    // 生徒の科目詳細情報を取得
    @Transactional(readOnly = true)
    public StudentSubjectDetailDto getSubjectDetail(Integer studentId, Integer subjectId) {
        StudentSubjectDetailDto dto = new StudentSubjectDetailDto();

        // 生徒情報のセット
        StudentEntity student = studentRepository.findById(studentId).orElse(null);
        if (student != null) {
            dto.setStudentId(studentId);
            if (student.getUser() != null) {
                dto.setStudentName(student.getUser().getName());

                //在籍情報から学年を取得してセット
                EnrollmentsEntity enrollment = enrollmentsRepository.findByUserAndIsActiveTrue(student.getUser()).orElse(null);
                if (enrollment != null) {
                    dto.setGrade(enrollment.getGrade());
                    if (dto.getDepartmentId() == null && enrollment.getDepartment() != null) {
                        dto.setDepartmentId(enrollment.getDepartment().getDepartmentId());
                    }
                }
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

            // セッションから学科IDを取得してセット
            if (first.getSession().getDepartment() != null) {
                dto.setDepartmentId(first.getSession().getDepartment().getDepartmentId());
            }
            
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

        // 各ステータスのカウント値を取得
        int totalPresent    = dto.getAttendanceCount();       // 出席
        int totalAbsent     = dto.getAbsenceCount();          // 欠席
        int totalLate       = dto.getLateCount();             // 遅刻
        int totalOfficial   = dto.getPublicAbsenceCount();    // 公欠(公欠候補含む)
        int totalEarlyLeave = dto.getEarlyLeaveCount();       // 早退

        // 実施回数の計算
        int totalConducted = totalPresent + totalAbsent + totalLate + totalOfficial + totalEarlyLeave;

        // 出席率の計算
        int numerator = totalPresent + totalOfficial;

        double rate = 0.0;
        if (totalConducted > 0) {
            rate = (double) numerator / totalConducted * 100;
        }

        // 授業総数は参考情報としてセッ
        dto.setTotalClasses(list.size());

        // 完了数表示: 「出席+公欠 / 実施回数」の形式に変更
        dto.setCompletedCount(numerator + " / " + totalConducted);

        // 出席率セット
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