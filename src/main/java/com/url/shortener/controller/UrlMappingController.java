package com.url.shortener.controller;

import com.url.shortener.dtos.ClickEventDTO;
import com.url.shortener.dtos.UrlMappingDTO;
import com.url.shortener.exceptions.ShortUrlTooLongException;
import com.url.shortener.models.User;
import com.url.shortener.service.UrlMappingService;
import com.url.shortener.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/urls")
public class UrlMappingController {

    //Long Url --> short url(8 character)

    @Autowired
    private UrlMappingService urlMappingService;

    @Autowired
    private UserService userService;

    //{"":""} {"originalUrl" : "https:example.com"}
    // https://abc.com/QN7X0a0a --> https://example.com

    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createShortUrl(@RequestBody Map<String, String> request,
                                            Principal principal) {
        try {
            String originalUrl = request.get("originalUrl");
            User user = userService.findByUsername(principal.getName());
            UrlMappingDTO urlMappingDTO = urlMappingService.createShortUrl(originalUrl, user);
            return ResponseEntity.ok(urlMappingDTO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
    
    @GetMapping("/redirect/{shortUrl}")
    public ResponseEntity<?> redirect(@PathVariable String shortUrl) {
        try {
            String originalUrl = urlMappingService.getOriginalUrl(shortUrl).getOriginalUrl();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(originalUrl))
                    .build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/delete-url-mapping/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteShortUrl(@PathVariable String shortUrl, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        boolean deleted = urlMappingService.deleteShortUrl(shortUrl, user);

        if (deleted) {
            return ResponseEntity.ok("Short URL deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Short URL not found or unauthorized.");
        }
    }

    @GetMapping("/myUrls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UrlMappingDTO>> getUserUrls(Principal principal){
        //getting the user with help of security context
        User user = userService.findByUsername(principal.getName());
        //getting urls for a particular user
        List<UrlMappingDTO> urls = urlMappingService.getUrlsByUser(user);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/analytics/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClickEventDTO>> getUrlAnalytics(@PathVariable String shortUrl,
                                                               @RequestParam("startDate") String startDate,
                                                               @RequestParam("endDate") String endDate) throws Throwable {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        // 2025-12-01T00:00:00
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);
        List<ClickEventDTO> clickEventDTOS = urlMappingService.getClickEventsByDate(shortUrl, start, end);
        return ResponseEntity.ok(clickEventDTOS);
    }

    @GetMapping("/totalClicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<LocalDate, Long>> getTotalClicksByDate(Principal principal,
                                                                     @RequestParam("startDate") String startDate,
                                                                     @RequestParam("endDate") String endDate){
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        User user = userService.findByUsername(principal.getName());
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        Map<LocalDate, Long> totalClicks = urlMappingService.getTotalClicksByUserAndDate(user, start, end);
        return ResponseEntity.ok(totalClicks);

    }

    @GetMapping("/totalclicksByDateTime")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<LocalDateTime, Long>> getTotalClicksByDateTime(
            Principal principal,
            @RequestParam("startDateTime") String startDateTime,
            @RequestParam("endDateTime") String endDateTime) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        User user = userService.findByUsername(principal.getName());
        LocalDateTime start = LocalDateTime.parse(startDateTime, formatter);
        LocalDateTime end = LocalDateTime.parse(endDateTime, formatter);
        Map<LocalDateTime, Long> clickData = urlMappingService.getTotalClicksByUserAndDateTime(user, start, end);
        return ResponseEntity.ok(clickData);
    }

    //New feature
    @PutMapping("/customize-shortUrl/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingDTO> updateShortUrl(
            @PathVariable String shortUrl,
            @RequestBody Map<String, String> request,
            Principal principal) {

        String newShortUrl = request.get("newShortUrl");
        User user = userService.findByUsername(principal.getName());

        try{
            UrlMappingDTO updatedMapping = urlMappingService.updateShortUrl(shortUrl, newShortUrl, user);
            return ResponseEntity.ok(updatedMapping);
        } catch (ShortUrlTooLongException ex){
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body((UrlMappingDTO) Collections.singletonMap("error", "Custom short URL must not exceed 15 characters."));

        } catch (RuntimeException ex){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body((UrlMappingDTO) Collections.singletonMap("error", ex.getMessage()));
        }
    }



}
