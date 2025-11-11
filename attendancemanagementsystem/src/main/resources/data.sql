-- 起動のたびにテストユーザーを作成する

-- パスワードは "password123" を暗号化(BCrypt)したものです
-- (この暗号化された文字列をそのままコピペしてください)
INSERT INTO users (user_id, user_type, name, email, password)
VALUES ('admin001', 'admin', 'テスト管理者', 'admin@test.com', '$2a$10$3g.c0b.hP0k0i.Yc.iQ5yOJVWJdDyPeiSIJ8xviCrqJPEHnN3aU5W');

INSERT INTO users (user_id, user_type, name, email, password)
VALUES ('student001', 'student', 'テスト学生', 'student@test.com', '$2a$10$3g.c0b.hP0k0i.Yc.iQ5yOJVWJdDyPeiSIJ8xviCrqJPEHnN3aU5W');
