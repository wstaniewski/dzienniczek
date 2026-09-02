package com.mobilernd.dzienniczek.model;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ByteArrayMultipartFile implements MultipartFile {

    private final String filename;
    private final byte[] content;

    public ByteArrayMultipartFile(String filename, byte[] content) {
        this.filename = filename;
        this.content = content;
    }

    @Override
    public String getName() {
        return filename;
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return null; // możesz dodać wykrywanie MIME jeśli chcesz
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.write(dest.toPath(), content);
    }
}
