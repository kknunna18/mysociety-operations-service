package com.mysociety.operations;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    Optional<Complaint> findByIdAndSocietyId(UUID id, UUID societyId);
    Page<Complaint> findBySocietyIdAndStatusIn(UUID societyId, Collection<ComplaintStatus> statuses, Pageable pageable);
    Page<Complaint> findBySocietyId(UUID societyId, Pageable pageable);
    boolean existsBySocietyIdAndComplaintNumber(UUID societyId, String complaintNumber);
}
interface ComplaintCommentRepository extends JpaRepository<ComplaintComment, UUID> {
    List<ComplaintComment> findByComplaintIdAndSocietyIdOrderByCreatedAtAsc(UUID complaintId, UUID societyId);
}
interface ComplaintHistoryRepository extends JpaRepository<ComplaintStatusHistory, UUID> {
    List<ComplaintStatusHistory> findByComplaintIdAndSocietyIdOrderByChangedAtAsc(UUID complaintId, UUID societyId);
}
interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    Optional<WorkOrder> findByIdAndSocietyId(UUID id, UUID societyId);
    Page<WorkOrder> findBySocietyId(UUID societyId, Pageable pageable);
    boolean existsBySocietyIdAndWorkOrderNumber(UUID societyId, String workOrderNumber);
}
interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {}
