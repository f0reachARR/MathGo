package me.f0reach.mathgo.quiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GeneratedQuestionProvider implements QuestionProvider {
    private final List<QuestionType> typePool;
    private final int easyTime;
    private final int normalTime;
    private final int hardTime;

    public GeneratedQuestionProvider(Set<QuestionType> enabledTypes, int easyTime, int normalTime, int hardTime) {
        List<QuestionType> pool = new ArrayList<>(enabledTypes);
        if (pool.isEmpty()) {
            pool.add(QuestionType.ADD);
        }
        this.typePool = pool;
        this.easyTime = easyTime;
        this.normalTime = normalTime;
        this.hardTime = hardTime;
    }

    @Override
    public QuizQuestion next(Difficulty difficulty) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        QuestionType type = typePool.get(rng.nextInt(typePool.size()));
        if (type == QuestionType.MIXED) {
            return generateMixed(difficulty, rng);
        }
        return generateBinary(type, difficulty, rng);
    }

    private QuizQuestion generateBinary(QuestionType type, Difficulty difficulty, ThreadLocalRandom rng) {
        int rangeMax = switch (difficulty) {
            case EASY -> 10;
            case NORMAL -> 30;
            case HARD -> 99;
        };
        int a;
        int b;
        long answer;
        String expr;
        switch (type) {
            case ADD -> {
                a = rng.nextInt(1, rangeMax + 1);
                b = rng.nextInt(1, rangeMax + 1);
                answer = (long) a + b;
                expr = a + " + " + b;
            }
            case SUB -> {
                a = rng.nextInt(1, rangeMax + 1);
                b = rng.nextInt(1, a + 1);
                answer = (long) a - b;
                expr = a + " - " + b;
            }
            case MUL -> {
                int mulMax = switch (difficulty) {
                    case EASY -> 5;
                    case NORMAL -> 9;
                    case HARD -> 12;
                };
                a = rng.nextInt(2, mulMax + 1);
                b = rng.nextInt(2, mulMax + 1);
                answer = (long) a * b;
                expr = a + " × " + b;
            }
            case DIV -> {
                int divMax = switch (difficulty) {
                    case EASY -> 5;
                    case NORMAL -> 9;
                    case HARD -> 12;
                };
                b = rng.nextInt(2, divMax + 1);
                int quotient = rng.nextInt(2, divMax + 1);
                a = b * quotient;
                answer = quotient;
                expr = a + " ÷ " + b;
            }
            default -> {
                a = rng.nextInt(1, rangeMax + 1);
                b = rng.nextInt(1, rangeMax + 1);
                answer = (long) a + b;
                expr = a + " + " + b;
            }
        }
        return build(expr, answer, difficulty, type);
    }

    private QuizQuestion generateMixed(Difficulty difficulty, ThreadLocalRandom rng) {
        int rangeMax = switch (difficulty) {
            case EASY -> 10;
            case NORMAL -> 20;
            case HARD -> 40;
        };
        int a = rng.nextInt(1, rangeMax + 1);
        int b = rng.nextInt(1, rangeMax + 1);
        int c = rng.nextInt(1, rangeMax + 1);
        boolean addThenSub = rng.nextBoolean();
        long answer;
        String expr;
        if (addThenSub) {
            answer = (long) a + b - c;
            expr = a + " + " + b + " - " + c;
        } else {
            answer = (long) a - b + c;
            if (a < b) {
                int t = a; a = b; b = t;
                answer = (long) a - b + c;
                expr = a + " - " + b + " + " + c;
            } else {
                expr = a + " - " + b + " + " + c;
            }
        }
        return build(expr, answer, difficulty, QuestionType.MIXED);
    }

    private QuizQuestion build(String expr, long answer, Difficulty difficulty, QuestionType type) {
        int limit = switch (difficulty) {
            case EASY -> easyTime;
            case NORMAL -> normalTime;
            case HARD -> hardTime;
        };
        String id = UUID.randomUUID().toString();
        String display = expr + " = ?";
        return new QuizQuestion(id, expr, display, answer, difficulty, type, limit);
    }
}
