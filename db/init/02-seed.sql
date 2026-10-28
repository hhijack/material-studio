-- Material Studio 种子数据：6 个模板、4 个分类（幂等：重复执行按主键覆盖更新）
-- 与 backend/src/main/resources/db/init/02-seed.sql 保持同一文件

INSERT INTO `template`
    (`id`, `title`, `category`, `cover_url`, `description`, `required_image_count`,
     `accepted_mime_types`, `max_file_size_bytes`, `sort_order`)
VALUES
    ('travel-postcard', '旅行明信片', '创意图片', '/demo/covers/travel-postcard.svg',
     '上传 2 张旅行照片，模拟生成一张复古风格的明信片。', 2,
     '["image/jpeg", "image/png"]', 5242880, 1),
    ('art-sketch', '速写画框', '创意图片', '/demo/covers/art-sketch.svg',
     '上传 1 张图片，模拟生成一幅铅笔速写风格画框。', 1,
     '["image/jpeg", "image/png"]', 3145728, 2),
    ('festival-greeting', '节日贺卡', '节日祝福', '/demo/covers/festival-greeting.svg',
     '上传 3 张图片，模拟生成一张节日祝福贺卡。', 3,
     '["image/jpeg", "image/png", "image/webp"]', 5242880, 3),
    ('love-mosaic', '爱心拼图', '节日祝福', '/demo/covers/love-mosaic.svg',
     '上传 4 张照片，模拟生成爱心形状的照片拼图。', 4,
     '["image/jpeg", "image/png"]', 2097152, 4),
    ('pet-avatar', '宠物卡通头像', '宠物日常', '/demo/covers/pet-avatar.svg',
     '上传 1 张宠物照片，模拟生成卡通头像。', 1,
     '["image/jpeg", "image/png"]', 5242880, 5),
    ('trip-collage', '旅行拼贴', '旅行纪念', '/demo/covers/trip-collage.svg',
     '上传 3 张旅途照片，模拟生成一张旅行拼贴海报。', 3,
     '["image/jpeg", "image/png"]', 4194304, 6)
ON DUPLICATE KEY UPDATE
    `title`                = VALUES(`title`),
    `category`             = VALUES(`category`),
    `cover_url`            = VALUES(`cover_url`),
    `description`          = VALUES(`description`),
    `required_image_count` = VALUES(`required_image_count`),
    `accepted_mime_types`  = VALUES(`accepted_mime_types`),
    `max_file_size_bytes`  = VALUES(`max_file_size_bytes`),
    `sort_order`           = VALUES(`sort_order`);
