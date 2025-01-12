package com.ead.course.controllers;


import com.ead.course.dtos.ModuleDto;
import com.ead.course.models.CourseModel;
import com.ead.course.models.ModuleModel;
import com.ead.course.services.CourseService;
import com.ead.course.services.ModuleService;
import com.ead.course.specifications.SpecificationTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class ModuleController {

    @Autowired
    ModuleService moduleService;

    @Autowired
    CourseService courseService;

    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    @PostMapping("/courses/{courseId}/modules")
    @Operation(summary = "Salvar módulo", description = "Cria um novo módulo para o curso especificado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Módulo criado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Curso não encontrado")
    })
    public ResponseEntity<Object> saveModule(@PathVariable(value="courseId") UUID courseId,
                                             @RequestBody @Valid ModuleDto moduleDto){
        log.debug("POST saveModule moduleDto received {} ", moduleDto.toString());
        Optional<CourseModel> courseModelOptional = courseService.findById(courseId);
        if(!courseModelOptional.isPresent()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course Not Found.");
        }
        var moduleModel = new ModuleModel();
        BeanUtils.copyProperties(moduleDto, moduleModel);
        moduleModel.setCreationDate(LocalDateTime.now(ZoneId.of("UTC")));
        moduleModel.setCourse(courseModelOptional.get());
        moduleService.save(moduleModel);
        log.debug("POST saveModule moduleId saved {} ", moduleModel.getModuleId());
        log.info("Module saved successfully moduleId {} ", moduleModel.getModuleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(moduleModel);
    }

    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    @DeleteMapping("/courses/{courseId}/modules/{moduleId}")
    @Operation(summary = "Deletar módulo", description = "Deleta o módulo especificado do curso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Módulo deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Módulo não encontrado para este curso")
    })
    public ResponseEntity<Object> deleteModule(@PathVariable(value="courseId") UUID courseId,
                                               @PathVariable(value="moduleId") UUID moduleId){
        log.debug("DELETE deleteModule moduleId received {} ", moduleId);
        Optional<ModuleModel> moduleModelOptional = moduleService.findModuleIntoCourse(courseId, moduleId);
        if(!moduleModelOptional.isPresent()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Module not found for this course.");
        }
        moduleService.delete(moduleModelOptional.get());
        log.debug("DELETE deleteModule moduleId deleted {} ", moduleId);
        log.info("Module deleted successfully moduleId {} ", moduleId);
        return ResponseEntity.status(HttpStatus.OK).body("Module deleted successfully.");
    }


    @PreAuthorize("hasAnyRole('INSTRUCTOR')")
    @PutMapping("/courses/{courseId}/modules/{moduleId}")
    @Operation(summary = "Atualizar módulo", description = "Atualiza os dados do módulo especificado no curso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Módulo atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Módulo não encontrado para este curso")
    })
    public ResponseEntity<Object> updateModule(@PathVariable(value="courseId") UUID courseId,
                                               @PathVariable(value="moduleId") UUID moduleId,
                                               @RequestBody @Valid ModuleDto moduleDto){
        log.debug("PUT updateModule moduleDto received {} ", moduleDto.toString());
        Optional<ModuleModel> moduleModelOptional = moduleService.findModuleIntoCourse(courseId, moduleId);
        if(!moduleModelOptional.isPresent()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Module not found for this course.");
        }
        var moduleModel = moduleModelOptional.get();
        moduleModel.setTitle(moduleDto.getTitle());
        moduleModel.setDescription(moduleDto.getDescription());
        moduleService.save(moduleModel);
        log.debug("PUT updateModule moduleId saved {} ", moduleModel.getModuleId());
        log.info("Module updated successfully moduleId {} ", moduleModel.getModuleId());
        return ResponseEntity.status(HttpStatus.OK).body(moduleModel);
    }



    @PreAuthorize("hasAnyRole('STUDENT')")
    @GetMapping("/courses/{courseId}/modules")
    @Operation(summary = "Listar módulos", description = "Lista todos os módulos de um curso com opções de filtragem e paginação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Módulos listados com sucesso")
    })
    public ResponseEntity<Page<ModuleModel>> getAllModules(@PathVariable(value="courseId") UUID courseId,
                                                           SpecificationTemplate.ModuleSpec spec,
                                                           @PageableDefault(page = 0, size = 10, sort = "moduleId", direction = Sort.Direction.ASC) Pageable pageable){
        return ResponseEntity.status(HttpStatus.OK).body(moduleService.findAllByCourse(SpecificationTemplate.moduleCourseId(courseId).and(spec), pageable));
    }

    @PreAuthorize("hasAnyRole('STUDENT')")
    @GetMapping("/courses/{courseId}/modules/{moduleId}")
    @Operation(summary = "Obter módulo", description = "Recupera os detalhes de um módulo específico de um curso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Módulo recuperado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Módulo não encontrado para este curso")
    })
    public ResponseEntity<Object> getOneModule(@PathVariable(value="courseId") UUID courseId,
                                               @PathVariable(value="moduleId") UUID moduleId){
        Optional<ModuleModel> moduleModelOptional = moduleService.findModuleIntoCourse(courseId, moduleId);
        if(!moduleModelOptional.isPresent()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Module not found for this course.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(moduleModelOptional.get());
    }

}
