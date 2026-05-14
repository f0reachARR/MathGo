package me.f0reach.mathgo.quiz;

public interface QuestionProvider {
    QuizQuestion next(Difficulty difficulty);
}
