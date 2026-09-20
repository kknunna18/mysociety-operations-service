package com.mysociety.operations;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/complaints")
class ComplaintController {
    private final OperationsService service;
    ComplaintController(OperationsService service) { this.service = service; }
    @PostMapping @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_CREATE')")
    ResponseEntity<ComplaintResponse> create(@Valid @RequestBody CreateComplaintRequest request) {
        ComplaintResponse response=service.create(request); return ResponseEntity.created(URI.create("/api/v1/complaints/"+response.id())).body(response);
    }
    @GetMapping @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_VIEW')")
    Page<ComplaintResponse> list(@RequestParam(required=false) ComplaintStatus status,
             @PageableDefault(sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) { return service.list(status,pageable); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_VIEW')")
    ComplaintResponse detail(@PathVariable UUID id) { return service.get(id); }
    @PatchMapping("/{id}") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_CREATE') or hasAuthority('PERMISSION_COMPLAINT_MANAGE')")
    ComplaintResponse update(@PathVariable UUID id,@Valid @RequestBody UpdateComplaintRequest request){return service.update(id,request);}
    @PostMapping("/{id}/comments") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_VIEW')")
    ResponseEntity<CommentResponse> comment(@PathVariable UUID id,@Valid @RequestBody CommentRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.comment(id,request));}
    @GetMapping("/{id}/comments") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_VIEW')")
    List<CommentResponse> comments(@PathVariable UUID id){return service.comments(id);}
    @PostMapping("/{id}/transitions") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_MANAGE')")
    ComplaintResponse transition(@PathVariable UUID id,@Valid @RequestBody TransitionRequest request){return service.transition(id,request);}
    @GetMapping("/{id}/history") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_VIEW')")
    List<HistoryResponse> history(@PathVariable UUID id){return service.history(id);}
    @PostMapping("/{id}/resolution") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_MANAGE')")
    ComplaintResponse resolve(@PathVariable UUID id,@Valid @RequestBody ResolutionRequest request){return service.resolve(id,request);}
    @PostMapping("/{id}/feedback") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_CREATE')")
    ResponseEntity<Void> feedback(@PathVariable UUID id,@Valid @RequestBody FeedbackRequest request){service.feedback(id,request);return ResponseEntity.noContent().build();}
    @PostMapping("/sla/escalations") @PreAuthorize("hasAuthority('PERMISSION_COMPLAINT_MANAGE')")
    List<ComplaintResponse> escalate(){return service.escalateBreaches();}
}

@RestController
@RequestMapping("/work-orders")
class WorkOrderController {
    private final OperationsService service;
    WorkOrderController(OperationsService service) { this.service=service; }
    @PostMapping @PreAuthorize("hasAuthority('PERMISSION_WORK_ORDER_MANAGE')")
    ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody CreateWorkOrderRequest request){WorkOrderResponse r=service.createWorkOrder(request);return ResponseEntity.created(URI.create("/api/v1/work-orders/"+r.id())).body(r);}
    @GetMapping @PreAuthorize("hasAuthority('PERMISSION_WORK_ORDER_MANAGE')")
    Page<WorkOrderResponse> list(@PageableDefault(sort="createdAt",direction=Sort.Direction.DESC) Pageable pageable){return service.listWorkOrders(pageable);}
    @PatchMapping("/{id}") @PreAuthorize("hasAuthority('PERMISSION_WORK_ORDER_MANAGE')")
    WorkOrderResponse update(@PathVariable UUID id,@Valid @RequestBody UpdateWorkOrderRequest request){return service.updateWorkOrder(id,request);}
}
