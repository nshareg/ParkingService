package com.parkingsystem.impl;

import com.parkingsystem.contract.ParkingRepository;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    on 27.05.26
*/
@ExtendWith(MockitoExtension.class)
class ParkingServiceImplTest {

    @Mock private ParkingRepository repo;
    private ParkingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ParkingServiceImpl(repo, null);
    }

    @Test
    void addSlotPersists() throws SQLException {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);

        assertNotNull(slot.getSlotID());
        assertEquals(SlotType.REGULAR, slot.getType());
        assertFalse(slot.isBooked());
        verify(repo).add(slot);
    }

    @Test
    void parkAssignsFreeSlot() throws SQLException {
        ParkingSlot free = new ParkingSlot(SlotType.REGULAR);
        when(repo.findAllFree()).thenReturn(List.of(free));

        Optional<ParkingSlot> parked = service.park("AREG-1");

        assertTrue(parked.isPresent());
        assertTrue(parked.get().isBooked());
        assertEquals("AREG-1", parked.get().getNumberPlate());
        verify(repo).update(free);
    }

    @Test
    void parkSelectsMatchingType() throws SQLException {
        ParkingSlot regular = new ParkingSlot(SlotType.REGULAR);
        ParkingSlot electric = new ParkingSlot(SlotType.ELECTRIC);
        when(repo.findAllFree()).thenReturn(List.of(regular, electric));

        Optional<ParkingSlot> parked = service.park("AREG-1", SlotType.ELECTRIC);

        assertTrue(parked.isPresent());
        assertEquals(SlotType.ELECTRIC, parked.get().getType());
        verify(repo).update(electric);
    }

    @Test
    void parkReturnsEmptyWhenFull() throws SQLException {
        when(repo.findAllFree()).thenReturn(List.of());

        assertTrue(service.park("AREG-1").isEmpty());
        verify(repo, never()).update(any());
    }

    @Test
    void releaseUnbooksSlot() throws SQLException {
        ParkingSlot booked = new ParkingSlot(SlotType.REGULAR);
        booked.book("AREG-1");
        when(repo.findByNumberPlate("AREG-1")).thenReturn(Optional.of(booked));

        Optional<ParkingSlot> released = service.release("AREG-1");

        assertTrue(released.isPresent());
        assertFalse(released.get().isBooked());
        verify(repo).update(booked);
    }

    @Test
    void releaseUnknownPlateReturnsEmpty() throws SQLException {
        when(repo.findByNumberPlate("AREG-2")).thenReturn(Optional.empty());

        assertTrue(service.release("AREG-2").isEmpty());
        verify(repo, never()).update(any());
    }

    @Test
    void removeSlotDelegates() throws SQLException {
        UUID id = UUID.randomUUID();
        ParkingSlot slot = new ParkingSlot(id, SlotType.DISABLED);
        when(repo.remove(id)).thenReturn(Optional.of(slot));

        assertTrue(service.removeSlot(id).isPresent());
        verify(repo).remove(id);
    }

    @Test
    void findByTypeFilters() throws SQLException {
        List<ParkingSlot> regulars = List.of(
                new ParkingSlot(SlotType.REGULAR),
                new ParkingSlot(SlotType.REGULAR));
        when(repo.findByType(SlotType.REGULAR)).thenReturn(regulars);

        assertEquals(2, service.findByType(SlotType.REGULAR).size());
    }
}
