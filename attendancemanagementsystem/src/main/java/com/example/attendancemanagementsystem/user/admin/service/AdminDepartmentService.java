package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.CourseEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.repository.CourseRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;

@Service
public class AdminDepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    public List<DepartmentEntity> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public List<CourseEntity> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional
    public void saveDepartmentList(List<Integer> departmentIds, List<String> classNames, List<Integer> courseIds) {
        if (classNames == null) return;

        // 削除処理
        List<Integer> keptIds = new ArrayList<>();
        if (departmentIds != null) {
            for (Integer id : departmentIds) {
                if (id != null) keptIds.add(id);
            }
        }

        List<DepartmentEntity> allDepts = departmentRepository.findAll();
        for (DepartmentEntity dept : allDepts) {
            if (!keptIds.contains(dept.getDepartmentId())) {
                try {
                    departmentRepository.delete(dept);
                } catch (Exception e) {
                    System.err.println("削除失敗: DeptID=" + dept.getDepartmentId());
                }
            }
        }

        // 保存・更新処理
        for (int i = 0; i < classNames.size(); i++) {
            String name = classNames.get(i);
            Integer currentId = (departmentIds != null && departmentIds.size() > i) ? departmentIds.get(i) : null;
            Integer courseId = (courseIds != null && courseIds.size() > i) ? courseIds.get(i) : null;

            DepartmentEntity entity;
            if (currentId != null) {
                entity = departmentRepository.findById(currentId).orElse(new DepartmentEntity());
            } else {
                entity = new DepartmentEntity();
            }

            entity.setClassName(name);

            if (courseId != null) {
                entity.setCourseId(courseId);
                courseRepository.findById(courseId).ifPresent(entity::setCourse);
            } else {
                entity.setCourseId(null);
                entity.setCourse(null);
            }

            departmentRepository.save(entity);
        }
    }
}