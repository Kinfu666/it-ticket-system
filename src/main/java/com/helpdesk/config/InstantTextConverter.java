package com.helpdesk.config;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InstantTextConverter implements AttributeConverter<Instant, String> {
    // 固定九位小數與 UTC，讓 SQLite TEXT 的字典排序等同時間排序。
    private static final DateTimeFormatter FORMAT =
            new DateTimeFormatterBuilder().appendInstant(9).toFormatter();

    public String convertToDatabaseColumn(Instant value) {
        return value == null ? null : FORMAT.format(value);
    }

    public Instant convertToEntityAttribute(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
