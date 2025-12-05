package com.example.attendancemanagementsystem.attendance.writer.controller;

import com.example.attendancemanagementsystem.common.component.NfcDataHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/nfc")
public class WriterPageController {

    private final NfcDataHolder nfcDataHolder;

    // コンストラクタ
    public WriterPageController(NfcDataHolder nfcDataHolder) {
        this.nfcDataHolder = nfcDataHolder;
    }

    // 待機画面
    @GetMapping("/idle")
    public String showIdle() {

        // NFCデータホルダーをリセット
        nfcDataHolder.reset();
        
        return "nfc/nfc_idle";
    }

    // 確認画面
    @GetMapping("/confirm")
    public String showConfirm(
            @RequestParam(required = false) String cardId,
            @RequestParam(required = false) String userId,
            Model model) {
        
        model.addAttribute("cardId", cardId);
        model.addAttribute("userId", userId);
        return "nfc/nfc_confirm";
    }

    // 結果画面
    @GetMapping("/result")
    public String showResult(
            @RequestParam(required = false) String status, 
            @RequestParam(required = false) String msg,
            Model model) {
        
        if (status == null) {
            return "redirect:/admin/nfc/idle";
        }
        
        model.addAttribute("status", status);
        model.addAttribute("message", msg);
        
        return "nfc/nfc_result";
    }
}