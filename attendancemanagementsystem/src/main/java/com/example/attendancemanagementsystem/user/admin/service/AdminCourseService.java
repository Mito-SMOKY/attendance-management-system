package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.CourseEntity;
import com.example.attendancemanagementsystem.common.entity.MajorEntity;
import com.example.attendancemanagementsystem.common.repository.CourseRepository;
import com.example.attendancemanagementsystem.common.repository.MajorRepository;

@Service
public class AdminCourseService {

    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private MajorRepository majorRepository;

    public List<CourseEntity> getAllCourses() {
        return courseRepository.findAll();
    }
    
    public List<MajorEntity> getAllMajors() {
        return majorRepository.findAll();
    }

    @Transactional
    public void saveCourseList(List<Integer> courseIds, List<String> courseNames, List<Integer> majorIds) {
        if (courseNames == null) return;

        // 削除処理
        List<Integer> keptIds = new ArrayList<>();
        if (courseIds != null) {
            for (Integer id : courseIds) {
                if (id != null) keptIds.add(id);
            }
        }

        List<CourseEntity> allCourses = courseRepository.findAll();
        for (CourseEntity course : allCourses) {
            if (!keptIds.contains(course.getCourseId())) {
                try {
                    courseRepository.delete(course);
                } catch (Exception e) {
                    System.err.println("削除スキップ (使用中): CourseID=" + course.getCourseId());
                }
            }
        }

        // 保存・更新処理
        for (int i = 0; i < courseNames.size(); i++) {
            String name = courseNames.get(i);
            Integer currentId = (courseIds != null && courseIds.size() > i) ? courseIds.get(i) : null;
            Integer majorId = (majorIds != null && majorIds.size() > i) ? majorIds.get(i) : null;

            CourseEntity entity;
            if (currentId != null) {
                entity = courseRepository.findById(currentId).orElse(new CourseEntity());
            } else {
                entity = new CourseEntity();
            }
            
            entity.setCourseName(name);
            
            if (majorId != null) {
                entity.setMajorId(majorId);
                // 整合性のため関連エンティティもセット
                majorRepository.findById(majorId).ifPresent(entity::setMajor);
            } else {
                entity.setMajorId(null);
                entity.setMajor(null);
            }
            
            courseRepository.save(entity);
        }
    }
}