package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.admin.service.RequestListService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/approval")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RequestListController {

    @Autowired
    private RequestListService approvalService;

    // 申請承認・履歴画面を表示
    @GetMapping("/list")
    public String list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page, //ページ番号を受け取る
            Model model) {
        
        // 1. 全件取得する
        List<Map<String, Object>> fullList = approvalService.getApprovalList(userDetails.getUserId());
        
        // 2. ページネーション
        int pageSize = 10; // 1ページあたりの件数
        int startItem = page * pageSize;
        List<Map<String, Object>> pagedList;

        if (fullList.size() < startItem) {
            // ページ番号がデータの範囲を超えている場合は空リスト
            pagedList = Collections.emptyList();
        } else {
            // 必要な範囲（startItem ～ toIndex）だけ切り出す
            int toIndex = Math.min(startItem + pageSize, fullList.size());
            pagedList = fullList.subList(startItem, toIndex);
        }

        // 3. Pageオブジェクトに変換（HTML側で便利に使うため）
        Page<Map<String, Object>> requestPage = new PageImpl<>(pagedList, PageRequest.of(page, pageSize), fullList.size());

        model.addAttribute("pageTitle", "申請承認・履歴");
        model.addAttribute("requestPage", requestPage);
        
        return "admin/superAdmin/RequestList";
    }
}