package com.ead.course.controllers;

import com.ead.course.dtos.SubscriptionDto;
import com.ead.course.enums.UserStatus;
import com.ead.course.models.CourseModel;
import com.ead.course.models.UserModel;
import com.ead.course.services.CourseService;
import com.ead.course.services.UserService;
import com.ead.course.specifications.SpecificationTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class CourseUserController {

    @Autowired
    CourseService courseService;

    @Autowired
    UserService userService;

    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    @GetMapping("/courses/{courseId}/users")
    @Operation(summary = "Obter todos os usuários matriculados em um curso",
            description = "Retorna uma lista paginada de usuários que estão matriculados no curso especificado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Curso não encontrado", content = @Content)
    })
    public ResponseEntity<Object> getAllUsersByCourse(SpecificationTemplate.UserSpec spec,
                                                      @PageableDefault(page = 0, size = 10, sort = "userId", direction = Sort.Direction.ASC) Pageable pageable,
                                                      @PathVariable(value = "courseId") UUID courseId){
        Optional<CourseModel> courseModelOptional = courseService.findById(courseId);
        if(!courseModelOptional.isPresent()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course Not Found.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userService.findAll(SpecificationTemplate.userCourseId(courseId).and(spec), pageable));
    }

    @PreAuthorize("hasAnyRole('STUDENT')")
    @PostMapping("/courses/{courseId}/users/subscription")
    @Operation(summary = "Inscrever um usuário em um curso",
            description = "Permite que um estudante se inscreva em um curso específico, caso atenda aos requisitos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inscrição criada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Curso ou usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito - Inscrição já existe ou usuário está bloqueado")
    })
    public  ResponseEntity<Object> saveSubscriptionUserInCourse(@PathVariable(value = "courseId") UUID courseId,
                                                                @RequestBody @Valid SubscriptionDto subscriptionDto){
        Optional<CourseModel> courseModelOptional = courseService.findById(courseId);
        if(!courseModelOptional.isPresent()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course Not Found.");
        }
        if(courseService.existsByCourseAndUser(courseId, subscriptionDto.getUserId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: subscription already exists!");
        }
        Optional<UserModel> userModelOptional = userService.findById(subscriptionDto.getUserId());
        if(!userModelOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        if(userModelOptional.get().getUserStatus().equals(UserStatus.BLOCKED.toString())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User is blocked.");
        }
        courseService.saveSubscriptionUserInCourseAndSendNotification(courseModelOptional.get(), userModelOptional.get());
        return ResponseEntity.status(HttpStatus.CREATED).body("Subscription created successfully.");
    }
}
