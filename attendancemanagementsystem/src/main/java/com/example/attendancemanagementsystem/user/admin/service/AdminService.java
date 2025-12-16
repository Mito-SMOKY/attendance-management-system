package com.example.attendancemanagementsystem.user.admin.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.attendancemanagementsystem.common.entity.Datalist;
import com.example.attendancemanagementsystem.common.entity.Student;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.DatalistRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.model.DatalistForm;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountForm;
import com.example.attendancemanagementsystem.user.admin.model.TempAccountData;

@Service
public class AdminService {

    @Autowired
    private DatalistRepository datalistRepository;

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. 全履歴取得
    public List<Datalist> getAllDatalists() {
        return datalistRepository.findAllWithCreator();
    }
    
    // 2. 自分の履歴のみ取得
    public List<Datalist> getDatalistsByCreator(Integer creatorId) {
        return datalistRepository.findByCreatorId(creatorId);
    }

    // 3. 詳細取得
    public Datalist getDatalistById(Integer id) {
        return datalistRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Datalist not found with ID: " + id));
    }

    // 4. CSV保存
    @Transactional
    public List<String> saveDatalist(DatalistForm form, Integer creatorId) {
        Datalist datalist = new Datalist();
        datalist.setDataListName(form.getDatalistName());
        datalist.setCreatorId(creatorId);

        List<Student> students = new ArrayList<>();
        List<String> skippedIds = new ArrayList<>();

        if (form.getTempAccounts() != null) {
            for (TempAccountData acc : form.getTempAccounts()) {
                if (usersRepository.existsByLoginId(acc.getLoginId())) {
                    skippedIds.add(acc.getLoginId());
                    continue;
                }

                UsersEntity user = new UsersEntity();
                user.setName(acc.getName());
                user.setLoginId(acc.getLoginId());
                String rawPassword = acc.getPassword();
                if (rawPassword == null || rawPassword.trim().isEmpty()) {
                    rawPassword = "password"; 
                }
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setUserTypeId(1);

                Student student = new Student();
                student.setStudentStatusId(1);
                student.setDatalist(datalist);
                student.setUser(user);

                students.add(student);
            }
        }

        if (!students.isEmpty()) {
            datalist.setStudents(students);
            datalistRepository.save(datalist);
        }

        return skippedIds;
    }

    // 5. 手動登録保存 (★ここを修正しました)
    @Transactional
    public Datalist saveDatalistFromForm(ManualAccountForm form, Integer creatorId) {
        Datalist datalist = new Datalist();
        
        // ★修正: getDatalistName() -> getDataListName()
        datalist.setDataListName(form.getDataListName());
        datalist.setCreatorId(creatorId);

        List<Student> students = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        // ★修正: リストを個別に取得してループ処理
        List<String> names = form.getName();
        List<String> studentIds = form.getStudentId();
        // affiliationIdを使う場合はここで取得: List<String> affIds = form.getAffiliationId();

        if (names != null && studentIds != null) {
            // サイズに合わせてループ（安全のため小さい方のサイズに合わせる）
            int size = Math.min(names.size(), studentIds.size());

            for (int i = 0; i < size; i++) {
                String name = names.get(i);
                String sId = studentIds.get(i);

                // 空チェック
                if (sId == null || sId.trim().isEmpty()) {
                    continue;
                }

                String loginId = sId.trim();
                
                // 重複チェック
                if (seenIds.contains(loginId) || usersRepository.existsByLoginId(loginId)) {
                    continue;
                }
                seenIds.add(loginId);

                UsersEntity user = new UsersEntity();
                user.setName(name);
                user.setLoginId(loginId);
                
                // ★修正: フォームにパスワードがないため、学籍番号を初期パスワードとして設定
                user.setPassword(passwordEncoder.encode(loginId));
                
                user.setUserTypeId(1); 

                Student student = new Student();
                student.setStudentStatusId(1);
                student.setDatalist(datalist);
                student.setUser(user);

                students.add(student);
            }
        }
        
        if (students.isEmpty()) { return null; }

        datalist.setStudents(students);
        return datalistRepository.save(datalist);
    }

    // 6. CSV解析
    public DatalistForm parseAccountFile(MultipartFile file) {
        DatalistForm form = new DatalistForm();
        List<TempAccountData> tempList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 2) {
                    TempAccountData data = new TempAccountData();
                    data.setLoginId(values[0].trim());
                    data.setName(values[1].trim());
                    if (values.length >= 3) {
                        data.setPassword(values[2].trim());
                    } else {
                        data.setPassword("");
                    }
                    tempList.add(data);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CSVファイルの読み込みに失敗しました", e);
        }

        form.setTempAccounts(tempList);
        form.setDatalistName(file.getOriginalFilename());
        return form;
    }

    // 7. CSVファイル生成 (ダウンロード用)
    public byte[] createCsvFile(Integer datalistId) {
        Datalist datalist = getDatalistById(datalistId);
        StringBuilder sb = new StringBuilder();

        sb.append("\uFEFF");

        for (Student student : datalist.getStudents()) {
            if (student.getUser() == null) continue;

            sb.append(student.getUser().getLoginId()).append(",");
            sb.append(student.getUser().getName()).append(",");
            sb.append(student.getUser().getPassword());
            sb.append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 8. 手動入力データからのCSV生成 (★ここを修正しました)
    public byte[] createCsvFromForm(ManualAccountForm form) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // BOM

        List<String> names = form.getName();
        List<String> studentIds = form.getStudentId();

        if (names != null && studentIds != null) {
            int size = Math.min(names.size(), studentIds.size());
            
            for (int i = 0; i < size; i++) {
                String name = names.get(i);
                String sId = studentIds.get(i);

                if (sId == null || sId.trim().isEmpty()) {
                    continue;
                }
                
                sb.append(sId).append(",");
                sb.append(name).append(",");
                // パスワード列（フォームにないので学籍番号を出力しておく）
                sb.append(sId); 
                sb.append("\r\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}