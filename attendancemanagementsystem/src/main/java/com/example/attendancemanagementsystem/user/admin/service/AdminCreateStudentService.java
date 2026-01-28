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
import com.example.attendancemanagementsystem.common.entity.DatalistDetailEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.DatalistDetailRepository;
import com.example.attendancemanagementsystem.common.repository.DatalistRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.model.DatalistForm;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountData;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountForm;
import com.example.attendancemanagementsystem.user.admin.model.TempAccountData;

@Service
public class AdminCreateStudentService {

    @Autowired
    private DatalistRepository datalistRepository;

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private EnrollmentsRepository enrollmentsRepository;

    @Autowired
    private DatalistDetailRepository datalistDetailRepository;

    // 1. 履歴取得
    public List<Datalist> getAllDatalists() {
        return datalistRepository.findByRoleWithCreator(1);
    }
    
    // 2. 自分の履歴のみ取得
    public List<Datalist> getDatalistsByCreator(Integer creatorId) {
        return datalistRepository.findByCreatorId(creatorId);
    }

    // 3. 詳細取得
    @Transactional(readOnly = true)
    public Datalist getDatalistById(Integer id) {
        return datalistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Datalist not found with ID: " + id));
    }

    // 4. CSV保存
    @Transactional
    public List<String> saveDatalist(DatalistForm form, Integer creatorId) {
        
        List<String> duplicateIds = new ArrayList<>();
        Set<String> seenIdsInCsv = new HashSet<>();

        if (form.getTempAccounts() != null) {
            for (TempAccountData acc : form.getTempAccounts()) {
                String loginId = acc.getLoginId();
                if (seenIdsInCsv.contains(loginId) || usersRepository.existsByLoginId(loginId)) {
                    duplicateIds.add(loginId);
                }
                seenIdsInCsv.add(loginId);
            }
        }

        if (!duplicateIds.isEmpty()) {
            return duplicateIds;
        }

        DepartmentEntity department = null;
        if (form.getDepartmentId() != null) {
            department = departmentRepository.findById(form.getDepartmentId()).orElse(null);
        }

        Datalist datalist = new Datalist();
        datalist.setDataListName(form.getDatalistName());
        datalist.setCreatorId(creatorId);
        datalist.setRole(1);

        List<StudentEntity> students = new ArrayList<>();

        if (form.getTempAccounts() != null) {
            for (TempAccountData acc : form.getTempAccounts()) {
                UsersEntity user = new UsersEntity();
                user.setName(acc.getName());
                user.setLoginId(acc.getLoginId());
                String rawPassword = acc.getPassword();
                if (rawPassword == null || rawPassword.trim().isEmpty()) {
                    rawPassword = "password"; 
                }
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setUserTypeId(1);

                StudentEntity student = new StudentEntity();
                student.setStudentStatusId(1);
                student.setDatalist(datalist);
                student.setUser(user);

                students.add(student);
            }
        }

        if (!students.isEmpty()) {
            datalist.setStudents(students);
            Datalist savedDatalist = datalistRepository.save(datalist);

            //みゆがDTOにデータを追加したからそれに伴っての変更
            form.setDataListId(savedDatalist.getDataListId());

            // 在籍情報保存
            saveEnrollments(savedDatalist, form.getAcademicYear(), form.getGrade(), department);

            // 履歴詳細（スナップショット）保存
            saveSnapshotDetails(savedDatalist, form.getAcademicYear(), form.getGrade(), department);
        }

        return new ArrayList<>();
    }

    // 5. 手動登録保存
    @Transactional
    public Datalist saveDatalistFromForm(ManualAccountForm form, Integer creatorId) {
        Datalist datalist = new Datalist();
        datalist.setDataListName(form.getDataListName());
        datalist.setCreatorId(creatorId);
        datalist.setRole(1);

        DepartmentEntity department = null;
        if (form.getDepartmentId() != null) {
            department = departmentRepository.findById(form.getDepartmentId()).orElse(null);
        }

        List<StudentEntity> students = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        List<ManualAccountData> accounts = form.getAccounts();

        if (accounts != null) {
            for (ManualAccountData acc : accounts) {
                String sId = acc.getStudentNumber();
                String name = acc.getName();
                String rawPass = acc.getPassword();

                if (sId == null || sId.trim().isEmpty()) continue;
                String loginId = sId.trim();

                if (seenIds.contains(loginId) || usersRepository.existsByLoginId(loginId)) {
                    continue;
                }
                seenIds.add(loginId);

                UsersEntity user = new UsersEntity();
                user.setName(name);
                user.setLoginId(loginId);
                user.setPassword(passwordEncoder.encode(rawPass));
                user.setUserTypeId(1);

                StudentEntity student = new StudentEntity();
                student.setStudentStatusId(1);
                student.setDatalist(datalist);
                student.setUser(user);
                
                students.add(student);
            }
        }
        
        if (students.isEmpty()) { return null; }

        datalist.setStudents(students);
        Datalist savedDatalist = datalistRepository.save(datalist);

        // 在籍情報保存
        saveEnrollments(savedDatalist, form.getAcademicYear(), form.getGrade(), department);

        // 履歴詳細（スナップショット）保存
        saveSnapshotDetails(savedDatalist, form.getAcademicYear(), form.getGrade(), department);

        return savedDatalist;
    }

    private void saveEnrollments(Datalist savedDatalist, Integer year, Integer grade, DepartmentEntity department) {
    if (department != null && grade != null && year != null) {
        List<EnrollmentsEntity> enrollmentsList = new ArrayList<>();
        for (StudentEntity savedStudent : savedDatalist.getStudents()) {
            // savedStudent は既に保存済みの実体なので、これを使う
            EnrollmentsEntity enroll = new EnrollmentsEntity();
            enroll.setStudent(savedStudent); // new せず、保存されたインスタンスを直接セット
            enroll.setDepartment(department);
            enroll.setGrade(grade);
            enroll.setAcademicYear(year);
            enroll.setIsActive(true);
            
            enrollmentsList.add(enroll);
        }
        enrollmentsRepository.saveAll(enrollmentsList);
    }
}

    // --- 共通処理: スナップショット保存 (LoginID対応版) ---
    private void saveSnapshotDetails(Datalist savedDatalist, Integer year, Integer grade, DepartmentEntity dept) {
        List<DatalistDetailEntity> details = new ArrayList<>();
        
        String majorName = (dept != null && dept.getMajor() != null) ? dept.getMajor().getMajorName() : "未設定";
        String className = (dept != null) ? dept.getClassName() : "";

        for (StudentEntity savedStudent : savedDatalist.getStudents()) {
            DatalistDetailEntity detail = new DatalistDetailEntity();
            detail.setDatalistId(savedDatalist.getDataListId());
            
            if (savedStudent.getUser() != null) {
                detail.setLoginId(savedStudent.getUser().getLoginId());
                detail.setName(savedStudent.getUser().getName());
            }

            detail.setAcademicYear(year);
            detail.setGrade(grade);
            detail.setMajorName(majorName);
            detail.setClassName(className);

            details.add(detail);
        }
        
        if (!details.isEmpty()) {
            datalistDetailRepository.saveAll(details);
            datalistDetailRepository.flush(); // ★追加: 即座に反映させる
        }
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
                    data.setPassword(values.length >= 3 ? values[2].trim() : "");
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
    @Transactional(readOnly = true)
    public byte[] createCsvFile(Integer datalistId) {
        Datalist datalist = getDatalistById(datalistId);
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        for (StudentEntity student : datalist.getStudents()) {
            if (student.getUser() == null) continue;
            sb.append(student.getUser().getLoginId()).append(",");
            sb.append(student.getUser().getName()).append(",");
            sb.append(student.getUser().getPassword());
            sb.append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 8. CSV生成 (手動登録完了後用)
    public byte[] createCsvFromForm(ManualAccountForm form) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        List<ManualAccountData> accounts = form.getAccounts();
        if (accounts != null) {
            for (ManualAccountData acc : accounts) {
                String sId = acc.getStudentNumber();
                String name = acc.getName();
                String pass = acc.getPassword();
                if (sId == null || sId.trim().isEmpty()) continue;
                sb.append(sId).append(",");
                sb.append(name).append(",");
                sb.append(pass).append("\r\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}