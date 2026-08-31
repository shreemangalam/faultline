package dev.shreemangalam.faultline.survey.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveLandscapeRepository extends JpaRepository<ActiveLandscapeEntity, Short> {
}