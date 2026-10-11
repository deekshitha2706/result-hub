package dao;

import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultDAO {
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean addResult(Result result) {
        String sql = "INSERT INTO results (student_id, course_id, marks_obtained, total_marks, grade, exam_date, semester, academic_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, result.getStudentId());
            pstmt.setInt(2, result.getCourseId());
            pstmt.setDouble(3, result.getMarksObtained());
            pstmt.setDouble(4, result.getTotalMarks());
            pstmt.setString(5, result.getGrade());
            pstmt.setString(6, result.getExamDate().toString());
            pstmt.setInt(7, result.getSemester());
            pstmt.setString(8, result.getAcademicYear());
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @SuppressWarnings("CallToPrintStackTrace")
    public List<Result> getResultsByStudentId(int studentId) {
        List<Result> results = new ArrayList<>();
        String sql = "SELECT r.*, s.first_name, s.last_name, c.course_name " +
                     "FROM results r " +
                     "JOIN students s ON r.student_id = s.student_id " +
                     "JOIN courses c ON r.course_id = c.course_id " +
                     "WHERE r.student_id = ? " +
                     "ORDER BY r.semester, r.exam_date";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Result result = new Result();
                result.setResultId(rs.getInt("result_id"));
                result.setStudentId(rs.getInt("student_id"));
                result.setCourseId(rs.getInt("course_id"));
                result.setMarksObtained(rs.getDouble("marks_obtained"));
                result.setTotalMarks(rs.getDouble("total_marks"));
                result.setGrade(rs.getString("grade"));
                result.setExamDate(java.time.LocalDate.parse(rs.getString("exam_date")));
                result.setSemester(rs.getInt("semester"));
                result.setAcademicYear(rs.getString("academic_year"));
                result.setStudentName(rs.getString("first_name") + " " + rs.getString("last_name"));
                result.setCourseName(rs.getString("course_name"));
                
                results.add(result);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return results;
    }
    @SuppressWarnings("CallToPrintStackTrace")
    public List<Result> getAllResults() {
        List<Result> results = new ArrayList<>();
        String sql = "SELECT r.*, s.first_name, s.last_name, s.roll_number, c.course_name " +
                     "FROM results r " +
                     "JOIN students s ON r.student_id = s.student_id " +
                     "JOIN courses c ON r.course_id = c.course_id " +
                     "ORDER BY r.academic_year DESC, r.semester, s.roll_number";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Result result = new Result();
                result.setResultId(rs.getInt("result_id"));
                result.setStudentId(rs.getInt("student_id"));
                result.setCourseId(rs.getInt("course_id"));
                result.setMarksObtained(rs.getDouble("marks_obtained"));
                result.setTotalMarks(rs.getDouble("total_marks"));
                result.setGrade(rs.getString("grade"));
                result.setExamDate(java.time.LocalDate.parse(rs.getString("exam_date")));
                result.setSemester(rs.getInt("semester"));
                result.setAcademicYear(rs.getString("academic_year"));
                result.setStudentName(rs.getString("first_name") + " " + rs.getString("last_name"));
                result.setCourseName(rs.getString("course_name"));
                
                results.add(result);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return results;
    }
    
    public static String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        else if (percentage >= 80) return "A";
        else if (percentage >= 70) return "B+";
        else if (percentage >= 60) return "B";
        else if (percentage >= 50) return "C";
        else if (percentage >= 40) return "D";
        else return "F";
    }
}