# Kiểm thử Security Rules bằng Rules Playground (task 2.6 / 3.8)

Cách làm: Firebase Console → Firestore Database → **Rules** → nút **Rules Playground**.
Chọn *Simulation type*, nhập *Location* (đường dẫn), bật **Authenticated** và nhập **Firebase UID**
của tài khoản cần thử. Cần có sẵn trên Firestore ba tài khoản mẫu (đúng như app tạo):

| Tài khoản | `users/{uid}.role` | `users/{uid}.status` |
|---|---|---|
| ADMIN_UID | `admin` | `Normal` |
| MANAGER_UID | `manager` | `Normal` |
| EMPLOYEE_UID | `employee` | `Normal` |
| LOCKED_UID | `employee` | `Locked` |

Kết quả mong đợi (✓ = Allowed, ✗ = Denied):

| # | Loại | Đường dẫn | Người thử | Kết quả |
|---|---|---|---|---|
| 1 | get | `students/SV001` | EMPLOYEE_UID | ✓ |
| 2 | get | `students/SV001` | LOCKED_UID | ✗ |
| 3 | get | `students/SV001` | (chưa đăng nhập) | ✗ |
| 4 | create | `students/SV999` | MANAGER_UID | ✓ |
| 5 | create | `students/SV999` | EMPLOYEE_UID | ✗ |
| 6 | delete | `students/SV001` | EMPLOYEE_UID | ✗ |
| 7 | create | `students/SV001/certificates/c1` | MANAGER_UID | ✓ |
| 8 | create | `students/SV001/certificates/c1` | EMPLOYEE_UID | ✗ |
| 9 | list | `students/SV001/certificates` | EMPLOYEE_UID | ✓ |
| 10 | list | `users` | ADMIN_UID | ✓ |
| 11 | list | `users` | MANAGER_UID | ✗ |
| 12 | get | `users/EMPLOYEE_UID` | EMPLOYEE_UID (hồ sơ của mình) | ✓ |
| 13 | get | `users/ADMIN_UID` | EMPLOYEE_UID (hồ sơ người khác) | ✗ |
| 14 | create | `users/NEW_UID` | ADMIN_UID | ✓ |
| 15 | create | `users/NEW_UID` | MANAGER_UID | ✗ |
| 16 | update | `users/EMPLOYEE_UID`, chỉ đổi `avatarUrl` | EMPLOYEE_UID | ✓ |
| 17 | update | `users/EMPLOYEE_UID`, đổi `role` thành `admin` | EMPLOYEE_UID | ✗ |
| 18 | update | `users/MANAGER_UID`, đổi `avatarUrl` | EMPLOYEE_UID (sửa ảnh người khác) | ✗ |
| 19 | update | `users/EMPLOYEE_UID`, đổi `avatarUrl` | LOCKED_UID dùng uid của chính mình | ✗ |
| 20 | list | `users/ADMIN_UID/loginHistory` | EMPLOYEE_UID | ✗ |
| 21 | list | `users/EMPLOYEE_UID/loginHistory` | EMPLOYEE_UID | ✓ |
| 22 | list | `users/EMPLOYEE_UID/loginHistory` | ADMIN_UID | ✓ |
| 23 | update | `users/EMPLOYEE_UID/loginHistory/h1` | ADMIN_UID | ✗ (lịch sử không được sửa) |
| 24 | get | `bat_ky_collection_khac/x` | ADMIN_UID | ✗ |

Cách mô phỏng hàng 17/19 (update): trong Playground chọn *update*, bấm *Build document* và nhập
giá trị **sau khi cập nhật** của document (ví dụ giữ nguyên các field cũ nhưng đổi `role`).

> Hàng 19: ở bước này nên sửa `status` của LOCKED_UID thành `Locked` trước khi thử.

## Nếu một dòng sai kết quả
1. Kiểm tra `users/{uid}` của tài khoản đó có field `role` và `status` đúng chữ không
   (rules không phân biệt hoa thường và coi thiếu `status` là `Normal`).
2. Dán lại đúng nội dung `firestore.rules` rồi bấm **Publish**.
3. Ghi lại dòng bị sai và gửi cho người phụ trách Rules (C).

## Ghi vào báo cáo / video demo
- Manager và employee **không** xem được danh sách người dùng (rules chặn `list users`), khớp với giao diện (ẩn mục quản lý người dùng).
- Employee **được** xuất (export) danh sách vì export chỉ đọc dữ liệu; employee không thêm/sửa/xóa/import.
- Ảnh đại diện lưu dạng Base64 (ảnh vuông 320 px, nén JPEG) trong `users/{uid}.avatarUrl`
  để không phụ thuộc gói Firebase Storage trả phí; mỗi document Firestore tối đa khoảng 1 MiB.
