package com.lms.model;

public class QuizQuestion {
    private long id;
    private long quizId;
    private String questionText;
    private String optionA, optionB, optionC, optionD;
    private char correctOption; // 'A'..'D'
    private int marks;
    private int position;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getQuizId() { return quizId; }
    public void setQuizId(long quizId) { this.quizId = quizId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public char getCorrectOption() { return correctOption; }
    public void setCorrectOption(char correctOption) { this.correctOption = correctOption; }
    public int getMarks() { return marks; }
    public void setMarks(int marks) { this.marks = marks; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String optionText(char option) {
        return switch (option) {
            case 'A' -> optionA;
            case 'B' -> optionB;
            case 'C' -> optionC;
            case 'D' -> optionD;
            default -> "";
        };
    }
}
