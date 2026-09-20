package com.mysociety.operations;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record CreateComplaintRequest(@NotBlank @Size(max=60) String category, @Size(max=80) String subcategory,
    @NotBlank @Size(max=200) String subject, @NotBlank String description, @Size(max=300) String locationDetails,
    @NotNull Priority priority, UUID unitId, Instant preferredVisitAt, boolean entryPermission) {}
record UpdateComplaintRequest(@Size(max=60) String category, @Size(max=80) String subcategory,
    @Size(max=200) String subject, String description, @Size(max=300) String locationDetails,
    Priority priority, Instant preferredVisitAt, Boolean entryPermission) {}
record CommentRequest(@NotBlank String comment, boolean internal) {}
record TransitionRequest(@NotNull ComplaintStatus status, @Size(max=1000) String reason) {}
record ResolutionRequest(@NotBlank String summary) {}
record FeedbackRequest(@NotNull @Min(1) @Max(5) Short rating, @Size(max=1000) String feedback) {}
record ComplaintResponse(UUID id, String complaintNumber, String category, String subject, String description,
    Priority priority, ComplaintStatus status, UUID assignedUserId, Instant slaDueAt, Instant resolvedAt, Instant closedAt,
    Instant createdAt, Instant updatedAt, long version) {}
record CommentResponse(UUID id, UUID authorUserId, String comment, boolean internal, Instant createdAt) {}
record HistoryResponse(UUID id, ComplaintStatus oldStatus, ComplaintStatus newStatus, UUID changedBy, String reason, Instant changedAt) {}

record CreateWorkOrderRequest(UUID complaintId, @NotBlank @Size(max=200) String title, String description,
    Priority priority, UUID assignedUserId, @Size(max=200) String assignedVendorName, Instant scheduledAt, Instant dueAt,
    @DecimalMin("0.0") BigDecimal estimatedCost) {}
record UpdateWorkOrderRequest(UUID assignedUserId, @Size(max=200) String assignedVendorName,
    WorkOrderStatus status, Instant scheduledAt, Instant dueAt, String completionNotes, @DecimalMin("0.0") BigDecimal actualCost) {}
record WorkOrderResponse(UUID id, String workOrderNumber, UUID complaintId, String title, Priority priority,
    WorkOrderStatus status, UUID assignedUserId, Instant dueAt, Instant completedAt, long version) {}
