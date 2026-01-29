package com.example.attendancemanagementsystem.attendance.writer.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.CardsEntity;
import com.example.attendancemanagementsystem.common.repository.CardsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WriterService {

    private final CardsRepository cardsRepository;

    public WriterService(CardsRepository cardsRepository) {
        this.cardsRepository = cardsRepository;
    }

    /**
     * カード発行処理
     * @param targetUserId 書き込むユーザーID
     * @param targetCardId すり替え防止用のカードID（最初に読み込んだID）
     * @return 結果メッセージ
     */
    @Transactional
    public String issueCard(Integer targetUserId, String targetCardId) {

        //パス指定
        String projectDir = System.getProperty("user.dir");
        String scriptPath = Paths.get(projectDir, "attendancemanagementsystem", "python_scripts", "nfc_writer_json.py").toString();
        String pythonExe = "C:/Users/Student/AppData/Local/Programs/Python/Python313/python.exe";

        try {
            // Pythonスクリプトを実行
            ProcessBuilder pb = new ProcessBuilder(
                pythonExe, 
                scriptPath, 
                String.valueOf(targetUserId), 
                targetCardId 
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 出力を読み取る
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python Script Error: " + output.toString());
            }

            // JSON結果をパース
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(output.toString());

            if (!node.path("success").asBoolean()) {
                throw new RuntimeException(node.path("message").asText());
            }

            // 実際に書き込まれたカードIDを取得
            String newCardId = node.path("card_id").asText();

            // --- DB更新処理 ---

            // 旧カード無効化 (このユーザーが既に持っているカードがあれば無効化)
            Optional<CardsEntity> oldCardOptional = cardsRepository.findByUserIdAndIsActiveTrue(targetUserId);
            if (oldCardOptional.isPresent()) {
                CardsEntity oldCard = oldCardOptional.get();
                // 新しいカードと違うIDなら無効化する
                if (!oldCard.getCardId().equals(newCardId)) {
                    oldCard.setIsActive(false);
                    cardsRepository.save(oldCard);
                }
            }

            // 新カード登録/更新
            CardsEntity targetCard = cardsRepository.findByCardId(newCardId)
                    .orElse(new CardsEntity());
            
            targetCard.setCardId(newCardId);
            targetCard.setUserId(targetUserId);
            targetCard.setIsActive(true);
            targetCard.setIssuedDate(LocalDateTime.now());
            
            cardsRepository.save(targetCard);

            return "登録完了: Card[" + newCardId + "] -> User[" + targetUserId + "]";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("処理が中断されました", e);
        } catch (IOException e) {
            throw new RuntimeException("Pythonスクリプトの実行に失敗しました", e);
        } catch (Exception e) {
            throw new RuntimeException("予期せぬエラー: " + e.getMessage(), e);
        }
    }
}