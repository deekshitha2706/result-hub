package dao;

import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (roll_number, first_name, last_name, email, phone, date_of_birth, gender, address) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, student.getRollNumber());
            pstmt.setString(2, student.getFirstName());
            pstmt.setString(3, student.getLastName());
            pstmt.setString(4, student.getEmail());
            pstmt.setString(5, student.getPhone());
            pstmt.setString(6, student.getDateOfBirth().toString());
            pstmt.setString(7, student.getGender());
            pstmt.setString(8, student.getAddress());
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
        @SuppressWarnings("CallToPrintStackTrace")
    public Student getStudentById(int studentId) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        Student student = null;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                student = new Student();
                student.setStudentId(rs.getInt("student_id"));
                student.setRollNumber(rs.getString("roll_number"));
                student.setFirstName(rs.getString("first_name"));
                student.setLastName(rs.getString("last_name"));
                student.setEmail(rs.getString("email"));
                student.setPhone(rs.getString("phone"));
                student.setDateOfBirth(java.time.LocalDate.parse(rs.getString("date_of_birth")));
                student.setGender(rs.getString("gender"));
                student.setAddress(rs.getString("address"));
                student.setEnrollmentDate(java.time.LocalDate.parse(rs.getString("enrollment_date")));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return student;
    }
    @SuppressWarnings("CallToPrintStackTrace")
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY roll_number";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Student student = new Student();
                student.setStudentId(rs.getInt("student_id"));
                student.setRollNumber(rs.getString("roll_number"));
                student.setFirstName(rs.getString("first_name"));
                student.setLastName(rs.getString("last_name"));
                student.setEmail(rs.getString("email"));
                student.setPhone(rs.getString("phone"));
                student.setDateOfBirth(java.time.LocalDate.parse(rs.getString("date_of_birth")));
                student.setGender(rs.getString("gender"));
                student.setAddress(rs.getString("address"));
                student.setEnrollmentDate(java.time.LocalDate.parse(rs.getString("enrollment_date")));
                
                students.add(student);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return students;
    }
        @SuppressWarnings("CallToPrintStackTrace")
    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET first_name=?, last_name=?, email=?, phone=?, date_of_birth=?, gender=?, address=? WHERE student_id=?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, student.getFirstName());
            pstmt.setString(2, student.getLastName());
            pstmt.setString(3, student.getEmail());
            pstmt.setString(4, student.getPhone());
            pstmt.setString(5, student.getDateOfBirth().toString());
            pstmt.setString(6, student.getGender());
            pstmt.setString(7, student.getAddress());
            pstmt.setInt(8, student.getStudentId());
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean deleteStudent(int studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}