package me.f0reach.mathgo.quiz;

import java.util.OptionalLong;

public final class AnswerJudge {
    private AnswerJudge() {}

    public static OptionalLong parseInt(String raw) {
        if (raw == null) return OptionalLong.empty();
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return OptionalLong.empty();
        int start = 0;
        if (trimmed.charAt(0) == '+' || trimmed.charAt(0) == '-') {
            if (trimmed.length() == 1) return OptionalLong.empty();
            start = 1;
        }
        for (int i = start; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch < '0' || ch > '9') {
                return OptionalLong.empty();
            }
        }
        try {
            return OptionalLong.of(Long.parseLong(trimmed));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    public static boolean isCorrect(QuizQuestion question, long answer) {
        return question.answer() == answer;
    }
}
