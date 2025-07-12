package com.url.shortener.repository;

import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickEventRepository extends MongoRepository<ClickEvent, String> {

    // Find ClickEvents for a URL within a time range
    List<ClickEvent> findByUrlMappingAndClickDateBetween(
            UrlMapping urlMapping,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Custom MongoDB query for multiple urlMappings
    @Query("{ 'urlMapping' : { $in: ?0 }, 'clickDate' : { $gte: ?1, $lte: ?2 } }")
    List<ClickEvent> findByUrlMappingsAndClickDateRange(
            List<UrlMapping> urlMappings,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Delete all click events linked to a specific URL mapping
    void deleteByUrlMapping(UrlMapping urlMapping);
}
