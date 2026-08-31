package dev.shreemangalam.faultline.survey.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LandscapeImportRepository extends JpaRepository<LandscapeImportEntity, UUID> {
}
