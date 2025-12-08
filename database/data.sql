-- Roles
INSERT INTO roles VALUES (1,'admin'), (2,'seller'), (3,'bidder');

-- Categories
INSERT INTO categories (name, slug, parent_id) VALUES
-- 1. Điện tử
('Điện tử', 'dien-tu', NULL),

('Điện thoại di động', 'dien-tu/dien-thoai-di-dong', 1),
('Máy tính bảng', 'dien-tu/may-tinh-bang', 1),
('Laptop', 'dien-tu/laptop', 1),
('Tai nghe & Loa', 'dien-tu/tai-nghe-loa', 1),
('Máy ảnh & Máy quay', 'dien-tu/may-anh-may-quay', 1),
('Tivi & Màn hình', 'dien-tu/tivi-man-hinh', 1),
('Phụ kiện điện thoại', 'dien-tu/phu-kien-dien-thoai', 1),
('Đồng hồ thông minh', 'dien-tu/dong-ho-thong-minh', 1),

-- 2. Thời trang & Phụ kiện
('Thời trang & Phụ kiện', 'thoi-trang-phu-kien', NULL),

('Giày dép nam', 'thoi-trang-phu-kien/giay-dep-nam', 10),
('Giày dép nữ', 'thoi-trang-phu-kien/giay-dep-nu', 10),
('Túi xách & Ví', 'thoi-trang-phu-kien/tui-xach-vi', 10),
('Đồng hồ thời trang', 'thoi-trang-phu-kien/dong-ho-thoi-trang', 10),
('Trang sức', 'thoi-trang-phu-kien/trang-suc', 10),
('Quần áo nam', 'thoi-trang-phu-kien/quan-ao-nam', 10),
('Quần áo nữ', 'thoi-trang-phu-kien/quan-ao-nu', 10),
('Phụ kiện thời trang', 'thoi-trang-phu-kien/phu-kien-thoi-trang', 10),

-- 3. Nhà cửa & Đời sống
('Nhà cửa & Đời sống', 'nha-cua-doi-song', NULL),

('Nội thất phòng khách', 'nha-cua-doi-song/noi-that-phong-khach', 19),
('Nội thất phòng ngủ', 'nha-cua-doi-song/noi-that-phong-ngu', 19),
('Đồ trang trí nhà cửa', 'nha-cua-doi-song/do-trang-tri-nha-cua', 19),
('Đồ gia dụng & Bếp', 'nha-cua-doi-song/do-gia-dung-bep', 19),
('Đồ dùng phòng tắm', 'nha-cua-doi-song/do-dung-phong-tam', 19),
('Cây cảnh & Vườn', 'nha-cua-doi-song/cay-canh-vuong', 19),

-- 4. Sức khỏe & Làm đẹp
('Sức khỏe & Làm đẹp', 'suc-khoe-lam-dep', NULL),

('Mỹ phẩm', 'suc-khoe-lam-dep/my-pham', 26),
('Chăm sóc da', 'suc-khoe-lam-dep/cham-soc-da', 26),
('Chăm sóc tóc', 'suc-khoe-lam-dep/cham-soc-toc', 26),
('Nước hoa', 'suc-khoe-lam-dep/nuoc-hoa', 26),
('Thực phẩm chức năng', 'suc-khoe-lam-dep/thuc-pham-chuc-nang', 26),

-- 5. Thể thao & Dã ngoại
('Thể thao & Dã ngoại', 'the-thao-da-ngoai', NULL),

('Xe đạp & Phụ kiện', 'the-thao-da-ngoai/xe-dap-phu-kien', 32),
('Dụng cụ tập gym', 'the-thao-da-ngoai/dung-cu-tap-gym', 32),
('Cắm trại & Dã ngoại', 'the-thao-da-ngoai/cam-trai-da-ngoai', 32),
('Câu cá', 'the-thao-da-ngoai/cau-ca', 32),
('Bóng đá & Bóng rổ', 'the-thao-da-ngoai/bong-da-bong-ro', 32),

-- 6. Sách & Văn phòng phẩm
('Sách & Văn phòng phẩm', 'sach-van-phong-pham', NULL),

('Sách văn học', 'sach-van-phong-pham/sach-van-hoc', 38),
('Sách kinh doanh - kinh tế', 'sach-van-phong-pham/sach-kinh-doanh-kinh-te', 38),
('Sách thiếu nhi', 'sach-van-phong-pham/sach-thieu-nhi', 38),
('Truyện tranh - Manga', 'sach-van-phong-pham/truyen-tranh-manga', 38),
('Sách ngoại ngữ', 'sach-van-phong-pham/sach-ngoai-ngu', 38),
('Văn phòng phẩm', 'sach-van-phong-pham/van-phong-pham', 38),

-- 7. Ô tô & Xe máy
('Ô tô & Xe máy', 'oto-xe-may', NULL),

('Xe máy', 'oto-xe-may/xe-may', 45),
('Phụ tùng xe máy', 'oto-xe-may/phu-tung-xe-may', 45),
('Phụ kiện ô tô', 'oto-xe-may/phu-kien-oto', 45),
('Mũ bảo hiểm', 'oto-xe-may/mu-bao-hiem', 45),

-- 8. Sở thích & Sưu tầm
('Sở thích & Sưu tầm', 'so-thich-suu-tam', NULL),

('Mô hình - Figure', 'so-thich-suu-tam/mo-hinh-figure', 50),
('Đồ chơi - Board game', 'so-thich-suu-tam/do-choi-board-game', 50),
('Nhạc cụ', 'so-thich-suu-tam/nhac-cu', 50),
('Tem - Tiền cổ', 'so-thich-suu-tam/tem-tien-co', 50),
('Đồ cổ & Mỹ nghệ', 'so-thich-suu-tam/do-co-my-nghe', 50),
('Thú cưng & Phụ kiện', 'so-thich-suu-tam/thu-cung-phu-kien', 50);



-- System configuration (for admin-configurable settings)
INSERT INTO system_configs (key, value, description) VALUES
    ('auto_extend_trigger_min', '5', 'Số phút trước khi kết thúc để tự động gia hạn'),
    ('auto_extend_by_min', '10', 'Số phút gia hạn thêm khi có bid mới'),
    ('new_product_highlight_min', '60', 'Số phút để đánh dấu sản phẩm là mới'),
    ('seller_temp_duration_days', '7', 'Thời gian seller được bán tính từ khi được cấp quyền seller')