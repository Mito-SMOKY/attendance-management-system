package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.MajorEntity;
import com.example.attendancemanagementsystem.common.repository.MajorRepository;

@Service
public class AdminMajorService {

    @Autowired
    private MajorRepository majorRepository;

    public List<MajorEntity> getAllMajors() {
        return majorRepository.findAll();
    }

    @Transactional
    public void saveMajorList(List<Integer> majorIds, List<String> majorNames) {
        if (majorNames == null) return;

        // 削除処理 (画面から消されたIDを削除)
        List<Integer> keptIds = new ArrayList<>();
        if (majorIds != null) {
            for (Integer id : majorIds) {
                if (id != null) keptIds.add(id);
            }
        }

        List<MajorEntity> allMajors = majorRepository.findAll();
        for (MajorEntity major : allMajors) {
            if (!keptIds.contains(major.getMajorId())) {
                try {
                    majorRepository.delete(major);
                } catch (Exception e) {
                    // コースなどで使用中の場合は削除できない可能性があります（FK制約）
                    System.err.println("削除スキップ (使用中): MajorID=" + major.getMajorId());
                }
            }
        }

        // 保存・更新処理
        for (int i = 0; i < majorNames.size(); i++) {
            String name = majorNames.get(i);
            Integer currentId = (majorIds != null && majorIds.size() > i) ? majorIds.get(i) : null;

            MajorEntity entity;
            if (currentId != null) {
                entity = majorRepository.findById(currentId).orElse(new MajorEntity());
            } else {
                entity = new MajorEntity();
            }
            entity.setMajorName(name);
            majorRepository.save(entity);
        }
    }
}