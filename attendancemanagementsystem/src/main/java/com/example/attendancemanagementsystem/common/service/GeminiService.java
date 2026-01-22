package com.example.attendancemanagementsystem.common.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class GeminiService {

    // 必要なリポジトリの注入
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private ClassroomRepository classroomRepository;

    // Gemini APIの設定 
    @Value("${gemini.api.key}")
    private String apiKey;
    
    // 画像から時間割情報を解析する
    public String analyzeTimetableImage(MultipartFile file) {
        
        try {
            // APIのURLを構築 (キーを動的に埋め込む)
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            // DBからマスタデータを全件取得
            List<SubjectEntity> subjects = subjectRepository.findAll();
            List<UsersEntity> teachers = usersRepository.findByUserTypeId(2);
            List<ClassroomEntity> classrooms = classroomRepository.findAll();

            // プロンプトに含めるためのマスタデータリストを作成
            String subjectListStr = subjects.stream().map(SubjectEntity::getSubjectName).collect(Collectors.joining(", "));
            String teacherListStr = teachers.stream().map(UsersEntity::getName).collect(Collectors.joining(", "));

            // 画像データを取得してBase64形式にエンコード
            byte[] imageBytes = file.getBytes();
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // プロンプト構築
            String promptText = 
                "この画像は学校の時間割です。各コマには「科目名」「教室名」「教員名」が記載されています。\n" +
                "（例: 223 上野 → 教室:223, 教員:上野）\n\n" +
                "内容を読み取り、以下のJSON形式のリストで出力してください。\n" +
                "JSONフォーマット: \n" +
                "[{\"day\": \"MONDAY\", \"slot\": 1, \"subject\": \"科目名\", \"teacher\": \"教員名\", \"room\": \"教室名\"}, ...]\n\n" +
                "【重要: マッチング指示】\n" +
                "1. 科目名は、リスト: [" + subjectListStr + "] に最も近いものを選んでください。\n" +
                "2. 教員名は、リスト: [" + teacherListStr + "] にある名前を含んでいれば、そのフルネームを優先してください（例: '上野' -> '上野 太郎'）。\n" +
                "3. Markdown記法は不要です。生データのJSONのみを返してください。";

            // APIリクエストボディのJSONを作成
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = "{"
                    + "\"contents\": [{"
                    + "  \"parts\": ["
                    + "    {\"text\": " + mapper.writeValueAsString(promptText) + "},"
                    + "    {\"inline_data\": {"
                    + "      \"mime_type\": \"" + mimeType + "\","
                    + "      \"data\": \"" + base64Image + "\""
                    + "    }}"
                    + "  ]"
                    + "}]"
                    + "}";

            // HTTPクライアントでAPIへPOST送信
            System.out.println("【Debug】Gemini APIへリクエスト送信中...");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl)) // 構築したURLを使用
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // レスポンスからJSONを抽出しDB照合を実施して結果を返す
            return extractJsonFromResponse(responseBody, subjects, teachers, classrooms);

        } catch (Throwable e) {
            System.err.println("【Fatal Error】予期せぬエラーが発生しました");
            e.printStackTrace();
            return "[]";
        }
    }

    // APIレスポンスからJSONを抽出・整形しDBと照合する処理
    private String extractJsonFromResponse(String responseBody, 
                                            List<SubjectEntity> subjects,
                                            List<UsersEntity> teachers,
                                            List<ClassroomEntity> classrooms) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            
            // エラーレスポンスのチェック
            if (root.has("error")) {
                System.err.println("【API Error】" + root.path("error").toString());
                return "[]";
            }

            // AIの回答テキストを取得
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();

                // 純粋なJSON文字列へ
                text = text.replaceAll("```json", "").replaceAll("```", "").trim();
                
                // 文字列をJSON配列としてパース
                JsonNode timetableArray = mapper.readTree(text);
                if (timetableArray.isArray()) {
                    ArrayNode processedArray = mapper.createArrayNode();
                    
                    // 各コマデータの照合処理
                    for (JsonNode node : timetableArray) {
                        var newNode = mapper.createObjectNode();
                        newNode.put("day", node.path("day").asText().toUpperCase());
                        newNode.put("slot", node.path("slot").asInt());

                        String aiSub = node.path("subject").asText();
                        String aiTea = node.path("teacher").asText();
                        String aiRoom = node.path("room").asText();

                        // 科目名を正規化してDBマスタとあいまい照合
                        String normAiSub = normalize(aiSub);
                        var subOpt = subjects.stream().filter(s -> {
                            String dbName = normalize(s.getSubjectName());
                            return dbName.equals(normAiSub) || dbName.contains(normAiSub) || normAiSub.contains(dbName);
                        }).findFirst();

                        if (subOpt.isPresent()) {
                            newNode.put("subjectId", subOpt.get().getSubjectId());
                            System.out.print(" -> 科目OK");
                        } else {
                            System.out.print(" -> 科目NG");
                        }

                        // 教員名を正規化してDBマスタとあいまい照合
                        if (!aiTea.isEmpty()) {
                            String normAiTea = normalize(aiTea);
                            var tOpt = teachers.stream().filter(u -> {
                                String dbName = normalize(u.getName());
                                return dbName.equals(normAiTea) || dbName.contains(normAiTea);
                            }).findFirst();

                            if (tOpt.isPresent()) {
                                newNode.put("userId", tOpt.get().getUserId());
                                System.out.print(", 教員OK");
                            } else {
                                System.out.print(", 教員NG");
                            }
                        }

                        // 教室名を正規化してDBマスタとあいまい照合
                        if (!aiRoom.isEmpty()) {
                            String normAiRoom = normalize(aiRoom);
                            var rOpt = classrooms.stream().filter(r -> {
                                String dbName = normalize(r.getClassroomName());
                                return dbName.equals(normAiRoom) || dbName.contains(normAiRoom) || normAiRoom.contains(dbName);
                            }).findFirst();

                            if (rOpt.isPresent()) {
                                newNode.put("classroomId", rOpt.get().getClassroomId());
                                System.out.print(", 教室OK");
                            } else {
                                System.out.print(", 教室NG");
                            }
                        }
                        System.out.println(""); 

                        // いずれかのIDが特定できた場合のみ結果に追加
                        if (newNode.has("subjectId") || newNode.has("userId") || newNode.has("classroomId")) {
                            processedArray.add(newNode);
                        }
                    }
                    return processedArray.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("【Debug】エラー: " + e.getMessage());
            e.printStackTrace();
        }
        return "[]";
    }


   // PDFから「学期の期間」と「休日」を同時に抽出する
    public String extractHolidaysFromPdf(MultipartFile file, int year, int term, int targetGrade) {
        try {
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
            byte[] pdfBytes = file.getBytes();
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            String gradeLabel = targetGrade + "年生";
            String termLabel = (term == 1) ? "前期" : "後期";

            // プロンプト修正
            String promptText = 
                "あなたは学校の教務システム管理者です。添付のPDFから" + year + "年度の行事予定を読み取り、以下の情報を抽出してください。\n\n" +
                
                "【タスク1: 期間の特定】\n" +
                "**「" + gradeLabel + "」** の **「" + termLabel + "」** における**『授業開始日』**と**『授業終了日』**の日付を特定してください。\n" +
                "※PDF内の表記例: '1年前期授業開始', '前期授業終了' など。\n" +
                "※もし明確な終了日がなければ、学期の区切り（前期なら9月末、後期なら3月末）付近の授業最終日を推測してください。\n\n" +

                "【タスク2: 休日の抽出】\n" +
                "タスク1で特定した **開始日から終了日までの期間内** に含まれる『授業が行われない日』を全てリスト化してください。\n" +
                "・祝日、振替休日、創立記念日、開学記念日\n" +
                "・学校行事（研修、旅行、ヤツロゲ、入試、式典など授業がない日）\n" +
                "・長期休暇（夏休み・冬休み）が含まれる場合は、その期間中の平日全て\n\n" +

                "【出力形式 (JSONのみ)】\n" +
                "以下のJSONフォーマットのみを返してください。Markdownや説明は不要です。\n" +
                "{\n" +
                "  \"startDate\": \"YYYY-MM-DD\",\n" + 
                "  \"endDate\": \"YYYY-MM-DD\",\n" +   
                "  \"holidays\": [\"YYYY-MM-DD\", \"YYYY-MM-DD\", ...]\n" + 
                "}";

            // JSONボディ作成（共通処理）
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = "{"
                    + "\"contents\": [{"
                    + "  \"parts\": ["
                    + "    {\"text\": " + mapper.writeValueAsString(promptText) + "},"
                    + "    {\"inline_data\": {"
                    + "      \"mime_type\": \"application/pdf\"," 
                    + "      \"data\": \"" + base64Pdf + "\""
                    + "    }}"
                    + "  ]"
                    + "}]"
                    + "}";

            // 送信（共通処理）
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            JsonNode root = mapper.readTree(response.body());
            if (root.has("candidates")) {
                String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                // クリーニング
                return text.replaceAll("```json", "").replaceAll("```", "").trim();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        // エラー時は空のJSON構造を返す
        return "{ \"startDate\": \"\", \"endDate\": \"\", \"holidays\": [] }";
    }

    // 文字列の正規化
    private String normalize(String input) {
        if (input == null) return "";
        return input.replace(" ", "")
                    .replace("　", "")
                    .trim();
    }
}