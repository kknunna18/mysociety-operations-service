package com.mysociety.operations;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

enum ComplaintStatus {OPEN, UNDER_REVIEW, ASSIGNED, IN_PROGRESS, WAITING_FOR_RESIDENT, RESOLVED, CLOSED, REOPENED, REJECTED}

enum Priority {LOW, MEDIUM, HIGH, CRITICAL}

enum WorkOrderStatus {CREATED, ASSIGNED, ACCEPTED, IN_PROGRESS, ON_HOLD, COMPLETED, VERIFIED, CANCELLED}

@Entity
@Table(name = "complaints", schema = "mysociety")
class Complaint {
    @Id
    UUID id;
    @Column(name = "society_id", nullable = false)
    UUID societyId;
    @Column(name = "unit_id")
    UUID unitId;
    @Column(name = "complaint_number", nullable = false)
    String complaintNumber;
    @Column(name = "reported_by", nullable = false)
    UUID reportedBy;
    @Column(nullable = false)
    String category;
    String subcategory;
    @Column(nullable = false)
    String subject;
    @Column(nullable = false)
    String description;
    @Column(name = "location_details")
    String locationDetails;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Priority priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ComplaintStatus status;
    @Column(name = "assigned_user_id")
    UUID assignedUserId;
    @Column(name = "sla_due_at")
    Instant slaDueAt;
    @Column(name = "preferred_visit_at")
    Instant preferredVisitAt;
    @Column(name = "entry_permission", nullable = false)
    boolean entryPermission;
    @Column(name = "resolution_summary")
    String resolutionSummary;
    @Column(name = "resolved_at")
    Instant resolvedAt;
    @Column(name = "closed_at")
    Instant closedAt;
    @Column(name = "resident_rating")
    Short residentRating;
    @Column(name = "resident_feedback")
    String residentFeedback;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
    @Version
    Long version;
}

@Entity
@Table(name = "complaint_comments", schema = "mysociety")
class ComplaintComment {
    @Id
    UUID id;
    @Column(name = "society_id", nullable = false)
    UUID societyId;
    @Column(name = "complaint_id", nullable = false)
    UUID complaintId;
    @Column(name = "author_user_id")
    UUID authorUserId;
    @Column(name = "comment_text", nullable = false)
    String commentText;
    @Column(name = "is_internal", nullable = false)
    boolean internal;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}

@Entity
@Table(name = "complaint_status_history", schema = "mysociety")
class ComplaintStatusHistory {
    @Id
    UUID id;
    @Column(name = "society_id", nullable = false)
    UUID societyId;
    @Column(name = "complaint_id", nullable = false)
    UUID complaintId;
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    ComplaintStatus oldStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    ComplaintStatus newStatus;
    @Column(name = "changed_by")
    UUID changedBy;
    String reason;
    @Column(name = "changed_at", nullable = false)
    Instant changedAt;
}

@Entity
@Table(name = "work_orders", schema = "mysociety")
class WorkOrder {
    @Id
    UUID id;
    @Column(name = "society_id", nullable = false)
    UUID societyId;
    @Column(name = "complaint_id")
    UUID complaintId;
    @Column(name = "work_order_number", nullable = false)
    String workOrderNumber;
    @Column(nullable = false)
    String title;
    String description;
    @Column(name = "assigned_user_id")
    UUID assignedUserId;
    @Column(name = "assigned_vendor_name")
    String assignedVendorName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Priority priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WorkOrderStatus status;
    @Column(name = "scheduled_at")
    Instant scheduledAt;
    @Column(name = "due_at")
    Instant dueAt;
    @Column(name = "started_at")
    Instant startedAt;
    @Column(name = "completed_at")
    Instant completedAt;
    @Column(name = "completion_notes")
    String completionNotes;
    @Column(name = "estimated_cost")
    BigDecimal estimatedCost;
    @Column(name = "actual_cost")
    BigDecimal actualCost;
    @Column(name = "created_by")
    UUID createdBy;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
    @Version
    Long version;
}

@Entity
@Table(name = "outbox_events", schema = "mysociety")
class OutboxEvent {
    @Id
    UUID id;
    @Column(name = "society_id")
    UUID societyId;
    @Column(name = "aggregate_type", nullable = false)
    String aggregateType;
    @Column(name = "aggregate_id", nullable = false)
    UUID aggregateId;
    @Column(name = "event_type", nullable = false)
    String eventType;
    @Column(name = "event_version", nullable = false)
    int eventVersion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    JsonNode payload;
    @JdbcTypeCode(SqlTypes.JSON)
    JsonNode headers;
    @Column(nullable = false)
    String status = "PENDING";
    @Column(name = "attempt_count", nullable = false)
    int attemptCount;
    @Column(name = "available_at", nullable = false)
    Instant availableAt;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
