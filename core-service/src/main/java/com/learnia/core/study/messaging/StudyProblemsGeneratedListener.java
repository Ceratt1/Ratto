package com.learnia.core.study.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.learnia.core.study.services.StudyService;
import com.learnia.events.EventTopics;
import com.learnia.events.StudyProblemsGeneratedEvent;

@Component
public class StudyProblemsGeneratedListener {

    private final StudyService studyService;

    public StudyProblemsGeneratedListener(StudyService studyService) {
        this.studyService = studyService;
    }

    @KafkaListener(
            topics = EventTopics.STUDY_PROBLEMS_GENERATED,
            groupId = "core-service-study-projection",
            containerFactory = "studyProblemsKafkaListenerContainerFactory")
    public void consume(StudyProblemsGeneratedEvent event) {
        studyService.projectGeneratedProblems(event);
    }
}
