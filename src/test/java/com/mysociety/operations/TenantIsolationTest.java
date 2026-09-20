package com.mysociety.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantIsolationTest {
    private ComplaintRepository complaints;
    private UUID societyId;
    private OperationsService service;

    @BeforeEach void setUp() {
        complaints=mock(ComplaintRepository.class); societyId=UUID.randomUUID();
        service=new OperationsService(complaints,mock(ComplaintCommentRepository.class),mock(ComplaintHistoryRepository.class),
                mock(WorkOrderRepository.class),mock(OutboxEventRepository.class),new ObjectMapper(),72,48,24,4);
        Jwt jwt=Jwt.withTokenValue("test").header("alg","HS256").subject(UUID.randomUUID().toString())
                .claim("society_id",societyId.toString()).claim("permissions",List.of("COMPLAINT_VIEW")).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void detailLookupAlwaysUsesClaimSociety() {
        UUID complaintId=UUID.randomUUID(); Complaint complaint=new Complaint(); complaint.id=complaintId; complaint.societyId=societyId;
        complaint.complaintNumber="CMP-1"; complaint.category="MAINTENANCE"; complaint.subject="Leak"; complaint.description="Leak";
        complaint.priority=Priority.MEDIUM; complaint.status=ComplaintStatus.OPEN; complaint.createdAt=Instant.now(); complaint.updatedAt=Instant.now();
        when(complaints.findByIdAndSocietyId(complaintId,societyId)).thenReturn(Optional.of(complaint));

        assertThat(service.get(complaintId).id()).isEqualTo(complaintId);
        verify(complaints).findByIdAndSocietyId(complaintId,societyId);
        verify(complaints,never()).findById(complaintId);
    }
}
