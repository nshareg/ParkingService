package com.parkingsystem.impl;

import com.parkingsystem.contract.ParkingRepository;
import com.parkingsystem.contract.ParkingService;
import com.parkingsystem.contract.ParkingSessionRepository;
import com.parkingsystem.entity.ParkingSession;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import(ParkingServiceImpl.class)
class ParkingServiceImplTest {

    @MockitoBean
    private ParkingRepository repo;

    @MockitoBean
    private ParkingSessionRepository sessionRepo;

    @Autowired
    private ParkingService service;

    @BeforeEach
    void stubSaveToReturnArgument() {
        // JpaRepository.save() returns the persisted entity.
        // Without this stub the mock returns null, which breaks service logic
        // that passes the returned value back to the caller.
        when(repo.save(any(ParkingSlot.class))).thenAnswer(i -> i.getArgument(0));
        when(sessionRepo.save(any(ParkingSession.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ── addSlot ──────────────────────────────────────────────────────────────

    @Test
    void addSlotPersists() {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);

        assertNotNull(slot.getSlotID());
        assertEquals(SlotType.REGULAR, slot.getType());
        assertFalse(slot.isBooked());
        verify(repo).save(slot);
    }

    // ── park ─────────────────────────────────────────────────────────────────

    @Test
    void parkAssignsFreeSlot() {
        ParkingSlot free = new ParkingSlot(SlotType.REGULAR);
        when(repo.findFirstByTypeAndBookedFalse(SlotType.REGULAR)).thenReturn(Optional.of(free));
        when(repo.findByNumberPlate("AREG-1")).thenReturn(Optional.empty());

        Optional<ParkingSlot> parked = service.park("AREG-1");

        assertTrue(parked.isPresent());
        assertTrue(parked.get().isBooked());
        assertEquals("AREG-1", parked.get().getNumberPlate());

        verify(repo).save(free);

        assertEquals(1, parked.get().getSessions().size(), "Session must be cascaded onto the slot");
        ParkingSession activeSession = parked.get().getSessions().get(0);
        assertEquals("AREG-1", activeSession.getNumberPlate());
        assertTrue(activeSession.isActive());

    }

    @Test
    void parkSelectsMatchingType() {
        ParkingSlot regular  = new ParkingSlot(SlotType.REGULAR);
        ParkingSlot electric = new ParkingSlot(SlotType.ELECTRIC);
        when(repo.findFirstByTypeAndBookedFalse(SlotType.ELECTRIC)).thenReturn(Optional.of(electric));
        when(repo.findByNumberPlate("AREG-1")).thenReturn(Optional.empty());

        Optional<ParkingSlot> parked = service.park("AREG-1", SlotType.ELECTRIC);

        assertTrue(parked.isPresent());
        assertEquals(SlotType.ELECTRIC, parked.get().getType());
        verify(repo).save(electric);
        verify(repo, never()).save(regular);
    }

    @Test
    void parkReturnsEmptyWhenNoFreeSlotOfType() {
        when(repo.findFirstByTypeAndBookedFalse(any(SlotType.class))).thenReturn(Optional.empty());

        assertTrue(service.park("AREG-1").isEmpty());
        verify(repo, never()).save(any());
        verify(sessionRepo, never()).save(any());
    }

    @Test
    void parkReturnsEmptyWhenPlateAlreadyParked() {
        ParkingSlot free   = new ParkingSlot(SlotType.REGULAR);
        ParkingSlot booked = new ParkingSlot(SlotType.REGULAR);
        booked.book("DUP-1");
        when(repo.findFirstByTypeAndBookedFalse(SlotType.REGULAR)).thenReturn(Optional.of(free));
        // plate is already in an active slot → duplicate guard triggers
        when(repo.findByNumberPlate("DUP-1")).thenReturn(Optional.of(booked));

        assertTrue(service.park("DUP-1").isEmpty());
        verify(repo, never()).save(any());
        verify(sessionRepo, never()).save(any());
    }

    // ── release ──────────────────────────────────────────────────────────────

    @Test
    void releaseUnbooksSlot() {
        ParkingSlot booked = new ParkingSlot(SlotType.REGULAR);
        booked.book("AREG-1");
        ParkingSession activeSession = new ParkingSession(booked, "AREG-1");
        when(repo.findByNumberPlate("AREG-1")).thenReturn(Optional.of(booked));
        when(sessionRepo.findByActiveTrueAndNumberPlate("AREG-1"))
                .thenReturn(Optional.of(activeSession));

        Optional<ParkingSlot> released = service.release("AREG-1");

        assertTrue(released.isPresent());
        assertFalse(released.get().isBooked());
        verify(repo).save(booked);
        verify(sessionRepo).save(activeSession);
        assertFalse(activeSession.isActive(), "session must be closed after release");
    }

    @Test
    void releaseUnknownPlateReturnsEmpty() {
        when(repo.findByNumberPlate("AREG-2")).thenReturn(Optional.empty());

        assertTrue(service.release("AREG-2").isEmpty());
        verify(repo, never()).save(any());
        verify(sessionRepo, never()).save(any());
    }

    // ── removeSlot ───────────────────────────────────────────────────────────

    @Test
    void removeSlotReturnsSlotAndDelegatesDelete() {
        UUID id   = UUID.randomUUID();
        ParkingSlot slot = new ParkingSlot(SlotType.DISABLED);
        when(repo.findById(id)).thenReturn(Optional.of(slot));

        Optional<ParkingSlot> removed = service.removeSlot(id);

        assertTrue(removed.isPresent());
        verify(repo).delete(slot);
    }

    @Test
    void removeSlot_unknownId_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        Optional<ParkingSlot> removed = service.removeSlot(id);

        assertTrue(removed.isEmpty());
        verify(repo, never()).delete(any());
    }

    // ── findByType ───────────────────────────────────────────────────────────

    @Test
    void findByTypeFilters() {
        List<ParkingSlot> regulars = List.of(
                new ParkingSlot(SlotType.REGULAR),
                new ParkingSlot(SlotType.REGULAR));
        when(repo.findByType(SlotType.REGULAR)).thenReturn(regulars);

        assertEquals(2, service.findByType(SlotType.REGULAR).size());
    }
}
