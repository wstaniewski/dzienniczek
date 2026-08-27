package com.mobilernd.dzienniczek.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Collections;

@Service
public class OcrReaderService {

    private ImageAnnotatorClient createVisionClient() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("/Users/wojciechstaniewski/.config/google/vision/google-vision-key.json"))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return ImageAnnotatorClient.create(settings);
    }

    public String read(MultipartFile file) {
        try {
            File tempFile = File.createTempFile("ocr-", ".tmp");
            Files.write(tempFile.toPath(), file.getBytes());

            byte[] data = Files.readAllBytes(tempFile.toPath());
            ByteString imgBytes = ByteString.copyFrom(data);

            Image img = Image.newBuilder().setContent(imgBytes).build();

            Feature feat = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build();

            ImageContext context = ImageContext.newBuilder()
                    .addLanguageHints("pl")
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .setImageContext(context)
                    .build();

            try (ImageAnnotatorClient client = createVisionClient()) {
                BatchAnnotateImagesResponse response =
                        client.batchAnnotateImages(Collections.singletonList(request));

                AnnotateImageResponse res = response.getResponses(0);

                if (res.hasError()) {
                    throw new RuntimeException("Vision API error: " + res.getError().getMessage());
                }

                return normalize(res.getFullTextAnnotation().getText());
            } finally {
                tempFile.delete();
            }

        } catch (Exception e) {
            throw new RuntimeException("Błąd OCR", e);
        }
    }

    private String normalize(String text) {
        return text
                .replace("Piatek", "Piątek")
                .replace("Pulęty", "Pulpet")
                .replace("—", "-")
                .replace("–", "-")
                .replace("−", "-")
                .replace("  ", " ");
    }
}