package com.url.shortener.controller;

import com.url.shortener.models.UrlMapping;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.service.UrlMappingService;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
//@RequestMapping("api/urls")
public class RedirectController {

    @Autowired
    private UrlMappingService urlMappingService;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> redirect(@PathVariable String shortUrl) {
        try {
            UrlMapping mapping = urlMappingService.getOriginalUrl(shortUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(mapping.getOriginalUrl()));
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("expired")) {
                return ResponseEntity.status(HttpStatus.GONE) // <-- 410
                        .body(Map.of("error", "URL has expired"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND) // <-- 404
                        .body(Map.of("error", "Short URL not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected server error"));
        }
    }




//    public UrlMapping getOriginalUrl(String shortUrl){
//        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
//        if(urlMapping!=null){
//            urlMapping.setClickCount(urlMapping.getClickCount()+1);
//            urlMappingRepository.save(urlMapping);
//
//            //Record click event
//            ClickEvent clickEvent = new ClickEvent();
//            clickEvent.setClickDate(LocalDateTime.now());
//            clickEvent.setUrlMapping(urlMapping);
//            clickEventRepository.save(clickEvent);
//        }
//        return urlMapping;
//   }
}
