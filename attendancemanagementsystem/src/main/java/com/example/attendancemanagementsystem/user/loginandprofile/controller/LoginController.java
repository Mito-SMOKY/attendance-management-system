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
    @GetMapping("/studentClassInfo")
    public String showstudentClassInfo() {
        return "admin/studentClassInfo";
    }
    @GetMapping("/subjectInfo")
    public String showsubjectInfo() {
        return "admin/subjectInfo";
    }
    @GetMapping("/subjectList")
    public String showsubjectList() {
        return "admin/subjectList";
    }
}
//     @GetMapping("/main_calendar")
//     public String showMainCalendar() {
//         return "student/main_calendar";
//     }
// }
