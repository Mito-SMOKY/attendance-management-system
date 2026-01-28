package com.example.attendancemanagementsystem.classroom.subject.dto;

import java.util.List;

public record MasterDataResponse(
    List<String> subjects,
    List<String> courses,
    List<String> classrooms
) {}