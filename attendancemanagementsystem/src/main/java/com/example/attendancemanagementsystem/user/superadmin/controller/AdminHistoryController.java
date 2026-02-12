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
@RequestMapping("/admin/history")
public class AdminHistoryController {

    @Autowired
    private AdminHistoryRepository historyRepository;

    @GetMapping
    public String showHistoryList(
            Model model,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // ページネーション付きで全取得
        Page<AdminHistoryEntity> page = historyRepository.findAll(pageable);
        
        model.addAttribute("historyPage", page);
        
        // ★修正: 配置場所に合わせてパスを変更 (templates/admin/superadmin/sadminHistoryList.html)
        return "admin/superadmin/sadminHistoryList";
    }
}