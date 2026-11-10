package pl.warsztatpro.model;

import java.time.LocalDateTime;

public record ServiceHistoryEntry(LocalDateTime occurredAt, String type, String reference, String status,
                                  String description) {
    @Override public String toString() {
        return occurredAt.toString().replace('T',' ') + " | " + type + " " + reference + " | " + status + " | " + description;
    }
}
