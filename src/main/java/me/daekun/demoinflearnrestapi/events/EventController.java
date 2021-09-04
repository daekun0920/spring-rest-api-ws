package me.daekun.demoinflearnrestapi.events;

import me.daekun.demoinflearnrestapi.accounts.Account;
import me.daekun.demoinflearnrestapi.accounts.AccountAdapter;
import me.daekun.demoinflearnrestapi.accounts.AccountRole;
import me.daekun.demoinflearnrestapi.accounts.CurrentUser;
import me.daekun.demoinflearnrestapi.common.ErrorsResource;
import org.apache.coyote.Response;
import org.modelmapper.ModelMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Controller
@RequestMapping(value = "/api/events", produces = MediaTypes.HAL_JSON_VALUE)
public class EventController {

    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;
    private final EventValidator eventValidator;

    public EventController(EventRepository eventRepository, ModelMapper modelMapper, EventValidator eventValidator) {
        this.eventRepository = eventRepository;
        this.modelMapper = modelMapper;
        this.eventValidator = eventValidator;
    }

    @PostMapping
    public ResponseEntity createEvent(@RequestBody @Valid EventDto eventDto, Errors errors, @CurrentUser Account currentUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (errors.hasErrors()) {
            return getErrorsResourceResponseEntity(errors);
        }

        eventValidator.validate(eventDto, errors);

        if (errors.hasErrors()) {
            return getErrorsResourceResponseEntity(errors);
        }

        Event event = modelMapper.map(eventDto, Event.class);
        event.update();
        event.setManager(currentUser);
        Event newEvent = this.eventRepository.save(event);
        LinkBuilder selftLinkBuilder = linkTo(EventController.class).slash(newEvent.getId());
        URI createdUri = selftLinkBuilder.toUri();
        EventResource eventResource = new EventResource(event);
        eventResource.add(linkTo(EventController.class).withRel("query-events"));
        eventResource.add(selftLinkBuilder.withRel("update-event"));
        eventResource.add(new Link("/docs/index.html#resources-events-create").withRel("profile"));
        return ResponseEntity.created(createdUri).body(eventResource);
    }

    @GetMapping
    public ResponseEntity queryEvents(Pageable pageable,
                                      PagedResourcesAssembler<Event> assembler,
                                      @CurrentUser Account account) {
        Page<Event> page = this.eventRepository.findAll(pageable);
        var pagedModels = assembler.toModel(page, e -> new EventResource(e));
        pagedModels.add(new Link("/docs/index.html#resources-events-list").withRel("profile"));
        if (account != null) {
            pagedModels.add(linkTo(EventController.class).withRel("create-event"));
        }
        return ResponseEntity.ok(pagedModels);
    }

    @GetMapping("/{id}")
    public ResponseEntity getEvent(@PathVariable Integer id, @CurrentUser CurrentUser currentUser) {
        Optional<Event> optionalEvent = this.eventRepository.findById(id);
        if (optionalEvent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Event event = optionalEvent.get();
        EventResource eventResource = new EventResource(event);
        eventResource.add(new Link("/docs/index.html#resources-events-get").withRel("profile"));
        if (event.getManager().equals(currentUser)) {
            eventResource.add(linkTo(EventController.class).slash(event.getId()).withRel("update-event"));
        }
        return ResponseEntity.ok(eventResource);
    }

    @PutMapping("/{id}")
    public ResponseEntity updateEvent(@PathVariable Integer id, @RequestBody @Valid EventDto eventDto, Errors errors, @CurrentUser CurrentUser currentUser) {
        Optional<Event> optionalEvent = this.eventRepository.findById(id);
        if (optionalEvent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (errors.hasErrors()) {
            return getErrorsResourceResponseEntity(errors);
        }

        this.eventValidator.validate(eventDto, errors);
        if (errors.hasErrors()) {
            return getErrorsResourceResponseEntity(errors);
        }
        Account acc = new Account();

        Event existingEvent = optionalEvent.get();
        if (existingEvent.getManager().equals(currentUser)) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        this.modelMapper.map(eventDto, existingEvent);
        Event savedEvent = this.eventRepository.save(existingEvent);

        EventResource eventResource = new EventResource(savedEvent);
        eventResource.add(new Link("/docs/index.html#resources-events-update").withRel("profile"));

        return ResponseEntity.ok(eventResource);
    }

    private ResponseEntity getErrorsResourceResponseEntity(Errors errors) {
        return ResponseEntity.badRequest().body(ErrorsResource.modelOf(errors));
    }

}
