package com.learnia.models.study;

import java.util.List;

public record StudyProblemSet(
        String documentLanguage,
        String documentSummary,
        List<StudyProblem> problems) {
}
