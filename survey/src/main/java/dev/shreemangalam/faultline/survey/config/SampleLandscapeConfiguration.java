package dev.shreemangalam.faultline.survey.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shreemangalam.faultline.shared.domain.SystemId;
import dev.shreemangalam.faultline.survey.domain.IntegrationEdge;
import dev.shreemangalam.faultline.survey.domain.Landscape;
import dev.shreemangalam.faultline.survey.domain.SlaClass;
import dev.shreemangalam.faultline.survey.domain.SystemNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

@Configuration
public class SampleLandscapeConfiguration {

    @Bean
    Landscape sampleLandscape(ObjectMapper objectMapper) {
        try {
            SampleLandscapeDefinition definition = objectMapper.readValue(
                    new ClassPathResource("landscape.json").getInputStream(), SampleLandscapeDefinition.class);
            return definition.toLandscape();
        } catch (IOException exception) {
            throw new IllegalStateException("could not load the bundled sample landscape", exception);
        }
    }

    private record SampleLandscapeDefinition(List<SampleSystem> systems, List<SampleIntegration> integrations) {

        Landscape toLandscape() {
            return new Landscape(
                    systems.stream().map(system -> new SystemNode(
                            new SystemId(system.id()), system.label(), system.criticality())).toList(),
                    integrations.stream().map(integration -> new IntegrationEdge(
                            integration.id(),
                            new SystemId(integration.sourceSystemId()),
                            new SystemId(integration.targetSystemId()),
                            integration.protocol(),
                            integration.dataDomain(),
                            SlaClass.fromValue(integration.slaClass()))).toList());
        }
    }

    private record SampleSystem(String id, String label, int criticality) {
    }

    private record SampleIntegration(
            String id,
            String sourceSystemId,
            String targetSystemId,
            String protocol,
            String dataDomain,
            String slaClass) {
    }
}