package model;

import java.time.LocalDate;

public class Result {
    private int resultId;
    private int studentId;
    private int courseId;
    private double marksObtained;
    private double totalMarks;
    private String grade;
    private LocalDate examDate;
    private int semester;
    private String academicYear;
    private String studentName;
    private String studentRollNumber;
    private String courseName;
    
    public Result() {
    }
    
    public Result(int studentId, int courseId, double marksObtained, double totalMarks, 
                  String grade, LocalDate examDate, int semester, String academicYear) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.marksObtained = marksObtained;
        this.totalMarks = totalMarks;
        this.grade = grade;
        this.examDate = examDate;
        this.semester = semester;
        this.academicYear = academicYear;
    }
    
    // Getters and Setters
    public int getResultId() { return resultId; }
    public void setResultId(int resultId) { this.resultId = resultId; }
    
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    
    public double getMarksObtained() { return marksObtained; }
    public void setMarksObtained(double marksObtained) { this.marksObtained = marksObtained; }
    
    public double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(double totalMarks) { this.totalMarks = totalMarks; }
    
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
    
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentRollNumber() { return studentRollNumber; }
    public void setStudentRollNumber(String studentRollNumber) { this.studentRollNumber = studentRollNumber; }
    
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    
    public double getPercentage() {
        return (marksObtained / totalMarks) * 100;
    }
    
    @Override
    public String toString() {
        return "Result{" +
                "resultId=" + resultId +
                ", studentName='" + studentName + '\'' +
                ", courseName='" + courseName + '\'' +
                ", marksObtained=" + marksObtained +
                ", totalMarks=" + totalMarks +
                ", grade='" + grade + '\'' +
                ", percentage=" + String.format("%.2f", getPercentage()) + "%" +
                '}';
    }
}