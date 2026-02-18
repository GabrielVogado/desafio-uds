package br.com.gabrielvogado.desafiouds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersionDTO {

    private Long id;
    private Long documentId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String uploadedByUsername;
    private LocalDateTime uploadedAt;
}

