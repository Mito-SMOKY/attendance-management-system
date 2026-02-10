package com.example.attendancemanagementsystem.user.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.service.RequestService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/request")
public class RequestController {

    @Autowired
    @Qualifier("adminRequestService") // Admin側のServiceを明示的に指定
    private RequestService requestService;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UsersRepository usersRepository;

    /**
     * メニュー画面表示
     */
    @GetMapping("/menu")
    public String showRequestMenu() {
        return "admin/request/requestMain";
    }
    

    /**
     * 生徒選択画面表示
     * (これはRequestController固有の機能として残します)
     */
    @GetMapping("/select")
    public String showStudentSelect(@RequestParam("mode") String mode, Model model) {
        String pageTitle = "生徒選択";
        
        switch (mode) {
            case "delete":
                pageTitle = "アカウント削除 - 生徒選択";
                break;
            case "status":
                pageTitle = "ステータス変更 - 生徒選択";
                break;
            case "course":
                pageTitle = "学科・コース変更 - 生徒選択";
                break;
            default:
                pageTitle = "生徒選択";
                break;
        }

        model.addAttribute("mode", mode);
        model.addAttribute("pageTitle", pageTitle);
        
        return "admin/request/studentSelect";
    }

    // 注意: confirmDelete, confirmStatus などのメソッドは
    // StudentDeleteRequestController 等に存在するため、ここには記述しません。
    // (重複すると起動エラーになります)


    // ▼▼▼ 公欠申請の新機能 (詳細・承認) ▼▼▼

    /**
     * 申請詳細画面を表示する
     */
    @GetMapping("/detail")
    public String showRequestDetail(@RequestParam("requestId") Integer requestId, Model model) {
        // 1. 申請データを取得
        RequestEntity request = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("不正な申請IDです: " + requestId));
            
        // 2. 申請者のユーザー情報を取得
        UsersEntity applicant = usersRepository.findById(request.getRequesterUserId())
            .orElse(new UsersEntity());

        // 3. 画面に渡す
        model.addAttribute("request", request);
        model.addAttribute("applicant", applicant);

        return "admin/request/requestDetail";
    }

    /**
     * 公欠申請の承認を実行する
     */
    @PostMapping("/approve")
    public String approveRequest(
            @RequestParam("requestId") Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            Integer approverId = userDetails.getUsersEntity().getUserId();
            requestService.approveRequest(requestId, approverId);
            redirectAttributes.addFlashAttribute("successMessage", "承認が完了しました。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "承認に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/admin/request/detail?requestId=" + requestId;
    }
}