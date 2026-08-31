package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.IntegrationEdge;
import dev.shreemangalam.faultline.survey.domain.Landscape;
import dev.shreemangalam.faultline.survey.domain.SystemNode;
import dev.shreemangalam.faultline.survey.persistence.ActiveLandscapeEntity;
import dev.shreemangalam.faultline.survey.persistence.ActiveLandscapeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActiveLandscapeService {

    private final ActiveLandscapeRepository activeLandscapeRepository;
    private final LandscapeImportPersistenceService importPersistenceService;
    private final Landscape bundledLandscape;

    public ActiveLandscapeService(
            ActiveLandscapeRepository activeLandscapeRepository,
            LandscapeImportPersistenceService importPersistenceService,
            Landscape bundledLandscape) {
        this.activeLandscapeRepository = activeLandscapeRepository;
        this.importPersistenceService = importPersistenceService;
        this.bundledLandscape = bundledLandscape;
    }

    @Transactional(readOnly = true)
    public Landscape currentLandscape() {
        return activeLandscapeRepository.findById(ActiveLandscapeEntity.SELECTION_SLOT)
                .flatMap(selection -> importPersistenceService.findById(selection.getImportId()))
                .map(this::toLandscape)
                .orElse(bundledLandscape);
    }

    @Transactional(readOnly = true)
    public Optional<StoredLandscape> activeLandscape() {
        return activeLandscapeRepository.findById(ActiveLandscapeEntity.SELECTION_SLOT)
                .flatMap(selection -> importPersistenceService.findById(selection.getImportId()));
    }

    @Transactional
    public Optional<StoredLandscape> activate(UUID importId) {
        Optional<StoredLandscape> landscape = importPersistenceService.findById(importId);
        landscape.ifPresent(ignored -> activeLandscapeRepository.save(new ActiveLandscapeEntity(importId, Instant.now())));
        return landscape;
    }

    private Landscape toLandscape(StoredLandscape storedLandscape) {
        return new Landscape(
                storedLandscape.systems().stream()
                        .map(system -> new SystemNode(new SystemId(system.systemId()), system.label(), system.criticality()))
                        .toList(),
                storedLandscape.integrations().stream()
                        .map(integration -> new IntegrationEdge(
                                integration.integrationId(),
                                new SystemId(integration.sourceSystemId()),
                                new SystemId(integration.targetSystemId()),
                                integration.protocol(),
                                integration.dataDomain(),
                                dev.shreemangalam.faultline.survey.domain.SlaClass.fromValue(integration.slaClass())))
                        .toList());
    }
}