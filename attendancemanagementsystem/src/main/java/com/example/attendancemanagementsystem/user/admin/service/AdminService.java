package com.example.attendancemanagementsystem.user.admin.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.attendancemanagementsystem.common.entity.Datalist;
import com.example.attendancemanagementsystem.common.entity.Student;
import com.example.attendancemanagementsystem.common.entity.Users;
import com.example.attendancemanagementsystem.common.repository.DatalistRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.model.DatalistForm;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountData;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountForm;
import com.example.attendancemanagementsystem.user.admin.model.TempAccountData;

@Service
public class AdminService {

    @Autowired
    private DatalistRepository datalistRepository;

    @Autowired
    private UsersRepository usersRepository;

    // --- 1. 作成履歴取得 (Datalist一覧) ---
    public List<Datalist> getDatalistsByCreator(Integer creatorId) {
        return datalistRepository.findByCreatorId(creatorId);
    }

    // --- 2. 詳細取得 (Datalist ID指定) ---
    public Datalist getDatalistById(Integer id) {
        // 変更: 標準のfindByIdではなく、まとめて取得するカスタムメソッドを使う
        return datalistRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Datalist not found with ID: " + id));
    }

    // --- 3. ファイル読み込み結果の保存 (DatalistForm -> DB) ---
    @Transactional
    public List<String> saveDatalist(DatalistForm form, Integer creatorId) {
        Datalist datalist = new Datalist();
        datalist.setDataListName(form.getDatalistName());
        datalist.setCreatorId(creatorId);

        List<Student> students = new ArrayList<>();
        List<String> skippedIds = new ArrayList<>(); // 重複ID記録用

        if (form.getTempAccounts() != null) {
            for (TempAccountData acc : form.getTempAccounts()) {

                // 重複チェック
                if (usersRepository.existsByLoginId(acc.getLoginId())) {
                    // スキップリストに追加して処理をスキップ
                    skippedIds.add(acc.getLoginId());
                    continue;
                }

                // Users作成
                Users user = new Users();
                user.setName(acc.getName());
                user.setLoginId(acc.getLoginId());
                user.setPassword("password");
                user.setUserTypeId(1);

                // Student作成
                Student student = new Student();
                student.setStudentStatusId(1);
                student.setDatalist(datalist);
                student.setUser(user);

                students.add(student);
            }
        }

        // 1件でも登録できるデータがあればDBに保存
        if (!students.isEmpty()) {
            datalist.setStudents(students);
            datalistRepository.save(datalist);
        }

        // スキップされたIDのリストを返す
        return skippedIds;
    }

    // --- 4. 手動入力データの保存 (ManualAccountForm -> DB) ---
    @Transactional
    public Datalist saveDatalistFromForm(ManualAccountForm form, Integer creatorId) {
        Datalist datalist = new Datalist();
        datalist.setDataListName(form.getDatalistName());
        datalist.setCreatorId(creatorId);

        List<Student> students = new ArrayList<>();

        if (form.getAccounts() != null) {
            for (ManualAccountData acc : form.getAccounts()) {
                // 1. Users作成
                Users user = new Users();
                user.setName(acc.getName());
                // フォーム側のフィールド名がstudentNumberのままの場合は、ここでLoginIdにマッピング
                user.setLoginId(acc.getStudentNumber()); 
                user.setEmail(acc.getEmail());
                user.setPassword("password");
                user.setUserTypeId(1); // 1: Student

                // 2. Student作成
                Student student = new Student();
                student.setStudentStatusId(1); // 仮登録
                student.setDatalist(datalist);
                student.setUser(user); // Usersと紐付け

                students.add(student);
            }
        }

        datalist.setStudents(students);
        return datalistRepository.save(datalist);
    }

    // --- 5. ファイル解析処理 (CSV -> DatalistForm) ---
    // ※簡易的なCSVパーサーの実装です
    public DatalistForm parseAccountFile(MultipartFile file) {
        DatalistForm form = new DatalistForm();
        List<TempAccountData> tempList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            // ヘッダー行がある前提で1行読み飛ばすならコメントアウトを解除
            // br.readLine(); 

            while ((line = br.readLine()) != null) {
                // カンマ区切りで分割
                String[] values = line.split(",");
                if (values.length >= 2) {
                    TempAccountData data = new TempAccountData();
                    // CSVの1列目をID、2列目を名前として扱う例
                    data.setLoginId(values[0].trim()); 
                    data.setName(values[1].trim());
                    tempList.add(data);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CSVファイルの読み込みに失敗しました", e);
        }

        form.setTempAccounts(tempList);
        // ファイル名をデフォルトのリスト名にする
        form.setDatalistName(file.getOriginalFilename());
        return form;
    }

    // --- 6. ダウンロード用ファイル生成 (プレースホルダー) ---
    public Object createDownloadFile(Integer datalistId) {
        // 必要であれば実装（CSV生成ロジックなど）
        return null;
    }
}