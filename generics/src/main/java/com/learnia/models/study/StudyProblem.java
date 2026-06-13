package com.learnia.models.study;

import java.util.List;

public record StudyProblem(
        String question,
        String subject,
        String difficulty,
        String generalExplanation,
        List<StudyAnswer> answers) {
}
