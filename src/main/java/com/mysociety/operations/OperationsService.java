package com.mysociety.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OperationsService {
    private final ComplaintRepository complaints; private final ComplaintCommentRepository comments;
    private final ComplaintHistoryRepository histories; private final WorkOrderRepository workOrders;
    private final OutboxEventRepository outbox; private final ObjectMapper mapper; private final Map<Priority, Long> slaHours;
    OperationsService(ComplaintRepository complaints, ComplaintCommentRepository comments, ComplaintHistoryRepository histories,
                      WorkOrderRepository workOrders, OutboxEventRepository outbox, ObjectMapper mapper,
                      @Value("${operations.sla-hours.LOW}") long low, @Value("${operations.sla-hours.MEDIUM}") long medium,
                      @Value("${operations.sla-hours.HIGH}") long high, @Value("${operations.sla-hours.CRITICAL}") long critical) {
        this.complaints=complaints; this.comments=comments; this.histories=histories; this.workOrders=workOrders; this.outbox=outbox; this.mapper=mapper;
        this.slaHours=Map.of(Priority.LOW,low,Priority.MEDIUM,medium,Priority.HIGH,high,Priority.CRITICAL,critical);
    }
    @Transactional
    ComplaintResponse create(CreateComplaintRequest r) {
        TenantContext t=TenantContexts.current(); Instant now=Instant.now(); Complaint c=new Complaint();
        c.id=UUID.randomUUID(); c.societyId=t.societyId(); c.reportedBy=t.userId(); c.complaintNumber=number("CMP");
        c.category=r.category(); c.subcategory=r.subcategory(); c.subject=r.subject(); c.description=r.description(); c.locationDetails=r.locationDetails();
        c.priority=r.priority(); c.status=ComplaintStatus.OPEN; c.unitId=r.unitId(); c.preferredVisitAt=r.preferredVisitAt(); c.entryPermission=r.entryPermission();
        c.slaDueAt=now.plus(Duration.ofHours(slaHours.get(r.priority()))); c.createdAt=now; c.updatedAt=now; complaints.save(c);
        history(c, null, c.status, "Created", t); event(c.societyId,"Complaint",c.id,"complaint.created", response(c)); return response(c);
    }
    @Transactional(readOnly=true) Page<ComplaintResponse> list(ComplaintStatus status, Pageable page) {
        UUID society=TenantContexts.current().societyId();
        return (status == null ? complaints.findBySocietyId(society,page) : complaints.findBySocietyIdAndStatusIn(society,List.of(status),page)).map(this::response);
    }
    @Transactional(readOnly=true) ComplaintResponse get(UUID id) { return response(complaint(id)); }
    @Transactional
    ComplaintResponse update(UUID id, UpdateComplaintRequest r) {
        Complaint c=complaint(id); requireManageOrReporter(c); if(r.category()!=null)c.category=r.category(); if(r.subcategory()!=null)c.subcategory=r.subcategory();
        if(r.subject()!=null)c.subject=r.subject(); if(r.description()!=null)c.description=r.description(); if(r.locationDetails()!=null)c.locationDetails=r.locationDetails();
        if(r.priority()!=null){c.priority=r.priority(); c.slaDueAt=Instant.now().plus(Duration.ofHours(slaHours.get(r.priority())));} if(r.preferredVisitAt()!=null)c.preferredVisitAt=r.preferredVisitAt();
        if(r.entryPermission()!=null)c.entryPermission=r.entryPermission(); c.updatedAt=Instant.now(); event(c.societyId,"Complaint",c.id,"complaint.updated",response(c)); return response(c);
    }
    @Transactional
    ComplaintResponse transition(UUID id, TransitionRequest r) {
        Complaint c=complaint(id); requirePermission("COMPLAINT_MANAGE"); if(!allowed(c.status,r.status())) throw new DomainException("Illegal complaint status transition");
        TenantContext t=TenantContexts.current(); ComplaintStatus prior=c.status; c.status=r.status(); c.updatedAt=Instant.now();
        if(r.status()==ComplaintStatus.RESOLVED)c.resolvedAt=c.updatedAt; if(r.status()==ComplaintStatus.CLOSED)c.closedAt=c.updatedAt;
        history(c,prior,r.status(),r.reason(),t); event(c.societyId,"Complaint",c.id,"complaint.status-changed",response(c)); return response(c);
    }
    @Transactional
    ComplaintResponse resolve(UUID id, ResolutionRequest r) { Complaint c=complaint(id); requirePermission("COMPLAINT_MANAGE"); c.resolutionSummary=r.summary(); return transition(id,new TransitionRequest(ComplaintStatus.RESOLVED,"Resolved")); }
    @Transactional
    void feedback(UUID id, FeedbackRequest r) { Complaint c=complaint(id); if(!c.reportedBy.equals(TenantContexts.current().userId()))throw new ForbiddenException(); if(c.status!=ComplaintStatus.RESOLVED&&c.status!=ComplaintStatus.CLOSED)throw new DomainException("Feedback is allowed only after resolution"); c.residentRating=r.rating();c.residentFeedback=r.feedback();c.updatedAt=Instant.now();event(c.societyId,"Complaint",c.id,"complaint.feedback-recorded",response(c));}
    @Transactional
    CommentResponse comment(UUID id, CommentRequest r) { Complaint c=complaint(id); TenantContext t=TenantContexts.current(); if(r.internal())requirePermission("COMPLAINT_MANAGE"); ComplaintComment x=new ComplaintComment();x.id=UUID.randomUUID();x.societyId=t.societyId();x.complaintId=c.id;x.authorUserId=t.userId();x.commentText=r.comment();x.internal=r.internal();x.createdAt=Instant.now();comments.save(x);event(c.societyId,"Complaint",c.id,"complaint.comment-added",Map.of("commentId",x.id));return new CommentResponse(x.id,x.authorUserId,x.commentText,x.internal,x.createdAt); }
    @Transactional(readOnly=true) List<CommentResponse> comments(UUID id) { Complaint c=complaint(id); return comments.findByComplaintIdAndSocietyIdOrderByCreatedAtAsc(c.id,c.societyId).stream().map(x->new CommentResponse(x.id,x.authorUserId,x.commentText,x.internal,x.createdAt)).toList(); }
    @Transactional(readOnly=true) List<HistoryResponse> history(UUID id) { Complaint c=complaint(id); return histories.findByComplaintIdAndSocietyIdOrderByChangedAtAsc(c.id,c.societyId).stream().map(x->new HistoryResponse(x.id,x.oldStatus,x.newStatus,x.changedBy,x.reason,x.changedAt)).toList(); }
    @Transactional
    WorkOrderResponse createWorkOrder(CreateWorkOrderRequest r) { requirePermission("WORK_ORDER_MANAGE"); TenantContext t=TenantContexts.current(); if(r.complaintId()!=null) complaint(r.complaintId()); Instant now=Instant.now();WorkOrder w=new WorkOrder();w.id=UUID.randomUUID();w.societyId=t.societyId();w.complaintId=r.complaintId();w.workOrderNumber=number("WO");w.title=r.title();w.description=r.description();w.priority=r.priority();w.assignedUserId=r.assignedUserId();w.assignedVendorName=r.assignedVendorName();w.status=r.assignedUserId()!=null||r.assignedVendorName()!=null?WorkOrderStatus.ASSIGNED:WorkOrderStatus.CREATED;w.scheduledAt=r.scheduledAt();w.dueAt=r.dueAt();w.estimatedCost=r.estimatedCost();w.createdBy=t.userId();w.createdAt=now;w.updatedAt=now;workOrders.save(w);event(w.societyId,"WorkOrder",w.id,"work-order.created",workResponse(w));return workResponse(w); }
    @Transactional(readOnly=true) Page<WorkOrderResponse> listWorkOrders(Pageable page) { return workOrders.findBySocietyId(TenantContexts.current().societyId(),page).map(this::workResponse); }
    @Transactional
    WorkOrderResponse updateWorkOrder(UUID id, UpdateWorkOrderRequest r) { requirePermission("WORK_ORDER_MANAGE");WorkOrder w=workOrder(id);if(r.assignedUserId()!=null)w.assignedUserId=r.assignedUserId();if(r.assignedVendorName()!=null)w.assignedVendorName=r.assignedVendorName();if(r.scheduledAt()!=null)w.scheduledAt=r.scheduledAt();if(r.dueAt()!=null)w.dueAt=r.dueAt();if(r.actualCost()!=null)w.actualCost=r.actualCost();if(r.completionNotes()!=null)w.completionNotes=r.completionNotes();if(r.status()!=null){if(!workAllowed(w.status,r.status()))throw new DomainException("Illegal work-order status transition");w.status=r.status();if(r.status()==WorkOrderStatus.COMPLETED)w.completedAt=Instant.now();}w.updatedAt=Instant.now();event(w.societyId,"WorkOrder",w.id,"work-order.updated",workResponse(w));return workResponse(w); }
    @Transactional List<ComplaintResponse> escalateBreaches() { requirePermission("COMPLAINT_MANAGE"); return complaints.findBySocietyId(TenantContexts.current().societyId(),Pageable.unpaged()).stream().filter(c->c.slaDueAt!=null&&c.slaDueAt.isBefore(Instant.now())&&!Set.of(ComplaintStatus.CLOSED,ComplaintStatus.REJECTED).contains(c.status)).map(c->{event(c.societyId,"Complaint",c.id,"complaint.sla-breached",response(c));return response(c);}).toList(); }
    private Complaint complaint(UUID id){return complaints.findByIdAndSocietyId(id,TenantContexts.current().societyId()).orElseThrow(NotFoundException::new);}
    private WorkOrder workOrder(UUID id){return workOrders.findByIdAndSocietyId(id,TenantContexts.current().societyId()).orElseThrow(NotFoundException::new);}
    private void history(Complaint c, ComplaintStatus old, ComplaintStatus next,String reason,TenantContext t){ComplaintStatusHistory h=new ComplaintStatusHistory();h.id=UUID.randomUUID();h.societyId=c.societyId;h.complaintId=c.id;h.oldStatus=old;h.newStatus=next;h.changedBy=t.userId();h.reason=reason;h.changedAt=Instant.now();histories.save(h);}
    private void event(UUID society,String type,UUID id,String eventType,Object payload){OutboxEvent e=new OutboxEvent();e.id=UUID.randomUUID();e.societyId=society;e.aggregateType=type;e.aggregateId=id;e.eventType=eventType;e.eventVersion=1;e.payload=mapper.valueToTree(payload);e.headers=mapper.valueToTree(Map.of("eventId",e.id,"occurredAt",Instant.now().toString(),"correlationId",CorrelationIds.current()));e.availableAt=Instant.now();e.createdAt=e.availableAt;outbox.save(e);}
    private String number(String p){return p+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);}
    private boolean allowed(ComplaintStatus a,ComplaintStatus b){return switch(a){case OPEN->Set.of(ComplaintStatus.UNDER_REVIEW,ComplaintStatus.ASSIGNED,ComplaintStatus.REJECTED).contains(b);case UNDER_REVIEW->Set.of(ComplaintStatus.ASSIGNED,ComplaintStatus.REJECTED).contains(b);case ASSIGNED->Set.of(ComplaintStatus.IN_PROGRESS,ComplaintStatus.OPEN).contains(b);case IN_PROGRESS->Set.of(ComplaintStatus.WAITING_FOR_RESIDENT,ComplaintStatus.RESOLVED).contains(b);case WAITING_FOR_RESIDENT->Set.of(ComplaintStatus.IN_PROGRESS,ComplaintStatus.RESOLVED).contains(b);case RESOLVED->Set.of(ComplaintStatus.CLOSED,ComplaintStatus.REOPENED).contains(b);case REOPENED->Set.of(ComplaintStatus.ASSIGNED,ComplaintStatus.IN_PROGRESS).contains(b);default->false;};}
    private boolean workAllowed(WorkOrderStatus a,WorkOrderStatus b){return switch(a){case CREATED->Set.of(WorkOrderStatus.ASSIGNED,WorkOrderStatus.CANCELLED).contains(b);case ASSIGNED->Set.of(WorkOrderStatus.ACCEPTED,WorkOrderStatus.CANCELLED).contains(b);case ACCEPTED->Set.of(WorkOrderStatus.IN_PROGRESS,WorkOrderStatus.CANCELLED).contains(b);case IN_PROGRESS->Set.of(WorkOrderStatus.ON_HOLD,WorkOrderStatus.COMPLETED).contains(b);case ON_HOLD->Set.of(WorkOrderStatus.IN_PROGRESS,WorkOrderStatus.CANCELLED).contains(b);case COMPLETED->b==WorkOrderStatus.VERIFIED;default->false;};}
    private void requirePermission(String p){if(!TenantContexts.current().permissions().contains(p))throw new ForbiddenException();}
    private void requireManageOrReporter(Complaint c){if(!c.reportedBy.equals(TenantContexts.current().userId()))requirePermission("COMPLAINT_MANAGE");}
    private ComplaintResponse response(Complaint c){return new ComplaintResponse(c.id,c.complaintNumber,c.category,c.subject,c.description,c.priority,c.status,c.assignedUserId,c.slaDueAt,c.resolvedAt,c.closedAt,c.createdAt,c.updatedAt,c.version==null?0:c.version);}
    private WorkOrderResponse workResponse(WorkOrder w){return new WorkOrderResponse(w.id,w.workOrderNumber,w.complaintId,w.title,w.priority,w.status,w.assignedUserId,w.dueAt,w.completedAt,w.version==null?0:w.version);}
}
