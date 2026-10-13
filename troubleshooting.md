## Phạm vi

Workflow được sửa: `.github/workflows/broken-pipeline.yml`.

Pipeline thực hiện ba việc chính: chạy test Gradle, đăng nhập Docker Hub, sau đó build và push Docker image.

## Sự cố 1: Gradle Wrapper bị `Permission denied`

### Dấu hiệu

Runner Ubuntu dừng ở bước test với lỗi tương tự:

```text
./gradlew: Permission denied
```

### Nguyên nhân

Tệp `gradlew` không có quyền thực thi trên Linux runner.

### Khắc phục

Thêm một bước cấp quyền trước bước test:

```yaml
- name: Grant execute permission to Gradle Wrapper
  run: chmod +x gradlew

- name: Run tests
  run: ./gradlew test
```

## Sự cố 2: Đăng nhập Docker Registry thất bại

### Dấu hiệu

Bước `docker/login-action` báo lỗi xác thực, ví dụ `unauthorized` hoặc `incorrect username or password`.

### Nguyên nhân

Workflow tham chiếu sai tên secret chứa access token. Khi tên không khớp, GitHub truyền giá trị rỗng vào action đăng nhập.

### Khắc phục

Tham chiếu đúng secret `REGISTRY_TOKEN`:

```yaml
- name: Log in to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.REGISTRY_USERNAME }}
    password: ${{ secrets.REGISTRY_TOKEN }}
```

Repository cần cấu hình hai Actions secrets trong **Settings → Secrets and variables → Actions**:

- `REGISTRY_USERNAME`: tên tài khoản Docker Hub.
- `REGISTRY_TOKEN`: Docker Hub personal access token (không dùng mật khẩu tài khoản).

Workflow chỉ thực hiện đăng nhập và push khi cả hai secrets có giá trị. Nếu repository chưa cấu hình secrets, bước đăng nhập/push được bỏ qua và pipeline vẫn build image cục bộ để kiểm tra Dockerfile. Cơ chế này giúp CI của pull request hoặc repository mẫu không thất bại chỉ vì không được cấp thông tin nhạy cảm.

## Sự cố 3: Docker không tìm thấy Dockerfile

### Dấu hiệu

Bước build báo lỗi tương tự:

```text
failed to read dockerfile: open Dockerfile: no such file or directory
```

### Nguyên nhân

Build context hoặc đường dẫn `file` trỏ nhầm thư mục. Trong bài này, build context và `Dockerfile` đều nằm ở thư mục gốc của repository.

### Khắc phục

Khai báo tường minh context và Dockerfile từ repository root:

```yaml
- name: Build and push Docker image
  uses: docker/build-push-action@v5
  with:
    context: .
    file: ./Dockerfile
    push: true
    tags: ${{ secrets.REGISTRY_USERNAME }}/broken-pipeline:latest
```

## Cách kiểm tra sau khi sửa

1. Xác nhận `gradlew` và `Dockerfile` nằm ở thư mục gốc repository.
2. Tạo đủ hai repository secrets `REGISTRY_USERNAME` và `REGISTRY_TOKEN`.
3. Commit và push thay đổi lên nhánh `main`.
4. Mở tab **Actions**, chọn workflow **Repaired CI/CD Pipeline** và kiểm tra mọi step đều thành công.
5. Chụp màn hình trang workflow run có trạng thái màu xanh để nộp cùng hai tệp trên.

## Kết quả mong đợi

- `Grant execute permission to Gradle Wrapper`: thành công.
- `Run tests`: thành công.
- Khi đã cấu hình secrets: `Log in to Docker Hub` và `Build and push Docker image` thành công; image xuất hiện trên Docker Hub.
- Khi chưa cấu hình secrets: hai bước registry được bỏ qua và `Build Docker image without registry credentials` thành công.
