package com.url.shortener.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "click_event") // changed to valid collection name
public class ClickEvent {

    @Id
    private String id; // MongoDB ObjectId as String

    private LocalDateTime clickDate;

    @DBRef
    private UrlMapping urlMapping; // reference to UrlMapping document
}
