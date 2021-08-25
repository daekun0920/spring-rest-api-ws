package me.daekun.demoinflearnrestapi.common;

import me.daekun.demoinflearnrestapi.index.IndexController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.validation.Errors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class ErrorsResource extends EntityModel<Errors> {
    public static EntityModel<Errors> modelOf(Errors errors) {
        EntityModel<Errors> errorsModel = EntityModel.of(errors);
        // index 링크 정보 추가
        errorsModel.add(linkTo(methodOn(IndexController.class).index()).withRel("index"));
        return errorsModel;
    }
}
