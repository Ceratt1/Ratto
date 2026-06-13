package com.learnia.questiongenerator.model;

import com.learnia.models.study.StudyProblemSet;

public record GeneratedProblems(
        StudyProblemSet problemSet,
        String aiProvider,
        String aiModel) {

    public int problemCount() {
        return problemSet.problems().size();
    }
}
