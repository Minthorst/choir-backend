package me.choir_backend.Boundary;

import jakarta.validation.constraints.NotNull;

public record ArchiveMemberRequest(@NotNull Long memberId, @NotNull Boolean archived) {
}
