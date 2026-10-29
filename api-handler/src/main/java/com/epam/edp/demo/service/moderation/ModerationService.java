package com.epam.edp.demo.service.moderation;

import com.epam.edp.demo.dto.moderation.ModerationResult;

public interface ModerationService {

    ModerationResult moderate(String content);
}
