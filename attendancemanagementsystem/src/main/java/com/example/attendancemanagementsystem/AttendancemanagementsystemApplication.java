package com.example.attendancemanagementsystem;





import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class AttendancemanagementsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendancemanagementsystemApplication.class, args);
    }

    // 起動時に正しいハッシュ値を計算して表示する
    @Bean
    public CommandLineRunner generateCorrectHash(PasswordEncoder passwordEncoder) {
        return args -> {
            String rawPassword = "password";
            String encoded = passwordEncoder.encode(rawPassword);
            
            System.out.println("==========================================");
            System.out.println("【重要】このハッシュ値をSQLに使ってください:");
            System.out.println(encoded);
			System.err.println("これはpassword=\"password\"の正しいハッシュ値です。");
            System.out.println("==========================================");
        };
    }
}