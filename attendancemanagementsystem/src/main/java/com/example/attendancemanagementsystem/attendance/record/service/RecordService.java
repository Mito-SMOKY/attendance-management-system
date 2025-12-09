package com.example.attendancemanagementsystem.attendance.record.service;

import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO;
import com.example.attendancemanagementsystem.attendance.record.dto.VerifiedRecordDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import com.example.attendancemanagementsystem.common.entity.CardsEntity;
import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository; // ★追加
import com.example.attendancemanagementsystem.common.repository.CardsRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RecordService {

    private final CardsRepository cardsRepository;
    private final DecryptionService decryptionService;
    private final AttendanceRepository attendanceRepository;
    private final ClassroomRepository classroomRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;

    // コンストラクタ
    public RecordService(CardsRepository cardsRepository, 
                        DecryptionService decryptionService,
                        AttendanceRepository attendanceRepository,
                        ClassroomRepository classroomRepository,
                        AttendanceStatusRepository attendanceStatusRepository) {
        this.cardsRepository = cardsRepository;
        this.decryptionService = decryptionService;
        this.attendanceRepository = attendanceRepository;
        this.classroomRepository = classroomRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
    }

    @Transactional
    public VerifiedRecordDto processAttendanceScan(RecordDTO recordDTO) {

        // 復号処理
        String encryptedHex = recordDTO.getUserId();
        String originalUserIdString = decryptionService.decryptUserId(encryptedHex);
        Integer originalUserId = Integer.parseInt(originalUserIdString);

        // カードID検証
        String cardId = recordDTO.getCardId();
        CardsEntity card = cardsRepository.findByCardId(cardId)
                .orElseThrow(() -> new IllegalArgumentException("未登録のカードです: " + cardId));

        if (!Boolean.TRUE.equals(card.getIsActive())) {
            throw new IllegalStateException("無効化されたカードです。");
        }
        if (!card.getUserId().equals(originalUserId)) {
            throw new IllegalStateException("カード所有者とデータが不一致です(偽造の疑い)。");
        }

        // 時刻変換
        LocalDateTime scanTime;
        try {
            if (recordDTO.getReadTime() != null) {
                scanTime = LocalDateTime.parse(recordDTO.getReadTime());
            } else {
                scanTime = LocalDateTime.now();
            }
        } catch (Exception e) {
            scanTime = LocalDateTime.now();
        }

        // セキュリティチェック
        String readerMacAddress = recordDTO.getReaderId();
        if (readerMacAddress == null || readerMacAddress.isEmpty()) {
            throw new SecurityException("不正なアクセス: ReaderIDがありません。");
        }
        Optional<ClassroomEntity> classroomOpt = classroomRepository.findByMacAddress(readerMacAddress);
        if (classroomOpt.isEmpty()) {
            throw new SecurityException("未登録のリーダー(MAC: " + readerMacAddress + ")からのアクセスは拒否されました。");
        }
        System.out.println("認証OK: " + classroomOpt.get().getClassroomName() + " (MAC: " + readerMacAddress + ")");

        // DB保存処理
        AttendanceEntity attendance = new AttendanceEntity();
        
        // ユーザーセット
        StudentEntity studentRef = new StudentEntity();
        studentRef.setUserId(originalUserId); 
        attendance.setStudent(studentRef);

        attendance.setCreatedAt(scanTime);
        
        //リポジトリからステータス(ID=1:出席)を取得してセット
        AttendanceStatusEntity status = attendanceStatusRepository.findById(1)
            .orElseThrow(() -> new IllegalStateException("ステータスID=1 がDBに見つかりません。"));
        
        attendance.setStatusId(status);

        attendanceRepository.save(attendance);

        // 結果返却
        return new VerifiedRecordDto(
                originalUserId,
                cardId,
                scanTime,
                recordDTO.getReaderId()
        );
    }
}