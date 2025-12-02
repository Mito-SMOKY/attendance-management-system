package com.example.attendancemanagementsystem;

import com.example.attendancemanagementsystem.DummyModel.AttendanceData;
import com.example.attendancemanagementsystem.DummyModel.DailyAttendance;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 月次出席詳細画面用のダミーデータを生成するクラス
 */
public class DummyDataGenerator {

    /**
     * 月次出席詳細画面用のダミーデータを生成する
     * 💡 ER図の構造をシミュレーションしてデータを返す
     */
    public static AttendanceData createMonthlyAttendanceDummyData(String fullMonth) {
        
        // 月によって異なるダミーデータを返すロジック (シミュレーション)
        List<DailyAttendance> dailyList;
        int absentClasses;
        double attendanceRate;

        if ("2024/11".equals(fullMonth)) {
            dailyList = Arrays.asList(
                new DailyAttendance("11/1(金)", Arrays.asList("○", "○", "○", "○"), "231"),
                new DailyAttendance("11/4(月)", Arrays.asList("○", "○", "○", "○"), "231"),
                new DailyAttendance("11/5(火)", Arrays.asList("-", "-", "○", "○"), "231"),
                new DailyAttendance("11/6(水)", Arrays.asList("✕", "✕", "-", "-"), "231"), // 欠席
                new DailyAttendance("11/7(木)", Arrays.asList("-", "-", "○", "○"), "231"),
                new DailyAttendance("11/8(金)", Arrays.asList("○", "○", "○", "○"), "231"),
                new DailyAttendance("11/11(月)", Arrays.asList("○", "○", "○", "○"), "231")
            );
            absentClasses = 2; // 11月は2コマ欠席
            attendanceRate = 0.85;
        } else if ("2024/10".equals(fullMonth)) {
            dailyList = Arrays.asList(
                new DailyAttendance("10/1(火)", Arrays.asList("○", "○", "○", "○"), "231"),
                new DailyAttendance("10/2(水)", Arrays.asList("○", "遅", "○", "○"), "231"), // 遅刻
                new DailyAttendance("10/3(木)", Arrays.asList("-", "-", "○", "○"), "231")
            );
            absentClasses = 0; // 10月は欠席なし
            attendanceRate = 0.95;
        } else {
            // その他の月はデフォルト
            dailyList = List.of(); 
            absentClasses = 0;
            attendanceRate = 1.0;
        }

        // --- メインデータ（サマリーと詳細） ---
        return new AttendanceData(
            "システム構築", 
            "231", // Classroomテーブルから
            "平松 浩幸", // SubjectFaculty/Usersテーブルから
            24, // 必要コマ数 (Subjectテーブルから)
            attendanceRate, // 現在の出席率
            3, // 欠席可能日数 (仮の値)
            19, // 総日数
            19, // 出席コマ数 (出席率に応じて調整が必要だが、ダミーなので固定)
            absentClasses, // 欠席コマ数
            0, // 遅刻コマ数
            1, // 公欠コマ数
            0, // 公欠候補コマ数
            dailyList
        );
    }
    
    /**
     * 💡 年のプルダウンメニュー用のダミーデータを生成する
     */
    public static List<String> createYearDropdownDummyData() {
        // 例: 2023年から2025年
        return Arrays.asList("2025", "2024", "2023");
    }

    /**
     * 💡 月のプルダウンメニュー用のダミーデータを生成する
     */
    public static List<String> createMonthDropdownDummyData() {
        // 1月 ('01') から 12月 ('12') までを生成
        return IntStream.rangeClosed(1, 12)
                        .mapToObj(i -> String.format("%02d", i))
                        .collect(Collectors.toList());
    }
}