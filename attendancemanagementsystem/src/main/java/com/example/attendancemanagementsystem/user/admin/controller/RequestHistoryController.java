package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.admin.service.RequestHistoryService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/request/history")
public class RequestHistoryController {

    @Autowired
    private RequestHistoryService historyService;

    // 一覧画面
    @GetMapping
    public String list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page, // ★追加: ページ番号を受け取る
            Model model) {
        
        // 1. 全データを取得
        List<Map<String, Object>> fullList = historyService.getMyRequestHistory(userDetails.getUserId());
        
        // 2. ページネーション計算（リストを10件分だけ切り出す）
        int pageSize = 10;
        int totalItems = fullList.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        
        int startItem = page * pageSize;
        List<Map<String, Object>> pagedList;

        if (totalItems < startItem) {
            // ページ番号がデータの範囲を超えている場合は空リスト
            pagedList = Collections.emptyList();
        } else {
            // 必要な範囲（startItem ～ toIndex）だけ切り出す
            int toIndex = Math.min(startItem + pageSize, totalItems);
            pagedList = fullList.subList(startItem, toIndex);
        }

        model.addAttribute("pageTitle", "申請履歴");
        
        // リスト(pagedList)を渡す
        model.addAttribute("historyList", pagedList);
        
        //ページネーション用変数
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        
        return "admin/request/requestHistory";
    }

    // 詳細画面
    @GetMapping("/{id}")
    public String detail(
            @PathVariable("id") Integer requestId,
            Model model) {
        
        Map<String, Object> requestDetail = historyService.getRequestDetail(requestId);
        
        if (requestDetail == null) {
            return "redirect:/admin/request/history";
        }

        model.addAttribute("pageTitle", "申請詳細");
        model.addAttribute("detail", requestDetail);
        
        return "admin/request/requestDetail";
    }
}