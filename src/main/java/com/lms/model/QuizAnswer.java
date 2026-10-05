package com.lms.model;

public class QuizAnswer {
    private long id;
    private long attemptId;
    private long questionId;
    private Character selectedOption;
    private boolean correct;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getAttemptId() { return attemptId; }
    public void setAttemptId(long attemptId) { this.attemptId = attemptId; }
    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }
    public Character getSelectedOption() { return selectedOption; }
    public void setSelectedOption(Character selectedOption) { this.selectedOption = selectedOption; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
}
