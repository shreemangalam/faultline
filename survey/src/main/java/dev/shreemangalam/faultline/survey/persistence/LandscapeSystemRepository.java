package dev.shreemangalam.faultline.survey.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LandscapeSystemRepository extends JpaRepository<LandscapeSystemEntity, UUID> {

    List<LandscapeSystemEntity> findAllByImportIdOrderBySystemId(UUID importId);
}
