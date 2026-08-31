package dev.shreemangalam.faultline.survey.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LandscapeIntegrationRepository extends JpaRepository<LandscapeIntegrationEntity, UUID> {

    List<LandscapeIntegrationEntity> findAllByImportIdOrderByIntegrationId(UUID importId);
}
