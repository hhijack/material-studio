package com.example.materialstudio.model;

import java.time.LocalDateTime;

public record User(String id, String nickname, LocalDateTime createdAt) {
}
