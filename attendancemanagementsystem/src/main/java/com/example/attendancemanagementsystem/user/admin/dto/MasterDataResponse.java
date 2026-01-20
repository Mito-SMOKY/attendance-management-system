package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

public record MasterDataResponse(
    List<String> subjects,
    List<String> courses,
    List<String> classrooms
) {}