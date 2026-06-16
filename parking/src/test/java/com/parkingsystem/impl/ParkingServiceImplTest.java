package com.parkingsystem.impl;

import com.parkingsystem.contract.ParkingRepository;
import com.parkingsystem.contract.ParkingSessionRepository;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/*
    Created by anshanyan
    on 27.05.26
*/
@ExtendWith(MockitoExtension.class)
class ParkingServiceImplTest {

    @Mock private ParkingRepository repo;
    @Mock private ParkingSessionRepository sessionRepo;
    private ParkingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ParkingServiceImpl(repo, sessionRepo);
    }

    @Test
    void addSlotPersists() {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);

        assertNotNull(slot.getSlotID());
        assertEquals(SlotType.REGULAR, slot.getType());
        assertFalse(slot.isBooked());
        verify(repo).save(slot);
    }

    @Test
    void parkAssignsFreeSlot() {
        ParkingSlot free = new ParkingSlot(SlotType.REGULAR);
        when(repo.findByBookedFalse()).thenReturn(List.of(free));

        Optional<ParkingSlot> parked = service.park("AREG-1");

        assertTrue(parked.isPresent());
        assertTrue(parked.get().isBooked());
        assertEquals("AREG-1", parked.get().getNumberPlate());
        verify(repo).save(free);
    }

    @Test
    void parkSelectsMatchingType() {
        ParkingSlot regular = new ParkingSlot(SlotType.REGULAR);
        ParkingSlot electric = new ParkingSlot(SlotType.ELECTRIC);
        when(repo.findByBookedFalse()).thenReturn(List.of(regular, electric));

        Optional<ParkingSlot> parked = service.park("AREG-1", SlotType.ELECTRIC);

        assertTrue(parked.isPresent());
        assertEquals(SlotType.ELECTRIC, parked.get().getType());
        verify(repo).save(electric);
    }

    @Test
    void parkReturnsEmptyWhenFull() {
        when(repo.findByBookedFalse()).thenReturn(List.of());

        assertTrue(service.park("AREG-1").isEmpty());
        verify(repo, never()).save(any());
    }

    @Test
    void releaseUnbooksSlot() {
        ParkingSlot booked = new ParkingSlot(SlotType.REGULAR);
        booked.book("AREG-1");
        when(repo.findByNumberPlate("AREG-1")).thenReturn(Optional.of(booked));

        Optional<ParkingSlot> released = service.release("AREG-1");

        assertTrue(released.isPresent());
        assertFalse(released.get().isBooked());
        verify(repo).save(booked);
    }

    @Test
    void releaseUnknownPlateReturnsEmpty() {
        when(repo.findByNumberPlate("AREG-2")).thenReturn(Optional.empty());

        assertTrue(service.release("AREG-2").isEmpty());
        verify(repo, never()).save(any());
    }

    @Test
    void removeSlotDelegates() {
        UUID id = UUID.randomUUID();
        ParkingSlot slot = new ParkingSlot(SlotType.DISABLED);
        when(repo.findById(id)).thenReturn(Optional.of(slot));

        assertTrue(service.removeSlot(id).isPresent());
        verify(repo).delete(slot);
    }

    @Test
    void findByTypeFilters() {
        List<ParkingSlot> regulars = List.of(
                new ParkingSlot(SlotType.REGULAR),
                new ParkingSlot(SlotType.REGULAR));
        when(repo.findByType(SlotType.REGULAR)).thenReturn(regulars);

        assertEquals(2, service.findByType(SlotType.REGULAR).size());
    }
}
