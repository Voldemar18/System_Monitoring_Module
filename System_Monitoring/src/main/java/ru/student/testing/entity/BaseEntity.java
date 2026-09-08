
package ru.student.testing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 Абстрактный базовый класс для всех сущностей.
 Обеспечивает единообразие полей аудита и демонстрирует использование наследования.
 */
@Data
@NoArgsConstructor
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //Инициализация полей аудита перед сохранением
    public void initAuditFields() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public abstract String getEntityDisplayName();
}