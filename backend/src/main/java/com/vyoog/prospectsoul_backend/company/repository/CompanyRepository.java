package com.vyoog.prospectsoul_backend.company.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {

    Optional<Company> findByPrimaryPhoneNormalized(String phone);

    Optional<Company> findByWebsiteDomain(String domain);

    @Query("SELECT c FROM Company c WHERE c.normalizedName = :name AND c.city = :city")
    Optional<Company> findByNormalizedNameAndCity(@Param("name") String normalizedName, @Param("city") String city);

    List<Company> findByPrimaryPhoneNormalizedIn(List<String> phones);

    List<Company> findByWebsiteDomainIn(List<String> domains);

    Optional<Company> findBySourceAndSourceReference(String source, String sourceReference);

    @Query("SELECT c FROM Company c WHERE c.normalizedName = :name AND c.pincode = :pincode")
    Optional<Company> findByNormalizedNameAndPincode(@Param("name") String normalizedName,
                                                     @Param("pincode") String pincode);
}
