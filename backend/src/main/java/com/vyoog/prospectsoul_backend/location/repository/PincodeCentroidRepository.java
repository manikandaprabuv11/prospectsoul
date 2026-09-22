package com.vyoog.prospectsoul_backend.location.repository;

import java.util.Optional;

import com.vyoog.prospectsoul_backend.location.entity.PincodeCentroid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PincodeCentroidRepository extends JpaRepository<PincodeCentroid, String> {
    Optional<PincodeCentroid> findByPincode(String pincode);
}
