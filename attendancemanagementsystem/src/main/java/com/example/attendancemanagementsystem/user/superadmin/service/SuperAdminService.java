package com.example.attendancemanagementsystem.user.superadmin.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AdministratorEntity;
import com.example.attendancemanagementsystem.common.entity.CreationLogEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AdministratorRepository;
import com.example.attendancemanagementsystem.common.repository.CreationLogRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.SearchService;
import com.example.attendancemanagementsystem.user.superadmin.dto.SuperAdminCreateDto;
import com.example.attendancemanagementsystem.user.superadmin.dto.SuperAdminDetailDto;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.JoinType;

@Service
public class SuperAdminService {

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private CreationLogRepository creationLogRepository;

    @Autowired
    private SearchService searchService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    // --- 1. 一覧取得メソッド ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdminList(String keyword, String authFilter) {
        Specification<AdministratorEntity> spec = Specification.where(null);
        spec = spec.and((root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("user", JoinType.LEFT);
            }
            return cb.conjunction();
        });

        if (keyword != null && !keyword.trim().isEmpty()) {
            String cleanKeyword = keyword.trim();
            List<String> targetColumns = Arrays.asList("user.name", "user.loginId");
            Specification<AdministratorEntity> textSpec = searchService.createKeywordSpec(cleanKeyword, targetColumns);
            
            Specification<AdministratorEntity> finalKeywordSpec = textSpec;

            if (cleanKeyword.matches("\\d+")) {
                try {
                    int searchId = Integer.parseInt(cleanKeyword);
                    Specification<AdministratorEntity> idSpec = (root, query, cb) -> 
                        cb.equal(root.get("userId"), searchId);
                    finalKeywordSpec = Specification.where(textSpec).or(idSpec);
                } catch (NumberFormatException e) {}
            }
            spec = spec.and(finalKeywordSpec);
        }

        if ("1".equals(authFilter)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("adminLevelId"), 1));
        } else if ("2".equals(authFilter)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("adminLevelId"), 2));
        }

        List<AdministratorEntity> entities = administratorRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "userId"));
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (AdministratorEntity admin : entities) {
            if (admin.getUser() == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("UserID", admin.getUserId());
            map.put("Name", admin.getUser().getName());
            map.put("authority", (admin.getAdminLevelId() == 1) ? "1" : "0"); 
            resultList.add(map);
        }
        return resultList;
    }

    // --- 2. 詳細取得メソッド ---
    @Transactional(readOnly = true)
    public SuperAdminDetailDto getAdminDetail(Integer id) {
        AdministratorEntity admin = administratorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("管理者が見つかりません ID: " + id));
        
        SuperAdminDetailDto dto = new SuperAdminDetailDto();
        dto.setUserId(admin.getUserId());
        dto.setName(admin.getUser().getName());
        dto.setEmail(admin.getUser().getEmail());
        dto.setAdminLevelID(admin.getAdminLevelId() == 1 ? 1 : 0);
        return dto;
    }

    // --- 3. 更新メソッド ---
    @Transactional
    public void updateAdmin(SuperAdminDetailDto dto) {
        AdministratorEntity admin = administratorRepository.findById(dto.getUserId())
            .orElseThrow(() -> new RuntimeException("管理者が見つかりません"));
        UsersEntity user = admin.getUser();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        admin.setAdminLevelId((dto.getAdminLevelID() == 1) ? 1 : 2);
        usersRepository.save(user);
        administratorRepository.save(admin);
    }

    // --- 4. 新規管理者作成メソッド (修正: 操作者認証を追加) ---
    @Transactional
    public Integer createAdmin(SuperAdminCreateDto dto, Integer operatorId) {
        
        // ★追加: 操作実行者(自分)のパスワード確認
        UsersEntity currentUser = usersRepository.findById(operatorId)
            .orElseThrow(() -> new RuntimeException("操作ユーザーが見つかりません"));

        if (dto.getCurrentAdminPassword() == null || 
            !passwordEncoder.matches(dto.getCurrentAdminPassword(), currentUser.getPassword())) {
            throw new RuntimeException("操作用パスワードが正しくありません。");
        }

        // 1. 重複チェック
        if (usersRepository.findByLoginId(dto.getLoginId()).isPresent()) {
            throw new RuntimeException("このログインIDは既に使用されています: " + dto.getLoginId());
        }

        // 2. Users保存
        UsersEntity newUser = new UsersEntity();
        newUser.setLoginId(dto.getLoginId());
        newUser.setName(dto.getName());
        // emailセットなし
        newUser.setPassword(passwordEncoder.encode(dto.getPassword())); // DBはハッシュ化
        newUser.setUserTypeId(2); 
        
        usersRepository.save(newUser);
        usersRepository.flush(); 

        // 3. Administrator保存
        AdministratorEntity newAdmin = new AdministratorEntity();
        newAdmin.setUser(newUser); 
        newAdmin.setUserId(newUser.getUserId());
        newAdmin.setAdminLevelId(dto.getAdminLevelID());
        
        entityManager.persist(newAdmin); // 強制INSERT

        // 4. ログ保存
        try {
            CreationLogEntity log = new CreationLogEntity(
                operatorId,
                newUser.getName(),
                newUser.getLoginId(),
                dto.getPassword() // 平文パスワード(PDF用)
            );
            creationLogRepository.save(log);
            return log.getLogId(); 

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("ログの保存に失敗しました");
        }
    }

    // --- 5. PDF生成メソッド (修正: 平文パスワード印字) ---
    public byte[] generateRegistrationPdf(Integer logId) {
        CreationLogEntity log = creationLogRepository.findById(logId)
            .orElseThrow(() -> new RuntimeException("ログが見つかりません"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont bf = BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font fontTitle = new Font(bf, 18, Font.BOLD);
            Font fontBody = new Font(bf, 12, Font.NORMAL);

            document.add(new Paragraph("【管理者登録通知書】", fontTitle));
            document.add(new Paragraph(" ", fontBody));
            document.add(new Paragraph("以下の内容で管理者アカウントを発行しました。", fontBody));
            document.add(new Paragraph("--------------------------------------------------", fontBody));
            document.add(new Paragraph("登録日時 : " + log.getCreatedAt(), fontBody));
            document.add(new Paragraph("氏名     : " + log.getTargetName(), fontBody));
            document.add(new Paragraph("ログインID: " + log.getTargetLoginId(), fontBody));
            
            // ★修正: 平文パスワードを印字
            document.add(new Paragraph("パスワード: " + log.getPassword(), fontBody));
            
            document.add(new Paragraph("--------------------------------------------------", fontBody));
            document.add(new Paragraph("※この用紙は厳重に保管してください。", fontBody));

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("PDF生成に失敗しました");
        }
    }
}