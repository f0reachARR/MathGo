package me.f0reach.mathgo.quiz;

public record QuizQuestion(
        String id,
        String expression,
        String displayText,
        long answer,
        Difficulty difficulty,
        QuestionType type,
        int timeLimitSeconds
) {
}
