package com.url.shortener.service;

import com.url.shortener.dtos.ClickEventDTO;
import com.url.shortener.dtos.UrlMappingDTO;
import com.url.shortener.exceptions.ShortUrlTooLongException;
import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.models.User;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ✅ Use this instead of jakarta.transaction

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UrlMappingService {

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    public UrlMappingDTO createShortUrl(String originalUrl, User user) {
        if (!isValidURL(originalUrl)) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        String shortUrl;
        do {
            shortUrl = generateShortUrl();
        } while (urlMappingRepository.existsByShortUrl(shortUrl));

        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl(originalUrl)
                .shortUrl(shortUrl)
                .user(user)
                .clickCount(0)
                .createdDate(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusMinutes(5)) // ✅ 5 minutes expiry
                .build();

        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);
        return convertToDto(savedUrlMapping);
    }

    private boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private UrlMappingDTO convertToDto(UrlMapping urlMapping) {
        return UrlMappingDTO.builder()
                .id(urlMapping.getId())
                .originalUrl(urlMapping.getOriginalUrl())
                .shortUrl(urlMapping.getShortUrl())
                .clickCount(urlMapping.getClickCount())
                .createdDate(urlMapping.getCreatedDate())
                .username(urlMapping.getUser().getUsername())
                .build();
    }

    private String generateShortUrl() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder shortUrl = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            shortUrl.append(characters.charAt(random.nextInt(characters.length())));
        }
        return shortUrl.toString();
    }
    
    private boolean isExpired(UrlMapping urlMapping) {
        return urlMapping.getExpiryDate() != null &&
               LocalDateTime.now().isAfter(urlMapping.getExpiryDate());
    }

    
    public UrlMapping getOriginalUrl(String shortUrl) {
        System.out.println("short url getOriginalUrl: " + shortUrl);

        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (urlMapping == null) {
            System.out.println("Short URL not found.");
            throw new RuntimeException("Short URL not found.");
        }

        if (isExpired(urlMapping)) {
            System.out.println("URL is expired.");
            throw new RuntimeException("This URL has expired.");
        }

        System.out.println("Found URL mapping: " + urlMapping.getOriginalUrl());

        urlMapping.setClickCount(urlMapping.getClickCount() + 1);
        urlMappingRepository.save(urlMapping);

        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setClickDate(LocalDateTime.now());
        clickEvent.setUrlMapping(urlMapping);
        clickEventRepository.save(clickEvent);

        return urlMapping;
    }


    public List<UrlMappingDTO> getUrlsByUser(User user) {
        return urlMappingRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Transactional
    public boolean deleteShortUrl(String shortUrl, User user) {
        Optional<UrlMapping> urlMappingOpt = urlMappingRepository.findByShortUrlAndUser(shortUrl, user);
        if (urlMappingOpt.isPresent()) {
            UrlMapping urlMapping = urlMappingOpt.get();
            clickEventRepository.deleteByUrlMapping(urlMapping); // delete related click events
            urlMappingRepository.delete(urlMapping); // delete the url mapping
            return true;
        }
        return false;
    }

    public List<ClickEventDTO> getClickEventsByDate(String shortUrl, LocalDateTime start, LocalDateTime end) {
    UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
    if (urlMapping != null) {
        return clickEventRepository.findByUrlMappingAndClickDateBetween(urlMapping, start, end.plusHours(1))
                .stream()
                .collect(Collectors.groupingBy(
                        click -> click.getClickDate().toLocalDate(),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map((Map.Entry<LocalDate, Long> entry) -> ClickEventDTO.builder()
                        .clickDate(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList()); // Use collect(Collectors.toList()) instead of .toList() for compatibility
    }
    return Collections.emptyList();
}

    public Map<LocalDate, Long> getTotalClicksByUserAndDate(User user, LocalDate start, LocalDate end) {
        List<UrlMapping> urlMappings = urlMappingRepository.findByUser(user);
        List<ClickEvent> clickEvents = clickEventRepository
                .findByUrlMappingsAndClickDateRange(urlMappings, start.atStartOfDay(), end.plusDays(1).atStartOfDay());

        return clickEvents.stream()
                .collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(), Collectors.counting()));
    }

//    public UrlMapping getOriginalUrl(String shortUrl) {
//        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
//        if (urlMapping != null) {
//            urlMapping.setClickCount(urlMapping.getClickCount() + 1);
//            urlMappingRepository.save(urlMapping);
//
//            ClickEvent clickEvent = new ClickEvent();
//            clickEvent.setClickDate(LocalDateTime.now());
//            clickEvent.setUrlMapping(urlMapping);
//            clickEventRepository.save(clickEvent);
//        }
//        return urlMapping;
//    }

    public Map<LocalDateTime, Long> getTotalClicksByUserAndDateTime(User user, LocalDateTime start, LocalDateTime end) {
        List<UrlMapping> urlMappings = urlMappingRepository.findByUser(user);
        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMappingsAndClickDateRange(urlMappings, start, end);

        return clickEvents.stream()
                .collect(Collectors.groupingBy(ClickEvent::getClickDate, Collectors.counting()));
    }

    public UrlMappingDTO updateShortUrl(String shortUrl, String newShortUrl, User user) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (!urlMapping.getUser().getUsername().equals(user.getUsername())) {
            throw new RuntimeException("User is not allowed");
        }

        if (urlMappingRepository.existsByShortUrl(newShortUrl)) {
            throw new RuntimeException("This URL is already taken, please try another one!");
        }

        if (newShortUrl.length() > 15) {
            throw new ShortUrlTooLongException("The custom URL should be under 15 characters");
        }

        urlMapping.setShortUrl(newShortUrl);
        UrlMapping updatedMapping = urlMappingRepository.save(urlMapping);

        return convertToDto(updatedMapping);
    }
}
