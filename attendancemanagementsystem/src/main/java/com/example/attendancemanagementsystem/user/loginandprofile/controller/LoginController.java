package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginForm() {
        return "login/login";
    }

    @GetMapping("/first-login")
    public String showFirstLoginForm() {
        return "login/first-login";
    }

    @GetMapping("/main-calendar")
    public String showMainCalendar() {
        return "student/main_calendar";
    }

    @GetMapping("/sessionMenu")
    public String showsessionMenu() {
        return "admin/sessionMenu";
    }

    @GetMapping("/timetable")
    public String showtimetable() {
        return "admin/timetable";
    }

    @GetMapping("/attendance-information")
    public String showattendanceinformation() {
        return "admin/attendance-information";
    }
    @GetMapping("/classList")
    public String showclassList() {
        return "admin/classList";
    }
    @GetMapping("/classInfo")
    public String showclassInfo() {
        return "admin/classInfo";
    }
    @GetMapping("/studentList")
    public String showstudentList() {
        return "admin/studentList";
    }
    @GetMapping("/studentInfo")
    public String showstudentInfo() {
        return "admin/studentInfo";
    }
    @GetMapping("/home")
    public String showadminHome() {
        return "admin/home";
    }
    @GetMapping("/timeTableList2")
    public String showtimeTableList2() {
        return "admin/timeTableList2";
    }
    @GetMapping("/subjectList")
    public String showsubjectList() {
        return "admin/subjectList";
    }
    @GetMapping("/subjectInfo")
    public String showsubjectInfo() {
        return "admin/subjectInfo";
    }
    @GetMapping("/timeTableEdit")
    public String showtimeTableEdit() {
        return "admin/timeTableEdit";
    }
    @GetMapping("/mdClassroom")
    public String showmdClassroom() {
        return "admin/mdClassroom";
    }
    @GetMapping("/mdSubject")
    public String showmdSubject() {
        return "admin/mdSubject";
    }
    @GetMapping("/nfc_writer")
    public String shownfc_riter() {
        return "nfc/nfc_writer";
    }
    @GetMapping("/mdList")
    public String showmdList() {
        return "admin/mdList";
    }
    @GetMapping("/mdSubjectInformation")
    public String showmdSubjectInformation() {
        return "admin/mdSubjectInformation";
    }
}
//     @GetMapping("/main_calendar")
//     public String showMainCalendar() {
//         return "student/main_calendar";
//     }
// }
