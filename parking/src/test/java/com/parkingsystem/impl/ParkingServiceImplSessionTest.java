package com.parkingsystem.impl;

import com.parkingsystem.contract.ParkingRepository;
import com.parkingsystem.contract.ParkingSessionRepository;
import com.parkingsystem.entity.ParkingSession;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/*
    Created by anshanyan
    on 08.06.26
*/
@ExtendWith(MockitoExtension.class)
class ParkingServiceImplSessionTest {

    @Mock private ParkingRepository repo;
    @Mock private ParkingSessionRepository sessionRepo;

    @Test
    void parkOpensSessionLinkingSlotAndPlate() throws SQLException {
        ParkingSlot free = new ParkingSlot(SlotType.REGULAR);
        when(repo.findAllFree()).thenReturn(List.of(free));
        ParkingServiceImpl service = new ParkingServiceImpl(repo, sessionRepo);

        service.park("AREG-1");

        ArgumentCaptor<ParkingSession> captor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(sessionRepo).add(captor.capture());
        ParkingSession opened = captor.getValue();
        assertEquals(free.getSlotID(), opened.getSlotId());
        assertEquals("AREG-1", opened.getNumberPlate());
        assertTrue(opened.isActive());
    }

    @Test
    void releaseClosesActiveSession() throws SQLException {
        ParkingSlot booked = new ParkingSlot(SlotType.REGULAR);
        booked.book("AREG-1");
        ParkingSession open = new ParkingSession(booked.getSlotID(), "AREG-1");
        when(repo.findByNumberPlate("AREG-1")).thenReturn(Optional.of(booked));
        when(sessionRepo.findActiveByNumberPlate("AREG-1")).thenReturn(Optional.of(open));
        ParkingServiceImpl service = new ParkingServiceImpl(repo, sessionRepo);

        service.release("AREG-1");

        verify(sessionRepo).update(open);
        assertFalse(open.isActive(), "the session must be closed on release");
        assertNotNull(open.getReleasedAt());
    }

    @Test
    void releaseOfUnknownPlateTouchesNoSession() throws SQLException {
        when(repo.findByNumberPlate("GHOST")).thenReturn(Optional.empty());
        ParkingServiceImpl service = new ParkingServiceImpl(repo, sessionRepo);

        assertTrue(service.release("GHOST").isEmpty());
        verifyNoInteractions(sessionRepo);
    }

    @Test
    void historyMethodsDelegateToSessionRepository() throws SQLException {
        UUID slotId = UUID.randomUUID();
        ParkingSession s = new ParkingSession(slotId, "AREG-1");
        when(sessionRepo.findBySlot(slotId)).thenReturn(List.of(s));
        when(sessionRepo.findByNumberPlate("AREG-1")).thenReturn(List.of(s));
        when(sessionRepo.findAll()).thenReturn(List.of(s));
        ParkingServiceImpl service = new ParkingServiceImpl(repo, sessionRepo);

        assertEquals(1, service.slotHistory(slotId).size());
        assertEquals(1, service.plateHistory("AREG-1").size());
        assertEquals(1, service.allSessions().size());
    }

    @Test
    void legacyConstructorRecordsNoSessionsAndHistoryIsEmpty() throws SQLException {
        ParkingSlot free = new ParkingSlot(SlotType.REGULAR);
        when(repo.findAllFree()).thenReturn(List.of(free));
        ParkingServiceImpl service = new ParkingServiceImpl(repo); // single-arg, no session repo

        assertTrue(service.park("AREG-1").isPresent());
        verify(repo).update(free);

        assertTrue(service.allSessions().isEmpty());
        assertTrue(service.slotHistory(free.getSlotID()).isEmpty());
        assertTrue(service.plateHistory("AREG-1").isEmpty());
        verifyNoInteractions(sessionRepo);
    }
}
