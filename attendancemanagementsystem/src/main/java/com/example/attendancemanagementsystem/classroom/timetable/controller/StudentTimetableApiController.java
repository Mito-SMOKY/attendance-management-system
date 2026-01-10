
package com.example.attendancemanagementsystem.classroom.timetable.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendancemanagementsystem.classroom.timetable.dto.StudentTimetableDto;
import com.example.attendancemanagementsystem.classroom.timetable.service.StudentTimetableService;

@RestController // JSONを返す専用のコントローラー
@RequestMapping("/api") // フロントエンドのJSに合わせて "/api" から始まるURLにする
public class StudentTimetableApiController {

    private final StudentTimetableService timetableService;

    public StudentTimetableApiController(StudentTimetableService timetableService) {
        this.timetableService = timetableService;
    }

    
    //JSからの非同期リクエスト用API
    //フロントエンドのJSは "/api/timetabledata?date=YYYY-MM-DD" にアクセスしてきます
    @GetMapping("/timetabledata")
    public StudentTimetableDto getTimetableData(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("date") String dateStr) {
        
        // ログイン中のユーザーIDを取得
        String loginId = userDetails.getUsername();
        
        // 日付文字列をLocalDateに変換
        LocalDate date = LocalDate.parse(dateStr);

        // Serviceを使ってDBからデータを取得して返す
        return timetableService.getTimetableData(loginId, date);
    }
}