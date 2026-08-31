package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.survey.domain.LandscapeImportValidation;
import dev.shreemangalam.faultline.survey.domain.LandscapeImportValidator;
import dev.shreemangalam.faultline.survey.persistence.LandscapeImportEntity;
import dev.shreemangalam.faultline.survey.persistence.LandscapeImportRepository;
import dev.shreemangalam.faultline.survey.persistence.LandscapeIntegrationEntity;
import dev.shreemangalam.faultline.survey.persistence.LandscapeIntegrationRepository;
import dev.shreemangalam.faultline.survey.persistence.LandscapeSystemEntity;
import dev.shreemangalam.faultline.survey.persistence.LandscapeSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LandscapeImportPersistenceService {

    private final LandscapeImportRepository importRepository;
    private final LandscapeSystemRepository systemRepository;
    private final LandscapeIntegrationRepository integrationRepository;
    private final LandscapeImportValidator validator = new LandscapeImportValidator();

    public LandscapeImportPersistenceService(
            LandscapeImportRepository importRepository,
            LandscapeSystemRepository systemRepository,
            LandscapeIntegrationRepository integrationRepository) {
        this.importRepository = importRepository;
        this.systemRepository = systemRepository;
        this.integrationRepository = integrationRepository;
    }

    @Transactional
    public StoredLandscape store(LandscapeImportValidator.ImportDefinition definition) {
        LandscapeImportValidation validation = validator.validate(definition);
        if (!validation.valid()) {
            throw new IllegalArgumentException("cannot persist an invalid landscape import");
        }

        UUID importId = UUID.randomUUID();
        importRepository.save(new LandscapeImportEntity(importId, Instant.now()));
        systemRepository.saveAll(definition.systems().stream()
                .map(system -> new LandscapeSystemEntity(
                        UUID.randomUUID(), importId, system.id(), system.label(), system.criticality()))
                .toList());
        integrationRepository.saveAll(definition.integrations().stream()
                .map(integration -> new LandscapeIntegrationEntity(
                        UUID.randomUUID(),
                        importId,
                        integration.id(),
                        integration.sourceSystemId(),
                        integration.targetSystemId(),
                        integration.protocol(),
                        integration.dataDomain(),
                        integration.slaClass()))
                .toList());
        return findById(importId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Optional<StoredLandscape> findById(UUID importId) {
        return importRepository.findById(importId).map(importEntity -> new StoredLandscape(
                importEntity.getId(),
                importEntity.getCreatedAt(),
                systemRepository.findAllByImportIdOrderBySystemId(importId).stream()
                        .map(system -> new StoredLandscape.SystemSnapshot(
                                system.getSystemId(), system.getLabel(), system.getCriticality()))
                        .toList(),
                integrationRepository.findAllByImportIdOrderByIntegrationId(importId).stream()
                        .map(integration -> new StoredLandscape.IntegrationSnapshot(
                                integration.getIntegrationId(),
                                integration.getSourceSystemId(),
                                integration.getTargetSystemId(),
                                integration.getProtocol(),
                                integration.getDataDomain(),
                                integration.getSlaClass()))
                        .toList()));
    }
}
