package com.example.attendancemanagementsystem.common.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.dto.AttendanceMetricsDto;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;

@Service
@Transactional(readOnly = true)
public class AttendanceCalculationService {

    private final AttendanceRepository attendanceRepository;
    private final TimetableRepository timetableRepository;

    public AttendanceCalculationService(
            AttendanceRepository attendanceRepository,
            TimetableRepository timetableRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.timetableRepository = timetableRepository;
    }

    // 出席率と残り欠席可能日数の計算
    public AttendanceMetricsDto calculateAttendanceMetrics(Integer userId, Integer subjectId) {
        // 1. 科目の全授業数を取得 (Timetableからカウント)
        int totalClasses = timetableRepository.countBySubjectId(subjectId);
        
        // 2. 欠席可能上限の計算 (全授業数の 1/3)
        int maxAbsenceLimit = totalClasses / 3;

        // 3. 各ステータスの回数を取得
        int totalPresent    = attendanceRepository.countByStatusTotal(userId, subjectId, 1); // 出席
        int totalAbsent     = attendanceRepository.countByStatusTotal(userId, subjectId, 2); // 欠席
        int totalLate       = attendanceRepository.countByStatusTotal(userId, subjectId, 3); // 遅刻
        int totalOfficial   = attendanceRepository.countByStatusTotal(userId, subjectId, 4); // 公欠
        int totalEarlyLeave = attendanceRepository.countByStatusTotal(userId, subjectId, 7); // 早退

        // 全授業回数 (現在までの実施回数)
        int totalConducted = totalPresent + totalAbsent + totalLate + totalOfficial + totalEarlyLeave;

        // 4. 出席率計算: (出席 + 公欠) ÷ 実施回数
        double rate = 0.0;
        if (totalConducted > 0) {
            rate = (double) (totalPresent + totalOfficial) / totalConducted;
        }

        // 5. 残り欠席可能日数: 計算した上限 - 欠席数
        int remaining = maxAbsenceLimit - totalAbsent;
        if (remaining < 0) {
            remaining = 0;
        }

        return new AttendanceMetricsDto(rate, remaining);
    }
}