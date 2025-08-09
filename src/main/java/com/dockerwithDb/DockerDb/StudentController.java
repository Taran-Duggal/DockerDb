package com.dockerwithDb.DockerDb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StudentController {

    @Autowired
    private StudentRepo studentRepo;

    @RequestMapping("/getStudents")
    public List<Student> getStudents(){
        return studentRepo.findAll();
    }

    @PostMapping("/addStudent")
    public Student addStudent(@RequestBody Student student){
        return studentRepo.save(student);
    }
}
