package com.example.attendancemanagementsystem.user.superadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendancemanagementsystem.common.entity.AdminHistoryEntity;
import com.example.attendancemanagementsystem.common.repository.AdminHistoryRepository;


@Controller
@RequestMapping("/admin/history") //
public class AdminHistoryController {

    @Autowired
    private AdminHistoryRepository historyRepository;

    // ★ここを修正！ URLの後半部分 "/adminOperation" を追加するよ
    @GetMapping("/adminOperation") 
    public String showHistoryList(
            Model model,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // ページネーション付きで全取得
        Page<AdminHistoryEntity> page = historyRepository.findAll(pageable); //
        
        model.addAttribute("historyPage", page); //
        
        // 表示するHTMLファイル（sadminHistoryList.html）を指定
        return "admin/superadmin/sadminHistoryList"; 
    }
}