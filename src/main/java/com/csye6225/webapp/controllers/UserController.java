package com.csye6225.webapp.controllers;

import com.csye6225.webapp.entity.ImageEntity;
import com.csye6225.webapp.entity.UserEntity;
import com.csye6225.webapp.models.ResponseWrapper;
import com.csye6225.webapp.models.User;
import com.csye6225.webapp.services.ImageService;
import com.csye6225.webapp.services.UserService;
import com.csye6225.webapp.models.ResponseMessage;
import com.csye6225.webapp.utils.JsonUtils;
import com.timgroup.statsd.StatsDClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping(path = "/v1/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService _userService;

    @Autowired
    private ImageService _imageService;

    @Autowired
    private StatsDClient _statsDClient;

    @PostMapping(path = "", produces = "application/json")
    public ResponseEntity<String> createUser(@RequestBody User user) {
        _statsDClient.incrementCounter("endpoint.user.api.post");
        long currentTime = Instant.now().toEpochMilli();
        UserEntity existingUser = _userService.getUserByEmail(user.getEmail());
        if (existingUser != null) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.post.failure.execution.time", currentTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(JsonUtils.toJson(new ResponseWrapper(HttpStatus.BAD_REQUEST.value(),
                            ResponseMessage.USER_ALREADY_EXISTS.getMessage())));
        }
        if (!this._userService.isEmailValid(user.getEmail())) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.post.failure.execution.time", currentTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(JsonUtils.toJson(new ResponseWrapper(HttpStatus.BAD_REQUEST.value(),
                            ResponseMessage.INVALID_EMAIL.getMessage())));
        }
        if (!this._userService.isPasswordValid(user.getPassword())) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.post.failure.execution.time", currentTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(JsonUtils.toJson(new ResponseWrapper(HttpStatus.BAD_REQUEST.value(),
                            ResponseMessage.INVALID_PASSWORD.getMessage())));
        }
        UserEntity newUser = _userService.createUser(user);
        log.info("Created New User");
        _statsDClient.recordExecutionTimeToNow("endpoint.user.api.post.success.execution.time", currentTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(JsonUtils.toJson(newUser));
    }

    @GetMapping(path = "/self")
    public ResponseEntity<UserEntity> getUser(HttpServletRequest request) {
        _statsDClient.incrementCounter("endpoint.user.self.api.get");
        long currentTime = Instant.now().toEpochMilli();
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.get.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserEntity user = _userService.validateUserByToken(authorization);
        if (user == null) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.get.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        _statsDClient.recordExecutionTimeToNow("endpoint.user.api.get.success.execution.time", currentTime);
        return ResponseEntity.ok(user);
    }

    @PutMapping(path = "/self")
    public ResponseEntity<?> updateUser(@RequestBody User user, HttpServletRequest request) {
        _statsDClient.incrementCounter("endpoint.user.self.api.put");
        long currentTime = Instant.now().toEpochMilli();
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.put.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserEntity userEntity = _userService.validateUserByToken(authorization);
        if (userEntity == null) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.put.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!userEntity.getEmail().equals(user.getEmail())) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.put.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!this._userService.isPasswordValid(user.getPassword())) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.api.put.failure.execution.time", currentTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(JsonUtils.toJson(new ResponseWrapper(HttpStatus.BAD_REQUEST.value(),
                            ResponseMessage.INVALID_PASSWORD.getMessage())));
        }

        _statsDClient.recordExecutionTimeToNow("endpoint.user.api.put.success.execution.time", currentTime);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(_userService.updateUser(user));
    }

    @PostMapping(path = "/self/pic", produces = "application/json")
    public ResponseEntity<ImageEntity> addProfilePic(@RequestParam(value="profilePic") MultipartFile image,
                                                     HttpServletRequest request) {
        _statsDClient.incrementCounter("endpoint.user.self.pic.api.post");
        long currentTime = Instant.now().toEpochMilli();
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.post.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserEntity userEntity = _userService.validateUserByToken(authorization);
        if (userEntity == null) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.post.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!_imageService.isImageValid(image)) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.post.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.post.success.execution.time", currentTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(_imageService.addImage(image, userEntity.getId()));
    }

    @GetMapping(path = "/self/pic", produces = "application/json")
    public ResponseEntity<ImageEntity> getProfilePic(HttpServletRequest request) {
        _statsDClient.incrementCounter("endpoint.user.self.pic.api.get");
        long currentTime = Instant.now().toEpochMilli();
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.get.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserEntity userEntity = _userService.validateUserByToken(authorization);
        if (userEntity == null) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.get.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<ImageEntity> imageEntityOptional = _imageService.getImage(userEntity.getId());

        if (imageEntityOptional.isEmpty()) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.get.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.get.success.execution.time", currentTime);
        return ResponseEntity.status(HttpStatus.OK).body(imageEntityOptional.get());
    }

    @DeleteMapping(path = "/self/pic")
    public ResponseEntity<Void> addProfilePic(HttpServletRequest request) {
        _statsDClient.incrementCounter("endpoint.user.self.pic.api.delete");
        long currentTime = Instant.now().toEpochMilli();
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.delete.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserEntity userEntity = _userService.validateUserByToken(authorization);
        if (userEntity == null) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.delete.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<ImageEntity> imageEntityOptional = _imageService.getImage(userEntity.getId());

        if (imageEntityOptional.isEmpty()) {
            _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.delete.failure.execution.time", currentTime);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        _imageService.deleteFile(imageEntityOptional.get());
        _statsDClient.recordExecutionTimeToNow("endpoint.user.self.pic.api.delete.success.execution.time", currentTime);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(value = "/self", method = {RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Void> methodNotAllowedSelf() {
        _statsDClient.incrementCounter("endpoint.user.self.api.rest");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @RequestMapping(value = "", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Void> methodNotAllowed() {
        _statsDClient.incrementCounter("endpoint.user.api.rest");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @RequestMapping(value = "/self/pic", method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Void> methodNotAllowedSelfPic() {
        _statsDClient.incrementCounter("endpoint.user.self.pic.api.rest");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
