package com.example.attendancemanagementsystem.common.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired private SubjectRepository subjectRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private ClassroomRepository classroomRepository;

    private static final String API_KEY = "AIzaSyDUimnO51j5V5Jfk_CuvVNBEjA48HZKgxk"; 
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    public String analyzeTimetableImage(MultipartFile file) {
        System.out.println("【Debug】現在のAPIキー確認: " + API_KEY);
        System.out.println("【Debug】処理開始: ファイル名=" + file.getOriginalFilename());
        try {
            // 1. マスタデータ取得
            List<SubjectEntity> subjects = subjectRepository.findAll();
            List<UsersEntity> teachers = usersRepository.findByUserTypeId(2);
            List<ClassroomEntity> classrooms = classroomRepository.findAll();
            
            System.out.println("【Debug】マスタデータ取得: 科目=" + subjects.size() + "件, 教員=" + teachers.size() + "件, 教室=" + classrooms.size() + "件");

            String subjectListStr = subjects.stream().map(SubjectEntity::getSubjectName).collect(Collectors.joining(", "));
            String teacherListStr = teachers.stream().map(UsersEntity::getName).collect(Collectors.joining(", "));

            // 2. 画像変換
            byte[] imageBytes;
            String mimeType = "image/jpeg";
            String contentType = file.getContentType();

            if ((contentType != null && contentType.equalsIgnoreCase("application/pdf")) 
                || file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                System.out.println("【Debug】PDFを検知。画像変換を開始します...");
                imageBytes = convertPdfToJpg(file.getInputStream());
                System.out.println("【Debug】PDF変換完了。サイズ=" + imageBytes.length + "bytes");
            } else {
                imageBytes = file.getBytes();
                if (contentType != null) mimeType = contentType;
            }
            
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 3. プロンプト作成
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

            // 4. API送信
            System.out.println("【Debug】Gemini APIへリクエスト送信中...");
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

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            String responseBody = response.body();
            // ★超重要：APIからのナマの返事をログに出す
            System.out.println("================ AI RAW RESPONSE ================");
            System.out.println(responseBody);
            System.out.println("=================================================");

            // 5. 結果抽出
            return extractJsonFromResponse(responseBody, subjects, teachers, classrooms);

        } catch (Throwable e) {
            System.err.println("【Fatal Error】予期せぬエラーが発生しました");
            e.printStackTrace();
            return "[]";
        }
    }

    private byte[] convertPdfToJpg(InputStream pdfStream) throws IOException {
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(0, 2.0f); 
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        }
    }

    private String extractJsonFromResponse(String responseBody, 
                                           List<SubjectEntity> subjects,
                                           List<UsersEntity> teachers,
                                           List<ClassroomEntity> classrooms) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            
            // エラーチェック
            if (root.has("error")) {
                System.err.println("【API Error】Geminiからエラーが返ってきました: " + root.path("error").toString());
                return "[]";
            }

            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                System.out.println("【Debug】AI抽出テキスト(整形前): " + text);

                text = text.replaceAll("```json", "").replaceAll("```", "").trim();
                
                JsonNode timetableArray = mapper.readTree(text);
                if (timetableArray.isArray()) {
                    ArrayNode processedArray = mapper.createArrayNode();
                    System.out.println("【Debug】AIは " + timetableArray.size() + " 件のコマを認識しました。DB照合を開始します...");
                    
                    for (JsonNode node : timetableArray) {
                        var newNode = mapper.createObjectNode();
                        newNode.put("day", node.path("day").asText().toUpperCase());
                        newNode.put("slot", node.path("slot").asInt());

                        // マッピング詳細ログ
                        String subjectName = node.path("subject").asText();
                        String teacherName = node.path("teacher").asText();
                        String roomName = node.path("room").asText();

                        System.out.print("  - [解析] 科目:" + subjectName + ", 教員:" + teacherName + ", 教室:" + roomName);

                        // 科目
                        var subOpt = subjects.stream().filter(s -> s.getSubjectName().equals(subjectName)).findFirst();
                        if (subOpt.isPresent()) {
                            newNode.put("subjectId", subOpt.get().getSubjectId());
                            System.out.print(" -> 科目OK(ID:" + subOpt.get().getSubjectId() + ")");
                        } else {
                            System.out.print(" -> 科目NG(一致なし)");
                        }

                        // 教員
                        if (!teacherName.isEmpty()) {
                            var tOpt = teachers.stream().filter(u -> u.getName().contains(teacherName)).findFirst();
                            if (tOpt.isPresent()) {
                                newNode.put("userId", tOpt.get().getUserId());
                                System.out.print(", 教員OK(ID:" + tOpt.get().getUserId() + ")");
                            } else {
                                System.out.print(", 教員NG");
                            }
                        }

                        // 教室
                        if (!roomName.isEmpty()) {
                            var rOpt = classrooms.stream().filter(r -> r.getClassroomName().equals(roomName)).findFirst();
                            if (rOpt.isPresent()) {
                                newNode.put("classroomId", rOpt.get().getClassroomId());
                                System.out.print(", 教室OK(ID:" + rOpt.get().getClassroomId() + ")");
                            } else {
                                System.out.print(", 教室NG");
                            }
                        }
                        System.out.println(""); // 改行

                        // 一つでもマッチすれば採用
                        if (newNode.has("subjectId") || newNode.has("userId") || newNode.has("classroomId")) {
                            processedArray.add(newNode);
                        }
                    }
                    System.out.println("【Debug】最終結果: " + processedArray.size() + " 件のデータを返します。");
                    return processedArray.toString();
                }
            } else {
                System.err.println("【Debug】APIレスポンスに candidates が含まれていません。");
            }
        } catch (Exception e) {
            System.err.println("【Debug】JSONパースまたはマッピング中にエラー発生");
            e.printStackTrace();
        }
        return "[]";
    }
}