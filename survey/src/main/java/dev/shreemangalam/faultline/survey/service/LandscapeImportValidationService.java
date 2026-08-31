package dev.shreemangalam.faultline.survey.service;

import dev.shreemangalam.faultline.survey.domain.LandscapeImportValidation;
import dev.shreemangalam.faultline.survey.domain.LandscapeImportValidator;
import org.springframework.stereotype.Service;

@Service
public class LandscapeImportValidationService {

    private final LandscapeImportValidator validator = new LandscapeImportValidator();

    public LandscapeImportValidation validate(LandscapeImportValidator.ImportDefinition definition) {
        return validator.validate(definition);
    }
}
