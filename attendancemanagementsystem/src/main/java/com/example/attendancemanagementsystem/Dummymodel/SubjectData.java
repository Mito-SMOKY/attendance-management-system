package com.example.attendancemanagementsystem.Dummymodel; // 例: model パッケージ

/**
 * 教科一覧画面に表示する情報を格納するデータクラス（独立クラス、Lombok不使用）
 */
public class SubjectData {
    
    private final int subjectId;
    private final String subjectName;       // 教科名
    private final String courseName;        // コース名 (例: 情報システム学科)
    private final String teacherName;       // 担当教師名
    private final int totalClasses;         // 総授業コマ数
    private final int attendedClasses;      // 出席した授業コマ数
    private final double attendanceRate;    // 出席率 (0.0 から 1.0)
    
    // 全フィールドを初期化するためのコンストラクタ
    public SubjectData(int subjectId, String subjectName, String courseName, 
                       String teacherName, int totalClasses, int attendedClasses, 
                       double attendanceRate) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
        this.attendanceRate = attendanceRate;
    }
    
    // --- ゲッターメソッド（必須） ---
    public int getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public String getCourseName() { return courseName; }
    public String getTeacherName() { return teacherName; }
    public int getTotalClasses() { return totalClasses; }
    public int getAttendedClasses() { return attendedClasses; }
    public double getAttendanceRate() { return attendanceRate; }

    /**
     * 出席率が危険域（40%以下）かどうかを判定するメソッド
     */
    public boolean isAttendanceRisk() {
        return this.attendanceRate <= 0.40;
    }
}