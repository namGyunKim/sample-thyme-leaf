package gyun.sample.domain.aws.service.common;

import gyun.sample.domain.aws.enums.ImageType;
import gyun.sample.domain.aws.payload.dto.ImageUploadResult;
import gyun.sample.domain.aws.payload.dto.S3UrlParts;
import gyun.sample.domain.aws.service.S3Service;
import gyun.sample.global.exception.GlobalException;
import gyun.sample.global.exception.enums.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S3Service의 공통 로직을 구현하는 추상 클래스.
 * [성능 최적화] InputStream 기반 업로드 및 DeleteObjects를 이용한 일괄 삭제 적용.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractS3Service implements S3Service {

    protected final S3Client s3Client;

    @Autowired
    private Environment environment;

    @Value("${s3.bucket}")
    private String defaultBucketName;

    protected String bucketName;

    @Value("${aws.region}")
    protected String region;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    @Value("${s3.bucket-local:}")
    private String localBucketName;

    @PostConstruct
    public void init() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isLocal = Arrays.stream(activeProfiles)
                .anyMatch(profile -> profile.equalsIgnoreCase("local"));

        if (isLocal && localBucketName != null && !localBucketName.isBlank()) {
            this.bucketName = localBucketName;
            log.info("로컬 프로필이 감지되어 로컬 버킷({})을 사용합니다.", this.bucketName);
        } else {
            this.bucketName = defaultBucketName;
            log.info("운영 버킷({})을 사용합니다.", this.bucketName);
        }
    }

    protected abstract void validateImageType(ImageType imageType);

    @Override
    public ImageUploadResult uploadImage(MultipartFile file, ImageType imageType, Long entityId) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        try {
            validateImageType(imageType);
            return uploadToS3Internal(file.getInputStream(), file.getSize(), originalFilename, imageType, entityId);
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패 (IO 오류): {}, entityId: {}", originalFilename, entityId, e);
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public List<ImageUploadResult> uploadImages(List<MultipartFile> files, ImageType imageType, Long entityId) {
        return files.stream()
                .map(file -> uploadImage(file, imageType, entityId))
                .collect(Collectors.toList());
    }

    @Override
    public ImageUploadResult uploadImageFromUrl(String imageUrl, ImageType imageType, Long entityId) {
        try {
            log.info("이미지 스트리밍 다운로드 시도. URL: {}", imageUrl);
            validateImageType(imageType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.error("URL에서 이미지 다운로드 실패. URL: {}, 응답 코드: {}", imageUrl, response.statusCode());
                throw new GlobalException(ErrorCode.FILE_DOWNLOAD_FAILED);
            }

            InputStream inputStream = response.body();
            String contentType = response.headers().firstValue("Content-Type").orElse(null);
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);

            String originalFilename = extractOriginalFilenameFromUrl(imageUrl);
            if (getFileExtension(originalFilename).isEmpty()) {
                String extension = getExtensionFromContentType(contentType);
                originalFilename += (extension != null ? extension : ".png");
            }

            return uploadToS3Internal(inputStream, contentLength, originalFilename, imageType, entityId);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("URL에서 이미지를 읽는 중 오류 발생: {}", imageUrl, e);
            throw new GlobalException(ErrorCode.FILE_DOWNLOAD_FAILED, "URL에서 이미지 다운로드 실패: " + imageUrl);
        }
    }

    @Override
    public List<ImageUploadResult> uploadImagesFromUrls(List<String> imageUrls, ImageType imageType, Long entityId) {
        return imageUrls.stream()
                .map(url -> uploadImageFromUrl(url, imageType, entityId))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteImage(String fileName, ImageType imageType, Long entityId) {
        String s3Key = generateS3Key(fileName, imageType, entityId);
        // doesObjectExist 체크는 불필요한 API 호출일 수 있으므로 제거하거나 신중히 사용 (삭제 시 없으면 무시됨)
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public void deleteImages(List<String> fileNames, ImageType imageType, Long entityId) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }

        // [성능 개선] deleteObjects를 사용하여 일괄 삭제 (Batch Delete)
        List<ObjectIdentifier> objectsToDelete = new ArrayList<>();
        for (String fileName : fileNames) {
            String key = generateS3Key(fileName, imageType, entityId);
            objectsToDelete.add(ObjectIdentifier.builder().key(key).build());
        }

        try {
            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectsToDelete).build())
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(deleteObjectsRequest);
            if (response.hasErrors()) {
                log.warn("S3 일부 파일 삭제 실패: {}", response.errors());
            }
        } catch (Exception e) {
            log.error("S3 일괄 삭제 중 오류 발생", e);
            // S3 삭제 실패가 비즈니스 로직 전체 실패로 이어지지 않도록 로그만 남김
        }
    }

    @Override
    public String getImageUrl(String fileName, ImageType imageType, Long entityId) {
        String singleEncodedS3Key = generateS3Key(fileName, imageType, entityId);
        String doubleEncodedPath = singleEncodedS3Key.replace("%", "%25");
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, doubleEncodedPath);
    }

    @Override
    public ImageUploadResult cloneImageFromUrl(String sourceS3Url, ImageType destinationImageType, Long destinationEntityId) {
        log.info("S3-to-S3 복사 시작. Source URL: {}, DestType: {}, DestEntityId: {}",
                sourceS3Url, destinationImageType.name(), destinationEntityId);

        validateImageType(destinationImageType);
        S3UrlParts source = parseS3Url(sourceS3Url);

        HeadObjectResponse headResponse;
        String originalFilename;
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(source.getBucketName())
                    .key(source.getObjectKey())
                    .build();
            headResponse = s3Client.headObject(headRequest);
            originalFilename = extractFilenameFromContentDisposition(headResponse.contentDisposition());
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.FILE_NOT_FOUND, "Source S3 file not found or inaccessible.");
        }

        String finalFileName = generateFinalUploadFileName(destinationImageType, originalFilename);
        String destinationKey = generateS3Key(finalFileName, destinationImageType, destinationEntityId);

        try {
            String encodedOriginalFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");
            String newContentDisposition = "attachment; filename*=\"UTF-8''" + encodedOriginalFilename + "\"";

            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(source.getBucketName())
                    .sourceKey(source.getObjectKey())
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .contentDisposition(newContentDisposition)
                    .metadataDirective(MetadataDirective.REPLACE)
                    .build();

            s3Client.copyObject(copyRequest);

        } catch (Exception e) {
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED, "S3-to-S3 copy failed.");
        }

        return createImageUploadResult(finalFileName, null, null);
    }

    @Override
    public List<ImageUploadResult> cloneImagesFromUrls(List<String> sourceS3Urls, ImageType destinationImageType, Long destinationEntityId) {
        return sourceS3Urls.stream()
                .map(url -> cloneImageFromUrl(url, destinationImageType, destinationEntityId))
                .collect(Collectors.toList());
    }

    // === Protected Helper Methods ===

    protected ImageUploadResult uploadToS3Internal(InputStream inputStream, long size, String originalFilename, ImageType imageType, Long entityId) {
        validateExtension(originalFilename, imageType);

        boolean needDimensionCheck = (imageType.getWidth() != null || imageType.getHeight() != null);
        boolean sizeUnknown = (size == -1);

        if (needDimensionCheck || sizeUnknown) {
            return uploadWithTempFile(inputStream, originalFilename, imageType, entityId);
        } else {
            return uploadDirectly(inputStream, size, originalFilename, imageType, entityId);
        }
    }

    private ImageUploadResult uploadWithTempFile(InputStream inputStream, String originalFilename, ImageType imageType, Long entityId) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("upload_", "_" + originalFilename);
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            if (tempFile.length() > imageType.getMaxSize()) {
                throw new GlobalException(ErrorCode.FILE_SIZE_EXCEEDED);
            }

            Integer width = null;
            Integer height = null;
            if (imageType.getWidth() != null || imageType.getHeight() != null) {
                try {
                    BufferedImage image = ImageIO.read(tempFile);
                    if (image == null) throw new GlobalException(ErrorCode.INVALID_IMAGE_FILE);
                    width = image.getWidth();
                    height = image.getHeight();

                    if ((imageType.getWidth() != null && !width.equals(imageType.getWidth())) ||
                            (imageType.getHeight() != null && !height.equals(imageType.getHeight()))) {
                        throw new GlobalException(ErrorCode.INVALID_IMAGE_DIMENSIONS);
                    }
                } catch (IOException e) {
                    throw new GlobalException(ErrorCode.INVALID_IMAGE_FILE);
                }
            }

            String finalFileName = generateFinalUploadFileName(imageType, originalFilename);
            String s3Key = generateS3Key(finalFileName, imageType, entityId);

            String encodedOriginalFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");
            String contentDisposition = "attachment; filename*=\"UTF-8''" + encodedOriginalFilename + "\"";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentDisposition(contentDisposition)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(tempFile));

            return createImageUploadResult(finalFileName, width, height);

        } catch (IOException e) {
            log.error("임시 파일 처리 중 오류 발생", e);
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    private ImageUploadResult uploadDirectly(InputStream inputStream, long size, String originalFilename, ImageType imageType, Long entityId) {
        if (size > imageType.getMaxSize()) {
            throw new GlobalException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String finalFileName = generateFinalUploadFileName(imageType, originalFilename);
        String s3Key = generateS3Key(finalFileName, imageType, entityId);

        try {
            String encodedOriginalFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");
            String contentDisposition = "attachment; filename*=\"UTF-8''" + encodedOriginalFilename + "\"";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentDisposition(contentDisposition)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, size));

            return createImageUploadResult(finalFileName, null, null);

        } catch (Exception e) {
            log.error("S3 스트리밍 업로드 실패: {}", e.getMessage());
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    protected S3UrlParts parseS3Url(String s3Url) {
        try {
            URI uri = URI.create(s3Url);
            URL url = uri.toURL();
            String host = url.getHost();
            String path = url.getPath();
            String key = path.substring(1);

            String bucket;
            int s3Index = host.indexOf(".s3." + region + ".amazonaws.com");
            if (s3Index != -1) {
                bucket = host.substring(0, s3Index);
            } else {
                throw new IllegalArgumentException("Invalid S3 URL format.");
            }
            return new S3UrlParts(bucket, key);
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.INPUT_VALUE_INVALID, "Invalid S3 URL");
        }
    }

    protected String extractFilenameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null) {
            return "cloned-file-" + System.currentTimeMillis();
        }

        String prefix = "filename*=\"UTF-8''";
        int startIndex = contentDisposition.indexOf(prefix);
        if (startIndex != -1) {
            String encodedName = contentDisposition.substring(startIndex + prefix.length());
            if (encodedName.endsWith("\"")) encodedName = encodedName.substring(0, encodedName.length() - 1);
            try {
                return URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
            } catch (Exception e) { /* ignore */ }
        }

        String fnPrefix = "filename=";
        startIndex = contentDisposition.indexOf(fnPrefix);
        if (startIndex != -1) {
            String name = contentDisposition.substring(startIndex + fnPrefix.length());
            if (name.startsWith("\"")) name = name.substring(1);
            if (name.endsWith("\"")) name = name.substring(0, name.length() - 1);
            return name.replace("+", "%20");
        }
        return "cloned-file-" + System.currentTimeMillis();
    }

    protected ImageUploadResult createImageUploadResult(String fileName, Integer width, Integer height) {
        return new ImageUploadResult(fileName,
                width != null ? String.valueOf(width) : null,
                height != null ? String.valueOf(height) : null);
    }

    protected String generateFinalUploadFileName(ImageType imageType, String originalFilename) {
        return imageType.name() + "_" + originalFilename.replaceAll("\\s", "");
    }

    protected void validateExtension(String originalFilename, ImageType imageType) {
        String fileExtension = getFileExtension(originalFilename);
        imageType.isExtensionAllowed(fileExtension);
    }

    protected String getFileExtension(String filename) {
        if (filename == null) return "";
        String clean = filename.split("\\?")[0];
        int lastDot = clean.lastIndexOf('.');
        if (lastDot == -1 || lastDot == clean.length() - 1) return "";
        return clean.substring(lastDot + 1).toLowerCase();
    }

    protected String getExtensionFromContentType(String contentType) {
        if (contentType == null) return null;
        String mime = contentType.split(";")[0].trim().toLowerCase();
        return switch (mime) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/svg+xml" -> ".svg";
            default -> null;
        };
    }

    protected boolean doesObjectExist(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    protected String generateS3Key(String fileName, ImageType imageType, Long entityId) {
        String basePath = imageType.getPath().endsWith("/") ?
                imageType.getPath().substring(0, imageType.getPath().length() - 1) : imageType.getPath();
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return String.format("%s/%d/%s", basePath, entityId, encodedFileName);
    }

    protected String extractOriginalFilenameFromUrl(String imageUrl) {
        try {
            String targetUrl = imageUrl;
            URI uri = URI.create(imageUrl);
            String query = uri.getQuery();

            if (query != null && query.contains("src=")) {
                targetUrl = Arrays.stream(query.split("&"))
                        .filter(p -> p.startsWith("src="))
                        .map(p -> p.substring(4))
                        .findFirst()
                        .map(src -> URLDecoder.decode(src, StandardCharsets.UTF_8))
                        .orElse(imageUrl);
            }
            String path = URI.create(targetUrl).getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (Exception e) {
            String clean = imageUrl.split("\\?")[0];
            return clean.substring(clean.lastIndexOf('/') + 1);
        }
    }
}