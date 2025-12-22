package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;

@Service
public class AdminClassroomService {

    @Autowired
    private ClassroomRepository classroomRepository;

    // --- 全件取得 ---
    public List<ClassroomEntity> getAllClassrooms() {
        return classroomRepository.findAll();
    }

    // --- 一括保存・削除処理 ---
    // ★引数に macAddresses を追加
    @Transactional
    public void saveClassroomList(
            List<Integer> classroomIds, 
            List<String> classroomNames, 
            List<String> macAddresses) {
        
        if (classroomNames == null) return;

        // 1. 削除処理（画面から消されたIDを特定して削除）
        List<Integer> keptIds = new ArrayList<>();
        if (classroomIds != null) {
            for (Integer id : classroomIds) {
                if (id != null) keptIds.add(id);
            }
        }

        List<ClassroomEntity> allRooms = classroomRepository.findAll();
        for (ClassroomEntity room : allRooms) {
            if (!keptIds.contains(room.getClassroomId())) {
                classroomRepository.delete(room);
            }
        }

        // 2. 更新・新規登録処理
        for (int i = 0; i < classroomNames.size(); i++) {
            String name = classroomNames.get(i);
            Integer currentId = (classroomIds != null && classroomIds.size() > i) ? classroomIds.get(i) : null;
            // ★MACアドレスを取得
            String mac = (macAddresses != null && macAddresses.size() > i) ? macAddresses.get(i) : null;

            ClassroomEntity entity;
            if (currentId != null) {
                entity = classroomRepository.findById(currentId).orElse(new ClassroomEntity());
            } else {
                entity = new ClassroomEntity();
            }

            entity.setClassroomName(name);
            entity.setMacAddress(mac); // ★セット
            
            classroomRepository.save(entity);
        }
    }
}