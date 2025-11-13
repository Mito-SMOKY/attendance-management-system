-- data.sql (修正版)
-- 現在のDB構造 (User_IDは自動採番, UserTypeIDは外部キー) に合わせる

-- UserTypesテーブルから 'administrator' のID (例: 2) を取得し、
-- パスワード 'password123' (Bcryptハッシュ) で管理者を作成
INSERT INTO Users (UserTypeID, Name, Email, Password)
VALUES (
    (SELECT UserTypeID FROM UserTypes WHERE UserTypeName = 'administrator'),
    'テスト管理者',
    'admin@test.com',
    '$2a$10$3g.c0b.hP0k0i.Yc.iQ5yOJVWJdDyPeiSIJ8xviCrqJPEHnN3aU5W'
);

-- UserTypesテーブルから 'student' のID (例: 1) を取得し、
-- パスワード 'password123' (Bcryptハッシュ) で学生を作成
INSERT INTO Users (UserTypeID, Name, Email, Password)
VALUES (
    (SELECT UserTypeID FROM UserTypes WHERE UserTypeName = 'student'),
    'テスト学生',
    'student@test.com',
    '$2a$10$3g.c0b.hP0k0i.Yc.iQ5yOJVWJdDyPeiSIJ8xviCrqJPEHnN3aU5W'
);