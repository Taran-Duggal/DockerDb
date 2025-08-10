package com.dockerwithDb.DockerDb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private StudentRepo studentRepo;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Student API is running successfully");
        response.put("timestamp", new Date());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    // Additional test endpoint
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API Test Successful - Student Service is Working!");
    }

    // Get all students endpoint (mock data)
    @GetMapping("/students")
    public ResponseEntity<List<Map<String, Object>>> getAllStudents() {
        List<Map<String, Object>> students = new ArrayList<>();

        Map<String, Object> student1 = new HashMap<>();
        student1.put("id", 1);
        student1.put("name", "John Doe");
        student1.put("email", "john.doe@example.com");
        student1.put("course", "Computer Science");

        Map<String, Object> student2 = new HashMap<>();
        student2.put("id", 2);
        student2.put("name", "Jane Smith");
        student2.put("email", "jane.smith@example.com");
        student2.put("course", "Information Technology");

        students.add(student1);
        students.add(student2);

        return ResponseEntity.ok(students);
    }

    @PostMapping("/students")
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody Map<String, String> studentData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Student created successfully");
        response.put("student", studentData);
        response.put("id", new Random().nextInt(1000) + 100); // Mock ID generation
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> getStudentById(@PathVariable int id) {
        Map<String, Object> student = new HashMap<>();
        student.put("id", id);
        student.put("name", "Student " + id);
        student.put("email", "student" + id + "@example.com");
        student.put("course", "Sample Course");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("student", student);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/error-test")
    public ResponseEntity<Map<String, String>> errorTest() {
        throw new RuntimeException("This is a test error");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "ERROR");
        error.put("message", e.getMessage());
        error.put("timestamp", new Date().toString());
        return ResponseEntity.badRequest().body(error);
    }

    @RequestMapping("/getStudents")
    public List<Student> getStudents(){
        return studentRepo.findAll();
    }

    @PostMapping("/addStudent")
    public Student addStudent(@RequestBody Student student){
        return studentRepo.save(student);
    }
}
