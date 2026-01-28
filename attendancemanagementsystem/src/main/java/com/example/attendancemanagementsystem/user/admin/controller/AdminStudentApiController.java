// package com.example.attendancemanagementsystem.user.admin.controller;

// import java.util.Map;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.ResponseBody;

// import com.example.attendancemanagementsystem.user.admin.service.AdminStudentListService;
// import com.example.attendancemanagementsystem.user.admin.service.RequestService;

// @RestController
// @RequestMapping("/admin/api")
// public class AdminStudentApiController {

//     @Autowired
//     private AdminStudentListService adminStudentService;

//     // 生徒一覧取得API
//     @GetMapping("/api/students")
//     @ResponseBody
//     public Map<String, Object> getStudents(
//             @RequestParam(name = "page", defaultValue = "0") int page,
//             @RequestParam(name = "size", defaultValue = "10") int size,
//             @RequestParam(name = "keyword", required = false) String keyword,
//             @RequestParam(name = "departmentId", required = false) Integer departmentId,
//             @RequestParam(name = "grade", required = false) Integer grade,
//             @RequestParam(name = "courseId", required = false) Integer courseId
//     ) {
//         return adminStudentService.searchStudents(
//                 page, size, keyword, departmentId, grade, courseId
//         );
//     }

//     // フィルター選択肢取得API
//     @GetMapping("/api/search-options")
//     @ResponseBody
//     public Map<String, Object> getSearchOptions() {
//         return adminStudentService.getFilterOptions();
//     }
// }

package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendancemanagementsystem.user.admin.service.AdminStudentListService;
import com.example.attendancemanagementsystem.user.admin.service.RequestService;

@RestController
@RequestMapping("/admin/api/students")
public class AdminStudentApiController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private AdminStudentListService adminStudentService;

    // API用: 申請対象生徒の検索・取得
    @GetMapping("/request-target")
    public Page<Map<String, Object>> getRequestTargets(
            @RequestParam(name = "mode", defaultValue = "select") String mode,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Serviceの新しいメソッドを呼び出す（isPendingフラグ付き）
        return requestService.searchStudentsForSelection(mode, keyword, pageable);
    }

    // 生徒一覧取得API
    @GetMapping("/students")
    public Map<String, Object> getStudents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "departmentId", required = false) Integer departmentId,
            @RequestParam(name = "grade", required = false) Integer grade,
            @RequestParam(name = "courseId", required = false) Integer courseId
            ) {

        return adminStudentService.searchStudents(page, size, keyword, departmentId, grade, courseId);
    }

    // フィルター選択肢取得API
    @GetMapping("/search-options")
    public Map<String, Object> getSearchOptions() {
        return adminStudentService.getFilterOptions();
    }
}